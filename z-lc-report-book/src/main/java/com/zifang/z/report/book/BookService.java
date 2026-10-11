package com.zifang.z.report.book;

import com.zifang.z.report.common.schema.BookNodeDef;

import java.util.List;

/**
 * Book 服务: 树状组织管理 + 整体发布。
 */
public interface BookService {

    /** 创建 book (根节点), 返回 bookId */
    String createBook(String title);

    /** 追加节点 (文件夹或页面), 返回节点 id */
    String addNode(String bookId, String parentId, com.zifang.z.report.common.schema.BookNodeType nodeType,
                   String title, String viewId);

    /** 删除节点 (含子树); 根节点不可删 */
    void removeNode(String bookId, String nodeId);

    /** 移动节点 (调整父节点与排序) */
    void moveNode(String bookId, String nodeId, String newParentId, int order);

    /** 整棵树 (按 order 排序的前序遍历) */
    List<BookNodeDef> tree(String bookId);

    /**
     * 整体发布: 生成当前树的不可变快照 + 分享 token。
     * 快照与后续编辑隔离 —— "像应用一样整体发布被别人看"。
     */
    PublicationSnapshot publish(String bookId);

    /** 按 publishId 取发布快照 (观看侧入口; token 不匹配/缺失抛 IllegalArgumentException) */
    PublicationSnapshot getPublication(String publishId, String token);
}
