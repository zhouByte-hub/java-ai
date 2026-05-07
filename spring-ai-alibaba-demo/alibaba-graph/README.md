# Alibaba Graph 子项目说明

本子项目演示了如何在 Spring Boot 中使用 `spring-ai-alibaba-graph-core`
来构建 **基于大模型的有状态流程（Graph）**。  
它对应的 Maven 依赖为：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph-core</artifactId>
</dependency>
```

---

## 1. Graph 是什么？

在 Alibaba 的 Spring AI 生态中，**Graph** 是一种用于编排大模型工作流的
「状态图（StateGraph）」：

- 用 **节点（Node）** 表示一个步骤：可以是一次 LLM 调用、一个工具调用、
  一个子 Graph，甚至一个传统的业务方法。
- 用 **边（Edge）** 表示节点之间的执行顺序和条件跳转。
- 用 **状态（State / OverAllState）** 统一管理多步流程中产生的所有中间结果。
- 支持 **异步执行、流式返回、调度、检查点（Checkpoint）、持久化 Store、
  观测与指标** 等能力。

可以把它理解成：  
> 一个为 LLM 应用设计的轻量流程引擎 / 状态机，  
> 每个节点都可以是一个「Agent」或一个普通函数。

本项目中使用的核心类主要来自包：`com.alibaba.cloud.ai.graph.*`。

---

## 2. Graph 与 Agent 的关系

从更广义的 AI 应用架构看：

- **Agent**：  
  - 更偏向「**一个智能体**」的抽象，内部封装了：
    - 使用的大模型（如 Qwen）
    - 角色 / 系统提示词
    - 可调用的工具 / 插件
    - 记忆（Memory）
  - 关注 **一次或一轮推理**：根据当前输入决定要做什么。

- **Graph**：  
  - 更偏向「**多个步骤的编排**」，解决的是：
    - 多轮交互、多工具、多阶段推理如何串起来？
    - 如何记录、合并、回溯每一步产生的中间状态？
  - 每个节点可以：
    - 调用一个 Agent
    - 调用多个 Agent 并汇总结果
    - 调用外部系统 / 数据库 / RAG 检索等

在 `spring-ai-alibaba-graph-core` 中可以看到：

- `NodeOutput` 中包含 `agent` 字段，用来标记当前节点使用的 Agent 名称。
- `CompiledGraph#schedule(ScheduleConfig)` 等 API 允许以任务的方式调度 Graph，
  常见场景就是**长期运行的 Agent 工作流**。

**可以这样理解：**

> Graph 是「流程层」，负责编排；  
> Agent 是「能力层」，负责在每一个节点上完成具体的智能任务；  
> 两者经常配合使用，但也都可以单独使用。

---

## 3. 本子项目结构

关键代码路径：

- 启动类：`spring_ai_alibaba-demo/alibaba-graph/src/main/java/org/zhoubyte/alibabagraph/AlibabaGraphApplication.java`
- Graph 配置：`spring_ai_alibaba-demo/alibaba-graph/src/main/java/org/zhoubyte/alibabagraph/config/GraphConfig.java`
- REST 控制器：`spring_ai_alibaba-demo/alibaba-graph/src/main/java/org/zhoubyte/alibabagraph/config/GraphController.java`
- 配置文件：`spring_ai_alibaba-demo/alibaba-graph/src/main/resources/application.yaml`

对应的 Web 接口：

- 基础路径：`/alibaba-graph`（来自 `spring.webflux.base-path`）
- Graph 演示接口：`GET /alibaba-graph/v1/graph`

---

## 4. 核心概念与类

下面是 `spring-ai-alibaba-graph-core` 中最常接触的几个核心对象。

### 4.1 StateGraph – 定义图结构

`com.alibaba.cloud.ai.graph.StateGraph`

- 用于**声明 Graph 结构**（节点 + 边 + 状态合并策略）。
- 常用构造方法：

```java
// 传入图名称 + KeyStrategyFactory（定义状态字段与策略）
StateGraph graph = new StateGraph("myGraph", () -> Map.of(
        "input", new ReplaceStrategy(),
        "output", new ReplaceStrategy()
));
```

- 重要静态常量：
  - `StateGraph.START`：起始节点标记
  - `StateGraph.END`：结束节点标记
  - `StateGraph.ERROR`：错误节点标记（可以在发生异常时跳转）

- 重要方法（节选）：
  - `addNode(String, AsyncNodeAction)`：添加节点
  - `addNode(String, CompiledGraph)` / `addNode(String, StateGraph)`：嵌套子图
  - `addEdge(String from, String to)`：添加边
  - `compile()`：编译为可执行的 `CompiledGraph`
  - `getGraph(GraphRepresentation.Type)`：生成 Mermaid、PlantUML 图

### 4.2 CompiledGraph – 可执行的图

`com.alibaba.cloud.ai.graph.CompiledGraph`

- 由 `StateGraph#compile()` 得到，是 **Graph 的运行时表示**。
- 重要方法（节选）：

```java
// 一次性执行，返回最终 OverAllState
Optional<OverAllState> invoke(Map<String, Object> input);

// 一次性执行，返回最后一个 Node 的输出
Optional<NodeOutput> invokeAndGetOutput(Map<String, Object> input);

// 流式执行，返回 NodeOutput 的 Flux（适合 WebFlux SSE / 流式响应）
Flux<NodeOutput> stream(Map<String, Object> input);

// 流式执行 + 每一步完整响应（包含中间状态）
Flux<GraphResponse<NodeOutput>> graphResponseStream(Map<String, Object> input,
                                                    RunnableConfig config);

// 调度执行（定时 / 重试等）
ScheduledAgentTask schedule(ScheduleConfig scheduleConfig);
```

### 4.3 OverAllState – 全局状态

`com.alibaba.cloud.ai.graph.OverAllState`

- 是 Graph 在执行过程中的**统一状态容器**，内部维护一个
  `Map<String, Object>`（以及 KeyStrategy 映射）。
- 关键点：
  - 每个节点返回的是 `Map<String, Object>`，会被合并进 `OverAllState`。
  - 合并逻辑由对应 key 的 `KeyStrategy` 决定。
  - 提供丰富的读写方法，例如：

```java
// 获取某个 key 的值（Optional）
Optional<T> value(String key, Class<T> type);

// 获取值，带默认值（如果不存在则返回默认值）
T value(String key, T defaultValue);

// 更新状态（内部会应用 KeyStrategy）
Map<String, Object> updateState(Map<String, Object> updates);
```

### 4.4 节点动作：NodeAction / AsyncNodeAction

- `NodeAction`：

```java
public interface NodeAction {
    Map<String, Object> apply(OverAllState state) throws Exception;
}
```

- `AsyncNodeAction`：

```java
public interface AsyncNodeAction
        extends Function<OverAllState, CompletableFuture<Map<String, Object>>> {

    static AsyncNodeAction node_async(NodeAction nodeAction) { ... }
}
```

在实际使用中，最常见的写法是：

```java
graph.addNode("node1", AsyncNodeAction.node_async(state -> {
    // 读取当前状态
    System.out.println("current state = " + state);

    // 返回要写入状态的键值对
    return Map.of(
            "input", "some-value",
            "output", "some-other-value"
    );
}));
```

### 4.5 NodeOutput – 节点执行结果

`com.alibaba.cloud.ai.graph.NodeOutput`

- `CompiledGraph#stream(...)` / `graphResponseStream(...)` 中的元素类型。
- 主要字段：
  - `node()`：当前节点名称
  - `agent()`：本节点使用的 Agent 标识（如果有）
  - `state()`：当前节点执行后的 `OverAllState`
  - `tokenUsage()`：本节点大模型的 token 使用情况（兼容 Spring AI 的 `Usage`）

---

## 5. 状态值合并策略（KeyStrategy）

Graph 中最重要的设计之一是：**不同状态字段可以使用不同的合并策略**。  
相关类在包 `com.alibaba.cloud.ai.graph.state.strategy` 中：

- `ReplaceStrategy`
- `AppendStrategy`
- `MergeStrategy`

在创建 `StateGraph` 时，通过 `KeyStrategyFactory` 声明：

```java
StateGraph graph = new StateGraph("demo", () -> Map.of(
        // 每次更新都直接覆盖
        "input", new ReplaceStrategy(),

        // 把多个结果追加成一个列表
        "messages", new AppendStrategy(),

        // 对 Map / JSON 结构做合并
        "metadata", new MergeStrategy()
));
```

下面用伪代码来直观理解这三种策略。

### 5.1 ReplaceStrategy – 直接覆盖

```java
KeyStrategy replace = new ReplaceStrategy();

Object oldVal = "hello";
Object newVal = "world";

Object result = replace.apply(oldVal, newVal); // => "world"
```

- 非常简单：**无条件使用新值覆盖旧值**。
- 适用于：
  - 输入内容（`input`）
  - 最终答案（`answer`）
  - 某些不需要保留历史的字段

### 5.2 AppendStrategy – 追加列表 / 消息

`AppendStrategy` 的内部逻辑比较丰富，简化理解如下：

- 如果新值为 `null`：返回旧值。
- 如果旧值是 `List`：
  - 会尝试把新值转换为 `List`，然后**追加**到旧的列表中。
  - 支持特殊的「删除指令」类型（`AppenderChannel.RemoveIdentifier`），
    可以从列表中删除指定元素。
- 如果旧值不是 `List`：
  - 会创建一个新的 `ArrayList`，把旧值和新值都放进去返回。

示例：

```java
KeyStrategy append = new AppendStrategy();

Object oldVal = List.of("msg1");
Object newVal = "msg2";

Object result = append.apply(oldVal, newVal);
// result 大致类似于 List.of("msg1", "msg2")
```

典型使用场景：

- 对话历史（messages / history）
- 多步执行产生的「证据列表」「子问题列表」等需要保留全量记录的字段

### 5.3 MergeStrategy – 合并 Map / JSON

`MergeStrategy` 专门用来处理 Map / JSON 类的结构：

- 若新值为 `null`：返回旧值。
- 若旧值是 `Optional`：会先解包（`orElse(null)`）。
- 若旧值和新值都是 `Map`：
  - 返回一个新的 `HashMap`，先拷贝旧值，再 `putAll(newVal)`。
  - 同名 key 以新值为准。
- 若旧值不是 Map、新值是 Map：
  - 把旧值放入一个新的 Map 中，key 为 `"original"`。
  - 再把新 Map 的所有 key 放进去。
- 若旧值是 Map、新值不是 Map：
  - 拷贝旧 Map，然后添加一个 key `"additional"` 指向新值。
- 否则：返回新值。

示例：

```java
KeyStrategy merge = new MergeStrategy();

Map<String, Object> oldMeta = Map.of("a", 1, "b", 2);
Map<String, Object> newMeta = Map.of("b", 3, "c", 4);

Object result = merge.apply(oldMeta, newMeta);
// => {a=1, b=3, c=4}
```

适用于：

- 元数据（metadata）
- 多个 Agent 输出的结构化信息合并
- 复杂 JSON 结构的增量更新

---

## 6. quickStartGraph 示例代码解析

当前项目中已经提供了一个最小可运行的 Graph 示例：`quickStartGraph`。

### 6.1 GraphConfig – 定义 Graph

文件：`spring_ai_alibaba-demo/alibaba-graph/src/main/java/org/zhoubyte/alibabagraph/config/GraphConfig.java`

```java
@Configuration
public class GraphConfig {

    Logger logger = LoggerFactory.getLogger(GraphConfig.class);

    @Bean("quickStartGraph")
    public CompiledGraph quickStartGraph() throws GraphStateException {
        // 定义状态图：声明状态字段及其合并策略
        StateGraph quickStartGraph = new StateGraph("quickStartGraph", () -> {
            logger.info("quickStartGraph");
            return Map.of(
                    "input", new ReplaceStrategy(),
                    "output", new ReplaceStrategy()
            );
        });

        // 定义节点 node1
        quickStartGraph.addNode("node1", AsyncNodeAction.node_async(state -> {
            logger.info("node1 = {}", state.toString());
            // 返回要写入的状态，使用上面配置的 ReplaceStrategy 进行覆盖
            return Map.of(
                    "input", "graphConfig_addNode",
                    "output", "graphConfig_output"
            );
        }));

        // 定义节点 node2
        quickStartGraph.addNode("node2", AsyncNodeAction.node_async(state -> {
            logger.info("node2 = {}", state.toString());
            return Map.of(
                    "input", "ZhouByte",
                    "output", "EMPTY"
            );
        }));

        // 定义边：START -> node1 -> node2 -> END
        quickStartGraph.addEdge(StateGraph.START, "node1")
                .addEdge("node1", "node2")
                .addEdge("node2", StateGraph.END);

        // 编译得到可执行的 CompiledGraph
        return quickStartGraph.compile();
    }
}
```

要点说明：

- `StateGraph` 中声明了两个状态字段：`input` 和 `output`。
- `node1` 和 `node2` 都是异步节点（`AsyncNodeAction`），每个节点返回一个 Map。
- 最终的执行顺序是：`START -> node1 -> node2 -> END`。

### 6.2 GraphController – 以 WebFlux 方式暴露 Graph

文件：`spring_ai_alibaba-demo/alibaba-graph/src/main/java/org/zhoubyte/alibabagraph/config/GraphController.java`

```java
@RestController
@RequestMapping(value = "/v1")
public class GraphController {

    @Resource
    private CompiledGraph quickStartGraph;

    @GetMapping(value = "/graph")
    public Flux<String> startGraph() {
        // 使用响应式返回结果，避免在 WebFlux I/O 线程中调用 block()/blockFirst()
        return quickStartGraph.stream(Map.of())
                .map(NodeOutput::toString);
    }
}
```

说明：

- 通过 Spring 注入 `CompiledGraph quickStartGraph` Bean。
- 调用 `stream(Map.of())` 开始执行 Graph：
  - 初始输入状态为空 Map。
  - 返回值是 `Flux<NodeOutput>`，这里用 `toString()` 转成简单的字符串输出。
- 以 WebFlux（响应式）方式对外提供流式结果。

访问示例：

```bash
curl http://localhost:8087/alibaba-graph/v1/graph
```

你会看到包含 `node1`、`node2` 执行结果的输出。

---

## 7. 进阶示例：结合 ChatClient / Agent 的 Graph

在本工程的根项目中，已经通过 `spring-ai-starter-model-ollama`
配置好了本地大模型（例如 Qwen3:0.6b），并且定义了 `ChatClient`。

参考 `spring_ai_alibaba-demo/src/main/java/com/zhoubyte/spring_ai_alibaba_demo/chat/config/OllamaConfig.java`：

```java
@Configuration
public class OllamaConfig {

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatModel chatModel,
                                       BaseAdvisor memoriesAdvisor,
                                       SimpleMemories simpleMemories,
                                       ChatMemoryRepository databaseChatMemoryRepository) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(memoriesAdvisor) // 这里可以挂载记忆等 Advisor
                .build();
    }
}
```

下面是一个示意性的 Graph，它展示了如何在节点中调用 ChatClient，
把每个节点看作一个「Agent 步骤」：

```java
@Configuration
public class ChatGraphConfig {

    private final ChatClient chatClient;

    public ChatGraphConfig(@Qualifier("ollamaChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Bean
    public CompiledGraph chatGraph() throws GraphStateException {
        // 定义状态字段与策略
        StateGraph graph = new StateGraph("chatGraph", () -> Map.of(
                "input", new ReplaceStrategy(),   // 用户问题
                "analysis", new ReplaceStrategy(),// 中间分析
                "answer", new ReplaceStrategy()   // 最终回答
        ));

        // 节点 1：分析问题
        graph.addNode("analyze", AsyncNodeAction.node_async(state -> {
            String question = state.value("input", "");

            // 基于大模型做一步「思考/分析」
            String analysis = chatClient.prompt()
                    .user("请分析这个问题并给出关键步骤，不要生成最终答案：{question}")
                    .variables(Map.of("question", question))
                    .call()
                    .content();

            return Map.of("analysis", analysis);
        }));

        // 节点 2：生成最终回答（利用上一节点的分析）
        graph.addNode("answer", AsyncNodeAction.node_async(state -> {
            String question = state.value("input", "");
            String analysis = state.value("analysis", "");

            String answer = chatClient.prompt()
                    .user("""
                            用户问题：{question}
                            模型分析：{analysis}
                            请在保留分析结论的基础上，给出对用户友好的最终回答。
                            """)
                    .variables(Map.of(
                            "question", question,
                            "analysis", analysis
                    ))
                    .call()
                    .content();

            return Map.of("answer", answer);
        }));

        // 定义执行路径
        graph.addEdge(StateGraph.START, "analyze")
                .addEdge("analyze", "answer")
                .addEdge("answer", StateGraph.END);

        return graph.compile();
    }
}
```

在 Controller 中，你可以将用户输入注入到 `input` 字段后再执行：

```java
@RestController
@RequestMapping("/v1")
public class ChatGraphController {

    private final CompiledGraph chatGraph;

    public ChatGraphController(CompiledGraph chatGraph) {
        this.chatGraph = chatGraph;
    }

    @GetMapping("/chat-graph")
    public Flux<String> chatGraph(@RequestParam("q") String question) {
        Map<String, Object> input = Map.of("input", question);
        return chatGraph.stream(input).map(NodeOutput::toString);
    }
}
```

这样一个简单的「两步 Agent 流程」就完成了：

1. `analyze` 节点只负责分析问题。
2. `answer` 节点结合分析结果生成最终回答。
3. 所有中间状态都保存在 `OverAllState` 中，便于后续扩展和调试。

---

## 8. 更多高级能力概览

`spring-ai-alibaba-graph-core` 在市面资料和源码中还提供了许多进阶特性，
这里简要罗列，方便你按关键词进一步学习。

### 8.1 检查点（Checkpoint）与恢复

相关包：`com.alibaba.cloud.ai.graph.checkpoint.*`

- 核心类：`Checkpoint`, `BaseCheckpointSaver`, `SaverConfig`, `SaverEnum` 等。
- 已内置的 Saver（持久化实现）包括：
  - `MemorySaver` – 内存
  - `FileSystemSaver` – 文件系统
  - `RedisSaver` – Redis
  - `MongoSaver` – MongoDB
  - `VersionedMemorySaver` – 版本化内存 Saver

典型场景：

- 长时间运行的多步任务中间崩溃，希望能够**从上一次执行的节点继续恢复**。
- 带有「人工审核」环节的流程，等待人工确认后再继续执行。

### 8.2 Store 抽象（长期记忆 / 数据存储）

相关包：`com.alibaba.cloud.ai.graph.store.*`

- 核心接口：`Store`
- 实现类：
  - `MemoryStore`
  - `FileSystemStore`
  - `RedisStore`
  - `MongoStore`
  - `DatabaseStore`

配合 `OverAllState` 构造函数使用，可以让 Graph 把重要信息存入外部存储，
构建更长期的「记忆」或状态库。

### 8.3 流式与并行执行

相关包：`com.alibaba.cloud.ai.graph.streaming.*`

- `GraphFlux` / `ParallelGraphFlux` / `GraphFluxGenerator`
  - 用于封装对 Reactor `Flux` 的支持，包括并行执行等。
- 通过 `CompiledGraph#stream(...)` / `graphResponseStream(...)`，
  可以构建：
  - SSE / WebSocket 的流式输出
  - 多节点并行执行、动态路由等复杂场景

### 8.4 调度与重试

相关包：`com.alibaba.cloud.ai.graph.scheduling.*`

- `ScheduleConfig` – 调度配置：
  - 支持 `cronExpression`、`fixedDelay`、`fixedRate`、重试策略等。
- `ScheduledAgentTask` – 已调度任务的句柄。

配合 `CompiledGraph#schedule(ScheduleConfig)`，可以实现：

- 周期性运行的 Agent 工作流（例如每日自动报告）。
- 带有失败重试的后台任务。

### 8.5 观测与指标（Observation / Metrics）

相关包：`com.alibaba.cloud.ai.graph.observation.*`

- Graph / Node / Edge 粒度的 Observation：
  - `GraphObservationHandler`
  - `GraphNodeObservationHandler`
  - `GraphEdgeObservationHandler`
- 以及对应的 Metrics 名称、属性类：
  - `SpringAiAlibabaObservationMetricNames`
  - `SpringAiAlibabaObservationMetricAttributes`

这些可以与 Micrometer / Prometheus / 监控平台配合，用于：

- 统计 Graph 调用次数、时延、错误率
- 按节点、按 Agent 粒度分析性能瓶颈

### 8.6 图形化可视化（Mermaid / PlantUML）

`StateGraph` 与 `CompiledGraph` 均提供：

```java
GraphRepresentation graph = compiledGraph.getGraph(
        GraphRepresentation.Type.MERMAID, // 或 PLANTUML
        "myGraph",                        // 标题
        true                              // 是否包含状态信息
);

String mermaid = graph.content();
System.out.println(mermaid);
```

你可以把生成的 Mermaid 文本粘贴到支持 Mermaid 的工具中
（如 IDE 插件、在线编辑器）进行可视化，便于：

- 向团队分享 Graph 结构
- 排查复杂流程中的跳转问题

---

## 9. 学习路径建议

如果你处于学习阶段，可以按下面的顺序循序渐进：

1. **理解本项目的 quickStartGraph 示例**
   - 搭建并运行本子项目；
   - 打开 `GraphConfig` 和 `GraphController`，对照日志理解执行顺序和状态变化。
2. **增加自己的状态字段与节点**
   - 在 `StateGraph` 中加入新的 key（例如 `history`、`metadata`）；
   - 使用 `AppendStrategy` / `MergeStrategy` 观察状态合并行为。
3. **把 Graph 与 ChatClient / Agent 结合**
   - 参考上面的「进阶示例」，在节点中调用大模型；
   - 将业务拆成多步，每一步作为一个节点。
4. **尝试使用 Checkpoint / Store / Schedule 等高级能力**
   - 对于运行时间较长的任务，加上检查点与持久化；
   - 尝试安排一个按 cron 表达式周期执行的 Graph。
5. **结合市面资料深入**
   - 搜索关键词：`spring-ai-alibaba-graph-core`、`StateGraph`、
     `CompiledGraph`、`OverAllState`；
   - 查阅社区示例，看别人如何设计多 Agent 协同的 Graph。

如果你在阅读源码或扩展功能时遇到任何具体问题，可以在提问时贴出
相关 Java 代码和你期望的行为，我可以针对性帮你一起设计 Graph 结构和节点实现。

