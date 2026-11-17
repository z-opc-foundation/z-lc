package com.zifang.z.lc.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zifang.z.lc.schema.logic.LogicDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一页面 Schema —— 设计器、渲染器、逻辑引擎三者的唯一契约。
 *
 * <pre>
 * elements           扁平节点表：id → {type, props}
 * layout.structure   树形结构：id → {children, namedSlots}，与节点表分离
 * imports            物料依赖
 * context            对外暴露的变量/事件/方法 + 扩展点 + 全局配置
 * logic              逻辑流（flowBinds + flows）
 * </pre>
 *
 * <p>设计要点：节点与结构分离。改属性只动 elements，改布局只动 layout.structure，
 * 互不干扰；这也是结构化 diff 与三方合并能按属性粒度工作的基础。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageSchema {

    public static final String CURRENT_VERSION = "1.0";

    /** schema 版本 */
    private String version = CURRENT_VERSION;

    /** 页面 id / 标识 */
    private String id;

    /** 扁平节点表 */
    private Map<String, SchemaNode> elements = new LinkedHashMap<>();

    /** 树形结构 */
    private Layout layout = new Layout();

    /** 物料依赖 */
    private List<SchemaImport> imports = new ArrayList<>();

    /**
     * 数据绑定声明：dataset id → 取数描述（sourceId / baseTable / query ...）。
     *
     * <p>不是报表专有：组件树里的表格、统计卡同样按 id 引用它。内嵌在页面里
     * （而非引用外部 store），页面跨重启仍可渲染。</p>
     */
    private Map<String, Object> datasets = new LinkedHashMap<>();

    /** 上下文协议 */
    private SchemaContext context = new SchemaContext();

    /** 逻辑定义 */
    private LogicDef logic = new LogicDef();

    /** 扩展元数据 */
    private Map<String, Object> meta = new LinkedHashMap<>();

    public PageSchema() {
    }

    /** 树形结构容器 */
    public static class Layout {
        /** 流式排版 */
        public static final String MODE_FLOW = "flow";
        /** 绝对定位排版（节点带 rect） */
        public static final String MODE_ABSOLUTE = "absolute";

        /** 根节点 id */
        private String root;
        /** id → 结构 */
        private Map<String, NodeStructure> structure = new LinkedHashMap<>();
        /** 排版模式：flow(默认) / absolute */
        private String mode = MODE_FLOW;

        public Layout() {
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode == null || mode.isEmpty() ? MODE_FLOW : mode;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public Map<String, NodeStructure> getStructure() {
            return structure;
        }

        public void setStructure(Map<String, NodeStructure> structure) {
            this.structure = structure == null ? new LinkedHashMap<>() : structure;
        }

        public NodeStructure structureOf(String id) {
            return structure.computeIfAbsent(id, k -> new NodeStructure());
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, SchemaNode> getElements() {
        return elements;
    }

    public void setElements(Map<String, SchemaNode> elements) {
        this.elements = elements == null ? new LinkedHashMap<>() : elements;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout == null ? new Layout() : layout;
    }

    public List<SchemaImport> getImports() {
        return imports;
    }

    public Map<String, Object> getDatasets() {
        return datasets;
    }

    public void setDatasets(Map<String, Object> datasets) {
        this.datasets = datasets == null ? new LinkedHashMap<>() : datasets;
    }

    public void setImports(List<SchemaImport> imports) {
        this.imports = imports == null ? new ArrayList<>() : imports;
    }

    public SchemaContext getContext() {
        return context;
    }

    public void setContext(SchemaContext context) {
        this.context = context == null ? new SchemaContext() : context;
    }

    public LogicDef getLogic() {
        return logic;
    }

    public void setLogic(LogicDef logic) {
        this.logic = logic == null ? new LogicDef() : logic;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta == null ? new LinkedHashMap<>() : meta;
    }

    public SchemaNode addElement(String id, SchemaNode node) {
        this.elements.put(id, node);
        return node;
    }
}
