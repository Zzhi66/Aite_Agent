# JChatMind

JChatMind 是一个基于 Spring Boot、Spring AI 和 React 的智能体聊天系统。项目支持多智能体配置、流式对话、工具调用、知识库文档管理、Redis 短期记忆，以及基于 PostgreSQL + pgvector 的长期记忆召回。

## 功能特性

- 智能体管理：创建、查询、删除不同角色和配置的 Agent。
- 流式聊天：后端通过 SSE 将模型回复实时推送到前端。
- 工具调用：支持文件系统、邮件、终止任务等工具扩展。
- 知识库管理：支持知识库、文档上传、Markdown 解析与向量检索。
- 记忆能力：Redis 保存近期上下文，PostgreSQL + pgvector 保存长期偏好和事实。
- 查询重写：可在检索前将用户问题改写成更适合召回的查询语句。
- 前后端分离：后端使用 Spring Boot，前端使用 React、Vite、Ant Design。

## 技术栈

- 后端：Java 17、Spring Boot 3.5、Spring AI、MyBatis、PostgreSQL、Redis
- 前端：React 19、TypeScript、Vite、Ant Design 6、Tailwind CSS
- 数据库扩展：pgvector、pgcrypto
- 模型接入：DeepSeek、智谱 AI、OpenAI 兼容接口

## 项目结构

```text
JChatMind-main/
├── jchatmind/                 # Spring Boot 后端服务
│   ├── src/main/java/          # Java 源码
│   ├── src/main/resources/     # MyBatis XML 与运行配置
│   └── long-term-memory-ddl.sql# 长期记忆表结构脚本
├── ui/                         # React 前端项目
└── jchatmind_sql/              # 初始化 SQL 脚本
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 20+
- PostgreSQL 14+
- Redis 6+
- PostgreSQL 已安装 `vector` 与 `pgcrypto` 扩展

## 后端启动

1. 创建数据库：

```sql
CREATE DATABASE jchatmind;
```

2. 初始化业务表和长期记忆表：

```bash
psql -U postgres -d jchatmind -f jchatmind_sql/jchatmind.sql
psql -U postgres -d jchatmind -f jchatmind/long-term-memory-ddl.sql
```

3. 在 `jchatmind/src/main/resources/application.yaml` 中配置本地数据库、Redis、邮箱和模型 API Key。请不要把真实密钥提交到仓库。

4. 启动后端：

```bash
cd jchatmind
mvn spring-boot:run
```

默认健康检查地址：

```text
GET http://localhost:8080/health
```

## 前端启动

```bash
cd ui
npm install
npm run dev
```

如需配置后端接口地址，请在 `ui/.env` 中设置本地环境变量，并避免提交该文件。

## 常用接口

- `GET /api/agents`：查询智能体列表
- `POST /api/agents`：创建智能体
- `GET /api/chat-sessions`：查询聊天会话
- `POST /api/chat-messages`：发送聊天消息
- `GET /api/knowledge-bases`：查询知识库
- `POST /api/documents/upload`：上传知识库文档
- `GET /api/tools`：查询可用工具
- `GET /sse/connect/{chatSessionId}`：建立 SSE 连接

## 配置说明

关键配置项位于 `jchatmind/src/main/resources/application.yaml`：

- `spring.datasource`：PostgreSQL 连接信息
- `spring.data.redis`：Redis 连接信息
- `spring.ai.*`：模型供应商与 API Key
- `jchatmind.memory.redis`：短期记忆窗口、摘要阈值和 TTL
- `jchatmind.memory.long-term`：长期记忆抽取、召回和注入策略
- `jchatmind.query-rewrite`：查询重写开关与模型配置

## 安全注意事项

- 不要提交 `.env`、`application.yaml`、日志文件或任何真实密钥。
- 首次部署前请轮换已经暴露过的 API Key。
- 生产环境建议使用环境变量、密钥管理服务或独立的私有配置文件注入敏感配置。

## 开发命令

```bash
# 后端测试
cd jchatmind
mvn test

# 前端构建
cd ui
npm run build

# 前端代码检查
cd ui
npm run lint
```
