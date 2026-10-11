package com.zifang.z.report.book;

import com.zifang.z.report.common.schema.BookNodeDef;

import java.util.Collections;
import java.util.List;

/**
 * 发布快照: book 树的不可变副本 + 发布元信息。
 * M2 追加: 分享 token、有效期、访问权限 (SSO)。
 */
public class PublicationSnapshot {

    private final String publishId;
    private final String bookId;
    private final long publishTime;
    private final String shareToken;
    private final List<BookNodeDef> nodes;

    public PublicationSnapshot(String publishId, String bookId, long publishTime, List<BookNodeDef> nodes) {
        this(publishId, bookId, publishTime, null, nodes);
    }

    public PublicationSnapshot(String publishId, String bookId, long publishTime,
                               String shareToken, List<BookNodeDef> nodes) {
        this.publishId = publishId;
        this.bookId = bookId;
        this.publishTime = publishTime;
        this.shareToken = shareToken;
        this.nodes = nodes == null ? Collections.<BookNodeDef>emptyList() : Collections.unmodifiableList(nodes);
    }

    public String getPublishId() {
        return publishId;
    }

    public String getBookId() {
        return bookId;
    }

    public long getPublishTime() {
        return publishTime;
    }

    /** 分享 token (M2 匿名访问; SSO 接入前唯一凭证) */
    public String getShareToken() {
        return shareToken;
    }

    /** 不可变列表 */
    public List<BookNodeDef> getNodes() {
        return nodes;
    }
}
