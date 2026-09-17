package com.zifang.z.lc.common.dto.query;

import java.io.Serializable;
import java.util.List;

/**
 * 复杂查询条件（递归树结构）.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataQueryCondition}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>相比 {@link RuntimeQueryDTO} 的扁平结构（key=value），
 * 本 DTO 支持嵌套条件（AND / OR 树形组合），用于：
 * <ul>
 *   <li>前端「高级筛选」配置器输出的复杂查询树</li>
 *   <li>动态报表 / 仪表盘的查询条件构建</li>
 *   <li>权限表达式（行级过滤条件）</li>
 * </ul>
 *
 * <p>树形组合示例：
 * <pre>{@code
 *   // (status = 'ACTIVE' AND (city = 'SH' OR city = 'BJ')) AND age > 18
 *   ModelDataQueryCondition root = new ModelDataQueryCondition(1 /* AND *\/);
 *   root.setRowValues(new FieldCondition("status", "=", "ACTIVE"));
 *   ModelDataQueryCondition cityOr = new ModelDataQueryCondition(2 /* OR *\/);
 *   cityOr.addChild(new FieldCondition("city", "=", "SH"));
 *   cityOr.addChild(new FieldCondition("city", "=", "BJ"));
 *   root.addChild(cityOr);
 *   root.addChild(new FieldCondition("age", ">", 18));
 * }</pre>
 *
 * <p>字段说明：
 * <ul>
 *   <li>{@link #key} — 条件唯一标识（业务自定义 — 用于更新 / 删除场景）</li>
 *   <li>{@link #level} — 层级（用于 UI 缩进展示）</li>
 *   <li>{@link #type} — 组合类型（1 = AND / 2 = OR）</li>
 *   <li>{@link #rowValues} — 叶子节点条件；非叶子节点时为 null</li>
 *   <li>{@link #children} — 子条件列表；叶子节点时为 null 或空</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public class ModelDataQueryCondition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** AND 组合（所有子条件都为 true 才命中）. */
    public static final Integer TYPE_AND = 1;

    /** OR 组合（任一子条件为 true 即命中）. */
    public static final Integer TYPE_OR = 2;

    /**
     * 条件唯一标识（业务自定义 — 用于更新 / 删除场景）.
     */
    private String key;

    /**
     * 条件层级（用于 UI 缩进展示 — 从 0 开始）.
     */
    private Integer level;

    /**
     * 条件组合类型（{@link #TYPE_AND} / {@link #TYPE_OR}）.
     */
    private Integer type;

    /**
     * 行值条件（叶子节点 — 当 {@code children} 为空时本字段必填）.
     */
    private FieldCondition rowValues;

    /**
     * 子条件列表（非叶子节点时填写；递归构造查询树）.
     */
    private List<ModelDataQueryCondition> children;

    public ModelDataQueryCondition() {
    }

    public ModelDataQueryCondition(Integer type) {
        this.type = type;
    }

    /**
     * 便捷方法：添加子条件.
     */
    public void addChild(ModelDataQueryCondition child) {
        if (this.children == null) {
            this.children = new java.util.ArrayList<>();
        }
        this.children.add(child);
    }

    /**
     * 便捷方法：添加叶子条件.
     */
    public void addChild(FieldCondition leaf) {
        ModelDataQueryCondition node = new ModelDataQueryCondition();
        node.setRowValues(leaf);
        addChild(node);
    }

    /**
     * 便捷方法：构造「单字段 = 值」叶子条件.
     */
    public static ModelDataQueryCondition eq(String paramKey, Object paramValue) {
        return newEqCondition(paramKey, "=", paramValue);
    }

    /**
     * 便捷方法：构造「单字段 LIKE 值」叶子条件.
     */
    public static ModelDataQueryCondition like(String paramKey, Object paramValue) {
        return newEqCondition(paramKey, "like", paramValue);
    }

    private static ModelDataQueryCondition newEqCondition(String paramKey, String rule, Object paramValue) {
        ModelDataQueryCondition node = new ModelDataQueryCondition();
        node.setRowValues(new FieldCondition(paramKey, rule, paramValue));
        return node;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public FieldCondition getRowValues() {
        return rowValues;
    }

    public void setRowValues(FieldCondition rowValues) {
        this.rowValues = rowValues;
    }

    public List<ModelDataQueryCondition> getChildren() {
        return children;
    }

    public void setChildren(List<ModelDataQueryCondition> children) {
        this.children = children;
    }

    /**
     * 叶子条件 — 三元组（paramKey / rule / paramValue）.
     */
    public static class FieldCondition implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 参数名（数据库列名 / 模型字段 code）.
         */
        private String paramKey;

        /**
         * 比较规则（{@code =} / {@code !=} / {@code >} / {@code <} / {@code >=} / {@code <=} /
         * {@code like} / {@code in}）.
         */
        private String rule;

        /**
         * 参数值.
         */
        private Object paramValue;

        public FieldCondition() {
        }

        public FieldCondition(String paramKey, String rule, Object paramValue) {
            this.paramKey = paramKey;
            this.rule = rule;
            this.paramValue = paramValue;
        }

        public String getParamKey() {
            return paramKey;
        }

        public void setParamKey(String paramKey) {
            this.paramKey = paramKey;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public Object getParamValue() {
            return paramValue;
        }

        public void setParamValue(Object paramValue) {
            this.paramValue = paramValue;
        }
    }
}
