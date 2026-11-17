package com.zifang.z.lc.schema.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.lc.schema.model.NodeRect;
import com.zifang.z.lc.schema.model.NodeStructure;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面 schema 的**唯一归一化入口**。
 *
 * <p>只有一种 schema 模型：{@link PageSchema}（扁平 elements + layout.structure）。
 * 历史上报表域用的 {@code {widgets:[...], layout:[{widgetId,x,y,w,h}]}} 不是另一种
 * schema，而是同一模型的**扁平淡法**——每个 widget 就是「带栅格 rect 的节点」，
 * 栅格坐标来自 layout 数组。读入时在这里一次性折成标准形态，之后校验、存库、
 * 渲染都只认标准形态，全链路不再出现「报表型 / 组件树型」两套分支。</p>
 */
public final class WidgetSchemaAdapter {

    /** 标准形态的根节点 id */
    public static final String ROOT_ID = "root";

    /** 标准形态的根节点类型（容器），与设计器默认 schema 对齐 */
    public static final String ROOT_TYPE = "container";

    /**
     * 旧写法 → 标准写法的类型映射。
     * 未列出的类型按原名保留（前向兼容：新物料不需要改这张表）。
     */
    private static final Map<String, String> TYPE_MAP = new LinkedHashMap<>();

    static {
        TYPE_MAP.put("LINE", "chartLine");
        TYPE_MAP.put("BAR", "chartBar");
        TYPE_MAP.put("PIE", "chartPie");
        TYPE_MAP.put("TABLE", "table");
        TYPE_MAP.put("KPI", "statistic");
        TYPE_MAP.put("RAW", "html");
        TYPE_MAP.put("FORM", "formContainer");
    }

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private WidgetSchemaAdapter() {
    }

    /** 是否已经是标准形态（扁平 elements + layout.structure） */
    public static boolean isCanonical(JsonNode root) {
        return root != null && root.isObject() && root.has("elements") && root.get("elements").isObject();
    }

    /** 是否是旧的报表扁平淡法（widgets + layout 数组） */
    public static boolean isLegacyWidgets(JsonNode root) {
        return root != null && root.isObject() && root.has("widgets") && root.get("widgets").isArray();
    }

    /**
     * 任意写法 → 标准形态。已是标准形态则原样解码（不复制、不改写）。
     *
     * @throws IllegalArgumentException 既不是标准形态也不是可识别的旧写法
     */
    public static PageSchema normalize(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("page schema 顶层必须是对象");
        }
        if (isCanonical(root)) {
            return SchemaCodec.fromJson(root.toString());
        }
        if (isLegacyWidgets(root)) {
            return fromLegacyWidgets(root);
        }
        // 两者都不是：给一条能定位问题的信息，而不是笼统的「格式错」
        throw new IllegalArgumentException("page schema 缺少 elements（标准形态）或 widgets（旧写法）");
    }

    /**
     * 旧报表写法 → 标准形态。
     *
     * <p>widgets 顺序即树顺序；layout 里的 x/y/w/h 落到节点 rect 上；widget 自身字段
     * （title / datasetId / agg / entityCode ...）原样进 props，另存 {@code widgetType}
     * 保留原始类型，便于反向识别与迁移期排查。</p>
     */
    public static PageSchema fromLegacyWidgets(JsonNode root) {
        PageSchema schema = new PageSchema();
        schema.setVersion(root.path("version").asText(PageSchema.CURRENT_VERSION));

        // datasets 是页面自带的数据绑定，标准形态里同样是页面的一部分
        JsonNode datasets = root.get("datasets");
        if (datasets != null && datasets.isObject()) {
            schema.setDatasets(SchemaCodec.toMap(datasets));
        }

        // 栅格坐标：widgetId → rect
        Map<String, NodeRect> rects = new LinkedHashMap<>();
        JsonNode layout = root.get("layout");
        if (layout != null && layout.isArray()) {
            for (JsonNode cell : layout) {
                String widgetId = cell.path("widgetId").asText(null);
                if (widgetId == null) {
                    continue;
                }
                rects.put(widgetId, new NodeRect(
                        cell.path("x").asInt(0),
                        cell.path("y").asInt(0),
                        cell.path("w").asInt(12),
                        cell.path("h").asInt(4)));
            }
        }

        // 根节点：容器，持有全部 widget
        SchemaNode rootNode = new SchemaNode(ROOT_TYPE);
        rootNode.withProp("direction", "column");
        schema.addElement(ROOT_ID, rootNode);
        NodeStructure rootStructure = new NodeStructure();

        List<String> children = new ArrayList<>();
        for (JsonNode widget : root.get("widgets")) {
            String id = widget.path("id").asText(null);
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("widget.id required");
            }
            String legacyType = widget.path("type").asText("");
            SchemaNode node = new SchemaNode(TYPE_MAP.getOrDefault(legacyType, legacyType));
            node.withProp("widgetType", legacyType);

            // widget 自有字段整体进 props（id/type 只做身份与类型，不重复塞）
            Iterator<Map.Entry<String, JsonNode>> fields = widget.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if ("id".equals(field.getKey()) || "type".equals(field.getKey())) {
                    continue;
                }
                node.withProp(field.getKey(), SchemaCodec.toJava(field.getValue()));
            }
            NodeRect rect = rects.get(id);
            if (rect != null) {
                node.setRect(rect);
            }
            schema.addElement(id, node);
            schema.getLayout().structureOf(id);
            children.add(id);
        }

        rootStructure.setChildren(children);
        schema.getLayout().setRoot(ROOT_ID);
        schema.getLayout().structureOf(ROOT_ID).setChildren(children);
        schema.getLayout().setMode(PageSchema.Layout.MODE_ABSOLUTE);
        return schema;
    }

    /**
     * 标准形态 → 旧报表投影（供还没迁完的报表渲染器使用）。
     *
     * <p>只投影带 datasetId 的节点；表单类只投影 formContainer。丢失 rect 细节的
     * 情况一律按流式顺序补默认栅格，保证老渲染器不会因为缺坐标而崩。</p>
     */
    public static ObjectNode toLegacyWidgets(PageSchema schema) {
        ObjectNode root = JSON.objectNode();
        root.put("version", 1);
        if (!schema.getDatasets().isEmpty()) {
            root.set("datasets", SchemaCodec.toNode(schema.getDatasets()));
        }
        var widgets = JSON.arrayNode();
        var layout = JSON.arrayNode();
        List<String> order = schema.getLayout().getStructure()
                .getOrDefault(schema.getLayout().getRoot(), new NodeStructure()).getChildren();
        int index = 0;
        for (String id : order) {
            SchemaNode node = schema.getElements().get(id);
            if (node == null) {
                continue;
            }
            ObjectNode widget = JSON.objectNode();
            widget.put("id", id);
            widget.put("type", legacyTypeOf(node.getType(), node.getProps()));
            for (Map.Entry<String, Object> prop : node.getProps().entrySet()) {
                if ("widgetType".equals(prop.getKey())) {
                    continue;
                }
                widget.set(prop.getKey(), SchemaCodec.toNode(prop.getValue()));
            }
            widgets.add(widget);

            ObjectNode cell = JSON.objectNode();
            cell.put("widgetId", id);
            NodeRect rect = node.getRect();
            cell.put("x", rect == null ? 0 : rect.getX());
            cell.put("y", rect == null ? index * 4 : rect.getY());
            cell.put("w", rect == null ? 12 : rect.getW());
            cell.put("h", rect == null ? 4 : rect.getH());
            layout.add(cell);
            index++;
        }
        root.set("widgets", widgets);
        root.set("layout", layout);
        return root;
    }

    private static String legacyTypeOf(String materialType, Map<String, Object> props) {
        Object original = props.get("widgetType");
        if (original instanceof String s && !s.isEmpty()) {
            return s;
        }
        for (Map.Entry<String, String> e : TYPE_MAP.entrySet()) {
            if (e.getValue().equals(materialType)) {
                return e.getKey();
            }
        }
        return materialType;
    }
}
