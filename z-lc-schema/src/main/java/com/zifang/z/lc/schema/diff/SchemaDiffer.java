package com.zifang.z.lc.schema.diff;

import com.zifang.z.lc.schema.model.NodeStructure;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Schema 差异计算器：分类讨论节点、结构、属性、逻辑绑定的增删改。
 */
public final class SchemaDiffer {

    /** 这些渲染态属性不参与 diff（由运行时计算，非业务配置） */
    private static final List<String> IGNORED_PROPS = List.of("actual-width", "actual-height");

    private SchemaDiffer() {
    }

    /**
     * 计算从 old 到 new 的差异。
     */
    public static SchemaDiff diff(PageSchema oldSchema, PageSchema newSchema) {
        SchemaDiff d = new SchemaDiff();
        if (oldSchema == null || newSchema == null) {
            return d;
        }
        Map<String, SchemaNode> oldNodes = oldSchema.getElements();
        Map<String, SchemaNode> newNodes = newSchema.getElements();

        // 1. 节点增删
        for (Map.Entry<String, SchemaNode> e : newNodes.entrySet()) {
            if (!oldNodes.containsKey(e.getKey())) {
                d.getNodeAdded().add(new SchemaDiff.NodeChange(
                        e.getKey(), e.getValue().getType(), e.getValue().getProps()));
            }
        }
        for (Map.Entry<String, SchemaNode> e : oldNodes.entrySet()) {
            if (!newNodes.containsKey(e.getKey())) {
                d.getNodeRemoved().add(new SchemaDiff.NodeChange(
                        e.getKey(), e.getValue().getType(), e.getValue().getProps()));
            }
        }

        // 2. 节点类型 + 属性变更
        for (Map.Entry<String, SchemaNode> e : newNodes.entrySet()) {
            String id = e.getKey();
            SchemaNode oldNode = oldNodes.get(id);
            if (oldNode == null) {
                continue;
            }
            SchemaNode newNode = e.getValue();
            if (!Objects.equals(oldNode.getType(), newNode.getType())) {
                d.getNodeTypeChanged().add(id);
            }
            diffProps(id, oldNode, newNode, d);
        }

        // 3. 结构变更
        diffStructure(oldSchema, newSchema, d);

        // 4. 逻辑绑定变更
        if (!Objects.equals(oldSchema.getLogic().getFlowBinds(), newSchema.getLogic().getFlowBinds())) {
            d.getFlowBindChanged().addAll(newSchema.getLogic().getFlowBinds().keySet());
        }

        return d;
    }

    private static void diffProps(String nodeId, SchemaNode oldNode, SchemaNode newNode, SchemaDiff d) {
        Map<String, Object> op = oldNode.getProps();
        Map<String, Object> np = newNode.getProps();
        List<String> allKeys = new ArrayList<>(op.keySet());
        for (String k : np.keySet()) {
            if (!allKeys.contains(k)) {
                allKeys.add(k);
            }
        }
        for (String key : allKeys) {
            if (IGNORED_PROPS.contains(key)) {
                continue;
            }
            Object ov = op.get(key);
            Object nv = np.get(key);
            if (!Objects.equals(ov, nv)) {
                d.getPropChanged().put(nodeId + "::" + key, new SchemaDiff.PropChange(ov, nv));
            }
        }
    }

    private static void diffStructure(PageSchema oldSchema, PageSchema newSchema, SchemaDiff d) {
        Map<String, NodeStructure> os = oldSchema.getLayout().getStructure();
        Map<String, NodeStructure> ns = newSchema.getLayout().getStructure();
        List<String> parents = new ArrayList<>(os.keySet());
        for (String k : ns.keySet()) {
            if (!parents.contains(k)) {
                parents.add(k);
            }
        }
        for (String parent : parents) {
            NodeStructure o = os.get(parent);
            NodeStructure n = ns.get(parent);
            List<String> oc = o == null ? List.of() : o.getChildren();
            List<String> nc = n == null ? List.of() : n.getChildren();
            if (!oc.equals(nc)) {
                d.getStructureChanged().add(new SchemaDiff.StructureChange(parent, oc, nc));
            }
        }
    }
}
