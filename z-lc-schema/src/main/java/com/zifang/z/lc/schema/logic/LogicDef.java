package com.zifang.z.lc.schema.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面逻辑定义：flowBinds 把“节点事件”绑定到逻辑流，flows 存所有逻辑流。
 *
 * <p>绑定 key 形如 {@code nodeId::EventName-onEvent}（例如 button-1::onClick），
 * 事件触发时据此找到要执行的 flow。</p>
 */
public class LogicDef {

    /** 绑定 key → 逻辑流 id 列表 */
    private Map<String, List<String>> flowBinds = new LinkedHashMap<>();

    /** flow id → 逻辑流 */
    private Map<String, LogicFlow> flows = new LinkedHashMap<>();

    public LogicDef() {
    }

    public Map<String, List<String>> getFlowBinds() {
        return flowBinds;
    }

    public void setFlowBinds(Map<String, List<String>> flowBinds) {
        this.flowBinds = flowBinds == null ? new LinkedHashMap<>() : flowBinds;
    }

    public Map<String, LogicFlow> getFlows() {
        return flows;
    }

    public void setFlows(Map<String, LogicFlow> flows) {
        this.flows = flows == null ? new LinkedHashMap<>() : flows;
    }

    public LogicDef bind(String key, String flowId) {
        this.flowBinds.computeIfAbsent(key, k -> new ArrayList<>()).add(flowId);
        return this;
    }

    public LogicDef addFlow(LogicFlow flow) {
        this.flows.put(flow.getId(), flow);
        return this;
    }

    /** 设计态图数据（节点 + 连线）——仅在版本较低、尚未编译时携带 */
    private Object flowGraphConfigData;

    public Object getFlowGraphConfigData() {
        return flowGraphConfigData;
    }

    public void setFlowGraphConfigData(Object flowGraphConfigData) {
        this.flowGraphConfigData = flowGraphConfigData;
    }
}
