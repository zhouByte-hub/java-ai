package com.zhoubyte.pageindex.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推理式查询结果 —— 包含答案、推理路径和引用信息
 * 推理路径是 PageIndex 区别于传统 RAG 的核心特征：可追溯、可解释
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult {

    private String query;                   // 用户原始问题

    private String answer;                  // LLM 生成的最终答案

    private List<ReasoningStep> reasoningPath;  // 推理路径（每一步的决策过程）

    private List<String> referencedNodeIds;     // 引用的节点 ID 列表

    private List<String> referencedSections;    // 引用的章节描述（含页码）

    private boolean sufficient;             // 答案是否充分回答了问题

    /** 单步推理记录 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReasoningStep {
        private int step;           // 步骤序号
        private String nodeId;      // 当前所在节点
        private String title;       // 当前节点标题
        private String reasoning;   // LLM 的推理过程
        private String decision;    // 决策：NAVIGATE / EXTRACT / STOP
    }
}
