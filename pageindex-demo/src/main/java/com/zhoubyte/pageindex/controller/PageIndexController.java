package com.zhoubyte.pageindex.controller;

import com.zhoubyte.pageindex.indexer.TreeIndexBuilder;
import com.zhoubyte.pageindex.model.DocumentTree;
import com.zhoubyte.pageindex.model.QueryResult;
import com.zhoubyte.pageindex.retriever.ReasoningRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PageIndex REST API —— 提供文档索引构建和推理式查询接口
 *
 * 核心流程：
 * 1. POST /index 或 /index/text → 构建层级文档树索引
 * 2. POST /query → LLM 推理式树搜索，返回答案 + 推理路径
 * 3. GET /outline → 查看文档大纲（树结构可视化）
 */
@Slf4j
@RestController
@RequestMapping("/pageindex")
@RequiredArgsConstructor
public class PageIndexController {

    private final TreeIndexBuilder treeIndexBuilder;
    private final ReasoningRetriever reasoningRetriever;

    // 内存索引存储（生产环境可替换为持久化存储）
    private final Map<String, DocumentTree> indexStore = new ConcurrentHashMap<>();

    /** 从文件构建树索引（支持 PDF / Markdown） */
    @PostMapping("/index")
    public ResponseEntity<DocumentTree> buildIndex(@RequestParam("filePath") String filePath) {
        try {
            log.info("Building tree index for: {}", filePath);
            DocumentTree tree = treeIndexBuilder.buildTree(filePath);
            indexStore.put(tree.getDocId(), tree);
            log.info("Tree index built successfully. DocId: {}, Nodes: {}", tree.getDocId(), countNodes(tree.getRoot()));
            return ResponseEntity.ok(tree);
        } catch (Exception e) {
            log.error("Failed to build index for: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 从文本内容构建树索引（直接传入 Markdown 格式文本） */
    @PostMapping("/index/text")
    public ResponseEntity<DocumentTree> buildIndexFromText(
            @RequestParam("title") String title,
            @RequestBody String content) {
        try {
            log.info("Building tree index from text content, title: {}", title);
            DocumentTree tree = treeIndexBuilder.buildTreeFromText(title, content);
            indexStore.put(tree.getDocId(), tree);
            log.info("Tree index built from text. DocId: {}", tree.getDocId());
            return ResponseEntity.ok(tree);
        } catch (Exception e) {
            log.error("Failed to build index from text", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 推理式查询：LLM 在文档树上逐层推理导航，定位最相关内容并生成答案 */
    @PostMapping("/query")
    public ResponseEntity<QueryResult> query(
            @RequestParam("docId") String docId,
            @RequestParam("query") String query) {
        DocumentTree tree = indexStore.get(docId);
        if (tree == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            log.info("Querying docId: {}, query: {}", docId, query);
            QueryResult result = reasoningRetriever.query(tree, query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query docId: {}", docId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 获取索引详情 */
    @GetMapping("/index/{docId}")
    public ResponseEntity<DocumentTree> getIndex(@PathVariable("docId") String docId) {
        DocumentTree tree = indexStore.get(docId);
        if (tree == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tree);
    }

    /** 获取文档大纲（树结构文本表示） */
    @GetMapping("/index/{docId}/outline")
    public ResponseEntity<String> getOutline(@PathVariable("docId") String docId) {
        DocumentTree tree = indexStore.get(docId);
        if (tree == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tree.toOutlineString());
    }

    /** 列出所有已索引文档 */
    @GetMapping("/indices")
    public ResponseEntity<Map<String, String>> listIndices() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        indexStore.forEach((id, tree) ->
                summary.put(id, tree.getDocName() + " (" + tree.getTotalPages() + " pages)")
        );
        return ResponseEntity.ok(summary);
    }

    /** 删除索引 */
    @DeleteMapping("/index/{docId}")
    public ResponseEntity<Void> deleteIndex(@PathVariable("docId") String docId) {
        indexStore.remove(docId);
        return ResponseEntity.noContent().build();
    }

    /** 递归统计树节点数 */
    private int countNodes(com.zhoubyte.pageindex.model.TreeNode node) {
        int count = 1;
        if (node.getChildren() != null) {
            for (var child : node.getChildren()) {
                count += countNodes(child);
            }
        }
        return count;
    }
}
