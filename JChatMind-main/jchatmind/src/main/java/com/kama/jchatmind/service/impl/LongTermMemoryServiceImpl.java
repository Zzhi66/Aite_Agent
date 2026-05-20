package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.mapper.LongTermMemoryMapper;
import com.kama.jchatmind.model.entity.LongTermMemory;
import com.kama.jchatmind.security.UserContext;
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

    @Value("${jchatmind.memory.long-term.recall.scope:USER}")
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
     * 按当前登录用户隔离；agentId/sessionId 仅作来源溯源，可空。
     */
    public void ingestFromUserInput(String agentId, String sessionId, String userInput) {
        if (!enabled || !StringUtils.hasText(userInput)) {
            return;
        }
        String userId = resolveUserIdOrNull();
        if (userId == null) {
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
                        .userId(userId)
                        .agentId(StringUtils.hasText(agentId) ? agentId : null)
                        .sessionId(StringUtils.hasText(sessionId) ? sessionId : null)
                        .memoryType(candidate.type().getCode())
                        .content(candidate.content())
                        .metadata(toIngestMetadata(candidate, userInput, agentId, sessionId))
                        .embedding(toPgVector(embedding))
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                longTermMemoryMapper.insert(memory);
                inserted++;
            } catch (Exception e) {
                log.warn("Failed to ingest long-term memory, userId={}, agentId={}, sessionId={}, content={}",
                        userId, agentId, sessionId, candidate.content(), e);
            }
        }
    }

    @Override
    /**
     * 根据当前查询召回可用于提示词增强的长期记忆。
     * 检索范围为用户全量记忆池，不再按 agent/session 过滤。
     */
    public List<RecallMemory> recallForPrompt(String agentId, String sessionId, String query) {
        if (!enabled || !StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        String userId = resolveUserIdOrNull();
        if (userId == null) {
            return Collections.emptyList();
        }
        if (!isUserScope()) {
            log.debug("Long-term recall scope {} is deprecated, using USER scope", recallScope);
        }
        try {
            float[] queryEmbedding = ragService.embed(query);
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return Collections.emptyList();
            }
            String vectorLiteral = toPgVector(queryEmbedding);
            List<String> types = parseIncludeTypes();
            List<LongTermMemory> memories = longTermMemoryMapper.similaritySearchByUser(
                    userId, vectorLiteral, types, recallLimit);

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
            log.warn("Failed to recall long-term memories, userId={}, agentId={}, sessionId={}",
                    userId, agentId, sessionId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取当前用户 ID；未登录时返回 null（跳过 ingest/召回）。
     */
    private String resolveUserIdOrNull() {
        try {
            return UserContext.requireUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * 将原始输入按句段切分，并提取可入库的候选记忆。
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

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 组装 ingest 元数据，记录来源对话与原始输入。
     */
    private String toIngestMetadata(
            CandidateMemory candidate,
            String rawInput,
            String agentId,
            String sessionId
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "user_input");
        metadata.put("memoryType", candidate.type().getCode());
        metadata.put("rawInput", rawInput);
        if (StringUtils.hasText(agentId)) {
            metadata.put("sourceAgentId", agentId);
        }
        if (StringUtils.hasText(sessionId)) {
            metadata.put("sourceSessionId", sessionId);
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"source\":\"user_input\"}";
        }
    }

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

    /** 是否按用户维度召回（默认且唯一有效路径） */
    private boolean isUserScope() {
        return RecallScope.USER.getCode().equalsIgnoreCase(recallScope)
                || !RecallScope.AGENT.getCode().equalsIgnoreCase(recallScope)
                        && !RecallScope.SESSION.getCode().equalsIgnoreCase(recallScope);
    }

    private MemoryType parseMemoryType(String type) {
        if (MemoryType.FACT.getCode().equalsIgnoreCase(type)) {
            return MemoryType.FACT;
        }
        return MemoryType.PREFERENCE;
    }

    /**
     * 将向量数组转换为 pgvector 可识别的字符串字面量格式。
     */
    public static String toPgVector(float[] vector) {
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
