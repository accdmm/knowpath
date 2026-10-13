# 知途在线学习平台（knowpath）

> 面向在线教育场景的微服务学习平台，覆盖课程、学习、考试、交易、支付、搜索、消息、促销、数据等业务域；
> 并内置基于 Spring AI + 通义千问的 **AI 智能客服模块**（多 Agent 路由 / 工具调用 / RAG 检索 / SSE 流式输出）。

技术栈：`Spring Boot 3.3.5` · `Spring Cloud Alibaba（Nacos / Gateway）` · `Spring AI 1.0.0` · `Elasticsearch` · `RabbitMQ` · `MySQL` · `MyBatis-Plus` · `Redis` · `Docker`

---

## 一、模块一览

| 模块 | 服务名 | 端口 | 职责 |
|---|---|---|---|
| `zp-gateway` | gateway-service | 10010 | 统一入口、路由转发、鉴权拦截 |
| `zp-auth` | auth-service | 8081 | 账号、RBAC 权限、JWT 签发与校验 |
| `zp-user` | user-service | 8082 | 学生 / 教师 / 管理员信息 |
| `zp-search` | search-service | 8083 | 基于 Elasticsearch 的课程与资讯检索 |
| `zp-media` | media-service | 8084 | 文件上传、媒资管理 |
| `zp-message` | message-service | 8085 | 站内消息、短信、延迟通知 |
| `zp-course` | course-service | 8086 | 课程、章节、目录、教师与分类 |
| `zp-pay` | pay-service | 8087 | 支付单、第三方支付对接、退款 |
| `zp-trade` | trade-service | 8088 | 订单、购物车、下单与状态流转 |
| `zp-exam` | exam-service | 8089 | 题库、试卷、考试与判分 |
| `zp-learning` | learning-service | 8090 | 学习进度、课表、积分与签到 |
| `zp-remark` | remark-service | 8091 | 互动问答、笔记与评论 |
| `zp-promotion` | promotion-service | 8092 | 优惠券、促销活动 |
| `zp-data` | data-service | 8095 | 数据看板与统计 |
| **`zp-aigc`** | aigc-service | 8094 | **AI 智能客服（大模型应用模块）** |

`zp-common` / `zp-api` 为公共依赖与跨服务契约层。

---

## 二、AI 智能客服模块（zp-aigc）

这是本项目的技术重点：把大模型能力做成"可插拔的 Agent 体系"，而不是一次性的接口调用。

### 1. 多 Agent 路由

- `Agent` 接口用 **default 钩子**声明能力点（提示词、工具、Advisor、是否可用），`AbstractAgent` 用**模板方法**固定"取记忆 → 装配 ChatClient → 流式输出 → 落库"的流程骨架，具体 Agent 只写差异部分。
- 编排层 `AgentServiceImpl` 通过 **LLM 意图分类 + Spring `getBeansOfType` 自发现**把请求分发给场景 Agent（当前实现：`RouteAgent` 负责意图路由，`RecommendAgent` 负责课程推荐与问答）。
- 路由本身消耗一次非流式 LLM 调用，换来"每个业务 Agent 的提示词短、工具少"，准确率与 token 成本都优于一个大而全的提示词。

### 2. 工具调用与结构化结果回传

- 业务能力（课程详情查询、购买预下单）用 `@Tool` / `@ToolParam` 声明为 Spring AI Tool，工具内部经 **Feign 调用课程 / 交易微服务**，工具按 Agent 挂载而非全局注册，避免提示词膨胀与误调用。
- 工具返回的业务对象（课程卡片、订单单据）不能直接塞进文本流。方案是：请求开始时生成 `requestId` → 经 `ToolContext` 传给工具 → 工具把结构化对象 `ToolResultHolder.put(requestId, field, data)` 暂存 → **流尾用 `Flux.defer` 按 requestId 取出、包装成 `PARAM` 事件下发**并清理，前端据此渲染卡片。

### 3. RAG 检索增强

课程知识库经向量化写入 **Elasticsearch 向量库**（`spring-ai-starter-vector-store-elasticsearch`），问答侧通过 `QuestionAnswerAdvisor` 接入检索链路，实现基于真实课程资料的问答。

### 4. 会话记忆（自研 Redis ChatMemoryRepository）

- 未直接使用 Spring AI 默认的内存实现，而是自研 `RedisChatMemoryRepository`：会话历史以 **Redis List** 存储，天然支持多实例共享与过期淘汰。
- 关键点：Spring AI 的 `Message` 是接口/多实现，**不能直接 JSON 反序列化**，因此实现了自定义序列化（`MessageUtil` / `MyMessage`）。
- `conversationId` 采用"用户维度 + 会话维度"的规则拼装，保证多用户之间完全隔离。

### 5. 提示词管理与热更新

提示词正文放在 **Nacos 配置中心**（`DEFAULT_GROUP` 下的 `system-chat-message.txt`、`route-agent-system-message.txt`、`recommend-agent-system-message.txt`），支持不重启调整。会话元数据（会话列表、标题）落 **MySQL**。

### 6. SSE 流式输出

`ChatController` 以 `POST + Flux<ChatEventVO> + text/event-stream` 输出，自定义事件协议：`DATA`（正文片段）、`PARAM`（结构化参数）、`STOP`（结束），前端按事件类型分流渲染。该接口需跳过全局响应包装（`@NoWrapper`），否则 SSE 事件体会被套进统一响应结构而无法解析。

> 模块的完整设计说明与踩坑清单见 [`AI_MODULE_DESIGN.md`](AI_MODULE_DESIGN.md)。

---

## 三、快速开始

### 1. 环境要求

| 组件 | 说明 |
|---|---|
| JDK | 17 |
| Maven | 3.6+ |
| MySQL | 8.x，各服务数据库名见各自 `application.yml`（`zp.jdbc.database`） |
| Redis | 会话记忆与缓存 |
| Elasticsearch | 7.12+，课程检索与 AI 向量库 |
| RabbitMQ | 异步消息与延迟消息 |
| Nacos | 注册中心 + 配置中心（**必需**，AI 提示词也从这里下发） |
| Docker | 可选，`Dockerfile` + `startup.sh` 提供容器化启动脚本 |

### 2. 构建

```bash
mvn -DskipTests clean install
```

### 3. 启动顺序

1. 启动 Nacos，并把各服务的配置与 AI 提示词导入对应 `data-id`；
2. 按上表依次启动 `zp-auth` → `zp-user` / `zp-course` / `zp-trade` / `zp-pay` / `zp-search` / `zp-media` / `zp-message` / `zp-learning` / `zp-exam` / `zp-promotion` / `zp-data` / `zp-remark`；
3. 启动 `zp-aigc`；
4. 最后启动 `zp-gateway`（10010），所有外部请求经网关进入。

### 4. 配置说明

- 各服务的本地配置位于 `zp-*/src/main/resources/application-{local,test,dev}.yml`，`application.yml` 通过 `spring.profiles.active` 选择生效环境；
- 模型 API Key、共享数据源等敏感配置**不在仓库内**，从 Nacos 读取；
- `zp-auth` 的 JWT 签名使用 `zp-auth/zp-auth-service/src/main/resources/knowpath.jks`。

---

## 四、目录结构

```
knowpath
├── zp-common                  公共依赖：常量、异常、工具、自动装配
├── zp-api                     跨服务契约（DTO / Feign 接口）
├── zp-auth                    鉴权服务（含 zp-auth-common / 两个 SDK）
├── zp-gateway                 网关
├── zp-user / zp-course / zp-learning / zp-exam / zp-remark
├── zp-trade / zp-pay          交易与支付
├── zp-search / zp-media       检索与媒资
├── zp-message / zp-promotion / zp-data
├── zp-aigc                    AI 智能客服模块
├── AI_MODULE_DESIGN.md        AI 模块设计参考（架构 / 模式 / 踩坑清单）
├── Dockerfile / startup.sh    容器化构建与启动脚本
└── pom.xml                    聚合构建
```

---

## 五、已知限制

1. **提示词不在仓库内**：AI 提示词托管在 Nacos，clone 后需自行提供，否则 `zp-aigc` 无法完整工作。
2. **签名密钥库为开发用默认密钥**：`knowpath.jks` 与其口令写在配置中，仅用于本地开发；部署到任何公开环境前必须替换并外部化口令（如环境变量 / 配置中心）。
3. **各服务数据库需自行准备**：仓库不含建库脚本，需按 `application.yml` 中的库名建库并导入业务数据。
