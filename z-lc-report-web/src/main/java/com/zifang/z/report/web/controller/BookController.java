package com.zifang.z.report.web.controller;

import com.zifang.z.report.book.BookService;
import com.zifang.z.report.book.PublicationSnapshot;
import com.zifang.z.report.common.schema.BookNodeDef;
import com.zifang.z.report.common.schema.BookNodeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Book API: 创建/树编排/整体发布/快照读取。
 */
@RestController
@RequestMapping("/api/book")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** 建节点请求体 */
    public static class AddNodeRequest {
        private String parentId;
        private BookNodeType nodeType;
        private String title;
        private String viewId;

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public BookNodeType getNodeType() {
            return nodeType;
        }

        public void setNodeType(BookNodeType nodeType) {
            this.nodeType = nodeType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getViewId() {
            return viewId;
        }

        public void setViewId(String viewId) {
            this.viewId = viewId;
        }
    }

    @PostMapping
    public Map<String, Object> createBook(@RequestBody Map<String, String> body) {
        String bookId = bookService.createBook(body.get("title"));
        return java.util.Collections.<String, Object>singletonMap("bookId", bookId);
    }

    @PostMapping("/{bookId}/node")
    public Map<String, Object> addNode(@PathVariable("bookId") String bookId, @RequestBody AddNodeRequest req) {
        String nodeId = bookService.addNode(bookId, req.getParentId(), req.getNodeType(), req.getTitle(), req.getViewId());
        return java.util.Collections.<String, Object>singletonMap("nodeId", nodeId);
    }

    @GetMapping("/{bookId}/tree")
    public List<BookNodeDef> tree(@PathVariable("bookId") String bookId) {
        return bookService.tree(bookId);
    }

    @PostMapping("/{bookId}/publish")
    public PublicationSnapshot publish(@PathVariable("bookId") String bookId) {
        return bookService.publish(bookId);
    }

    @GetMapping("/publication/{publishId}")
    public PublicationSnapshot publication(@PathVariable("publishId") String publishId,
                                           @org.springframework.web.bind.annotation.RequestParam("token") String token) {
        return bookService.getPublication(publishId, token);
    }
}
