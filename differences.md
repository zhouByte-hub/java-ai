# Spring AI、Spring AI Alibaba、LangChain4j 差异

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

## 5. 总结性对比表

| 维度                     | Spring AI                         | Spring AI Alibaba                                    | LangChain4j                                       |
|--------------------------|-----------------------------------|------------------------------------------------------|---------------------------------------------------|
| 核心定位                 | Spring 官方 LLM 集成              | 面向国内模型生态的 Spring AI 增强/扩展              | Java 版 LangChain，LLM 编排框架                   |
| 是否依赖 Spring          | 是，深度绑定 Spring/Spring Boot  | 是，延续 Spring AI 模型                             | 否（可选 Spring 集成）                            |
| 编程模型                 | 客户端抽象 + 配置驱动             | 同 Spring AI，增加国内厂商适配                       | Chain / Agent / Tool / Memory 编排                |
| 国内模型支持友好度       | 取决于具体适配器，整体在增强中    | 高，优先考虑国内主流模型和本地化场景                | 视具体适配器而定，一般覆盖但未必本地化最佳         |
| 复杂智能体/多步骤编排    | 基础支持，非核心卖点             | 与 Spring AI 类似                                   | 核心能力，适合复杂 LLM 应用                       |
| 适合人群                 | Spring 后端团队                   | 使用国内云厂商、偏 Spring 体系的后端团队            | 想在 Java 中玩转 LangChain 思路的开发者           |

> 粗略理解：  
> - Spring AI：**Spring 里的“LLM 驱动层”**  
> - Spring AI Alibaba：**Spring AI 在国内模型生态上的增强版**  
> - LangChain4j：**Java 版 LangChain，用来编排复杂 AI 应用**

