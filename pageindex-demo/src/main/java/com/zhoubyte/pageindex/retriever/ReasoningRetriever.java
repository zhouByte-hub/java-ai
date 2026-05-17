package com.zhoubyte.pageindex.retriever;

import com.zhoubyte.pageindex.model.DocumentTree;
import com.zhoubyte.pageindex.model.QueryResult;
import com.zhoubyte.pageindex.model.TreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 推理式检索器 —— PageIndex 的核心组件之二
 * 模拟人类专家阅读文档的方式：先看目录 → 选择章节 → 逐层深入 → 提取内容
 * 对应 Python PageIndex 的 ReasoningRetriever
 *
 * 检索流程：
 * 1. 从根节点开始，将子节点摘要展示给 LLM
 * 2. LLM 推理选择最相关的子节点
 * 3. 深入选中的子节点，重复步骤 1-2
 * 4. 到达叶节点后，提取内容生成答案
 * 5. 评估答案充分性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReasoningRetriever {

    private final ChatClient.Builder chatClientBuilder;

    private static final int MAX_DEPTH = 5;
    private static final int MAX_ITERATIONS = 10;

    /** 执行推理式查询，返回答案和完整的推理路径 */
    public QueryResult query(DocumentTree tree, String query) {
        ChatClient chatClient = chatClientBuilder.build();
        List<QueryResult.ReasoningStep> reasoningPath = new ArrayList<>();
        List<TreeNode> relevantNodes = new ArrayList<>();

        TreeNode current = tree.getRoot();
        int step = 0;

        // 迭代式树导航：从根到叶
        while (current != null && step < MAX_ITERATIONS) {
            step++;

            // 到达叶节点：提取内容
            if (current.isLeaf()) {
                log.info("Step {}: Reached leaf node [{}] - {}", step, current.getNodeId(), current.getTitle());
                relevantNodes.add(current);
                reasoningPath.add(QueryResult.ReasoningStep.builder()
                        .step(step)
                        .nodeId(current.getNodeId())
                        .title(current.getTitle())
                        .reasoning("Reached leaf node containing relevant content")
                        .decision("EXTRACT")
                        .build());
                break;
            }

            // 非叶节点：让 LLM 推理选择子节点
            NavigationDecision decision = navigateNode(chatClient, query, current, step);
            reasoningPath.add(decision.toReasoningStep());

            if (decision.selectedNodeId == null) {
                log.info("Step {}: LLM decided no relevant child node found", step);
                break;
            }

            TreeNode selectedChild = findChildById(current, decision.selectedNodeId);
            if (selectedChild == null) {
                log.warn("Step {}: Selected node {} not found in children, stopping", step, decision.selectedNodeId);
                break;
            }

            current = selectedChild;
        }

        // 非叶节点但已停止导航时，将当前节点也纳入参考
        if (!current.isLeaf() && relevantNodes.isEmpty()) {
            relevantNodes.add(current);
        }

        // 基于检索到的内容生成最终答案
        String answer = generateAnswer(chatClient, query, relevantNodes);
        boolean sufficient = evaluateSufficiency(chatClient, query, answer);

        List<String> referencedNodeIds = relevantNodes.stream()
                .map(TreeNode::getNodeId)
                .toList();
        List<String> referencedSections = relevantNodes.stream()
                .map(n -> n.getTitle() + " (pp." + n.getPageStart() + "-" + n.getPageEnd() + ")")
                .toList();

        return QueryResult.builder()
                .query(query)
                .answer(answer)
                .reasoningPath(reasoningPath)
                .referencedNodeIds(referencedNodeIds)
                .referencedSections(referencedSections)
                .sufficient(sufficient)
                .build();
    }

    /**
     * 核心推理步骤：将当前节点的子节点摘要展示给 LLM，让其选择最相关的子节点
     * 这就是 PageIndex "推理式检索" 的关键 —— 用推理替代向量相似度匹配
     */
    private NavigationDecision navigateNode(ChatClient chatClient, String query, TreeNode node, int step) {
        // 构建子节点信息列表
        StringBuilder childrenInfo = new StringBuilder();
        for (int i = 0; i < node.getChildren().size(); i++) {
            TreeNode child = node.getChildren().get(i);
            childrenInfo.append(String.format(
                    "- Node ID: %s | Title: %s | Pages: %d-%d | Summary: %s\n",
                    child.getNodeId(),
                    child.getTitle(),
                    child.getPageStart(),
                    child.getPageEnd(),
                    child.getSummary() != null ? child.getSummary() : "N/A"
            ));
        }

        String prompt = """
                You are a document navigation agent. Your task is to find the most relevant section in a document tree to answer a user's question.
                
                User Question: %s
                
                Current Node: [%s] %s
                
                Available Child Nodes:
                %s
                
                Based on the user's question and the summaries of the child nodes, which child node is most likely to contain the answer?
                
                Respond in the following format:
                REASONING: <your step-by-step reasoning about which node to select and why>
                SELECTED_NODE: <the Node ID of the most relevant child node, or "NONE" if no child seems relevant>
                """.formatted(query, node.getNodeId(), node.getTitle(), childrenInfo);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseNavigationResponse(response, step, node);
        } catch (Exception e) {
            log.error("Failed to get navigation decision at step {}", step, e);
            return new NavigationDecision(step, node.getNodeId(), node.getTitle(),
                    "Error during LLM call: " + e.getMessage(), "NONE", null);
        }
    }

    /** 解析 LLM 返回的导航决策（REASONING + SELECTED_NODE 格式） */
    private NavigationDecision parseNavigationResponse(String response, int step, TreeNode currentNode) {
        String reasoning = "";
        String selectedNodeId = null;
        String decision = "SKIP";

        String[] lines = response.split("\n");
        StringBuilder reasoningBuilder = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith("REASONING:")) {
                reasoningBuilder.append(trimmed.substring("REASONING:".length()).trim());
            } else if (trimmed.toUpperCase().startsWith("SELECTED_NODE:")) {
                String nodeId = trimmed.substring("SELECTED_NODE:".length()).trim();
                if (!"NONE".equalsIgnoreCase(nodeId) && !"N/A".equalsIgnoreCase(nodeId)) {
                    selectedNodeId = nodeId;
                    decision = "NAVIGATE";
                } else {
                    decision = "STOP";
                }
            } else if (reasoningBuilder.length() > 0 && selectedNodeId == null) {
                reasoningBuilder.append(" ").append(trimmed);
            }
        }

        reasoning = reasoningBuilder.toString().isBlank() ? response : reasoningBuilder.toString();

        return new NavigationDecision(step, currentNode.getNodeId(), currentNode.getTitle(),
                reasoning, decision, selectedNodeId);
    }

    /** 基于检索到的叶节点内容，调用 LLM 生成最终答案 */
    private String generateAnswer(ChatClient chatClient, String query, List<TreeNode> relevantNodes) {
        StringBuilder context = new StringBuilder();
        for (TreeNode node : relevantNodes) {
            context.append("--- Section: ").append(node.getTitle())
                    .append(" (Pages ").append(node.getPageStart()).append("-").append(node.getPageEnd()).append(") ---\n");
            if (node.getContent() != null) {
                String content = node.getContent();
                if (content.length() > 3000) {
                    content = content.substring(0, 3000) + "\n... [content truncated]";
                }
                context.append(content).append("\n");
            }
            context.append("\n");
        }

        String prompt = """
                Based on the following document sections, please answer the user's question.
                If the answer is not found in the provided sections, say so explicitly.
                Always cite the section title and page numbers when referencing information.
                
                User Question: %s
                
                Document Sections:
                %s
                
                Answer:""".formatted(query, context);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Failed to generate answer", e);
            return "Error generating answer: " + e.getMessage();
        }
    }

    /** 评估答案是否充分回答了用户问题 */
    private boolean evaluateSufficiency(ChatClient chatClient, String query, String answer) {
        String prompt = """
                Given the following question and answer, evaluate whether the answer sufficiently addresses the question.
                Respond with only "YES" or "NO".
                
                Question: %s
                
                Answer: %s
                
                Is the answer sufficient?""".formatted(query, answer);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return response.trim().toUpperCase().startsWith("YES");
        } catch (Exception e) {
            log.warn("Failed to evaluate sufficiency, defaulting to true", e);
            return true;
        }
    }

    /** 在父节点的子节点中按 ID 查找 */
    private TreeNode findChildById(TreeNode parent, String nodeId) {
        if (parent.getChildren() == null) return null;
        return parent.getChildren().stream()
                .filter(c -> c.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /** 内部导航决策记录 */
    private record NavigationDecision(
            int step,
            String currentNodeId,
            String currentNodeTitle,
            String reasoning,
            String decision,
            String selectedNodeId
    ) {
        QueryResult.ReasoningStep toReasoningStep() {
            return QueryResult.ReasoningStep.builder()
                    .step(step)
                    .nodeId(currentNodeId)
                    .title(currentNodeTitle)
                    .reasoning(reasoning)
                    .decision(decision)
                    .build();
        }
    }
}
