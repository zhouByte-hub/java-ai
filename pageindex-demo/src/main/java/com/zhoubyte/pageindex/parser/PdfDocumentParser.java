package com.zhoubyte.pageindex.parser;

import com.zhoubyte.pageindex.model.ParsedSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 文档解析器 —— 基于 PDFBox 3.x
 * 逐页提取文本，识别标题层级（数字编号 + 中文章节），合并跨页段落
 */
@Slf4j
public class PdfDocumentParser implements DocumentParser {

    // 匹配数字编号标题，如 "1.2.3 概述"
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(\\d+(\\.\\d+)*)\\s+(.+)$"
    );

    // 匹配中文章节标题，如 "第三章 财务分析"
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(第[一二三四五六七八九十百千]+[章节篇部])\\s+(.+)$"
    );

    @Override
    public boolean supports(String filePath) {
        return filePath != null && filePath.toLowerCase().endsWith(".pdf");
    }

    @Override
    public List<ParsedSection> parse(String filePath) throws Exception {
        List<ParsedSection> sections = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();

            // 逐页提取文本并识别标题
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();

                if (pageText.isBlank()) continue;

                // 尝试从页面文本中提取标题段落
                List<ParsedSection> pageSections = extractSections(pageText, page);
                if (pageSections.isEmpty()) {
                    // 无标题则整页作为一个段落
                    sections.add(ParsedSection.builder()
                            .title("Page " + page)
                            .level(0)
                            .pageStart(page)
                            .pageEnd(page)
                            .content(pageText)
                            .build());
                } else {
                    sections.addAll(pageSections);
                }
            }
        }
        // 合并同名跨页段落
        return mergeSections(sections);
    }

    @Override
    public int getTotalPages(String filePath) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return document.getNumberOfPages();
        }
    }

    /** 从单页文本中提取标题段落 */
    private List<ParsedSection> extractSections(String pageText, int pageNum) {
        List<ParsedSection> sections = new ArrayList<>();
        String[] lines = pageText.split("\n");

        StringBuilder currentContent = new StringBuilder();
        ParsedSection.ParsedSectionBuilder currentBuilder = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher headingMatcher = HEADING_PATTERN.matcher(trimmed);
            Matcher chapterMatcher = CHAPTER_PATTERN.matcher(trimmed);

            // 遇到标题行：保存上一个段落，开始新段落
            if (headingMatcher.matches() || chapterMatcher.matches()) {
                if (currentBuilder != null && currentContent.length() > 0) {
                    currentBuilder.content(currentContent.toString().trim());
                    sections.add(currentBuilder.build());
                }

                String title;
                int level;
                if (headingMatcher.matches()) {
                    // 通过编号中的点号数量推断层级，如 "1.2.3" → level=3
                    String numbering = headingMatcher.group(1);
                    level = numbering.split("\\.").length;
                    title = trimmed;
                } else {
                    level = 1;
                    title = trimmed;
                }

                currentBuilder = ParsedSection.builder()
                        .title(title)
                        .level(level)
                        .pageStart(pageNum)
                        .pageEnd(pageNum);
                currentContent = new StringBuilder();
            } else {
                currentContent.append(trimmed).append("\n");
            }
        }

        // 保存最后一个段落
        if (currentBuilder != null && currentContent.length() > 0) {
            currentBuilder.content(currentContent.toString().trim());
            sections.add(currentBuilder.build());
        }

        return sections;
    }

    /** 合并同名跨页段落（同一标题在相邻页出现时合并内容和页码范围） */
    private List<ParsedSection> mergeSections(List<ParsedSection> sections) {
        if (sections.size() <= 1) return sections;

        List<ParsedSection> merged = new ArrayList<>();
        ParsedSection current = sections.get(0);

        for (int i = 1; i < sections.size(); i++) {
            ParsedSection next = sections.get(i);
            if (current.getTitle().equals(next.getTitle()) && current.getLevel() == next.getLevel()) {
                current = ParsedSection.builder()
                        .title(current.getTitle())
                        .level(current.getLevel())
                        .pageStart(current.getPageStart())
                        .pageEnd(next.getPageEnd())
                        .content(current.getContent() + "\n" + next.getContent())
                        .build();
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }
}
