# Spring AI MCP Server 示例

## 1. Spring AI 中的 MCP 概念（简要回顾）

MCP（Model Context Protocol）统一了“大模型 ↔ 外部工具 / 资源”的交互方式。协议本身不依赖某个模型厂商，主要定义了：

- 如何声明 Tools（可调用函数）、Resources（资源）和 Prompts（提示模板）；
- 客户端如何发现这些能力、发起调用、处理返回结果；
- 如何以标准方式进行传输（stdio、SSE over HTTP 等）。

在 Spring AI 中：

- `spring-ai-mcp-client`：在应用内部充当 MCP 客户端；
- `spring-ai-mcp-server`：作为 MCP Server 暴露工具与资源，实现真正的业务逻辑。

本子项目就是一个 MCP Server 的示例工程。

## 2. 本子项目（mcp-server）的角色

这个模块负责“对外暴露工具与资源”，可以被多个 MCP Client / LLM 应用复用：

- 它实现真正的业务逻辑（比如：查数据库、查向量库、调用第三方 HTTP 接口等）；
- 按照 MCP 标准描述 tools/resources/prompts，并通过传输层（stdio / SSE）对外暴露；
- 业务侧只需要通过 MCP Client 访问这个 Server，而不必关心工具的具体实现细节。

### 2.1 支持的传输方式（Spring AI 提供的实现）

- **STDIO MCP 服务器**
  - 适用于命令行和桌面工具；
  - 无需额外 Web 依赖；
  - 同时支持同步和异步服务器实现。

- **SSE WebMVC 服务器**
  - 使用 Spring MVC 的基于 HTTP 的传输（`WebMvcSseServerTransportProvider`）；
  - 自动配置 SSE 端点；
  - 可选 STDIO 传输（`spring.ai.mcp.server.stdio=true` 时启用）；
  - 依赖 `spring-boot-starter-web` 与 `mcp-spring-webmvc`。

- **SSE WebFlux 服务器**
  - 使用 Spring WebFlux 的响应式传输（`WebFluxSseServerTransportProvider`）；
  - 自动配置响应式 SSE 端点；
  - 可选 STDIO 传输（`spring.ai.mcp.server.stdio=true` 时启用）；
  - 依赖 `spring-boot-starter-webflux` 与 `mcp-spring-webflux`。

## 3. MCP Server 与传统 Tool Calling 的区别与好处

从 MCP Server 的视角来看：

- **传统 Tool Calling**
  - 工具逻辑往往“长在应用里”，每个应用都要重复实现类似的工具；
  - 不同语言 / 框架 / 模型提供商之间难以共享；
  - 很难集中做权限控制、审计记录、灰度发布等。

- **MCP Server**
  - 把工具实现集中放在一个“工具服务”里，对外以统一协议暴露；
  - 任意语言的应用、任意模型提供商，只要有 MCP Client，就能复用这些工具；
  - 容易在 Server 侧统一接入鉴权、审计、限流、监控等能力；
  - 适合团队级、组织级复用：一次实现，多处使用。

简要对比：

| 维度              | MCP Server                                         | 传统 Tool Calling                            |
|-------------------|----------------------------------------------------|----------------------------------------------|
| 工具实现位置      | 独立服务（Server），可跨项目复用                   | 通常在单个应用内实现                         |
| 语言 / 框架耦合度 | 低，协议层抽象，任意语言客户端都可接入             | 高，往往与具体应用栈强绑定                   |
| 治理能力          | 易于集中做权限、审计、限流、监控                   | 分散在各个应用里，难以统一治理               |
| 引入成本          | 需要额外部署与运维一个 Server                      | 无需独立服务，但每个应用都要维护自己的工具   |

## 4. 程序 - 大模型 - MCP 之间的调用关系（ASCII 示意）

下图从“整个系统”的角度展示了程序、LLM、MCP Client 与 MCP Server 的关系：

```text
             +--------------------+
             |  user / business   |
             |  Spring Boot app   |
             +---------+----------+
                       |
                       | chat(request)
                       v
             +--------------------+
             | Spring AI          |
             | ChatClient / Agent |
             +---------+----------+
                       |
           (1) call LLM API with tool schemas
                       |
                       v
                  +---------+
                  |  LLM    |
                  |  Model  |
                  +----+----+
                       |
           (2) function_call (needs tool)
                       |
                       v
             +--------------------+
             | Spring AI MCP      |
             | Client (in app)    |
             +---------+----------+
                       |
           (3) MCP protocol (stdio / SSE / HTTP)
                       |
                       v
             +--------------------+
             | Spring AI MCP      |
             | Server (this proj) |
             +---------+----------+
                       |
           (4) call real tools / resources
           +-----------+-----------+-----------+
           v           v           v           v
        DB / KV   Vector Store   REST APIs   Files ...

```

顺序概述：

1. 应用通过 ChatClient 向 LLM 发送对话请求，并携带从 MCP Server 获取到的 tool 描述；
2. LLM 决定调用某个 tool，返回 `function_call`；
3. 应用侧的 MCP Client 使用 MCP 协议调用本项目实现的 MCP Server；
4. MCP Server 执行具体工具逻辑（访问数据库、向量库、HTTP 服务等），返回 JSON 结果；
5. MCP Client 将工具结果回填给 LLM，LLM 生成最终回答并返回给应用。

通过这样的拆分，可以让“工具的实现与管理”集中在 MCP Server 中，而业务应用更多关注对话编排与用户体验。
