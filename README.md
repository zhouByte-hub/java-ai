# Spring AI、Spring AI Alibaba、LangChain4j、LangGraph 差异

## 1. 项目定位与生态

- **Spring AI**
    - 由 Spring 官方维护的 LLM 集成项目（类似 Spring Data / Spring Cloud 的定位）。
    - 目标：为 Spring / Spring Boot 应用提供统一、供应商无关的 AI 接入抽象（Chat、Embedding、Image、工具调用等）。
    - 更关注“如何在 Spring 生态里用好 AI 能力”，而不是编排复杂 Agent / Chain。

- **Spring AI Alibaba**
    - 由阿里相关社区维护，在理念和 API 上与 Spring AI 高度对齐，通常视为“面向国内模型生态的增强版 Spring AI”。
    - 目标：更友好地支持中国境内常用的大模型服务（如阿里云通义等），提供本地化配置示例、文档和最佳实践。
    - 对国内网络环境、鉴权方式、地域/专有云等场景有更多封装和约定。

- **LangChain4j**
    - LangChain 的 Java 生态实现，偏“LLM 应用编排框架”，不依赖 Spring，本身就是一个通用 Java 库。
    - 目标：提供 Chain、Agent、Memory、Tool 以及对话状态管理等高级能力，用来构建复杂的 LLM 应用。
    - 可以与 Spring/Spring Boot 集成（有 Starter），但它的核心设计不依赖 Spring。

- **LangGraph**
    - 可以理解为在 LangChain / LangChain4j 之上的“图式工作流框架”，更关注对话/Agent 的有状态编排。
    - 目标：用节点（Node）和边（Edge）描述复杂对话流、决策树、多 Agent 协作等，把 LLM 应用抽象成可观测、可维护的“有向图”。
    - 在本项目中，可以把它看成是对 LangChain4j 的增强层，类似于 Spring 体系里「Spring AI Alibaba 之于 Spring AI」的定位。

## 2. 核心抽象与编程模型

- **Spring AI**
    - 核心抽象：`ChatClient` / `EmbeddingClient` / `ImageClient` 等。
    - 强调“声明式 + 配置驱动”：通过 `application.yml` 配置模型供应商和参数，然后在代码里注入客户端使用。
    - 与 Spring Boot 深度集成：自动配置、`@Bean`、环境抽象、`RestClient` 等。
    - 提供简单的工具调用 / Function Calling 支持，但不会强行引入“链式 DSL”。

- **Spring AI Alibaba**
    - 在 Spring AI 模型之上补充/增强供应商适配和 Starter，保持 Spring 风格不变。
    - 常见特征：
        - 针对国内 LLM 厂商提供 Boot Starter（依赖少量配置即可接入）。
        - 针对国内 API 的差异（签名、地域、SSE/流式协议）做适配。
    - 如果你已经熟悉 Spring AI，它基本上是“同一套编程模型 + 更多国内厂商支持”。

- **LangChain4j**
    - 核心抽象：`ChatLanguageModel`、`StreamingChatLanguageModel`、`EmbeddingModel`、`AiServices`、`Tool` 等。
    - 提供“链式 / 组合式”编程模型，用代码装配 Prompt 模板、记忆模块、工具调用、路由等。
    - Agent / Chain / Memory 一等公民，更适合需要多步骤推理、复杂工作流的场景。
    - Spring 只是它的一个集成选项，你可以在纯 Java 项目、Micronaut、Quarkus 等环境中使用。

- **LangGraph**
    - 核心抽象：`Graph`、`Node`、`State` 等，通常在节点内部使用 LangChain4j 的 Chain / Agent / Tool。
    - 编程模型：先定义状态结构，再通过“图 + 路由规则”描述对话流转，让复杂逻辑可视化、可拆分。
    - 更强调“有状态对话 + 分支/循环 + 人机协同”等高级模式，而不是单条链式调用。

## 3. 模型与供应商支持（整体趋势）

- **Spring AI**
    - 官方优先支持主流国际供应商（OpenAI、Azure OpenAI、Anthropic、Ollama 等），随后逐步扩展到其他厂商。
    - 追求统一配置和统一返回结构，便于在不同模型间切换。

- **Spring AI Alibaba**
    - 优先支持国内主流模型服务，对阿里云生态有最佳支持。
    - 为国内用户提供较好的开箱体验（示例、中文文档、演示工程等）。

- **LangChain4j**
    - 支持多家国际/国内模型，覆盖 Chat、Embedding、RAG、向量库等完整链路。
    - 对“供应商多样性 + 高级编排能力”关注度高，适合需要频繁试验不同模型 / 检索方案的团队。

- **LangGraph**
    - 模型与向量库等底层能力全部复用 LangChain4j，理论上支持 LangChain4j 能接入的所有厂商。
    - 更关注“如何把这些能力编排成可观测的工作流”，而不是新增模型适配。

## 4. 典型使用场景对比

- **如果你主要是 Spring Boot 应用，希望：**
    - 简单地在现有微服务里调用 LLM（问答、摘要、分类、简单 RAG）。
    - 利用现有的 Spring 生态（配置、监控、安全、云原生环境）。
    - 需要的是“稳定的生产级集成方式”，而不是复杂的 Agent。
    - **优先选：Spring AI；国内模型优先时可考虑 Spring AI Alibaba。**

- **如果你主要面向国内模型生态，且：**
    - 使用阿里云等国内大模型服务为主。
    - 希望官方示例、中文文档、本地化配置更完善。
    - 需要跟 Spring 保持一致风格，同时解决国内网络/环境的细节问题。
    - **优先选：Spring AI Alibaba（通常仍然基于/兼容 Spring AI 体系）。**

- **如果你需要构建复杂 LLM 应用，希望：**
    - 做 Agent、多步推理、工具调用编排、复杂 RAG 和工作流。
    - 在 Java 世界里获得与 Python LangChain 相近的能力。
    - 框架独立，可运行在非 Spring 环境甚至桌面/命令行应用中。
    - **优先选：LangChain4j，并按需与 Spring Boot 集成。**

- **如果你的应用已经比较复杂，希望：**
    - 对话中存在大量条件分支、循环、人工干预（例如：客服流转、审批流、跨系统编排）。
    - 需要把多个 Agent / 工具 / RAG 流程组织成一个整体工作流，且希望后续能可视化、可观测。
    - 希望在 LangChain4j 的基础上，获得更清晰的“状态 + 流程图”编码体验。
    - **优先选：在 LangChain4j 之上叠加 LangGraph（LangChain4j 负责能力层，LangGraph 负责流程层）。**

## 5. 总结性对比表

| 维度                     | Spring AI                         | Spring AI Alibaba                                    | LangChain4j                                       | LangGraph                                                       |
|--------------------------|-----------------------------------|------------------------------------------------------|---------------------------------------------------|-----------------------------------------------------------------|
| 核心定位                 | Spring 官方 LLM 集成              | 面向国内模型生态的 Spring AI 增强/扩展              | Java 版 LangChain，LLM 编排框架                   | 基于 LangChain4j 的图式工作流 / 有状态 Agent 编排框架          |
| 是否依赖 Spring          | 是，深度绑定 Spring/Spring Boot  | 是，延续 Spring AI 模型                             | 否（可选 Spring 集成）                            | 否，依赖 LangChain4j，可与任意 Java 应用/框架组合              |
| 编程模型                 | 客户端抽象 + 配置驱动             | 同 Spring AI，增加国内厂商适配                       | Chain / Agent / Tool / Memory 编排                | Graph / Node / State + Agent / Tool 的图式编排                 |
| 国内模型支持友好度       | 取决于具体适配器，整体在增强中    | 高，优先考虑国内主流模型和本地化场景                | 视具体适配器而定，一般覆盖但未必本地化最佳         | 跟随底层 LangChain4j，取决于具体适配器                         |
| 复杂智能体/多步骤编排    | 基础支持，非核心卖点             | 与 Spring AI 类似                                   | 核心能力，适合复杂 LLM 应用                       | 面向复杂状态机、多 Agent、多分支流程，是核心卖点               |
| 适合人群                 | Spring 后端团队                   | 使用国内云厂商、偏 Spring 体系的后端团队            | 想在 Java 中玩转 LangChain 思路的开发者           | 已经在用 LangChain4j，希望把复杂对话和流程做成可观测工作流的团队 |

> 粗略理解：
> - Spring AI：**Spring 里的“LLM 驱动层”**
> - Spring AI Alibaba：**Spring AI 在国内模型生态上的增强版**
> - LangChain4j：**Java 版 LangChain，用来编排复杂 AI 应用**
> - LangGraph：**基于 LangChain4j 的有状态 Agent / 工作流图框架**

## 6. 四个框架之间的关系

- **按生态阵营划分**
    - Spring 生态线：`Spring AI`（官方 LLM 接入层） ← `Spring AI Alibaba`（面向国内模型的增强版）。
    - LangChain 生态线（Java）：`LangChain4j`（通用编排框架） ← `LangGraph`（图式工作流 / 有状态 Agent 增强层）。

- **按层次角色划分**
    - 接入层：`Spring AI`、`Spring AI Alibaba` 主要解决“如何在 Spring 应用里接好模型、配好参数、跑在生产环境中”。
    - 编排层：`LangChain4j`、`LangGraph` 主要解决“如何把模型 + 工具 + 向量库编排成复杂的对话/工作流”。

- **增强关系（纵向类比）**
    - `Spring AI Alibaba` 之于 `Spring AI`：在保持 Spring AI 编程模型的前提下，加强国内模型厂商支持和本地化体验。
    - `LangGraph` 之于 `LangChain4j`：在保持 LangChain4j 能力的前提下，引入图式状态机，让复杂 Agent / 对话流程更易表达与维护。

- **在本仓库中的推荐使用方式**
    - 如果你是 Spring 团队，想先把 LLM 接入到现有服务：从 `Spring AI` / `Spring AI Alibaba` 入手。
    - 如果你想进一步做复杂多轮对话、Agent 编排：在模型接入稳定后，引入 `LangChain4j`，并按需叠加 `LangGraph` 做工作流编排。
