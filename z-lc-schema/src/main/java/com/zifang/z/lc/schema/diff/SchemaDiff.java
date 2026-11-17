package com.zifang.z.lc.schema.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化 Schema 差异：按 DOM（节点/结构/属性）与 LOGIC（流/节点）分类。
 *
 * <p>diff 是三方合并的基础。它描述的是“从 old 到 new 发生了什么”，
 * 而不是简单的对象相等，以便合并时能按“属性粒度”裁决业务侧与平台侧的改动。</p>
 */
public class SchemaDiff {

    /** 节点级变更 */
    private final List<NodeChange> nodeAdded = new ArrayList<>();
    private final List<NodeChange> nodeRemoved = new ArrayList<>();
    private final List<String> nodeTypeChanged = new ArrayList<>();

    /** 结构级变更：父子关系调整 */
    private final List<StructureChange> structureChanged = new ArrayList<>();

    /** 属性级变更：nodeId::propName → {old,new} */
    private final Map<String, PropChange> propChanged = new LinkedHashMap<>();

    /** 逻辑流绑定变更 */
    private final List<String> flowBindChanged = new ArrayList<>();

    public static class NodeChange {
        private final String nodeId;
        private final String type;
        private final Map<String, Object> props;

        public NodeChange(String nodeId, String type, Map<String, Object> props) {
            this.nodeId = nodeId;
            this.type = type;
            this.props = props;
        }

        public String getNodeId() { return nodeId; }
        public String getType() { return type; }
        public Map<String, Object> getProps() { return props; }
    }

    public static class StructureChange {
        private final String parentId;
        private final List<String> before;
        private final List<String> after;

        public StructureChange(String parentId, List<String> before, List<String> after) {
            this.parentId = parentId;
            this.before = before;
            this.after = after;
        }

        public String getParentId() { return parentId; }
        public List<String> getBefore() { return before; }
        public List<String> getAfter() { return after; }
    }

    public static class PropChange {
        private final Object oldValue;
        private final Object newValue;

        public PropChange(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
    }

    public boolean isEmpty() {
        return nodeAdded.isEmpty() && nodeRemoved.isEmpty() && nodeTypeChanged.isEmpty()
                && structureChanged.isEmpty() && propChanged.isEmpty() && flowBindChanged.isEmpty();
    }

    public List<NodeChange> getNodeAdded() { return nodeAdded; }
    public List<NodeChange> getNodeRemoved() { return nodeRemoved; }
    public List<String> getNodeTypeChanged() { return nodeTypeChanged; }
    public List<StructureChange> getStructureChanged() { return structureChanged; }
    public Map<String, PropChange> getPropChanged() { return propChanged; }
    public List<String> getFlowBindChanged() { return flowBindChanged; }
}
