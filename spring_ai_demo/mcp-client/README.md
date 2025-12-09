# Spring AI MCP Client 示例

## 1. Spring AI 中的 MCP 概念

MCP（Model Context Protocol）是由 Anthropic 提出的一个标准协议，用来在“大模型 ↔ 外部系统”之间传递：

- Tools（可调用的函数 / 操作）
- Resources（可访问的数据资源，如文件、配置、知识库）
- Prompts（可复用的提示模板）

在 Spring AI 中：

- `spring-ai-mcp-client` 负责作为 **MCP 客户端**，连接到一个或多个 MCP Server；
- MCP 让“提供工具与数据的一侧”和“使用模型的一侧”解耦，不再把所有 Tool 定义硬编码在应用里或某家模型厂商的 API 里。

简单理解：

- Tool Calling：告诉某个模型“你有这些函数可以调”；
- MCP：告诉任意模型/应用“这里有一个标准化的工具与资源服务（MCP Server），你可以按统一协议发现和调用它们”。

## 2. 本子项目（mcp-client）的角色

本模块演示了如何在 Spring Boot / Spring AI 应用中使用 MCP Client：

- 通过 Spring AI 的 MCP Client Boot Starter 建立到 MCP Server 的连接；
- 自动发现 MCP Server 暴露的 tools / resources / prompts；
- 在 ChatClient / Agent 的对话过程中，当模型触发 tool 调用时，由 MCP Client 负责：
  - 把 tool 调用请求转发给 MCP Server；
  - 接收 MCP Server 执行结果，并回填给模型；
  - 最终把模型生成的回答返回给业务代码。

这样，业务只需要关注“调用 ChatClient / Agent”，而不必直接处理具体的工具实现和网络协议。

## 3. 为什么要拆分 MCP Client / Server？

- **解耦**：工具实现集中放在 MCP Server 中，多个应用、多个模型可以复用同一套工具与资源。
- **跨模型厂商**：同一个 MCP Server 可以被不同 LLM 提供商（OpenAI、Anthropic、阿里通义等）的客户端使用。
- **统一治理**：对于工具的权限控制、审计、版本管理，可以在 MCP Server 层统一处理。
- **应用更轻量**：业务应用只负责对话编排，把“外部能力”委托给 MCP，而不是在每个项目里重复实现。

## 4. MCP 与 Tool Calling 的对比

### 4.1 Tool Calling（函数调用）的特点

- 工具描述紧耦合在某个模型提供商的 API（例如 OpenAI function calling）。
- 工具定义通常由当前应用提供，生命周期和应用强绑定。
- 使用简单、集成成本低，适合单体应用或简单场景。

### 4.2 MCP 的特点

- 通过统一协议暴露 tools/resources/prompts，**与模型提供商无关**。
- 工具实现集中在 MCP Server 中，可被多端、多项目共享。
- 支持工具发现、元数据获取、增量更新等更丰富的管理能力。

### 4.3 优缺点对比（简表）

| 能力                | MCP                                          | Tool Calling（传统函数调用）                         |
|---------------------|----------------------------------------------|------------------------------------------------------|
| 与模型厂商的耦合度  | 低，协议层抽象，可跨多家模型                 | 高，通常绑定某个模型 API                             |
| 工具实现位置        | 独立 MCP Server，可复用                      | 应用内部或模型侧定义，项目间难以共享                 |
| 治理与运维          | 适合集中治理（权限、审计、版本）             | 分散在各个应用之中，难以统一管理                     |
| 集成复杂度          | 需要部署 MCP Server 和 Client，架构更复杂    | 应用内直接注册工具，入门简单                         |
| 网络与性能开销      | 多一跳 MCP 调用，有额外网络/序列化开销       | 直接由应用执行工具，没有额外协议层                   |

简单总结：

- 小应用、单模型：Tool Calling 往往足够用，方便快捷；
- 多应用、多模型、需要统一治理：引入 MCP 可以复用工具资产、降低长期维护成本。

## 5. 程序 - 大模型 - MCP 的调用链路（ASCII 示意）

下面示意了在本 `mcp-client` 项目中，一次“带 MCP Tool 调用”的完整链路：

```text
  1. user / business code
        |
        v
  [Spring Boot App]
        |
        | chat(request)
        v
  [Spring AI ChatClient / Agent]
        |
        | 2. call LLM API (with MCP tools advertised)
        v
      [LLM / Model]
        |
        | 3. returns function_call (needs external tool)
        v
  [Spring AI MCP Client]
        |
        | 4. MCP protocol call (stdio / SSE / HTTP ...)
        v
      [MCP Server]
        |
        | 5. execute real tool logic
        v
  external systems / DB / vector store / HTTP APIs
        |
        | 6. tool result -> MCP Server -> MCP Client
        v
  [Spring AI ChatClient] -- 7. call LLM again (with tool result) -->
      [LLM / Model] -- 8. generate final answer -->
  [Spring Boot App] -- 9. reply to user
```

更多关于 Spring AI MCP Client 的官方文档，可以参考：

- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
