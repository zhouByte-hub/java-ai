# Spring AI 中的 MCP 与 McpClient

## 1. MCP（Model Context Protocol）是什么？

MCP 是 Anthropic 提出的一个开放协议，用来规范“大模型 ↔ 外部世界”的交互。它把模型可用的外部能力抽象成三类对象：

- Tools：可调用的函数 / 命令，用来执行动作或查询信息；
- Resources：可读取的数据资源，如文件、配置、知识库条目等；
- Prompts：可复用的提示模板，用于统一指令和系统提示。

MCP 规定了：

- 客户端如何发现一个 MCP Server 暴露了哪些 tools / resources / prompts；
- 调用这些能力时，请求与响应的结构是什么；
- 在不同传输方式（stdin/stdout、SSE over HTTP 等）上如何编码这些消息。

简单来说：MCP 把“给模型提供工具和数据”这件事标准化了。

## 2. MCP 在 Spring AI 里的作用与好处

在 Spring AI 体系中，MCP 的目标是：

- 把“模型调用逻辑”（ChatClient / Agent 等）与“工具和数据的实现”解耦；
- 让同一套工具 / 资源可以被多个应用、多个模型提供商复用；
- 降低应用升级或切换模型时的改造成本。

引入 MCP 的主要好处包括：

- 解耦：业务应用只负责对话编排与业务逻辑，工具实现集中在 MCP Server 中；
- 跨模型：同一个 MCP Server 可以被不同 LLM（OpenAI、Anthropic、阿里通义等）的客户端调用；
- 统一治理：权限控制、审计、灰度发布等可以在 MCP Server 层统一处理；
- 长期可维护：工具资产沉淀在一个独立层，而不是散落在每个应用里。

## 3. McpClient 在 Spring AI 中扮演什么角色？

`McpClient` 是 Spring AI 中的 MCP 客户端组件，主要负责两类能力。

**能力发现与描述：**

- 连接一个或多个 MCP Server；
- 获取并缓存这些 Server 暴露的 tools、resources、prompts 的元数据；
- 把这些能力暴露给 Spring AI 的 ChatClient / Agent，让模型知道“有哪些工具可用”。

**工具调用代理：**

- 当模型在对话中触发某个 tool 调用时，McpClient 会根据 MCP 协议把调用请求转发给对应的 MCP Server；
- 接收 MCP Server 的执行结果，并以标准结构回传给模型；
- 可同时支持不同传输方式（如 stdio、SSE over HTTP），对上层应用透明。

对业务开发者来说：

- 只需要使用 Spring AI 的 ChatClient / Agent，就可以间接使用所有 MCP Server 暴露的工具与资源；
- 不必在应用中直接维护复杂的工具实现或协议细节。

## 4. MCP 支持的传输方式与典型使用场景

Spring AI 的 MCP 实现支持多种传输方式，常见包括：

- **STDIO 传输**
  - 通过标准输入 / 标准输出进行通信；
  - 适合命令行工具、桌面 IDE 插件等“本地进程内”场景；
  - 配置简单，不需要额外的 HTTP 服务。

- **SSE（Server-Sent Events）over HTTP**
  - 通过 HTTP + SSE 进行流式通信；
  - 分为基于 Spring MVC 的 WebMvcSse 实现和基于 Spring WebFlux 的 WebFluxSse 实现；
  - 适合微服务架构、跨进程 / 跨机器的 Server 部署；
  - 便于和现有的 Spring Boot Web 应用一起运行和运维。

McpClient 可以根据配置选择不同的传输 provider，连接到本地或远程的 MCP Server，以适应不同部署环境。

## 5. MCP 与传统 Tool Calling 的对比（从客户端视角）

在没有 MCP 的情况下，常见做法是直接使用“模型厂商提供的函数调用 / tool calling 功能”，在应用里注册工具描述。

### 5.1 传统 Tool Calling 的特点

- 工具 schema 往往与某一家模型厂商的 API 强绑定（例如 OpenAI Functions / Tools）；
- 工具定义通常写在当前应用代码中，项目之间难以共享；
- 集成门槛低、上手快，适合小型或单体应用。

### 5.2 MCP 的特点（客户端 + 协议层）

- 工具描述与实现从应用中抽离，集中在 MCP Server；
- 协议层对模型厂商相对中立，同一个 MCP Server 可以被多个 LLM 客户端重用；
- 客户端（McpClient）只需要实现一次协议与传输，便可接入多种工具和资源。

### 5.3 优缺点对比简表

| 维度                  | MCP（通过 McpClient 访问）                    | 传统 Tool Calling（直接在应用里注册）            |
|-----------------------|-----------------------------------------------|--------------------------------------------------|
| 与模型厂商的耦合度    | 低，协议抽象，可换不同 LLM                   | 高，往往与某个模型 API 强绑定                    |
| 工具定义与实现位置    | 集中在 MCP Server，可被多项目复用            | 分散在各个应用源代码中                           |
| 统一治理与审计        | 易于在 MCP Server 层集中实现                 | 需要在每个应用里重复实现                         |
| 接入 / 维护复杂度     | 需要部署 MCP Server 并配置 McpClient         | 初始简单，但项目多了以后整体维护成本较高        |

综合来看：

- 小团队 / 单应用 / 单模型：直接 Tool Calling 更轻量；
- 多应用 / 多模型 / 需要统一治理：通过 McpClient 接入 MCP Server 更适合长期演进。

## 6. 常见问题记录：McpClient 启动失败（Client failed to initialize）

### 6.1 问题现象

`mcp-client` 启动时报错，大量 Bean 创建失败，最内层异常类似：

- `java.lang.RuntimeException: Client failed to initialize by explicit API call`
- `TimeoutException: Did not observe any item or terminal signal within 20000ms in 'map'`

日志堆栈中可以看到是 `McpSyncClient.initialize(...)` 超时导致，最终 Spring Boot ApplicationContext 启动失败。

### 6.2 根因分析

1. `mcp-client` 通过 STDIO 方式自动拉起 MCP Server，配置在 `application.yaml` 和 `mcp-server.json` 中：
   - `spring.ai.mcp.client.stdio.servers-configuration=classpath:mcp-server.json`
   - `mcp-server.json` 里使用绝对路径执行：`/spring_ai_demo/mcp-server/target/mcp-server-0.0.1-SNAPSHOT.jar`
2. 初始状态下：
   - `mcp-server` 模块尚未打包，`target/mcp-server-0.0.1-SNAPSHOT.jar` 不存在；
   - 同时 `spring_ai_demo/mcp-server/pom.xml` 中配置 `java.version=21`，但本地 Maven/JDK 环境只支持到 17，导致 `mvn package` 报错：
     - `错误: 不支持发行版本 21`
3. MCP Client 在启动时尝试拉起 MCP Server 但失败，始终收不到初始化响应，直到 20 秒超时（`request-timeout: 20s`），抛出上述超时异常。

### 6.3 解决方案

1. 修改 MCP Server 的 Java 版本，使其与运行环境一致（本项目使用 JDK 17）：

   文件：`spring_ai_demo/mcp-server/pom.xml`

   ```xml
   <properties>
       <java.version>17</java.version>
       <spring-ai.version>1.1.2</spring-ai.version>
   </properties>
   ```

2. 在 `spring_ai_demo/mcp-server` 目录执行打包（可跳过测试）：

   ```bash
   mvn -DskipTests package
   ```

   确认生成文件：`spring_ai_demo/mcp-server/target/mcp-server-0.0.1-SNAPSHOT.jar`。

3. 再次启动 `mcp-client`（例如在 `spring_ai_demo/mcp-client` 下运行 `McpClientApplication` 或执行）：

   ```bash
   mvn -DskipTests spring-boot:run
   ```

   日志中应能看到：
   - STDIO Client 成功启动 MCP Server；
   - `LifecycleInitializer` 收到 server 的协议和 capabilities；
   - Netty Web 服务器正常启动（例如 `Netty started on port 8084`）。

### 6.4 经验建议

- 先确保 MCP Server 能成功打包并本地运行，再启动 MCP Client；
- 如果调整了项目路径，记得同步更新 `mcp-server.json` 中 `-jar` 的路径（或改成相对路径）；
- 遇到 `Client failed to initialize` 或初始化超时时，优先检查：
  - MCP Server jar 是否存在且可执行；
  - `java.version` 是否与实际 JDK 版本匹配；
  - `request-timeout` 是否过短（默认本例为 20 秒）。
