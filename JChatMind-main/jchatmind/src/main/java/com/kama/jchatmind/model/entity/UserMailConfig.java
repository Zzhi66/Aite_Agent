package com.kama.jchatmind.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户个人发件邮箱 SMTP 配置
 */
@Data
@Builder
public class UserMailConfig {
    private String userId;
    private String fromEmail;
    private String smtpHost;
    private Integer smtpPort;
    /** AES 加密后的 SMTP 授权码 */
    private String smtpPasswordEnc;
    private Boolean useSsl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
