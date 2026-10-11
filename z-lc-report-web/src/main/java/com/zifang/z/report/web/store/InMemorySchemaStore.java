package com.zifang.z.report.web.store;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.ViewSchema;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 契约存储内存实现 (M1/M2): DatasetSchema / ViewSchema 注册表。
 * M2 已接口化为 {@link SchemaStore}; M2.5 迁移至 DB (MyBatis-Plus),
 * 存储形态为 JSON 列 (对齐 z-lc viewconfig 模式), DDL 见 _doc/002_deploy/init/001_schema.sql。
 */
@Component
public class InMemorySchemaStore implements SchemaStore {

    private final Map<String, DatasetSchema> datasets = new ConcurrentHashMap<>();
    private final Map<String, ViewSchema> views = new ConcurrentHashMap<>();

    public void putDataset(DatasetSchema schema) {
        if (schema.getId() == null) {
            throw new IllegalArgumentException("dataset.id required");
        }
        datasets.put(schema.getId(), schema);
    }

    public DatasetSchema requireDataset(String id) {
        DatasetSchema s = datasets.get(id);
        if (s == null) {
            throw new IllegalArgumentException("dataset not found: " + id);
        }
        return s;
    }

    /** 全部数据集 (AI 生成视图时枚举可用数据集用) */
    public java.util.List<DatasetSchema> listDatasets() {
        return new java.util.ArrayList<>(datasets.values());
    }

    /** 幂等探测用 (不存在返回 null, 区别于 require 抛错语义) */
    public DatasetSchema getDataset(String id) {
        return datasets.get(id);
    }

    public void putView(ViewSchema view) {
        if (view.getId() == null) {
            throw new IllegalArgumentException("view.id required");
        }
        views.put(view.getId(), view);
    }

    public ViewSchema requireView(String id) {
        ViewSchema v = views.get(id);
        if (v == null) {
            throw new IllegalArgumentException("view not found: " + id);
        }
        return v;
    }

    /** 幂等探测用 (不存在返回 null) */
    public ViewSchema getView(String id) {
        return views.get(id);
    }

    /** 全部视图 (模板/落库迁移巡检用) */
    @Override
    public java.util.List<ViewSchema> listViews() {
        return new java.util.ArrayList<>(views.values());
    }
}
