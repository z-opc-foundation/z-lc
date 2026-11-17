package com.zifang.z.lc.logic.node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 逻辑节点注册表：type → 节点实现。支持外部覆盖默认实现。
 */
public class LogicNodeRegistry {

    private final Map<String, LogicNode> nodes = new LinkedHashMap<>();

    public LogicNodeRegistry register(LogicNode node) {
        nodes.put(node.type(), node);
        return this;
    }

    public LogicNode get(String type) {
        return nodes.get(type);
    }

    public boolean has(String type) {
        return nodes.containsKey(type);
    }

    public Map<String, LogicNode> all() {
        return nodes;
    }
}
