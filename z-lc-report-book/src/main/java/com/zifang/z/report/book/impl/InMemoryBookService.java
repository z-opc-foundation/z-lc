package com.zifang.z.report.book.impl;

import com.zifang.z.report.book.BookService;
import com.zifang.z.report.book.PublicationSnapshot;
import com.zifang.z.report.common.schema.BookNodeDef;
import com.zifang.z.report.common.schema.BookNodeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存实现 (M1): ConcurrentHashMap 存节点, 发布快照深拷贝。
 * M2 落库后接口签名保持不变, 上层无感切换。
 */
public class InMemoryBookService implements BookService {

    private final Map<String, Map<String, BookNodeDef>> books = new ConcurrentHashMap<>();
    private final Map<String, PublicationSnapshot> publications = new ConcurrentHashMap<>();

    @Override
    public String createBook(String title) {
        String bookId = newId("book");
        Map<String, BookNodeDef> nodes = new HashMap<>();
        BookNodeDef root = new BookNodeDef(bookId, bookId, null, BookNodeType.FOLDER, title, 0, null);
        nodes.put(bookId, root);
        books.put(bookId, nodes);
        return bookId;
    }

    @Override
    public String addNode(String bookId, String parentId, BookNodeType nodeType, String title, String viewId) {
        Map<String, BookNodeDef> nodes = requireBook(bookId);
        if (parentId == null) {
            parentId = bookId; // 缺省挂根下
        }
        if (!nodes.containsKey(parentId)) {
            throw new IllegalArgumentException("parent node not found: " + parentId);
        }
        if (nodeType == BookNodeType.PAGE && viewId == null) {
            throw new IllegalArgumentException("PAGE node requires viewId");
        }
        int order = nodes.size();
        String id = newId(nodeType == BookNodeType.PAGE ? "page" : "folder");
        BookNodeDef node = new BookNodeDef(id, bookId, parentId, nodeType, title, order, viewId);
        nodes.put(id, node);
        return id;
    }

    @Override
    public void removeNode(String bookId, String nodeId) {
        Map<String, BookNodeDef> nodes = requireBook(bookId);
        if (nodeId.equals(bookId)) {
            throw new IllegalArgumentException("book root cannot be removed");
        }
        if (nodes.remove(nodeId) == null) {
            throw new IllegalArgumentException("node not found: " + nodeId);
        }
        // 级联删除子树
        nodes.values().removeIf(n -> isDescendant(nodes, n, nodeId));
    }

    private boolean isDescendant(Map<String, BookNodeDef> nodes, BookNodeDef n, String ancestorId) {
        String p = n.getParentId();
        while (p != null) {
            if (p.equals(ancestorId)) {
                return true;
            }
            BookNodeDef parent = nodes.get(p);
            p = parent == null ? null : parent.getParentId();
        }
        return false;
    }

    @Override
    public void moveNode(String bookId, String nodeId, String newParentId, int order) {
        Map<String, BookNodeDef> nodes = requireBook(bookId);
        BookNodeDef node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("node not found: " + nodeId);
        }
        if (newParentId == null) {
            newParentId = bookId;
        }
        if (!nodes.containsKey(newParentId)) {
            throw new IllegalArgumentException("parent node not found: " + newParentId);
        }
        if (isDescendant(nodes, node, newParentId) || nodeId.equals(newParentId)) {
            throw new IllegalArgumentException("cannot move node into its own subtree");
        }
        node.setParentId(newParentId);
        node.setOrder(order);
    }

    @Override
    public List<BookNodeDef> tree(String bookId) {
        Map<String, BookNodeDef> nodes = requireBook(bookId);
        List<BookNodeDef> all = new ArrayList<>(nodes.values());
        all.sort(Comparator.comparingInt(BookNodeDef::getOrder));
        return all;
    }

    @Override
    public PublicationSnapshot publish(String bookId) {
        List<BookNodeDef> snapshot = new ArrayList<>();
        for (BookNodeDef n : tree(bookId)) {
            BookNodeDef copy = new BookNodeDef(n.getId(), n.getBookId(), n.getParentId(),
                    n.getNodeType(), n.getTitle(), n.getOrder(), n.getViewId());
            snapshot.add(copy);
        }
        String publishId = newId("pub");
        String token = UUID.randomUUID().toString().replace("-", "");
        PublicationSnapshot pub = new PublicationSnapshot(publishId, bookId, System.currentTimeMillis(), token, snapshot);
        publications.put(publishId, pub);
        return pub;
    }

    @Override
    public PublicationSnapshot getPublication(String publishId, String token) {
        PublicationSnapshot pub = publications.get(publishId);
        if (pub == null) {
            throw new IllegalArgumentException("publication not found: " + publishId);
        }
        if (pub.getShareToken() == null || !pub.getShareToken().equals(token)) {
            throw new IllegalArgumentException("invalid share token for publication: " + publishId);
        }
        return pub;
    }

    private Map<String, BookNodeDef> requireBook(String bookId) {
        Map<String, BookNodeDef> nodes = books.get(bookId);
        if (nodes == null) {
            throw new IllegalArgumentException("book not found: " + bookId);
        }
        return nodes;
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
