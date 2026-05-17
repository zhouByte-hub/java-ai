package com.zhoubyte.pageindex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档树 —— 一份文档的完整层级索引
 * 类似于"为 LLM 优化的智能目录"，保留文档天然结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentTree {

    private String docId;           // 文档唯一标识

    private String docName;         // 文档名称

    private String docDescription;  // 文档描述

    private int totalPages;         // 总页数

    private TreeNode root;          // 树根节点

    private LocalDateTime createdAt;// 索引创建时间

    /** 按 nodeId 查找节点（深度优先搜索） */
    public TreeNode findNodeById(String nodeId) {
        if (root == null) return null;
        return findNodeById(root, nodeId);
    }

    private TreeNode findNodeById(TreeNode node, String nodeId) {
        if (node.getNodeId().equals(nodeId)) return node;
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                TreeNode found = findNodeById(child, nodeId);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** 树的紧凑字符串表示（含摘要） */
    public String toTreeString() {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        buildTreeString(root, 0, sb);
        return sb.toString();
    }

    private void buildTreeString(TreeNode node, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append("- [").append(node.getNodeId()).append("] ")
                .append(node.getTitle());
        if (node.getSummary() != null && !node.getSummary().isBlank()) {
            sb.append(" | ").append(node.getSummary());
        }
        sb.append("\n");
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                buildTreeString(child, depth + 1, sb);
            }
        }
    }

    /** 文档大纲字符串（用于展示和 LLM 推理输入） */
    public String toOutlineString() {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        buildOutlineString(root, 0, sb);
        return sb.toString();
    }

    private void buildOutlineString(TreeNode node, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append("[").append(node.getNodeId()).append("] ")
                .append(node.getTitle()).append("\n");
        if (node.getSummary() != null && !node.getSummary().isBlank()) {
            sb.append(indent).append("   Summary: ").append(node.getSummary()).append("\n");
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                buildOutlineString(child, depth + 1, sb);
            }
        }
    }
}
