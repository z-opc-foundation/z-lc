package com.zifang.z.lc.core.page;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 页面 schema 校验器 (V2 页面域, 纯静态无 Spring)。
 * <p>
 * schema 即 z-frontend-render 组件树 JSON + FORM 扩展:
 * <pre>
 * {
 *   "version": 1,
 *   "datasets": {"ds1": {"sourceId": "dev-h2", "baseTable": "z_lc_app"}},
 *   "layout":   [{"widgetId": "w1", "x": 0, "y": 0, "w": 6, "h": 4}],
 *   "widgets":  [
 *     {"id": "w1", "type": "TABLE", "title": "应用清单", "datasetId": "ds1"},
 *     {"id": "w2", "type": "FORM", "entityCode": "app"}
 *   ]
 * }
 * </pre>
 * 校验失败抛 {@link IllegalArgumentException}（控制器映射 400）。
 * datasets 内嵌于页面（而非引用报表内存 store）：页面跨重启可渲染。
 */
public final class PageSchemaValidator {

    private PageSchemaValidator() {
    }

    /** 部件类型白名单: 渲染器内置六类 + V2 扩展的 FORM (绑实体, 走 RuntimeCrud) */
    public static final Set<String> WIDGET_TYPES = new LinkedHashSet<>(java.util.Arrays.asList(
            "LINE", "BAR", "PIE", "TABLE", "KPI", "RAW", "FORM"));

    private static final Set<String> AGG_TYPES = new HashSet<>(java.util.Arrays.asList(
            "SUM", "COUNT", "AVG", "MIN", "MAX"));

    /** 解析并校验, 返回解析后的根游标 (调用方无需再 parse) */
    public static JsonCursor validate(String schemaJson) {
        if (schemaJson == null || schemaJson.trim().isEmpty()) {
            throw new IllegalArgumentException("page schema required");
        }
        JsonCursor root;
        try {
            root = JsonCursor.of(JsonUtil.parse(schemaJson));
        } catch (Exception e) {
            throw new IllegalArgumentException("page schema 不是合法 JSON: " + e.getMessage());
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("page schema 顶层必须是对象");
        }

        // ---- widgets ----
        if (!root.has("widgets") || !root.path("widgets").isArray()) {
            throw new IllegalArgumentException("page schema.widgets 必须是数组");
        }
        Set<String> widgetIds = new LinkedHashSet<>();
        Set<String> datasetIds = new LinkedHashSet<>();
        for (JsonCursor w : root.path("widgets").elements()) {
            if (!w.isObject()) {
                throw new IllegalArgumentException("widgets 元素必须是对象");
            }
            String id = text(w, "id");
            if (id == null) {
                throw new IllegalArgumentException("widget.id required");
            }
            if (!widgetIds.add(id)) {
                throw new IllegalArgumentException("widget.id 重复: " + id);
            }
            String type = text(w, "type");
            if (type == null || !WIDGET_TYPES.contains(type)) {
                throw new IllegalArgumentException("widget [" + id + "] type 必须是 "
                        + WIDGET_TYPES + ", 实际: " + type);
            }
            if ("FORM".equals(type)) {
                if (text(w, "entityCode") == null) {
                    throw new IllegalArgumentException("FORM widget [" + id + "] 必须绑定 entityCode");
                }
            } else {
                String datasetId = text(w, "datasetId");
                if (datasetId == null) {
                    throw new IllegalArgumentException("widget [" + id + "] 必须绑定 datasetId");
                }
                datasetIds.add(datasetId);
            }
            String agg = text(w, "agg");
            if (agg != null && !AGG_TYPES.contains(agg)) {
                throw new IllegalArgumentException("widget [" + id + "] agg 非法: " + agg);
            }
        }

        // ---- datasets: 非 FORM widget 引用的 id 必须有内嵌定义 ----
        Set<String> declared = new HashSet<>();
        if (root.has("datasets") && !root.path("datasets").isNull()) {
            if (!root.path("datasets").isObject()) {
                throw new IllegalArgumentException("page schema.datasets 必须是对象");
            }
            for (String dsId : root.path("datasets").fieldNames()) {
                JsonCursor ds = root.path("datasets").path(dsId);
                if (!ds.isObject()) {
                    throw new IllegalArgumentException("dataset [" + dsId + "] 必须是对象");
                }
                if (text(ds, "sourceId") == null || text(ds, "baseTable") == null) {
                    throw new IllegalArgumentException("dataset [" + dsId + "] 必须带 sourceId 与 baseTable");
                }
                declared.add(dsId);
            }
        }
        for (String need : datasetIds) {
            if (!declared.contains(need)) {
                throw new IllegalArgumentException("widget 引用的 dataset [" + need + "] 未在 schema.datasets 定义");
            }
        }

        // ---- layout: 与 widgets 按 widgetId 双向对齐 ----
        if (!root.has("layout") || !root.path("layout").isArray()) {
            throw new IllegalArgumentException("page schema.layout 必须是数组");
        }
        Set<String> laidOut = new HashSet<>();
        for (JsonCursor cell : root.path("layout").elements()) {
            if (!cell.isObject()) {
                throw new IllegalArgumentException("layout 元素必须是对象");
            }
            String widgetId = text(cell, "widgetId");
            if (widgetId == null) {
                throw new IllegalArgumentException("layout.widgetId required");
            }
            if (!laidOut.add(widgetId)) {
                throw new IllegalArgumentException("layout.widgetId 重复: " + widgetId);
            }
            int x = cell.path("x").asInt(-1);
            int y = cell.path("y").asInt(-1);
            int w = cell.path("w").asInt(-1);
            int h = cell.path("h").asInt(-1);
            if (x < 0 || x > 11 || w < 1 || w > 12 || x + w > 12 || y < 0 || h < 1) {
                throw new IllegalArgumentException("layout [" + widgetId + "] 越界: 12 栅格要求 x∈[0,11], w∈[1,12], x+w≤12, y≥0, h≥1");
            }
        }
        for (String id : widgetIds) {
            if (!laidOut.contains(id)) {
                throw new IllegalArgumentException("widget [" + id + "] 缺 layout 单元");
            }
        }
        for (String laid : laidOut) {
            if (!widgetIds.contains(laid)) {
                throw new IllegalArgumentException("layout 引用了不存在的 widget: " + laid);
            }
        }
        return root;
    }

    private static String text(JsonCursor node, String key) {
        JsonCursor c = node.path(key);
        if (c.isMissing() || c.isNull()) {
            return null;
        }
        String v = c.asText();
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }
}
