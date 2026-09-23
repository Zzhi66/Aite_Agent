-- 执行 auth_migration.sql 后运行；确认记录通过聊天工具消息中的 approvalId 关联。
CREATE TABLE IF NOT EXISTS email_approval (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    recipient TEXT NOT NULL,
    subject TEXT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'CANCELLED', 'EXPIRED', 'FAILED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_email_approval_user_created ON email_approval(user_id, created_at);
COMMENT ON TABLE email_approval IS '邮件发送确认记录；草稿不可修改，同一请求只允许一次发送尝试';
