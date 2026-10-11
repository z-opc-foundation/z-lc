package com.zifang.z.report.common.schema;

/**
 * ReportBook 树节点: book 内树状组织 (章节/页面), 类"应用"结构。
 * PAGE 节点通过 viewId 关联 ViewSchema。
 */
public class BookNodeDef {

    private String id;
    /** 所属 book 根 id; 根节点自身 parent 为 null */
    private String bookId;
    private String parentId;
    private BookNodeType nodeType;
    private String title;
    private int order;
    /** nodeType=PAGE 时的视图 schema id */
    private String viewId;

    public BookNodeDef() {
    }

    public BookNodeDef(String id, String bookId, String parentId, BookNodeType nodeType, String title, int order, String viewId) {
        this.id = id;
        this.bookId = bookId;
        this.parentId = parentId;
        this.nodeType = nodeType;
        this.title = title;
        this.order = order;
        this.viewId = viewId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }
}
