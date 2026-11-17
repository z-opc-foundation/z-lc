package com.zifang.z.lc.schema.util;

import com.zifang.z.lc.schema.logic.FlowNode;
import com.zifang.z.lc.schema.logic.LogicDef;
import com.zifang.z.lc.schema.logic.LogicFlow;
import com.zifang.z.lc.schema.model.NodeStructure;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema 结构校验器：校验引用完整性、树可达性、逻辑流入口等结构性约束。
 * 不做业务校验（物料属性由物料自己校验），只保证 schema 是可渲染、可执行的结构。
 */
public final class SchemaValidator {

    private SchemaValidator() {
    }

    /** 校验结果：问题列表，空列表表示通过 */
    public static class Result {
        private final List<String> problems = new ArrayList<>();

        public boolean ok() {
            return problems.isEmpty();
        }

        public List<String> getProblems() {
            return problems;
        }

        void add(String format, Object... args) {
            problems.add(String.format(format, args));
        }

        @Override
        public String toString() {
            return ok() ? "OK" : String.join("\n", problems);
        }
    }

    public static Result validate(PageSchema schema) {
        Result r = new Result();
        if (schema == null) {
            r.add("schema 为 null");
            return r;
        }
        Map<String, SchemaNode> elements = schema.getElements();
        PageSchema.Layout layout = schema.getLayout();
        Map<String, NodeStructure> structure = layout.getStructure();

        // 1. 根节点必须存在且在 elements 中
        String root = layout.getRoot();
        if (root == null || root.isEmpty()) {
            r.add("layout.root 未设置");
        } else if (!elements.containsKey(root)) {
            r.add("layout.root=%s 在 elements 中不存在", root);
        }

        // 2. 结构里引用的节点必须都在 elements 中
        Set<String> referenced = new HashSet<>();
        for (Map.Entry<String, NodeStructure> e : structure.entrySet()) {
            String parentId = e.getKey();
            if (!elements.containsKey(parentId)) {
                r.add("structure 中的容器 %s 在 elements 中不存在", parentId);
            }
            for (String childId : e.getValue().allChildIds()) {
                referenced.add(childId);
                if (!elements.containsKey(childId)) {
                    r.add("节点 %s 的子节点 %s 在 elements 中不存在", parentId, childId);
                }
            }
        }

        // 3. 节点类型必须有值
        for (Map.Entry<String, SchemaNode> e : elements.entrySet()) {
            if (e.getValue() == null || e.getValue().getType() == null || e.getValue().getType().isEmpty()) {
                r.add("节点 %s 缺少 type", e.getKey());
            }
        }

        // 4. 从 root 做可达性检查：每个非根且被引用的节点应恰好有一个父节点，且都能从 root 到达
        Set<String> visited = new HashSet<>();
        if (root != null && elements.containsKey(root)) {
            dfs(root, structure, visited, r);
        }
        for (String nodeId : elements.keySet()) {
            if (!visited.contains(nodeId)) {
                r.add("节点 %s 从根节点不可达", nodeId);
            }
        }

        // 5. 逻辑流校验
        validateLogic(schema.getLogic(), elements, r);

        return r;
    }

    private static void dfs(String id, Map<String, NodeStructure> structure, Set<String> visited, Result r) {
        if (!visited.add(id)) {
            r.add("节点 %s 在树中出现多次（存在环或多个父节点）", id);
            return;
        }
        NodeStructure ns = structure.get(id);
        if (ns != null) {
            for (String child : ns.allChildIds()) {
                dfs(child, structure, visited, r);
            }
        }
    }

    private static void validateLogic(LogicDef logic, Map<String, SchemaNode> elements, Result r) {
        if (logic == null) {
            return;
        }
        // flowBinds 引用的节点必须存在。
        // key 格式是「节点id + 分隔符 + 事件名」，两种写法都合法：
        //   - "nodeId::event"（后端早期口径）
        //   - "nodeId.event"  （设计器产出口径：PropertyPanel/LogicDrawer 的事件绑定 key）
        // 取第一个分隔符之前的片段作为 nodeId；节点 id 本身不含 '.' 或 ':'（设计器 genId 用 '-'）。
        for (String key : logic.getFlowBinds().keySet()) {
            String nodeId = key.split("::", 2)[0].split("\\.", 2)[0];
            if (!elements.containsKey(nodeId)) {
                r.add("logic.flowBinds 绑定 key=%s 引用的节点 %s 不存在", key, nodeId);
            }
        }
        // 每条流的入口节点必须存在于流内
        for (LogicFlow flow : logic.getFlows().values()) {
            if (flow.getHeadAddress() == null || !flow.getFlowNodeMap().containsKey(flow.getHeadAddress())) {
                r.add("逻辑流 %s 的 headAddress 在 flowNodeMap 中不存在", flow.getId());
            }
            // 边的目标节点必须存在
            for (FlowNode node : flow.getFlowNodeMap().values()) {
                if (node.getExecute() == null) {
                    r.add("逻辑流 %s 存在没有 execute 的节点", flow.getId());
                    continue;
                }
                for (FlowNode.NextEdge edge : node.getNext()) {
                    if (!flow.getFlowNodeMap().containsKey(edge.getAddress())) {
                        r.add("逻辑流 %s 中节点 %s 的边指向不存在的 %s",
                                flow.getId(), node.getExecute().getLogicType(), edge.getAddress());
                    }
                }
            }
        }
    }
}
