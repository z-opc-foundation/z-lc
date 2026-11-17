package com.zifang.z.lc.schema.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 扁平节点表中的一个节点：id 为键，type 决定用哪个物料渲染，props 是该物料的属性。
 *
 * <p>节点与结构分离是整套 schema 的地基：改属性只动 elements，改布局只动 layout.structure，
 * 两者互不干扰，也是 diff 与三方合并能按属性粒度工作的基础。</p>
 */
public class SchemaNode {

    /** 物料类型，对应物料注册的 key，例如 input / table / pageContainer */
    private String type;

    /** 节点属性 */
    private Map<String, Object> props = new LinkedHashMap<>();

    /**
     * 绝对定位单元（可空）。为空表示参与流式排版；有值表示按栅格坐标摆放。
     * 报表形态的页面只是「每个节点都带 rect」的组件树，不另立模型。
     */
    private NodeRect rect;

    public SchemaNode() {
    }

    public SchemaNode(String type) {
        this.type = type;
    }

    public SchemaNode(String type, Map<String, Object> props) {
        this.type = type;
        if (props != null) {
            this.props = new LinkedHashMap<>(props);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props == null ? new LinkedHashMap<>() : new LinkedHashMap<>(props);
    }

    public SchemaNode withProp(String key, Object value) {
        this.props.put(key, value);
        return this;
    }

    public NodeRect getRect() {
        return rect;
    }

    public void setRect(NodeRect rect) {
        this.rect = rect;
    }

    public SchemaNode withRect(NodeRect rect) {
        this.rect = rect;
        return this;
    }
}
