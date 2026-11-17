package com.zifang.z.lc.logic.compile;

import com.zifang.z.lc.schema.logic.FlowNode;
import com.zifang.z.lc.schema.logic.LogicFlow;
import com.zifang.z.lc.schema.model.PageSchema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 逻辑编译器：把执行态逻辑流（flowNodeMap）编译成可顺序执行、分支/子流程递归的节点数组。
 *
 * <p>编译算法：从 headAddress 出发，
 * <ol>
 *   <li>条件节点（condition/rhombCondition）：每条出边递归成 Branch，主干到此为止；</li>
 *   <li>普通逻辑节点：直接入链表；</li>
 *   <li>子流程节点：按 targetId 找 flow，从它的 headAddress 递归展开，挂到 flowNodes；</li>
 *   <li>沿第一条 next 边继续，串成线性主干。</li>
 * </ol>
 * 用 visited 集合防环（设计态禁止成环，但脏数据仍要兜住）。</p>
 */
public final class LogicCompiler {

    private LogicCompiler() {
    }

    /** 把页面某条逻辑流编译成可执行节点数组 */
    public static List<ExecutableNode> compile(PageSchema schema, String flowId) {
        LogicFlow flow = schema.getLogic().getFlows().get(flowId);
        if (flow == null || flow.getHeadAddress() == null) {
            return List.of();
        }
        return parseFlow(flow.getHeadAddress(), flow.getFlowNodeMap(), schema, new HashSet<>());
    }

    /** 直接编译一条流对象（无需 schema 查子流程时也可用于无子流程场景） */
    public static List<ExecutableNode> compile(LogicFlow flow) {
        if (flow == null || flow.getHeadAddress() == null) {
            return List.of();
        }
        PageSchema s = new PageSchema();
        s.getLogic().addFlow(flow);
        return compile(s, flow.getId());
    }

    private static List<ExecutableNode> parseFlow(String address,
                                                   Map<String, FlowNode> flowNodeMap,
                                                   PageSchema schema,
                                                   Set<String> visiting) {
        List<ExecutableNode> result = new ArrayList<>();
        if (address == null || !flowNodeMap.containsKey(address) || !visiting.add(address)) {
            return result; // 脏数据/环：安全返回
        }
        FlowNode node = flowNodeMap.get(address);
        FlowNode.Execute exec = node.getExecute();
        if (exec == null) {
            visiting.remove(address);
            return result;
        }

        if ("flow".equals(exec.getType())) {
            // 子流程：递归内联展开
            LogicFlow sub = schema.getLogic().getFlows().get(exec.getTargetId());
            List<ExecutableNode> subNodes = sub == null ? List.of()
                    : parseFlow(sub.getHeadAddress(), sub.getFlowNodeMap(), schema, visiting);
            result.add(ExecutableNode.subFlow(address, exec.getProps(), subNodes));
        } else if ("condition".equals(exec.getLogicType()) || "rhombCondition".equals(exec.getLogicType())) {
            // 条件节点：每条出边递归成条件分支
            List<ExecutableNode.Branch> branches = new ArrayList<>();
            for (FlowNode.NextEdge edge : node.getNext()) {
                List<ExecutableNode> branchNodes = parseFlow(edge.getAddress(), flowNodeMap, schema, visiting);
                branches.add(new ExecutableNode.Branch(
                        edge.getConditionType(), edge.getCondition(), branchNodes));
            }
            ExecutableNode condNode = ExecutableNode.logic(address, exec.getLogicType(), exec.getProps());
            condNode.setCondition(branches);
            result.add(condNode);
            visiting.remove(address);
            return result; // 条件节点主干到此为止，后续由分支承载
        } else {
            // 普通逻辑节点
            result.add(ExecutableNode.logic(address, exec.getLogicType(), exec.getProps()));
        }

        // 主干：只跟第一条 next
        if (!node.getNext().isEmpty()) {
            String nextAddress = node.getNext().get(0).getAddress();
            result.addAll(parseFlow(nextAddress, flowNodeMap, schema, visiting));
        }
        visiting.remove(address);
        return result;
    }
}
