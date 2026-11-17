package com.zifang.z.lc.logic.compile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可执行节点树 —— 编译器（LogicCompiler）的输出，执行器（LogicExecutor）的输入。
 *
 * <p>编译策略（对应参考引擎 parseFlow 的精髓）：
 * <ul>
 *   <li>主干只跟第一条 next 边 → 线性化压平成数组；</li>
 *   <li>条件节点把每条出边递归成条件分支树（condition[].flowNodes）；</li>
 *   <li>子流程按 targetId 递归内联展开到 flowNodes。</li>
 * </ul>
 * 这样执行期只需一个简单的顺序循环，复杂分支交给递归结构承载，执行器完全不感知画布。</p>
 */
public class ExecutableNode {

    /** logic=逻辑节点 / flow=子流程 */
    private String type = "logic";
    /** 节点地址（flowNodeMap 的 key），也是上下文隔离的 key */
    private String address;
    /** 逻辑节点类型（type=logic 时） */
    private String logicType;
    /** 节点参数 */
    private Map<String, Object> props = new LinkedHashMap<>();
    /** 条件分支（条件节点）：条件类型 + 条件表达式 + 命中后执行的子树 */
    private List<Branch> condition = new ArrayList<>();
    /** 子流程内联展开的节点树（type=flow 时） */
    private List<ExecutableNode> flowNodes = new ArrayList<>();

    public static class Branch {
        private String conditionType;
        private String condition;
        private List<ExecutableNode> flowNodes = new ArrayList<>();

        public Branch() {
        }

        public Branch(String conditionType, String condition, List<ExecutableNode> flowNodes) {
            this.conditionType = conditionType;
            this.condition = condition;
            this.flowNodes = flowNodes;
        }

        public String getConditionType() { return conditionType; }
        public String getCondition() { return condition; }
        public List<ExecutableNode> getFlowNodes() { return flowNodes; }
    }

    public static ExecutableNode logic(String address, String logicType, Map<String, Object> props) {
        ExecutableNode n = new ExecutableNode();
        n.type = "logic";
        n.address = address;
        n.logicType = logicType;
        if (props != null) {
            n.props = new LinkedHashMap<>(props);
        }
        return n;
    }

    public static ExecutableNode subFlow(String address, Map<String, Object> props, List<ExecutableNode> nodes) {
        ExecutableNode n = new ExecutableNode();
        n.type = "flow";
        n.address = address;
        if (props != null) {
            n.props = new LinkedHashMap<>(props);
        }
        n.flowNodes = nodes;
        return n;
    }

    public String getType() { return type; }
    public String getAddress() { return address; }
    public String getLogicType() { return logicType; }
    public Map<String, Object> getProps() { return props; }
    public List<Branch> getCondition() { return condition; }
    public List<ExecutableNode> getFlowNodes() { return flowNodes; }

    public void setCondition(List<Branch> condition) { this.condition = condition; }

    public boolean isConditionNode() {
        return "condition".equals(logicType) || "rhombCondition".equals(logicType);
    }
}
