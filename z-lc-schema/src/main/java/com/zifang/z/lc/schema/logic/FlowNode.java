package com.zifang.z.lc.schema.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行态逻辑流中的一个节点。
 *
 * <p>execute 描述“做什么”，next/pre 描述“和谁相连”。条件语义编码在 next 边上
 * （conditionType + condition），而不是节点上——这是设计态“是/否分支线”到运行态布尔分支的映射基础。</p>
 */
public class FlowNode {

    /** 执行描述 */
    private Execute execute;

    /** 后继边 */
    private List<NextEdge> next = new ArrayList<>();

    /** 前驱节点地址 */
    private List<String> pre = new ArrayList<>();

    public FlowNode() {
    }

    public FlowNode(Execute execute) {
        this.execute = execute;
    }

    public Execute getExecute() {
        return execute;
    }

    public void setExecute(Execute execute) {
        this.execute = execute;
    }

    public List<NextEdge> getNext() {
        return next;
    }

    public void setNext(List<NextEdge> next) {
        this.next = next == null ? new ArrayList<>() : next;
    }

    public List<String> getPre() {
        return pre;
    }

    public void setPre(List<String> pre) {
        this.pre = pre == null ? new ArrayList<>() : pre;
    }

    /** execute.type = logic（普通逻辑节点）/ flow（子流程引用） */
    public static class Execute {
        private String type = "logic";
        /** logic 类型，对应逻辑节点注册的 key，如 condition / interfaceRequest / setVariables */
        private String logicType;
        /** 子流程引用的 flow id（type=flow 时） */
        private String targetId;
        /** 节点参数 */
        private Map<String, Object> props = new LinkedHashMap<>();

        public Execute() {
        }

        public static Execute logic(String logicType, Map<String, Object> props) {
            Execute e = new Execute();
            e.type = "logic";
            e.logicType = logicType;
            if (props != null) {
                e.props = new LinkedHashMap<>(props);
            }
            return e;
        }

        public static Execute subFlow(String targetId, Map<String, Object> props) {
            Execute e = new Execute();
            e.type = "flow";
            e.targetId = targetId;
            if (props != null) {
                e.props = new LinkedHashMap<>(props);
            }
            return e;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLogicType() {
            return logicType;
        }

        public void setLogicType(String logicType) {
            this.logicType = logicType;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public Map<String, Object> getProps() {
            return props;
        }

        public void setProps(Map<String, Object> props) {
            this.props = props == null ? new LinkedHashMap<>() : new LinkedHashMap<>(props);
        }
    }

    /** 一条后继边：address + 条件类型 + 条件表达式 */
    public static class NextEdge {
        private String address;
        /** expression（默认，恒真）/ ifLine（是/否线）/ hyperValue */
        private String conditionType = "expression";
        /** trueLine / falseLine / 表达式文本 */
        private String condition = "true";

        public NextEdge() {
        }

        public NextEdge(String address) {
            this.address = address;
        }

        public NextEdge(String address, String conditionType, String condition) {
            this.address = address;
            this.conditionType = conditionType;
            this.condition = condition;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getConditionType() {
            return conditionType;
        }

        public void setConditionType(String conditionType) {
            this.conditionType = conditionType;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }
    }
}
