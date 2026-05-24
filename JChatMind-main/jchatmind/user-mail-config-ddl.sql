-- 用户个人邮箱 SMTP 配置（每人用自己的邮箱发信）
CREATE TABLE IF NOT EXISTS user_mail_config (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    from_email VARCHAR(255) NOT NULL,
    smtp_host VARCHAR(255) NOT NULL DEFAULT 'smtp.qq.com',
    smtp_port INTEGER NOT NULL DEFAULT 465,
    smtp_password_enc TEXT NOT NULL,
    use_ssl BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE user_mail_config IS '用户个人发件邮箱 SMTP 配置';
COMMENT ON COLUMN user_mail_config.from_email IS '发件人邮箱地址';
COMMENT ON COLUMN user_mail_config.smtp_password_enc IS 'SMTP 授权码（AES 加密存储）';
