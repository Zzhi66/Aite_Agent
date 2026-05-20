package com.kama.jchatmind.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LongTermMemoryVO {
    private String id;
    private String memoryType;
    private String content;
    /** 来源智能体（实体列或 metadata） */
    private String sourceAgentId;
    /** 来源会话（实体列或 metadata） */
    private String sourceSessionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
