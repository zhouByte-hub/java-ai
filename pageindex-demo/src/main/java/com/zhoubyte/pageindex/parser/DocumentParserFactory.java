package com.zhoubyte.pageindex.parser;

import com.zhoubyte.pageindex.model.ParsedSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析器工厂 —— 根据文件扩展名自动选择解析器
 * 目前支持 PDF 和 Markdown，可扩展更多格式
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParserFactory() {
        this.parsers = List.of(
                new PdfDocumentParser(),
                new MarkdownDocumentParser()
        );
    }

    /** 根据文件路径选择合适的解析器 */
    public DocumentParser getParser(String filePath) {
        return parsers.stream()
                .filter(p -> p.supports(filePath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No parser found for file: " + filePath
                ));
    }

    public List<ParsedSection> parse(String filePath) throws Exception {
        DocumentParser parser = getParser(filePath);
        log.info("Parsing document: {} with parser: {}", filePath, parser.getClass().getSimpleName());
        return parser.parse(filePath);
    }

    public int getTotalPages(String filePath) throws Exception {
        DocumentParser parser = getParser(filePath);
        return parser.getTotalPages(filePath);
    }
}
