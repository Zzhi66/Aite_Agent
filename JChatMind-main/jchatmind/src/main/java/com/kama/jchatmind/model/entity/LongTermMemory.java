package com.kama.jchatmind.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LongTermMemory {

    private String id;

    /** 记忆归属用户，召回与管理均按此字段隔离 */
    private String userId;

    private String agentId;

    private String sessionId;

    private String memoryType;

    private String content;

    private String metadata;

    // pgvector literal string, e.g. "[0.1,0.2,...]"
    private String embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Query-only field
    private Double distance;
}
