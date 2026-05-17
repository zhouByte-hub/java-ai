package com.zhoubyte.pageindex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PageIndex 启动类 —— 无向量、推理式 RAG 系统
 *
 * 核心思想：让 LLM 像人类专家一样阅读文档
 * - 先看目录 → 选择章节 → 逐层深入 → 提取内容
 * - 无需向量数据库、无需切块、无需 Embedding
 */
@SpringBootApplication
public class PageIndexApplication {

    public static void main(String[] args) {
        SpringApplication.run(PageIndexApplication.class, args);
    }
}
