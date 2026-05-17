package com.zhoubyte.pageindex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档解析后的段落 —— 保留标题、层级、页码和内容
 * 是 Parser 输出 → TreeIndexBuilder 输入的中间数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedSection {

    private String title;       // 段落标题

    private int level;          // 层级（对应 Markdown # 的数量或 PDF 章节深度）

    private int pageStart;      // 起始页码

    private int pageEnd;        // 结束页码

    private String content;     // 段落文本内容
}
