package com.zifang.z.lc.schema.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个节点在树中的结构：普通子节点 children + 具名插槽 namedSlots。
 *
 * <p>children 是有序 id 列表；namedSlots 是“插槽名 → 有序 id 列表”，用于-tabs 的某个页签、
 * 弹窗的标题区等“按位置插入”的场景。容器类型节点才会有结构条目。</p>
 */
public class NodeStructure {

    private List<String> children = new ArrayList<>();

    private Map<String, List<String>> namedSlots = new LinkedHashMap<>();

    public NodeStructure() {
    }

    public NodeStructure(List<String> children) {
        if (children != null) {
            this.children = new ArrayList<>(children);
        }
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    public Map<String, List<String>> getNamedSlots() {
        return namedSlots;
    }

    public void setNamedSlots(Map<String, List<String>> namedSlots) {
        this.namedSlots = namedSlots == null ? new LinkedHashMap<>() : new LinkedHashMap<>(namedSlots);
    }

    public NodeStructure addChild(String id) {
        this.children.add(id);
        return this;
    }

    public NodeStructure addSlot(String slot, String id) {
        this.namedSlots.computeIfAbsent(slot, k -> new ArrayList<>()).add(id);
        return this;
    }

    /** 该结构下所有子节点 id（含具名插槽），用于遍历与引用检查 */
    public List<String> allChildIds() {
        List<String> all = new ArrayList<>(children);
        namedSlots.values().forEach(all::addAll);
        return all;
    }
}
