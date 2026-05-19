package com.zhoubyte.spring_ai_alibaba_demo.chat.config;

import com.zhoubyte.spring_ai_alibaba_demo.chat.adviser.SimpleMemories;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 使用 Spring AI 提供的 Ollama ChatModel，
 * 再通过 ChatClient 统一对外提供调用接口。
 * ChatClient本质是对 ChatModel的一层封装
 * 
 * <h3>OLTP vs OLAP 参数配置对比</h3>
 * <table border="1">
 *   <tr>
 *     <th>场景类型</th>
 *     <th>temperature</th>
 *     <th>topK</th>
 *     <th>特点</th>
 *     <th>典型应用</th>
 *   </tr>
 *   <tr>
 *     <td><b>OLTP</b></td>
 *     <td>0.1-0.3</td>
 *     <td>5-10</td>
 *     <td>快速、准确、一致、确定性输出</td>
 *     <td>订单查询、库存检查、用户验证、数据录入</td>
 *   </tr>
 *   <tr>
 *     <td><b>OLAP</b></td>
 *     <td>0.6-0.8</td>
 *     <td>30-50</td>
 *     <td>深度分析、推理能力、洞察力、创造性</td>
 *     <td>销售趋势分析、用户行为分析、预测建模、数据洞察</td>
 *   </tr>
 *   <tr>
 *     <td><b>通用对话</b></td>
 *     <td>0.7-0.8</td>
 *     <td>10-40</td>
 *     <td>平衡准确性和流畅性</td>
 *     <td>智能客服、日常对话、翻译</td>
 *   </tr>
 * </table>
 * 
 * <h3>参数配置原理</h3>
 * <ul>
 *   <li><b>temperature（温度）</b>：控制输出随机性
 *     <ul>
 *       <li>低值（0.0-0.3）：输出更确定、保守 → 适合事务性操作</li>
 *       <li>中值（0.4-0.7）：平衡创造性和一致性 → 适合分析场景</li>
 *       <li>高值（0.8-1.0）：输出更随机、有创意 → 适合创意写作</li>
 *     </ul>
 *   </li>
 *   <li><b>topK（候选词数量）</b>：限制生成步骤中的候选词
 *     <ul>
 *       <li>低值（1-10）：输出更确定、重复性高 → 适合精确输出</li>
 *       <li>中值（20-40）：平衡质量和多样性 → 适合大多数场景</li>
 *       <li>高值（50-100）：输出更多样化 → 可能降低连贯性</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>使用建议</h3>
 * <ul>
 *   <li>订单处理系统 → 使用 {@link #buildOltpChatClient(ChatModel)}</li>
 *   <li>数据分析平台 → 使用 {@link #buildOlapChatClient(ChatModel)}</li>
 *   <li>智能客服对话 → 使用 {@link #buildDefaultChatClient(ChatModel)}</li>
 * </ul>
 */
@Configuration
public class OllamaConfig {

    /**
     * 基于本地 Ollama ChatModel 创建 ChatClient。
     * ChatModel 由 spring-ai-starter-model-ollama 根据 spring.ai.ollama.* 配置自动装配。
     */
    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatModel chatModel,
                                       BaseAdvisor memoriesAdvisor,
                                       SimpleMemories simpleMemories,
                                       ChatMemoryRepository databaseChatMemoryRepository) {
        // 大模型记忆：方式一
//        ChatMemory.CONVERSATION_ID
        MessageWindowChatMemory build = MessageWindowChatMemory.builder().maxMessages(20).chatMemoryRepository(databaseChatMemoryRepository).build();

        MessageChatMemoryAdvisor chatMemoriesSessionId = MessageChatMemoryAdvisor.builder(simpleMemories)    // 使用 simpleMemories或者使用 build
                .conversationId("chat_memories_session_id").order(1).build();

        // 大模型记忆：方式二
        return ChatClient.builder(chatModel).defaultAdvisors(memoriesAdvisor).build();
    }


    /**
     * 创建带有自定义配置的 ChatClient。
     * 
     * temperature 参数说明：
     * - 作用：控制模型输出的随机性和创造性，取值范围 [0.0, 2.0]
     * - 较低值（0.0-0.3）：输出更确定、保守，适合事实性任务（如问答、代码生成）
     * - 中等值（0.4-0.7）：平衡创造性和一致性，适合对话、翻译
     * - 较高值（0.8-1.0）：输出更随机、有创意，适合创意写作、头脑风暴
     * - 推荐值：0.7-0.8（适合一般对话场景）
     * 
     * topK 参数说明：
     * - 作用：限制模型在每个生成步骤中考虑的候选词数量，取值范围 [1, 100]
     * - 较低值（1-10）：输出更确定、重复性高，可能缺乏多样性
     * - 中等值（20-40）：平衡质量和多样性，适合大多数场景
     * - 较高值（50-100）：输出更多样化，但可能降低连贯性
     * - 推荐值：40（适合一般场景），10-20（需要更精确输出时）
     * 
     * @param chatModel 自动注入的 ChatModel 实例
     * @return 配置好的 ChatClient
     */
    @Bean("defaultChatClient")
    public ChatClient buildDefaultChatClient(ChatModel chatModel) {
        OllamaChatOptions build = OllamaChatOptions.builder()
                .temperature(0.8)  // 创造性适中，适合对话场景
                .topK(10)          // 输出更精确，减少随机性
                .model("qwen 2.5b")
                .build();
        return ChatClient.builder(chatModel).defaultOptions(build).build();
    }

    /**
     * OLTP（在线事务处理）场景专用 ChatClient。
     * 
     * OLTP 场景特点：
     * - 需要快速、准确、一致的响应
     * - 事务性操作，错误成本高
     * - 需要确定性输出，避免随机性
     * - 典型应用：订单查询、库存检查、用户验证、数据录入
     * 
     * 参数配置策略：
     * - temperature: 0.1-0.3（低值）
     *   确保输出高度确定性和一致性，减少随机波动
     * - topK: 5-10（低值）
     *   限制候选词范围，提高输出准确性和可预测性
     * 
     * @param chatModel 自动注入的 ChatModel 实例
     * @return 配置好的 OLTP 场景 ChatClient
     */
    @Bean("oltpChatClient")
    public ChatClient buildOltpChatClient(ChatModel chatModel) {
        OllamaChatOptions build = OllamaChatOptions.builder()
                .temperature(0.2)  // 低温度，确保输出确定性
                .topK(5)           // 低 topK，提高准确性
                .model("qwen 2.5b")
                .build();
        return ChatClient.builder(chatModel).defaultOptions(build).build();
    }

    /**
     * OLAP（在线分析处理）场景专用 ChatClient。
     * 
     * OLAP 场景特点：
     * - 需要深度分析和推理能力
     * - 可以有多种可能的分析结果
     * - 更注重洞察力、创造性和分析深度
     * - 典型应用：销售趋势分析、用户行为分析、预测建模、数据洞察
     * 
     * 参数配置策略：
     * - temperature: 0.6-0.8（中等值）
     *   允许一定创造性，支持多样化的分析视角
     * - topK: 30-50（中等值）
     *   平衡分析质量和观点多样性，避免过于保守
     * 
     * @param chatModel 自动注入的 ChatModel 实例
     * @return 配置好的 OLAP 场景 ChatClient
     */
    @Bean("olapChatClient")
    public ChatClient buildOlapChatClient(ChatModel chatModel) {
        OllamaChatOptions build = OllamaChatOptions.builder()
                .temperature(0.7)  // 中等温度，支持分析创造性
                .topK(40)          // 中等 topK，平衡质量和多样性
                .model("qwen 2.5b")
                .build();
        return ChatClient.builder(chatModel).defaultOptions(build).build();
    }
}

