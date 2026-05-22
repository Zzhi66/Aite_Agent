package com.kama.jchatmind.model.request;

import lombok.Data;

@Data
public class SaveUserMailConfigRequest {
    /** 发件人邮箱 */
    private String fromEmail;
    /** SMTP 主机，如 smtp.qq.com */
    private String smtpHost;
    /** SMTP 端口，QQ 邮箱 SSL 一般为 465 */
    private Integer smtpPort;
    /** SMTP 授权码（更新时可留空表示不修改） */
    private String smtpPassword;
    /** 是否使用 SSL，默认 true */
    private Boolean useSsl;
}
