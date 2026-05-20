package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.mapper.LongTermMemoryMapper;
import com.kama.jchatmind.model.entity.LongTermMemory;
import com.kama.jchatmind.model.request.CreateLongTermMemoryRequest;
import com.kama.jchatmind.model.request.UpdateLongTermMemoryRequest;
import com.kama.jchatmind.model.response.CreateLongTermMemoryResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoriesResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoryResponse;
import com.kama.jchatmind.model.vo.LongTermMemoryVO;
import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.LongTermMemoryFacadeService;
import com.kama.jchatmind.service.LongTermMemoryService;
import com.kama.jchatmind.service.RagService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class LongTermMemoryFacadeServiceImpl implements LongTermMemoryFacadeService {

    private final LongTermMemoryMapper longTermMemoryMapper;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    @Override
    public GetLongTermMemoriesResponse listMemories(String memoryType) {
        String userId = UserContext.requireUserId();
        String normalizedType = normalizeMemoryTypeFilter(memoryType);
        List<LongTermMemory> rows = longTermMemoryMapper.selectByUserId(userId, normalizedType);
        List<LongTermMemoryVO> result = new ArrayList<>();
        for (LongTermMemory row : rows) {
            result.add(toVO(row));
        }
        return GetLongTermMemoriesResponse.builder()
                .memories(result.toArray(new LongTermMemoryVO[0]))
                .build();
    }

    @Override
    public GetLongTermMemoryResponse getMemory(String memoryId) {
        LongTermMemory memory = requireOwnedMemory(memoryId);
        return GetLongTermMemoryResponse.builder()
                .memory(toVO(memory))
                .build();
    }

    @Override
    public CreateLongTermMemoryResponse createMemory(CreateLongTermMemoryRequest request) {
        String memoryType = requireMemoryType(request.getMemoryType());
        if (!StringUtils.hasText(request.getContent())) {
            throw new BizException("记忆内容不能为空");
        }
        float[] embedding = embedOrThrow(request.getContent().trim());
        LocalDateTime now = LocalDateTime.now();
        LongTermMemory memory = LongTermMemory.builder()
                .userId(UserContext.requireUserId())
                .agentId(request.getSourceAgentId())
                .sessionId(request.getSourceSessionId())
                .memoryType(memoryType)
                .content(request.getContent().trim())
                .metadata(buildManualMetadata(request))
                .embedding(LongTermMemoryServiceImpl.toPgVector(embedding))
                .createdAt(now)
                .updatedAt(now)
                .build();
        int inserted = longTermMemoryMapper.insert(memory);
        if (inserted <= 0) {
            throw new BizException("创建记忆失败");
        }
        return CreateLongTermMemoryResponse.builder()
                .memoryId(memory.getId())
                .build();
    }

    @Override
    public void updateMemory(String memoryId, UpdateLongTermMemoryRequest request) {
        LongTermMemory existing = requireOwnedMemory(memoryId);
        boolean contentChanged = StringUtils.hasText(request.getContent())
                && !request.getContent().trim().equals(existing.getContent());
        boolean typeChanged = StringUtils.hasText(request.getMemoryType())
                && !requireMemoryType(request.getMemoryType()).equals(existing.getMemoryType());

        if (!contentChanged && !typeChanged) {
            return;
        }

        LongTermMemory updated = LongTermMemory.builder()
                .id(existing.getId())
                .memoryType(typeChanged ? requireMemoryType(request.getMemoryType()) : existing.getMemoryType())
                .content(contentChanged ? request.getContent().trim() : existing.getContent())
                .metadata(existing.getMetadata())
                .updatedAt(LocalDateTime.now())
                .build();

        if (contentChanged) {
            float[] embedding = embedOrThrow(updated.getContent());
            updated.setEmbedding(LongTermMemoryServiceImpl.toPgVector(embedding));
        }

        int rows = longTermMemoryMapper.updateById(updated);
        if (rows <= 0) {
            throw new BizException("更新记忆失败");
        }
    }

    @Override
    public void deleteMemory(String memoryId) {
        requireOwnedMemory(memoryId);
        int rows = longTermMemoryMapper.deleteById(memoryId);
        if (rows <= 0) {
            throw new BizException("删除记忆失败");
        }
    }

    /**
     * 校验记忆归属当前用户；user_id 为空的历史数据拒绝访问。
     */
    private LongTermMemory requireOwnedMemory(String memoryId) {
        LongTermMemory memory = longTermMemoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BizException("记忆不存在: " + memoryId);
        }
        if (!StringUtils.hasText(memory.getUserId())
                || !memory.getUserId().equals(UserContext.requireUserId())) {
            throw new BizException("无权操作该记忆");
        }
        return memory;
    }

    private float[] embedOrThrow(String content) {
        try {
            float[] embedding = ragService.embed(content);
            if (embedding == null || embedding.length == 0) {
                throw new BizException("记忆向量化失败");
            }
            return embedding;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("记忆向量化失败: " + e.getMessage());
        }
    }

    private String requireMemoryType(String memoryType) {
        if (!StringUtils.hasText(memoryType)) {
            throw new BizException("记忆类型不能为空");
        }
        String normalized = memoryType.trim().toUpperCase();
        if (!LongTermMemoryService.MemoryType.PREFERENCE.getCode().equals(normalized)
                && !LongTermMemoryService.MemoryType.FACT.getCode().equals(normalized)) {
            throw new BizException("无效的记忆类型: " + memoryType);
        }
        return normalized;
    }

    private String normalizeMemoryTypeFilter(String memoryType) {
        if (!StringUtils.hasText(memoryType)) {
            return null;
        }
        return requireMemoryType(memoryType);
    }

    private String buildManualMetadata(CreateLongTermMemoryRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "manual");
        if (StringUtils.hasText(request.getSourceAgentId())) {
            metadata.put("sourceAgentId", request.getSourceAgentId());
        }
        if (StringUtils.hasText(request.getSourceSessionId())) {
            metadata.put("sourceSessionId", request.getSourceSessionId());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{\"source\":\"manual\"}";
        }
    }

    private LongTermMemoryVO toVO(LongTermMemory memory) {
        Map<String, Object> meta = parseMetadata(memory.getMetadata());
        String sourceAgentId = firstNonBlank(
                memory.getAgentId(),
                asString(meta.get("sourceAgentId"))
        );
        String sourceSessionId = firstNonBlank(
                memory.getSessionId(),
                asString(meta.get("sourceSessionId"))
        );
        return LongTermMemoryVO.builder()
                .id(memory.getId())
                .memoryType(memory.getMemoryType())
                .content(memory.getContent())
                .sourceAgentId(sourceAgentId)
                .sourceSessionId(sourceSessionId)
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        return StringUtils.hasText(fallback) ? fallback : null;
    }
}
