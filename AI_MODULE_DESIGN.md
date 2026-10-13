# 知途 · AI 智能客服模块设计参考（zp-aigc）

> **本文档的用途**：沉淀知途在线学习平台中大模型应用模块（`zp-aigc`）的架构设计、关键代码模式与踩坑经验，作为一份可复用的设计参考——按同样的模式可以在其他项目中搭建同类 AI 能力。
>
> 阅读方式：第 2 章是整体架构，第 3~9 章是各能力的模式与代码，第 10 章是踩坑清单（最有价值，务必读），第 11 章是速查表，第 12 章是落地步骤。

---

## 1. 技术栈概览

| 类别 | 选型 | 说明 |
|---|---|---|
| 基础框架 | Spring Boot 3.3.5 / Java 17 | |
| AI 框架 | **Spring AI 1.0.0 GA**（`spring-ai-bom`） | 本文档所有 API 均基于 1.0.0 正式版 |
| 模型接入 | `spring-ai-alibaba-starter-dashscope` 1.0.0.2 | 阿里通义千问（DashScope），ChatModel + EmbeddingModel 各一个；换厂商只需换 starter |
| 向量库 | `spring-ai-starter-vector-store-elasticsearch`（ES 8.15.5） | RAG 知识库 |
| 聊天记忆 | Redis（自研 `ChatMemoryRepository` 实现） | 多轮对话历史 |
| 会话元数据 | MySQL + MyBatis-Plus | 会话列表/标题 |
| 配置中心 | Nacos | 提示词、模型 api-key、共享配置；支持提示词热更新 |

---

## 2. 整体架构

### 2.1 模块分层

```
前端（EventSource / fetch 流式读取）
   │  POST /chat          body: {question, sessionId}    （text/event-stream）
   │  POST /chat/stop?sessionId=xxx                      （停止生成）
   ▼
ChatController ──── SessionController（会话管理）──── EmbeddingController（向量库写入/调试）
   │
   ▼
ChatService 接口（@ConditionalOnProperty(tj.ai.chat-type=...) 可切换实现）
   │
   ├── AgentServiceImpl（@Primary，实际生效：路由式多 Agent 编排）
   │     │ ① RouteAgent.process()        —— 非流式，让 LLM 做意图分类，输出目标 Agent 名
   │     │ ② 目标 Agent.processStream()  —— 流式回答（RAG + 工具调用）
   │     ▼
   └── ChatServiceImpl（旧版单 Agent 实现，教学对比用，正常不生效）

Agent 体系（模板方法模式）：
   Agent 接口（default 钩子） ← AbstractAgent（流程骨架 + SSE 管线）
        ├── RouteAgent        只覆盖提示词：LLM 当意图分类器
        └── RecommendAgent    RAG(QuestionAnswerAdvisor) + CourseTools（课程工具）
        └── （新增业务 Agent = 一个 @Component + 一份提示词，编排层零改动）

支撑组件：
   SpringAIConfig            ChatClient 全局单例 + ChatMemory 三件套 + 全局 Advisor
   RedisChatMemoryRepository 聊天记忆持久化（Redis List，key = CHAT:{userId}_{sessionId}）
   ToolResultHolder          工具结构化结果 → PARAM 事件的中转站
   SystemPromptConfig        Nacos 提示词动态加载 + 热更新
   ChatSessionService        MySQL 会话元数据 + @Async 自动标题
```

### 2.2 一次聊天请求的完整时序

```
用户提问
  → ChatController.chat()                        POST，返回 Flux<ChatEventVO>
  → AgentServiceImpl.chat()
      → RouteAgent.process()                     非流式 call()，LLM 输出一个 Agent 名（如 RECOMMEND）
      → AgentTypeEnum.agentNameOf(result)        把 Agent 名解析回枚举
      → 未命中任何 Agent？ → 路由回答直接作为兜底回复（DATA + STOP 两个事件结束）
      → 命中 → 目标 Agent.processStream()
          → 组装请求：system(动态提示词) + advisors(记忆+RAG) + tools + toolContext(userId, requestId)
          → .stream().chatResponse()             SSE 流
  → 流中：每段文本包装为 DATA 事件下发
      （工具执行时调 Feign 查业务系统，结构化结果存入 ToolResultHolder）
  → 流尾：concatWith 追加 PARAM（工具结构化结果）+ STOP（结束标记）
```

**设计取舍**：路由本身消耗一次 LLM 调用（非流式、速度快），换来的是"每个业务 Agent 提示词聚焦单一职责"——提示词短、工具少，准确率和 token 成本都优于一个大而全的提示词。

---

## 3. 多 Agent 设计（核心）

### 3.1 Agent 接口：default 钩子定义"可选能力点"

```java
public interface Agent {

    // ===== 必须实现：每个 Agent 要回答的 4 个问题 =====
    Flux<ChatEventVO> processStream(String question, String sessionId); // 流式回答
    String process(String question, String sessionId);                  // 非流式回答（路由用）
    AgentTypeEnum getAgentType();                                       // 我是谁（枚举身份）
    void stop(String sessionId);                                        // 停止生成

    // ===== 可选能力点：子类按需覆盖，不覆盖 = 没有该能力 =====
    default String systemMessage() { return ""; }                       // 系统提示词
    default Object[] tools() { return EMPTY_OBJECTS; }                  // 工具列表
    default Map<String, Object> toolContext(String sessionId, String requestId) { return Map.of(); }
    default List<Advisor> advisors() { return List.of(); }              // Advisor 增强链（RAG 等）
    default Map<String, Object> advisorParams(String sessionId, String requestId) { return Map.of(); }
    default Map<String, Object> systemMessageParams() { return Map.of(); } // 提示词占位符参数
}
```

设计要点：

- **必须实现的 4 个方法**是每个 Agent 的身份与入口；**6 个 default 钩子**是可插拔能力。避免"为了实现接口写一堆空方法"。
- `systemMessageParams()` 与 `systemMessage()` 配套：提示词模板里的 `{now}` 之类占位符由它填充。
- `toolContext()` 与 `tools()` 配套：给工具传递身份与回传 key（见第 5 章）。

### 3.2 AbstractAgent：模板方法统一"流程骨架 + SSE 管线"

抽象类持有三个注入：`ChatClient`（全局单例）、`ChatMemory`、`ChatSessionService`。

**骨架方法（private，不可覆盖）**——组装请求的可变点全部委托给钩子：

```java
private ChatClient.ChatClientRequestSpec getChatClientRequest(String sessionId, String requestId, String question) {
    return this.chatClient.prompt()
            .system(p -> p.text(this.systemMessage()).params(this.systemMessageParams()))
            .advisors(a -> a.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
            .tools(this.tools())
            .toolContext(this.toolContext(sessionId, requestId))
            .user(question);
}
```

**非流式**（路由 Agent 用）：`call().content()` 拿全文，之前异步刷新一下会话时间/标题。

**流式**（业务 Agent 用）——这是整个模块最核心的 30 行，包含停止控制、取消落库、PARAM 追加三件事：

```java
public Flux<ChatEventVO> processStream(String question, String sessionId) {
    var userId = UserContext.getUser();
    var requestId = this.generateRequestId();          // 每次请求的唯一 id，工具结果回传的 key
    var outputBuilder = new StringBuilder();           // 累积已输出内容，取消时落库
    var conversationId = ChatService.getConversationId(sessionId); // = userId + "_" + sessionId

    this.chatSessionService.update(sessionId, question, userId);   // @Async 刷新会话时间/自动标题

    return this.getChatClientRequest(sessionId, requestId, question)
            .stream().chatResponse()
            .doFirst(() -> GENERATE_STATUS.put(sessionId, true))       // 开始输出
            .doOnError(t -> GENERATE_STATUS.remove(sessionId))
            .doOnComplete(() -> GENERATE_STATUS.remove(sessionId))
            .doOnCancel(() -> this.saveStopHistoryRecord(conversationId, outputBuilder.toString()))
            .takeWhile(r -> GENERATE_STATUS.getOrDefault(sessionId, false)) // stop() 删 key → 流终止
            .map(chatResponse -> {
                var text = chatResponse.getResult().getOutput().getText();
                outputBuilder.append(text);
                return ChatEventVO.builder().eventData(text)
                        .eventType(ChatEventTypeEnum.DATA.getValue()).build();   // DATA 事件
            })
            .concatWith(Flux.defer(() -> {                             // 流尾：PARAM + STOP
                var map = ToolResultHolder.get(requestId);
                if (CollUtil.isNotEmpty(map)) {
                    ToolResultHolder.remove(requestId);
                    return Flux.just(
                            ChatEventVO.builder().eventData(map)
                                    .eventType(ChatEventTypeEnum.PARAM.getValue()).build(),
                            STOP_EVENT);
                }
                return Flux.just(STOP_EVENT);
            }));
}
```

**停止生成的机制**：`GENERATE_STATUS` 是 `public static final Map<String, Boolean>`（跨 Agent 实例共享，所以编排层只调 RouteAgent.stop 也能停掉任何 Agent 的流）。`stop(sessionId)` 只是 `remove(key)`，流在下一个信号点被 `takeWhile` 截断。单机够用；分布式部署应换 Redis（见踩坑 #2）。

### 3.3 具体 Agent：只写差异

路由 Agent——最薄，只覆盖提示词和身份，靠默认的记忆 Advisor 维持多轮：

```java
@Component
public class RouteAgent extends AbstractAgent {
    private final SystemPromptConfig systemPromptConfig;

    @Override public String systemMessage() { return this.systemPromptConfig.getRouteAgentSystemMessage().get(); }
    @Override public AgentTypeEnum getAgentType() { return AgentTypeEnum.ROUTE; }
}
```

业务 Agent——RAG + 工具 + 工具上下文：

```java
@Component
public class RecommendAgent extends AbstractAgent {
    private final SystemPromptConfig systemPromptConfig;
    private final VectorStore vectorStore;      // ES 向量库
    private final CourseTools courseTools;      // 业务工具（内部是 Feign 调课程服务）

    @Override public String systemMessage() { ... }
    @Override public AgentTypeEnum getAgentType() { return AgentTypeEnum.RECOMMEND; }

    @Override public List<Advisor> advisors() {          // RAG：检索增强
        return List.of(QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                .build());
    }
    @Override public Object[] tools() { return new Object[]{courseTools}; }

    @Override public Map<String, Object> toolContext(String sessionId, String requestId) {
        return Map.of(Constant.USER_ID, UserContext.getUser(),   // 身份透传给工具
                      Constant.REQUEST_ID, requestId);           // 结构化结果回传 key
    }
}
```

### 3.4 编排层：LLM 做意图分类 + Bean 自发现

```java
@Primary @Service
@ConditionalOnProperty(prefix = "tj.ai", name = "chat-type", havingValue = "ROUTE")
public class AgentServiceImpl implements ChatService {

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // ① 路由 Agent 非流式分类（提示词要求它只输出一个 Agent 名）
        var result = this.findAgentByType(AgentTypeEnum.ROUTE).process(question, sessionId);
        var agentTypeEnum = AgentTypeEnum.agentNameOf(result);   // 字符串 → 枚举（容错：null）
        // ② 按 getAgentType() 匹配
        var agent = this.findAgentByType(agentTypeEnum);
        if (agent == null) {
            // 未命中：把路由 Agent 的回答直接作为兜底回复
            return Flux.just(DATA事件(result), AbstractAgent.STOP_EVENT);
        }
        // ③ 命中：交给目标 Agent 流式处理
        return agent.processStream(question, sessionId);
    }

    // 反射发现所有 Agent Bean，不硬编码注入——新增 Agent 无需改编排层
    private Agent findAgentByType(AgentTypeEnum type) {
        if (type == null) return null;
        for (var agent : SpringUtil.getBeansOfType(Agent.class).values()) {
            if (type == agent.getAgentType()) return agent;
        }
        return null;
    }
}
```

配套的枚举设计（保证 LLM 输出能精确回解）：

```java
public enum AgentTypeEnum {
    ROUTE("ROUTE", "路由智能体"),
    RECOMMEND("RECOMMEND", "课程推荐智能体"),
    // ...更多业务 Agent
    ;
    private final String agentName;   // 提示词里写明可选值，LLM 输出它
    private final String desc;

    @Override public String toString() { return this.name(); }  // 打印/比对都用枚举名

    public static AgentTypeEnum agentNameOf(String name) {       // 解析失败返回 null → 走兜底
        return EnumUtil.getBy(AgentTypeEnum::getAgentName, name);
    }
}
```

### 3.5 ChatClient 全局单例的分工

`SpringAIConfig` 里 `ChatClient` 只挂**全局性**的 Advisor（日志 `SimpleLoggerAdvisor`、记忆 `MessageChatMemoryAdvisor`）；**工具和 RAG 不放全局**，由各 Agent 的 `tools()` / `advisors()` 钩子按需注入。

> 为什么工具不挂全局：挂全局意味着每个意图（包括路由分类）都携带全部工具定义，既浪费 token，又容易让模型在不该调工具的场景误调。

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder, Advisor loggerAdvisor,
                             Advisor messageChatMemoryAdvisor /*, 工具故意不挂 */) {
    return builder.defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor).build();
}
```

---

## 4. SSE 流式协议

### 4.1 事件协议（前端约定）

SSE 返回 `Flux<ChatEventVO>`，`ChatEventVO { Object eventData; int eventType; }`：

| eventType | 值 | 含义 | eventData 内容 |
|---|---|---|---|
| DATA | 1001 | 文本增量 | String（一段文本） |
| PARAM | 1003 | 工具的结构化结果 | Map（字段名 → 对象，如课程卡片、订单单据） |
| STOP | 1002 | 流结束标记 | null |

正常一条回答 = N 个 DATA + （可选）1 个 PARAM + 1 个 STOP。前端收到 STOP 才算结束。

### 4.2 Controller 要点

```java
@NoWrapper   // 项目自定义注解：跳过全局 ResponseWrap 包装，否则 SSE 事件体被包进 data 字段无法解析
@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
    return this.chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
}

@PostMapping("/stop")
public void stop(@RequestParam("sessionId") String sessionId) { this.chatService.stop(sessionId); }
```

- **用 POST 而不是 GET**：要带 JSON body（question + sessionId），EventSource 原生只支持 GET，前端用 fetch 流式读取。
- **返回 `Flux`**：Spring WebFlux/ MVC 均支持把 Flux 作为 SSE 响应流式写出（本项目是 MVC + reactive 流支持）。

---

## 5. 工具调用（Tool Calling）

### 5.1 工具定义：`@Tool` / `@ToolParam` / `ToolContext`

```java
@Component
public class CourseTools {
    private final CourseClient courseClient;   // Feign 调课程微服务

    @Tool(description = "根据课程id查询课程详细信息")
    public CourseInfo queryCourseById(
            @ToolParam(description = "课程id") Long courseId,
            ToolContext toolContext) {                      // 框架注入的上下文，不是 LLM 传参
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(this.courseClient.baseInfo(id, true)))
                .map(courseInfo -> {
                    // 把结构化结果按 requestId 暂存，流结束后以 PARAM 事件下发
                    var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
                    ToolResultHolder.put(requestId, "courseInfo_" + courseInfo.getId(), courseInfo);
                    return courseInfo;                      // 同时返回给 LLM（它据此组织话术）
                }).orElse(null);
    }
}
```

### 5.2 ToolResultHolder 模式：结构化结果如何到达前端

**问题**：工具在流的中途执行，其返回值会被 LLM 消费掉（变成自然语言），前端拿不到**原始结构化数据**（渲染卡片、跳转下单都需要）。

**方案**：请求开始时生成 `requestId` → 经 `toolContext` 传给工具 → 工具把结构化结果 `ToolResultHolder.put(requestId, field, data)` 暂存 → 流尾 `concatWith(Flux.defer(...))` 按 `requestId` 取出、包装成 PARAM 事件下发、`remove` 清理。

```
Agent.processStream ── toolContext{userId, requestId} ──► 工具方法
        ▲                                                        │ put(requestId, data)
        │                                                        ▼
        └── 流尾 get(requestId) ◄──────── ToolResultHolder (静态 ConcurrentHashMap)
```

注意：`ToolResultHolder` 是 JVM 内存，多实例部署时**同一会话的流必须落在同一实例**（网关按 sessionId hash），或换 Redis 实现。

### 5.3 工具线程的身份恢复（易踩坑）

工具调用跑在框架自己的线程上，**没有 HTTP 请求上下文**——ThreadLocal 里的用户信息、拦截器都不会生效，直接 Feign 调用会 401。做法：Agent 层把 userId 放进 `toolContext`，工具第一行恢复：

```java
@Tool(description = "购买课程预下单操作")
public PrePlaceOrder prePlaceOrder(@ToolParam(description = "课程id列表") List<Number> ids,
                                   ToolContext toolContext) {
    // 恢复用户身份，否则 Feign 调用 401
    UserContext.setUser(Convert.toLong(toolContext.getContext().get(Constant.USER_ID)));
    // LLM 传来的数字可能是 Integer/Long 混杂，统一转 long
    var orderConfirmVO = this.tradeClient.prePlaceOrder(CollStreamUtil.toList(ids, Number::longValue));
    ...
}
```

---

## 6. 会话记忆（自研 Redis ChatMemoryRepository）

### 6.1 三层结构

```
MessageChatMemoryAdvisor          挂在 ChatClient 上的 Advisor：请求前把历史注入 prompt，响应后追加新消息
        │  参数：ChatMemory.CONVERSATION_ID（字面值 "chat_memory_conversation_id"）
        ▼
MessageWindowChatMemory           窗口策略：maxMessages=100，超出自动淘汰最旧；每次都全量调 saveAll
        │
        ▼
RedisChatMemoryRepository         自研持久化：Redis List，key = "CHAT:" + conversationId
```

```java
@Bean public ChatMemoryRepository redisChatMemoryRepository() { return new RedisChatMemoryRepository(); }

@Bean
public ChatMemory chatMemory(ChatMemoryRepository repo) {
    return MessageWindowChatMemory.builder().chatMemoryRepository(repo)
            .maxMessages(this.maxMessages)   // @Value("${tj.ai.memory.max:100}")
            .build();
}

@Bean
public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
    return MessageChatMemoryAdvisor.builder(chatMemory).build();
}
```

### 6.2 conversationId 规则：多用户隔离

会话 id 只在前端流转，记忆的 key 用 `userId + "_" + sessionId`，从根上隔离不同用户（也防越权读他人历史）：

```java
public interface ChatService {
    static String getConversationId(String sessionId) {
        return UserContext.getUser() + "_" + sessionId;   // 如 "1001_ab12cd34"
    }
}
```

### 6.3 Repository 实现（Redis List）

```java
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    public static final String DEFAULT_PREFIX = "CHAT:";
    private final StringRedisTemplate stringRedisTemplate;

    @Override public List<Message> findByConversationId(String id) {
        var messages = this.stringRedisTemplate.boundListOps(this.getKey(id)).range(0, -1);
        return CollStreamUtil.toList(messages, MessageUtil::toMessage);   // JSON → Message
    }
    @Override public void saveAll(String id, List<Message> messages) {
        // MessageWindowChatMemory 每次传全量窗口 → 先删后写（全量覆盖）
        this.deleteByConversationId(id);
        messages.forEach(m -> listOps.rightPush(MessageUtil.toJson(m)));
    }
    @Override public void deleteByConversationId(String id) { this.stringRedisTemplate.delete(this.getKey(id)); }
    private String getKey(String id) { return prefix + id; }   // "CHAT:{userId}_{sessionId}"
}
```

### 6.4 自定义序列化（关键！Spring AI Message 不能直接 JSON 反序列化）

Spring AI 的 `Message` 有四个实现（SystemMessage / UserMessage / AssistantMessage / ToolResponseMessage），没有默认无参构造，直接序列化存 Redis 再反序列化会失败。做法：引入中间 DTO `MyMessage`，**必须保留 `toolCalls` 和 `toolResponses`**——否则带工具调用的多轮对话还原后，assistant 消息带着 tool_call 却找不到对应 tool 消息，下一次模型请求直接报错。

```java
@Data
public class MyMessage {
    private String messageType;                                  // SYSTEM/USER/ASSISTANT/TOOL
    private Map<String, Object> metadata = Map.of();
    private List<Media> media = List.of();
    private List<AssistantMessage.ToolCall> toolCalls = List.of();          // 关键字段
    private String textContent;
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of(); // 关键字段
    private Map<String, Object> params = Map.of();
}

public static Message toMessage(String json) {
    var my = JSONUtil.toBean(json, MyMessage.class);
    return switch (MessageType.valueOf(my.getMessageType())) {
        case SYSTEM   -> new SystemMessage(my.getTextContent());
        case USER     -> UserMessage.builder().text(my.getTextContent())
                             .metadata(my.getMetadata()).media(my.getMedia()).build();
        case ASSISTANT-> new AssistantMessage(my.getTextContent(), my.getMetadata(), my.getToolCalls());
        case TOOL     -> new ToolResponseMessage(my.getToolResponses(), my.getMetadata());
    };
}
```

---

## 7. RAG（检索增强）

- **读取侧**：`QuestionAnswerAdvisor`（来自 `spring-ai-advisors-vector-store`）挂在业务 Agent 的 `advisors()` 钩子上，自动检索并把知识片段拼进 prompt。参数经验值：`similarityThreshold=0.6`（低于阈值的片段不注入）、`topK=6`。
- **写入侧**：`EmbeddingController` 提供运维/调试接口——批量文本构建 `Document` 后 `vectorStore.add()`，另有单条 embed、相似度 search（topK=5）、按 id delete 等接口。
- **按需挂载**：只有需要知识库的 Agent 覆盖 `advisors()`，路由 Agent 等不挂，避免每次多一次向量检索开销。

---

## 8. 提示词管理：配置中心 + 热更新

提示词**不进代码库**，存配置中心（Nacos dataId，txt 文件），`@ConfigurationProperties` 绑定 dataId/group/timeoutMs。启动时拉取 + 注册 Listener，改提示词**不重启生效**（Agent 每次请求都 `.get()` 读最新值）：

```java
@Configuration
public class SystemPromptConfig {
    private final NacosConfigManager nacosConfigManager;
    private final AIProperties aiProperties;
    // AtomicReference 保证线程安全的热更新引用
    private final AtomicReference<String> routeAgentSystemMessage = new AtomicReference<>();

    @PostConstruct
    public void init() {
        var cfg = aiProperties.getSystem().getRouteAgent();     // {dataId, group, timeoutMs}
        var config = nacosConfigManager.getConfigService().getConfig(cfg.getDataId(), cfg.getGroup(), cfg.getTimeoutMs());
        this.routeAgentSystemMessage.set(config);
        nacosConfigManager.getConfigService().addListener(cfg.getDataId(), cfg.getGroup(), new Listener() {
            @Override public void receiveConfigInfo(String info) { routeAgentSystemMessage.set(info); }
            @Override public Executor getExecutor() { return null; }
        });
    }
}
```

非微服务项目可平移为：把提示词放数据库表/管理后台，同样的 `AtomicReference` + 定时刷新（或版本化 reload 接口）模式。

---

## 9. 会话管理（MySQL 元数据 + Redis 历史）

接口：`POST /session`（新建，随机抽 N 个示例问题 + UUID sessionId 落库）、`GET /session/hot`、`GET /session/{sessionId}`（历史消息）、`GET /session/history`（会话列表）、`DELETE /session/history?sessionId=`。

设计要点：

1. **双存储分工**：MySQL `chat_session` 表存元数据（sessionId、userId、标题、时间）；Redis 只存聊天记录（就是第 6 章的 ChatMemory）。删除会话要**双删**：删 MySQL 记录 + `chatMemory.clear(conversationId)` 删 Redis。
2. **自动标题**：每次提问 `@Async update(sessionId, question, userId)`——标题为空时取首条问题截前 100 字作标题，并刷新 updateTime（用于列表排序）。异步执行不阻塞聊天主链路。
3. **历史消息读取**：直接 `chatMemory.get(conversationId)` 从 Redis 读，**过滤只保留 USER/ASSISTANT**（SYSTEM/TOOL 消息不给前端）。
4. **历史列表分组**：查最近 30 条有标题的会话，按更新时间分组为「当天 / 最近30天 / 最近1年 / 1年以上」。

---

## 10. 踩坑与经验清单（给 AI/开发者的 do & don't）

1. **`takeWhile` 截断触发的是 `doOnComplete` 而非 `doOnCancel`**：调 `/stop` 接口截断的内容**不会**写回历史（doOnCancel 里才有落库逻辑）；只有客户端主动断开 SSE 才走 cancel。若希望 stop 也保留半截回答，需在 map 里持续累积并在 stop 时主动落库。
2. **`GENERATE_STATUS` 必须 `static`**：它是跨 Agent 实例共享的状态表，`stop()` 删的是全局 key。分布式部署要换 Redis（key 加 TTL 兜底防泄漏），且同一会话的流必须粘在同一实例。
3. **工具线程没有请求上下文**：ThreadLocal 用户信息失效，Feign 调用 401。必须 `toolContext` 带 userId、工具第一行 `UserContext.setUser(...)` 恢复。
4. **`MessageWindowChatMemory` 每次全量 `saveAll`** → Redis 里是"删了重写"，长对话有写放大；对话量大时可优化为增量追加（只写新增消息）。`findConversationIds()` 用了 `keys` 命令，生产大库禁用（换 SCAN 或干脆不提供该能力）。
5. **Spring AI 的 `Message` 不能直接 JSON 反序列化**：必须自定义中间格式（MyMessage），并且**保留 `toolCalls`/`toolResponses`**，否则带工具调用的历史还原后模型请求直接失败。
6. **连续两次 `.toolContext(...)` 会互相覆盖**（后者胜出），一次性传全。
7. **LLM 传的工具参数类型不可控**（数字可能是 int/long 混杂），用 `List<Number>` 接参再统一转换，不要直接 `List<Long>`。
8. **路由 Agent 的提示词必须强约束输出格式**（只输出 Agent 名），解析用 `agentNameOf` 容错返回 null，未命中时把路由回答当兜底回复直接返回给用户。
9. **ChatClient 全局单例只放全局 Advisor**：工具/RAG 挂到具体 Agent，避免 token 浪费和误调用工具。
10. **SSE 接口要跳过全局响应包装**（`@NoWrapper`），且用 POST 传 JSON body。
11. **PARAM/状态类 Map 用完必须 remove**（流尾 `defer` 里清理），否则内存泄漏。
12. 参考代码中的遗留点，新项目不要照抄：`AbstractAgent.processStream` 的 map 阶段有一处 `ToolResultHolder.put(messageId, REQUEST_ID, requestId)` 与流尾按 `requestId` 读取的 key 不一致（无实际作用）；`SpringAIConfig` 里注入了 `OrderTools` 但 `defaultTools` 已注释（悬空参数）；`AgentTypeEnum` 里 CONSULT/BUY/KNOWLEDGE 尚无实现类。

---

## 11. 可复用设计模式速查表

| 要什么能力 | 用什么模式 | 参考类（zp-aigc 内路径） |
|---|---|---|
| 多意图分流 | RouteAgent 意图分类 + Agent 枚举 + `getBeansOfType` 自发现 | `service/impl/AgentServiceImpl` |
| Agent 可插拔 | 接口 default 钩子（能力点）+ 抽象类模板方法（流程骨架） | `agent/Agent`、`agent/AbstractAgent` |
| 流式输出 | POST + `Flux<ChatEventVO>` + `text/event-stream` + 事件协议(DATA/PARAM/STOP) | `controller/ChatController` |
| 结构化结果下发前端 | ToolResultHolder（requestId 中转）+ PARAM 事件 | `config/ToolResultHolder`、`tools/CourseTools` |
| 停止生成 | 静态状态 Map + `takeWhile` + `/stop` 接口 | `agent/AbstractAgent` |
| 多轮对话记忆 | 自研 `ChatMemoryRepository`(Redis) + `MessageWindowChatMemory` + `MessageChatMemoryAdvisor` | `memory/` 包、`config/SpringAIConfig` |
| 记忆序列化 | 中间 DTO + 按 MessageType switch 还原（保留 toolCalls/toolResponses） | `memory/MessageUtil`、`memory/MyMessage` |
| RAG | `QuestionAnswerAdvisor` + VectorStore（阈值 0.6 / topK 6） | `agent/RecommendAgent` |
| 提示词热更新 | 配置中心 dataId + `AtomicReference` + Listener | `config/SystemPromptConfig` |
| 会话管理 | MySQL 元数据 + Redis 历史 + `@Async` 自动标题 + 双删 | `service/impl/ChatSessionServiceImpl` |
| 工具身份透传 | toolContext 带 userId + 工具线程 `UserContext.setUser` 恢复 | `tools/OrderTools` |

---

## 12. 新项目落地清单

1. 引依赖：`spring-ai-bom` + 模型 starter（DashScope/OpenAI 等按需）+ 向量库 starter + Redis。
2. 写 `SpringAIConfig`：ChatClient 单例（全局只挂日志 + 记忆 Advisor）、ChatMemory 三件套（Repository / Window / Advisor）。
3. 实现 `Agent` 接口 + `AbstractAgent`（可整体照抄，含 SSE 管线与停止机制）。
4. 定义 `AgentTypeEnum`，实现 RouteAgent（意图分类提示词）+ 各业务 Agent（提示词 + tools/advisors 钩子）。
5. 写编排层（`getBeansOfType` + 未命中兜底）。
6. 写 Controller：聊天（SSE，跳过响应包装）+ 停止 + 会话管理。
7. 提示词外置到配置中心/管理后台，支持热更新。
8. 业务工具：`@Tool` 定义 + `toolContext` 传 userId/requestId + ToolResultHolder 回传结构化数据。
9. 按第 10 章清单自查避坑。

---

## 附：本项目特有、移植时需替换的部分

| 本项目实现 | 移植时替换为 |
|---|---|
| `UserContext`（ThreadLocal，来自内部 common 包） | 新项目的用户上下文（SecurityContext / ThreadLocal / Sa-Token 等） |
| `zp-auth-resource-sdk`（网关传 token、拦截器注入用户） | 新项目的鉴权体系 |
| Nacos 提示词存储 | 数据库/管理后台/本地文件（保留热更新思路即可） |
| 工具里调 `CourseClient`/`TradeClient`（Feign） | 新项目的 Service 层或外部 API |
| `@NoWrapper` / knife4j | 新项目的响应包装注解与文档方案 |
