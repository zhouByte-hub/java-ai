package com.zhoubyte.pageindex.parser;

import com.zhoubyte.pageindex.model.ParsedSection;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 文档解析器 —— 按 # 标题层级拆分段落
 * 也用于将纯文本（带 Markdown 格式）解析为段落列表
 */
@Slf4j
public class MarkdownDocumentParser implements DocumentParser {

    // 匹配 Markdown 标题，如 "## 2.1 营收概览"
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

    @Override
    public boolean supports(String filePath) {
        return filePath != null && (filePath.toLowerCase().endsWith(".md") || filePath.toLowerCase().endsWith(".markdown"));
    }

    @Override
    public List<ParsedSection> parse(String filePath) throws Exception {
        String content = Files.readString(Path.of(filePath));
        return parseContent(content);
    }

    @Override
    public int getTotalPages(String filePath) throws Exception {
        // Markdown 无真实页码，按 50 行一页估算
        String content = Files.readString(Path.of(filePath));
        long lineCount = content.lines().count();
        return (int) Math.max(1, Math.ceil(lineCount / 50.0));
    }

    /** 解析文本内容为段落列表（供 TreeIndexBuilder 直接调用） */
    public List<ParsedSection> parseContent(String content) {
        List<ParsedSection> sections = new ArrayList<>();
        String[] lines = content.split("\n");

        ParsedSection.ParsedSectionBuilder currentBuilder = null;
        StringBuilder currentContent = new StringBuilder();
        int currentLine = 0;

        for (String line : lines) {
            currentLine++;
            Matcher matcher = HEADING_PATTERN.matcher(line.trim());

            if (matcher.matches()) {
                // 遇到标题行：保存上一个段落，开始新段落
                if (currentBuilder != null && currentContent.length() > 0) {
                    currentBuilder.content(currentContent.toString().trim());
                    sections.add(currentBuilder.build());
                }

                // # 的数量决定层级
                int level = matcher.group(1).length();
                String title = matcher.group(2).trim();
                int page = Math.max(1, (int) Math.ceil(currentLine / 50.0));

                currentBuilder = ParsedSection.builder()
                        .title(title)
                        .level(level)
                        .pageStart(page)
                        .pageEnd(page);
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }

        // 保存最后一个段落
        if (currentBuilder != null && currentContent.length() > 0) {
            currentBuilder.content(currentContent.toString().trim());
            sections.add(currentBuilder.build());
        }

        // 无标题时整篇文档作为一个段落
        if (sections.isEmpty() && !content.isBlank()) {
            sections.add(ParsedSection.builder()
                    .title("Document")
                    .level(0)
                    .pageStart(1)
                    .pageEnd(1)
                    .content(content.trim())
                    .build());
        }

        return sections;
    }
}
