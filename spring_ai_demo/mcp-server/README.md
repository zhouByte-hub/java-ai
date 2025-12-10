# Spring AI 中的 MCP 与 McpServer

## 1. MCP（Model Context Protocol）是什么？

MCP 是一个规范“大模型 ↔ 外部能力”的开放协议。它把模型可以使用的外部能力抽象为三类对象：

- Tools：可调用的函数 / 命令，用于执行动作或查询信息；
- Resources：可读取的数据资源，如文件、配置、知识库条目等；
- Prompts：可复用的提示模板，用于统一系统提示和指令。

协议本身与具体模型厂商无关，主要定义：

- MCP Server 如何声明自己提供的 tools / resources / prompts；
- MCP Client 如何发现这些能力、发起调用、处理返回结果；
- 在不同传输方式（stdio、SSE over HTTP 等）上消息的编码和流转方式。

通过 MCP，可以用统一方式对外暴露各种工具和数据，而不依赖某一家的 LLM API 设计。

## 2. MCP 在 Spring AI 中的作用与优势

在 Spring AI 中，MCP 的核心价值是为“工具和数据”建立一个独立层：

- 对上：由 ChatClient / Agent 代表业务与模型交互；
- 对下：由 McpServer 统一对外暴露 tools / resources / prompts。

带来的优势包括：

- 解耦：业务应用与工具实现解耦，工具逻辑集中在 MCP Server 中；
- 跨模型：同一套 MCP Server 能被不同模型提供商的客户端复用；
- 易治理：权限、审计、限流、监控等可以集中在 Server 端实现；
- 资产沉淀：工具与资源从单个应用中抽离，沉淀为可复用的“能力服务”。

## 3. McpServer 的角色

`McpServer` 是 MCP 规范中的服务端实现，在 Spring AI 环境中主要负责：

- 注册并管理自己的 Tools、Resources、Prompts；
- 通过 MCP 协议对外暴露这些能力；
- 根据客户端发来的调用请求，执行实际业务逻辑并返回结果。

可以将 McpServer 理解为“面向大模型的能力网关”：

- 对上游（McpClient / LLM）呈现为一组结构化的工具与资源；
- 对下游连接数据库、向量库、REST API、文件系统等实际系统。

## 4. McpServer 中的核心概念

### 4.1 Tools

Tools 是 McpServer 暴露的“可调用函数”，常见特点：

- 每个 Tool 有唯一名称、描述以及参数 schema（通常是 JSON Schema）；
- 调用时由客户端传入参数，Server 执行后返回结构化结果；
- 非常适合：
  - 查询类操作（查数据库、查向量库）；
  - 动作类操作（创建工单、触发工作流、发送通知等）。

对于 LLM 来说，Tools 与传统的“函数调用 / tool calling”概念类似，但通过 MCP 做了标准化，使得同一 Tool 可以被多个客户端按统一协议调用。

### 4.2 Resources

Resources 代表的是“可读取的数据资源”，可以理解为一个更抽象的“文件 / 文档 / 记录”接口：

- 由 Resource 名称、标识符（id / path / 参数）以及内容类型组成；
- 客户端可以枚举可用的资源、或者按标识读取具体内容；
- 适用于：
  - 提供静态或半静态文档（说明书、规范、FAQ 等）；
  - 提供配置、知识库条目，供模型在回答时引用；
  - 暴露某些“只读视图”，避免直接开放底层数据源。

通过 Resources，McpServer 可以以受控的方式向模型开放数据，而不是直接暴露数据库或文件系统。

### 4.3 Prompts

Prompts 是 McpServer 暴露的“提示模板”：

- 可包含变量占位符，由客户端在使用时填充；
- 用于统一系统提示、角色设定、任务说明等；
- 可由多个应用共享同一套提示模板，避免拷贝粘贴。

在大型系统中，Prompts 可以作为“提示资产”集中维护，由 McpServer 提供统一访问与版本管理。

### 4.4 三者之间的关系

- Tools 更偏“动作 / 调用”，通常返回结构化结果；
- Resources 更偏“数据 / 文档”，通常返回内容或其片段；
- Prompts 更偏“指令 / 模板”，用于指导模型如何使用 Tools 与 Resources。

三者共同构成了 McpServer 对外暴露的完整能力面。

## 5. MCP 支持的传输方式与适用场景（Server 端）

Spring AI 为 McpServer 提供了多种传输实现，常见包括：

- **STDIO MCP 服务器**
  - 通过标准输入 / 标准输出与客户端通信；
  - 适合命令行工具、桌面集成（如 IDE 插件）等“本地进程”场景；
  - 无需额外 HTTP 依赖，部署简单。

- **SSE WebMVC 服务器**
  - 基于 Spring MVC 的 HTTP + SSE 实现（`WebMvcSseServerTransportProvider`）；
  - 通过 Spring Boot Web 应用对外暴露 MCP 接口；
  - 适合传统 Servlet 模型的 Web 项目，在现有微服务体系中落地。

- **SSE WebFlux 服务器**
  - 基于 Spring WebFlux 的响应式 SSE 实现（`WebFluxSseServerTransportProvider`）；
  - 更适合高并发、流式场景；
  - 适合集成在响应式 Web 应用或网关中。

McpServer 可以按需要选择一种或多种传输方式，对上仍然通过统一的 MCP 协议暴露能力。

## 6. MCP Server 与传统 Tool Calling 的对比（从服务端 / 架构视角）

在没有 MCP 的情况下，工具逻辑往往直接写在各个业务应用中，由每个应用分别暴露给对应的模型 API 使用。

### 6.1 传统 Tool Calling 的局限

- 工具实现分散在多个应用中，重复建设严重；
- 与具体语言 / 框架 / 模型 API 强绑定，难以迁移与复用；
- 权限、审计、配额等难以集中治理，只能在每个应用单独实现。

### 6.2 基于 McpServer 的架构优势

- 工具实现集中在 McpServer 中，多个应用、多个模型可以复用同一套能力；
- 协议标准化，任意语言实现的 MCP Client 都可以接入；
- 便于在 Server 层统一接入认证、审计、限流、监控等基础设施；
- 工具、资源、提示模板可以像“服务”一样演进和版本管理。

### 6.3 对比简表

| 维度              | 基于 McpServer 的 MCP 架构                       | 传统 Tool Calling（各应用自定义）                 |
|-------------------|--------------------------------------------------|--------------------------------------------------|
| 工具实现位置      | 独立服务（McpServer），可跨项目 / 团队复用       | 分散在每个应用内部                               |
| 语言 / 框架耦合度 | 低，协议层抽象，任意语言客户端都可接入           | 高，往往与具体技术栈强绑定                       |
| 权限与审计        | 易于集中实现与统一治理                           | 需在每个应用中重复开发                           |
| 演进与运维        | 统一升级与运维，影响所有接入方                   | 每个应用独立升级，整体演进成本高                 |

## 7. 典型调用链路概览

从整体上看，程序、大模型、McpClient 与 McpServer 之间的大致调用关系如下：

```text
  [业务应用 / Agent]
          |
          | 1. 通过 ChatClient 调用 LLM（携带工具描述）
          v
       [LLM / Model]
          |
          | 2. 决定调用某个 MCP Tool，返回 function_call
          v
     [McpClient / Spring AI]
          |
          | 3. 按 MCP 协议调用 McpServer（stdio / SSE / HTTP）
          v
       [McpServer]
          |
          | 4. 调用 Tools / 读取 Resources / 使用 Prompts
          v
   各类后端系统（DB / 向量库 / REST API / 文件等）
```

通过这样的拆分，McpServer 成为“大模型可用能力”的集中出口，而业务应用则更多关注对话编排与业务流程设计。

