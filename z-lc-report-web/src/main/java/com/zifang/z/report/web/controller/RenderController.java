package com.zifang.z.report.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.common.schema.ViewSchema;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.DatasetExecutor;
import com.zifang.z.report.render.EChartsRenderService;
import com.zifang.z.report.web.store.SchemaStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 渲染 API (通用渲染器的后端契约):
 * 单 widget 渲染 / 整页 ViewSchema 渲染 (widgetId → option 映射)。
 * 前端 UniversalRenderer 消费本接口产物, schema 驱动, 无逐图表定制。
 */
@RestController
@RequestMapping("/api/render")
public class RenderController {

    private final DatasetExecutor executor;
    private final EChartsRenderService renderService;
    private final SchemaStore store;

    public RenderController(DatasetExecutor executor, EChartsRenderService renderService, SchemaStore store) {
        this.executor = executor;
        this.renderService = renderService;
        this.store = store;
    }

    /** 单图表渲染: 数据按 widget.datasetId 现取 */
    @PostMapping("/widget")
    public JsonNode renderWidget(@RequestBody WidgetSpec widget) {
        QueryResult data = executor.execute(store.requireDataset(widget.getDatasetId()));
        ObjectNode option = renderService.render(widget, data);
        return wrap(widget, option);
    }

    /** 整页渲染: ViewSchema → {widgetId: option} */
    @GetMapping("/view/{viewId}")
    public Map<String, Object> renderView(@PathVariable("viewId") String viewId) {
        ViewSchema view = store.requireView(viewId);
        Map<String, JsonNode> options = new LinkedHashMap<>();
        if (view.getWidgets() != null) {
            for (WidgetSpec w : view.getWidgets()) {
                QueryResult data = executor.execute(store.requireDataset(w.getDatasetId()));
                options.put(w.getId(), wrap(w, renderService.render(w, data)));
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("viewId", view.getId());
        resp.put("title", view.getTitle());
        resp.put("version", view.getVersion());
        resp.put("layout", view.getLayout());
        resp.put("options", options);
        return resp;
    }

    /** option 包一层元信息 (图表类型/标题), 便于前端分发到对应渲染分支 */
    private ObjectNode wrap(WidgetSpec w, ObjectNode option) {
        option.put("_widgetType", w.getType() == null ? "TABLE" : w.getType().name());
        option.put("_title", w.getTitle());
        return option;
    }
}
