package com.zifang.z.lc.schema.logic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一条执行态逻辑流：headAddress 指向入口节点，flowNodeMap 是“地址 → 节点”的表。
 */
public class LogicFlow {

    private String id;
    private String headAddress;
    private Map<String, FlowNode> flowNodeMap = new LinkedHashMap<>();

    public LogicFlow() {
    }

    public LogicFlow(String id, String headAddress) {
        this.id = id;
        this.headAddress = headAddress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHeadAddress() {
        return headAddress;
    }

    public void setHeadAddress(String headAddress) {
        this.headAddress = headAddress;
    }

    public Map<String, FlowNode> getFlowNodeMap() {
        return flowNodeMap;
    }

    public void setFlowNodeMap(Map<String, FlowNode> flowNodeMap) {
        this.flowNodeMap = flowNodeMap == null ? new LinkedHashMap<>() : flowNodeMap;
    }

    public LogicFlow putNode(String address, FlowNode node) {
        this.flowNodeMap.put(address, node);
        return this;
    }
}
