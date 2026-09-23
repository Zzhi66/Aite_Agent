package com.kama.jchatmind.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
public class EmailApprovalRecord {
    private String id;
    private String userId;
    private String to;
    private String subject;
    private String content;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
