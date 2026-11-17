package com.zifang.z.lc.schema.diff;

import com.zifang.z.lc.schema.model.NodeStructure;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 三方 Schema 合并：以业务侧 schema 为基线，把平台侧的新增/修改合并进来，
 * **业务侧改动优先**——当平台与业务改了同一处，保留业务侧的值。
 *
 * <p>典型场景：平台升级了公共模板（base → platform），业务在此模板上做过定制（base → business），
 * 合并后既要拿到平台的新能力，又不能冲掉业务的定制。</p>
 */
public final class SchemaMerger {

    private SchemaMerger() {
    }

    /**
     * @param baseSchema     共同基线（旧平台 schema）
     * @param platformSchema 新平台 schema（平台侧改动）
     * @param businessSchema 业务 schema（业务侧改动，作为合并基线）
     * @return 合并后的新 schema
     */
    public static PageSchema merge(PageSchema baseSchema, PageSchema platformSchema, PageSchema businessSchema) {
        SchemaDiff platformDiff = SchemaDiffer.diff(baseSchema, platformSchema);
        SchemaDiff businessDiff = SchemaDiffer.diff(baseSchema, businessSchema);

        // 以业务侧为基线深拷贝一份结果
        PageSchema result = copyOf(businessSchema);

        // 1. 平台新增的节点：业务侧没有，直接补进来（业务若删了这个节点，则尊重业务删除）
        for (SchemaDiff.NodeChange add : platformDiff.getNodeAdded()) {
            if (!result.getElements().containsKey(add.getNodeId())
                    && !wasRemovedByBusiness(add.getNodeId(), businessDiff)) {
                result.getElements().put(add.getNodeId(),
                        new SchemaNode(add.getType(), add.getProps()));
            }
        }

        // 2. 平台删除的节点：业务若未改它，跟随平台删除；业务改了则保留
        for (SchemaDiff.NodeChange remove : platformDiff.getNodeRemoved()) {
            if (result.getElements().containsKey(remove.getNodeId())
                    && !wasTouchedByBusiness(remove.getNodeId(), businessDiff)) {
                result.getElements().remove(remove.getNodeId());
            }
        }

        // 3. 属性合并：平台改的属性，业务没动过的才合并进来（业务优先）
        for (Map.Entry<String, SchemaDiff.PropChange> e : platformDiff.getPropChanged().entrySet()) {
            String key = e.getKey();
            if (businessDiff.getPropChanged().containsKey(key)) {
                continue; // 业务也改了同一属性 → 业务优先
            }
            String nodeId = key.split("::")[0];
            String propName = key.substring(key.indexOf("::") + 2);
            SchemaNode node = result.getElements().get(nodeId);
            if (node != null) {
                node.getProps().put(propName, e.getValue().getNewValue());
            }
        }

        // 4. 结构合并：平台调整了某容器的 children，业务未调整则跟随平台
        for (SchemaDiff.StructureChange sc : platformDiff.getStructureChanged()) {
            boolean businessTouched = businessDiff.getStructureChanged().stream()
                    .anyMatch(b -> b.getParentId().equals(sc.getParentId()));
            if (!businessTouched) {
                NodeStructure ns = result.getLayout().structureOf(sc.getParentId());
                ns.setChildren(new ArrayList<>(sc.getAfter()));
            }
        }

        return result;
    }

    private static boolean wasRemovedByBusiness(String nodeId, SchemaDiff businessDiff) {
        return businessDiff.getNodeRemoved().stream().anyMatch(n -> n.getNodeId().equals(nodeId));
    }

    private static boolean wasTouchedByBusiness(String nodeId, SchemaDiff businessDiff) {
        if (wasRemovedByBusiness(nodeId, businessDiff)) {
            return true;
        }
        return businessDiff.getPropChanged().keySet().stream()
                .anyMatch(k -> k.startsWith(nodeId + "::"));
    }

    /** 浅拷贝 + 关键可变容器复制，避免合并污染输入 */
    private static PageSchema copyOf(PageSchema src) {
        PageSchema copy = new PageSchema();
        copy.setId(src.getId());
        copy.setVersion(src.getVersion());
        copy.setMeta(new LinkedHashMap<>(src.getMeta()));
        for (Map.Entry<String, SchemaNode> e : src.getElements().entrySet()) {
            copy.addElement(e.getKey(), new SchemaNode(e.getValue().getType(), e.getValue().getProps()));
        }
        PageSchema.Layout layout = new PageSchema.Layout();
        layout.setRoot(src.getLayout().getRoot());
        for (Map.Entry<String, NodeStructure> e : src.getLayout().getStructure().entrySet()) {
            NodeStructure ns = new NodeStructure();
            ns.setChildren(new ArrayList<>(e.getValue().getChildren()));
            Map<String, java.util.List<String>> slots = new LinkedHashMap<>();
            e.getValue().getNamedSlots().forEach((k, v) -> slots.put(k, new ArrayList<>(v)));
            ns.setNamedSlots(slots);
            layout.getStructure().put(e.getKey(), ns);
        }
        copy.setLayout(layout);
        copy.setImports(new ArrayList<>(src.getImports()));
        copy.setContext(src.getContext());
        copy.setLogic(src.getLogic());
        return copy;
    }
}
