package com.zhoubyte.pageindex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档树节点 —— PageIndex 的核心数据结构
 * 每个节点对应文档中的一个章节/段落，保留原始层级关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TreeNode {

    private String nodeId;      // 节点唯一标识

    private String title;       // 章节/段落标题

    private int level;          // 层级深度（0=根, 1=章, 2=节, ...）

    private int pageStart;      // 起始页码

    private int pageEnd;        // 结束页码

    private String content;     // 原始文本内容（叶节点才有）

    private String summary;     // LLM 生成的摘要（用于推理检索）

    @Builder.Default
    private List<TreeNode> children = new ArrayList<>();  // 子节点列表

    /** 是否为叶节点（无子节点） */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    public int getChildCount() {
        return children == null ? 0 : children.size();
    }

    public void addChild(TreeNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
}
