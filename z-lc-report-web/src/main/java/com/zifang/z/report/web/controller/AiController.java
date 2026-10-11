package com.zifang.z.report.web.controller;

import com.zifang.z.report.ai.AiReportService;
import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.ViewSchema;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.dataset.DatasetExecutor;
import com.zifang.z.report.web.store.SchemaStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 报表 API (M3 核心): 一句话生成数据集 / 视图页。
 * 产物经本地契约校验后自动入库, 前端直接消费 id 渲染。
 * <p>
 * 大表注意: 视图生成需要数据集列元数据, fields 契约缺失时会实际执行数据集获取列头
 * (M1 全量内存语义可接受; M2 改用字段契约/下推元数据, 见 _doc/008_troubleshooting/失败要点与坑.md)。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiReportService ai;
    private final DataSourceRegistry registry;
    private final DatasetExecutor executor;
    private final SchemaStore store;

    public AiController(AiReportService ai, DataSourceRegistry registry,
                        DatasetExecutor executor, SchemaStore store) {
        this.ai = ai;
        this.registry = registry;
        this.executor = executor;
        this.store = store;
    }

    /** NL → 数据集定义 (生成并注册) */
    @PostMapping("/dataset")
    public DatasetSchema toDataset(@RequestBody Map<String, String> body) {
        String requirement = require(body, "requirement");
        String sourceId = require(body, "sourceId");
        // 元数据: 源内全部表的列清单 (LLM 上下文)
        Map<String, Map<String, String>> tableSchemas = new LinkedHashMap<>();
        for (String table : registry.require(sourceId).tables()) {
            Map<String, String> cols = new LinkedHashMap<>();
            registry.require(sourceId).schema(table)
                    .forEach((col, type) -> cols.put(col, type.name()));
            tableSchemas.put(table, cols);
        }
        DatasetSchema schema = ai.nlToDataset(requirement, sourceId, tableSchemas);
        store.putDataset(schema);
        return schema;
    }

    /** NL → 视图页 schema (生成并注册) */
    @PostMapping("/view")
    public ViewSchema toView(@RequestBody Map<String, String> body) {
        String requirement = require(body, "requirement");
        String viewId = body.getOrDefault("viewId", "view-ai-" + System.currentTimeMillis());
        Map<String, List<FieldDef>> datasetFields = new LinkedHashMap<>();
        for (DatasetSchema ds : store.listDatasets()) {
            List<FieldDef> fields = ds.getFields();
            if (fields == null || fields.isEmpty()) {
                // fields 契约未填: 执行取真实列头 (见类注释的 M2 备注)
                fields = executor.executeTable(ds).toQueryResult().getColumns();
            }
            datasetFields.put(ds.getId(), fields);
        }
        ViewSchema view = ai.nlToView(requirement, viewId, datasetFields);
        store.putView(view);
        return view;
    }

    private static String require(Map<String, String> body, String key) {
        String v = body.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("request field required: " + key);
        }
        return v;
    }
}
