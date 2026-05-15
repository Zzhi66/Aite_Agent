-- PostgreSQL + pgvector
-- Run this script once before enabling long-term memory.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS long_term_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL,
    session_id UUID NOT NULL,
    memory_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_long_term_memory_agent_session
    ON long_term_memory (agent_id, session_id, memory_type);

CREATE INDEX IF NOT EXISTS idx_long_term_memory_embedding
    ON long_term_memory USING ivfflat (embedding vector_l2_ops)
    WITH (lists = 100);
