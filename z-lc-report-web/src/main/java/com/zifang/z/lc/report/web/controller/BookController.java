package com.zifang.z.lc.report.web.controller;

import com.zifang.z.lc.report.book.BookService;
import com.zifang.z.lc.report.book.PublicationSnapshot;
import com.zifang.z.lc.report.common.schema.BookNodeDef;
import com.zifang.z.lc.report.common.schema.BookNodeType;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
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
@RequestMapping("/api/lc/report/book")
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
    @BizApi(summary = "createBook", description = "", tags = { "z-lc" })
    public Map<String, Object> createBook(@RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String bookId = bookService.createBook(body.get("title"));
        return java.util.Collections.<String, Object>singletonMap("bookId", bookId);
    }

    @PostMapping("/{bookId}/node")
    @BizApi(summary = "addNode", description = "", tags = { "z-lc" })
    public Map<String, Object> addNode(@PathVariable("bookId") @BizParam(name = "bookId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String bookId, @RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) AddNodeRequest req) {
        String nodeId = bookService.addNode(bookId, req.getParentId(), req.getNodeType(), req.getTitle(), req.getViewId());
        return java.util.Collections.<String, Object>singletonMap("nodeId", nodeId);
    }

    @GetMapping("/{bookId}/tree")
    @BizApi(summary = "tree", description = "", tags = { "z-lc" })
    public List<BookNodeDef> tree(@PathVariable("bookId") @BizParam(name = "bookId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String bookId) {
        return bookService.tree(bookId);
    }

    @PostMapping("/{bookId}/publish")
    @BizApi(summary = "publish", description = "", tags = { "z-lc" })
    public PublicationSnapshot publish(@PathVariable("bookId") @BizParam(name = "bookId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String bookId) {
        return bookService.publish(bookId);
    }

    @GetMapping("/publication/{publishId}")
    @BizApi(summary = "publication", description = "", tags = { "z-lc" })
    public PublicationSnapshot publication(@PathVariable("publishId") @BizParam(name = "publishId", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String publishId,
                                           @org.springframework.web.bind.annotation.RequestParam("token")
                                           @BizParam(name = "token", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String token) {
        return bookService.getPublication(publishId, token);
    }
}



