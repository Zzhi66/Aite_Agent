-- PostgreSQL + pgvector
-- 新环境安装：长期记忆按用户隔离，agent_id/session_id 仅作来源溯源（可空）

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS long_term_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    agent_id UUID,
    session_id UUID,
    memory_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_long_term_memory_user_id
    ON long_term_memory (user_id);

CREATE INDEX IF NOT EXISTS idx_long_term_memory_user_type
    ON long_term_memory (user_id, memory_type);

CREATE INDEX IF NOT EXISTS idx_long_term_memory_embedding
    ON long_term_memory USING ivfflat (embedding vector_l2_ops)
    WITH (lists = 100);
