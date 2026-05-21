# Aite Agent

> 完整文档见仓库根目录 [README.md](../README.md)。以下为在 `JChatMind-main/` 目录内开发时的快速指引。

## 快速启动（本目录内）

### 数据库

```bash
psql -U postgres -d jchatmind -f jchatmind_sql/jchatmind_assert/jchatmind.sql
psql -U postgres -d jchatmind -f jchatmind/long-term-memory-ddl.sql
psql -U postgres -d jchatmind -f jchatmind_sql/jchatmind_assert/auth_migration.sql
psql -U postgres -d jchatmind -f jchatmind/long-term-memory-user-scope.sql
```

### 后端

```bash
cd jchatmind
mvn spring-boot:run
```

### 前端

```bash
cd ui
npm install
npm run dev
```

访问 `http://localhost:5173`，在登录页注册/登录后使用。**Aite助手** 为主界面品牌名。

## 鉴权与记忆

- 鉴权后端：`controller/AuthController`、`security/` 包
- 鉴权前端：`ui/src/components/views/LoginView.tsx`、`ui/src/contexts/AuthContext.tsx`
- 长期记忆：`controller/LongTermMemoryController`、`ui/src/components/views/MemoryView.tsx`
- 迁移脚本：`jchatmind_sql/jchatmind_assert/auth_migration.sql`、`jchatmind/long-term-memory-user-scope.sql`

详细 API、配置项与架构说明请参阅 [根目录 README](../README.md)。
