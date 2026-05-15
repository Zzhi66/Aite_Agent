package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.mapper.LongTermMemoryMapper;
import com.kama.jchatmind.model.entity.LongTermMemory;
import com.kama.jchatmind.service.LongTermMemoryService;
import com.kama.jchatmind.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LongTermMemoryServiceImpl implements LongTermMemoryService {

    private static final List<String> PREFERENCE_KEYWORDS = List.of(
            "我喜欢", "我更喜欢", "我偏好", "我习惯", "我常用", "我通常", "我不喜欢", "不要", "请叫我"
    );
    private static final List<String> FACT_KEYWORDS = List.of(
            "我是", "我的", "我在", "我住在", "我来自", "我的邮箱", "我的电话", "我叫"
    );

    private final LongTermMemoryMapper longTermMemoryMapper;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    @Value("${jchatmind.memory.long-term.enabled:true}")
    private boolean enabled;

    @Value("${jchatmind.memory.long-term.extract.max-items-per-message:3}")
    private int maxItemsPerMessage;

    @Value("${jchatmind.memory.long-term.extract.max-content-length:300}")
    private int maxContentLength;

    @Value("${jchatmind.memory.long-term.recall.limit:6}")
    private int recallLimit;

    @Value("${jchatmind.memory.long-term.recall.max-distance:1.1}")
    private double maxDistance;

    @Value("${jchatmind.memory.long-term.recall.scope:SESSION}")
    private String recallScope;

    @Value("${jchatmind.memory.long-term.recall.include-types:PREFERENCE,FACT}")
    private String includeTypes;

    /**
     * 构造长期记忆服务实现，注入存储、向量化与 JSON 序列化依赖。
     */
    public LongTermMemoryServiceImpl(
            LongTermMemoryMapper longTermMemoryMapper,
            RagService ragService,
            ObjectMapper objectMapper
    ) {
        this.longTermMemoryMapper = longTermMemoryMapper;
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * 从用户输入中提取候选记忆并写入长期记忆表。
     * 仅在功能开启且必要参数完整时生效，并受单条消息最大入库数量限制。
     */
    public void ingestFromUserInput(String agentId, String sessionId, String userInput) {
        if (!enabled || !StringUtils.hasText(agentId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(userInput)) {
            return;
        }
        List<CandidateMemory> candidates = extractCandidates(userInput);
        if (candidates.isEmpty()) {
            return;
        }

        int inserted = 0;
        for (CandidateMemory candidate : candidates) {
            if (inserted >= maxItemsPerMessage) {
                break;
            }
            try {
                float[] embedding = ragService.embed(candidate.content());
                if (embedding == null || embedding.length == 0) {
                    continue;
                }
                LocalDateTime now = LocalDateTime.now();
                LongTermMemory memory = LongTermMemory.builder()
                        .agentId(agentId)
                        .sessionId(sessionId)
                        .memoryType(candidate.type().getCode())
                        .content(candidate.content())
                        .metadata(toMetadata(candidate, userInput))
                        .embedding(toPgVector(embedding))
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                longTermMemoryMapper.insert(memory);
                inserted++;
            } catch (Exception e) {
                log.warn("Failed to ingest long-term memory, agentId={}, sessionId={}, content={}",
                        agentId, sessionId, candidate.content(), e);
            }
        }
    }

    @Override
    /**
     * 根据当前查询召回可用于提示词增强的长期记忆。
     * 通过向量相似度检索后，会做距离阈值过滤与内容去重。
     */
    public List<RecallMemory> recallForPrompt(String agentId, String sessionId, String query) {
        if (!enabled || !StringUtils.hasText(agentId) || !StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        if (!isAgentScope() && !StringUtils.hasText(sessionId)) {
            return Collections.emptyList();
        }
        try {
            float[] queryEmbedding = ragService.embed(query);
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return Collections.emptyList();
            }
            String vectorLiteral = toPgVector(queryEmbedding);
            List<String> types = parseIncludeTypes();
            List<LongTermMemory> memories = isAgentScope()
                    ? longTermMemoryMapper.similaritySearchByAgent(agentId, vectorLiteral, types, recallLimit)
                    : longTermMemoryMapper.similaritySearchBySession(agentId, sessionId, vectorLiteral, types, recallLimit);

            return memories.stream()
                    .filter(Objects::nonNull)
                    .filter(memory -> memory.getDistance() == null || memory.getDistance() <= maxDistance)
                    .filter(memory -> StringUtils.hasText(memory.getContent()))
                    .collect(Collectors.toMap(
                            LongTermMemory::getContent,
                            memory -> RecallMemory.builder()
                                    .type(parseMemoryType(memory.getMemoryType()))
                                    .content(memory.getContent())
                                    .distance(memory.getDistance())
                                    .build(),
                            (left, right) -> left,
                            LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to recall long-term memories, agentId={}, sessionId={}", agentId, sessionId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 将原始输入按句段切分，并提取可入库的候选记忆。
     * 对每个句段执行去空白、长度裁剪和类型判定。
     */
    private List<CandidateMemory> extractCandidates(String input) {
        String[] segments = input.split("[\\n。！？!?；;]");
        List<CandidateMemory> candidates = new ArrayList<>();
        for (String segment : segments) {
            String content = segment == null ? "" : segment.trim();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            if (content.length() > maxContentLength) {
                content = content.substring(0, maxContentLength);
            }
            MemoryType type = classify(content);
            if (type != null) {
                candidates.add(new CandidateMemory(type, content));
            }
        }
        return candidates;
    }

    /**
     * 根据关键词规则判定记忆类型（偏好或事实）。
     */
    private MemoryType classify(String content) {
        if (containsAny(content, PREFERENCE_KEYWORDS)) {
            return MemoryType.PREFERENCE;
        }
        if (containsAny(content, FACT_KEYWORDS)) {
            return MemoryType.FACT;
        }
        return null;
    }

    /**
     * 判断文本是否包含任一关键词。
     */
    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 组装并序列化记忆元数据，记录来源、类型与原始输入。
     */
    private String toMetadata(CandidateMemory candidate, String rawInput) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "user_input");
        metadata.put("memoryType", candidate.type().getCode());
        metadata.put("rawInput", rawInput);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"source\":\"user_input\"}";
        }
    }

    /**
     * 解析配置中的召回记忆类型列表；为空时使用默认类型集合。
     */
    private List<String> parseIncludeTypes() {
        if (!StringUtils.hasText(includeTypes)) {
            return List.of(MemoryType.PREFERENCE.getCode(), MemoryType.FACT.getCode());
        }
        return Arrays.stream(includeTypes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .toList();
    }

    /**
     * 判断召回范围是否为 AGENT 级别（跨会话）。
     */
    private boolean isAgentScope() {
        return RecallScope.AGENT.getCode().equalsIgnoreCase(recallScope);
    }

    /**
     * 将数据库中的类型编码转换为内部记忆类型枚举。
     */
    private MemoryType parseMemoryType(String type) {
        if (MemoryType.FACT.getCode().equalsIgnoreCase(type)) {
            return MemoryType.FACT;
        }
        return MemoryType.PREFERENCE;
    }

    /**
     * 将向量数组转换为 pgvector 可识别的字符串字面量格式。
     */
    private String toPgVector(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            builder.append(vector[i]);
            if (i < vector.length - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private record CandidateMemory(MemoryType type, String content) {
    }
}
