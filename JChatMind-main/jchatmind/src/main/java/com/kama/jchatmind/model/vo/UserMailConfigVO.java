package com.kama.jchatmind.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMailConfigVO {
    /** 是否已配置发件邮箱 */
    private boolean configured;
    private String fromEmail;
    private String smtpHost;
    private Integer smtpPort;
    private Boolean useSsl;
    /** 是否已保存过授权码（不返回明文） */
    private boolean passwordSet;
}
