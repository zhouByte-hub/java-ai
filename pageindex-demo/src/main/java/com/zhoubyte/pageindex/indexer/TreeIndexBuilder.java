package com.zhoubyte.pageindex.indexer;

import com.zhoubyte.pageindex.model.DocumentTree;
import com.zhoubyte.pageindex.model.ParsedSection;
import com.zhoubyte.pageindex.model.TreeNode;
import com.zhoubyte.pageindex.parser.DocumentParserFactory;
import com.zhoubyte.pageindex.parser.MarkdownDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 树索引构建器 —— PageIndex 的核心组件之一
 * 将解析后的段落列表构建为层级文档树，并使用 LLM 为每个节点生成摘要
 * 对应 Python PageIndex 的 TreeIndexer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TreeIndexBuilder {

    private final DocumentParserFactory parserFactory;
    private final ChatClient.Builder chatClientBuilder;

    private final MarkdownDocumentParser markdownParser = new MarkdownDocumentParser();

    /** 从文件构建文档树：解析 → 构建层级 → 生成摘要 */
    public DocumentTree buildTree(String filePath) throws Exception {
        List<ParsedSection> sections = parserFactory.parse(filePath);
        int totalPages = parserFactory.getTotalPages(filePath);

        String docName = extractFileName(filePath);
        String docDescription = generateDocDescription(sections);

        TreeNode root = buildHierarchy(sections);
        generateSummaries(root);

        return DocumentTree.builder()
                .docId(UUID.randomUUID().toString())
                .docName(docName)
                .docDescription(docDescription)
                .totalPages(totalPages)
                .root(root)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** 从文本内容构建文档树（无需文件，直接传入 Markdown 格式文本） */
    public DocumentTree buildTreeFromText(String title, String content) throws Exception {
        List<ParsedSection> sections = markdownParser.parseContent(content);

        TreeNode root = buildHierarchy(sections);
        generateSummaries(root);

        return DocumentTree.builder()
                .docId(UUID.randomUUID().toString())
                .docName(title)
                .docDescription("Text document: " + title)
                .totalPages(1)
                .root(root)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 将扁平的段落列表构建为层级树
     * 使用栈维护当前路径，遇到更深层级时入栈，遇到更浅层级时出栈
     */
    private TreeNode buildHierarchy(List<ParsedSection> sections) {
        if (sections.isEmpty()) {
            return TreeNode.builder()
                    .nodeId("0")
                    .title("Empty Document")
                    .level(0)
                    .pageStart(1)
                    .pageEnd(1)
                    .content("")
                    .build();
        }

        // 创建虚拟根节点
        TreeNode root = TreeNode.builder()
                .nodeId("0")
                .title("Document Root")
                .level(0)
                .pageStart(1)
                .pageEnd(1)
                .build();

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        int nodeCounter = 1;

        for (ParsedSection section : sections) {
            TreeNode node = TreeNode.builder()
                    .nodeId(String.valueOf(nodeCounter++))
                    .title(section.getTitle())
                    .level(section.getLevel())
                    .pageStart(section.getPageStart())
                    .pageEnd(section.getPageEnd())
                    .content(section.getContent())
                    .build();

            // 弹出栈中层级 >= 当前段落的节点，找到合适的父节点
            while (stack.size() > 1 && stack.peek().getLevel() >= section.getLevel()) {
                stack.pop();
            }

            stack.peek().addChild(node);
            stack.push(node);
        }

        root.setPageEnd(findMaxPage(root));
        return root;
    }

    /** 递归查找树中的最大页码 */
    private int findMaxPage(TreeNode node) {
        int max = node.getPageEnd();
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                max = Math.max(max, findMaxPage(child));
            }
        }
        return max;
    }

    /** 为整棵树生成摘要（先叶节点后父节点，父节点摘要由子节点摘要聚合） */
    private void generateSummaries(TreeNode root) {
        ChatClient chatClient = chatClientBuilder.build();
        generateSummariesRecursive(root, chatClient);
    }

    private void generateSummariesRecursive(TreeNode node, ChatClient chatClient) {
        // 叶节点：用 LLM 根据内容生成摘要
        if (node.getContent() != null && !node.getContent().isBlank()) {
            String summary = generateSummary(chatClient, node.getTitle(), node.getContent());
            node.setSummary(summary);
            log.debug("Generated summary for node [{}]: {}", node.getNodeId(), summary);
        }

        // 递归处理子节点
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                generateSummariesRecursive(child, chatClient);
            }
        }

        // 非叶节点且无摘要：由子节点摘要聚合生成
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            if (node.getSummary() == null || node.getSummary().isBlank()) {
                String childrenSummary = buildChildrenSummary(node);
                node.setSummary(childrenSummary);
            }
        }
    }

    /** 调用 LLM 为单个段落生成 2-3 句摘要 */
    private String generateSummary(ChatClient chatClient, String title, String content) {
        String truncatedContent = content.length() > 2000 ? content.substring(0, 2000) : content;

        String prompt = """
                Please provide a concise summary (2-3 sentences) for the following document section.
                The summary should capture the key information and help a reader decide if this section is relevant to their question.
                
                Section Title: %s
                
                Section Content:
                %s
                
                Summary:""".formatted(title, truncatedContent);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Failed to generate summary for section '{}', using truncated content", title, e);
            return truncatedContent.length() > 200 ? truncatedContent.substring(0, 200) + "..." : truncatedContent;
        }
    }

    /** 由子节点摘要聚合为父节点摘要 */
    private String buildChildrenSummary(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contains sections: ");
        for (int i = 0; i < node.getChildren().size(); i++) {
            TreeNode child = node.getChildren().get(i);
            if (i > 0) sb.append("; ");
            sb.append(child.getTitle());
            if (child.getSummary() != null && !child.getSummary().isBlank()) {
                sb.append(" - ").append(child.getSummary());
            }
        }
        return sb.toString();
    }

    /** 生成文档描述（取前 5 个段落标题） */
    private String generateDocDescription(List<ParsedSection> sections) {
        StringBuilder sb = new StringBuilder("Document with ");
        sb.append(sections.size()).append(" sections");
        if (!sections.isEmpty()) {
            sb.append(": ");
            int count = 0;
            for (ParsedSection section : sections) {
                if (count >= 5) {
                    sb.append("...");
                    break;
                }
                if (count > 0) sb.append(", ");
                sb.append(section.getTitle());
                count++;
            }
        }
        return sb.toString();
    }

    private String extractFileName(String filePath) {
        int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;
    }
}
