-- 长期记忆：用户级隔离迁移（在 auth_migration.sql 之后执行）
-- agent_id / session_id 改为可空，仅作来源溯源；召回与管理以 user_id 为准

-- 来源字段可空
ALTER TABLE long_term_memory ALTER COLUMN agent_id DROP NOT NULL;
ALTER TABLE long_term_memory ALTER COLUMN session_id DROP NOT NULL;

-- user_id 列（auth_migration 已添加则跳过）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'long_term_memory' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE long_term_memory ADD COLUMN user_id UUID REFERENCES app_user(id);
    END IF;
END $$;

-- 通过 agent 反查回填历史行的 user_id
UPDATE long_term_memory ltm
SET user_id = a.user_id
FROM agent a
WHERE ltm.agent_id = a.id
  AND ltm.user_id IS NULL
  AND a.user_id IS NOT NULL;

-- 用户维度查询索引
CREATE INDEX IF NOT EXISTS idx_long_term_memory_user_id ON long_term_memory(user_id);
CREATE INDEX IF NOT EXISTS idx_long_term_memory_user_type ON long_term_memory(user_id, memory_type);
