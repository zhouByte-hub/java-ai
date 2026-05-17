package com.zhoubyte.pageindex.parser;

import com.zhoubyte.pageindex.model.ParsedSection;

import java.util.List;

/**
 * 文档解析器接口 —— 将原始文档转换为 ParsedSection 列表
 * 支持 PDF、Markdown 等格式，通过工厂模式扩展
 */
public interface DocumentParser {

    /** 是否支持该文件格式 */
    boolean supports(String filePath);

    /** 解析文档，返回按标题层级划分的段落列表 */
    List<ParsedSection> parse(String filePath) throws Exception;

    /** 获取文档总页数 */
    int getTotalPages(String filePath) throws Exception;
}
