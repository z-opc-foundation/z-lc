package com.zifang.z.report.web.controller;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.dataset.DatasetExecutor;
import com.zifang.z.report.web.store.SchemaStore;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据集 API: 注册语义层定义 (主源 + join + 过滤) 与明细预览。
 */
@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    private final DatasetExecutor executor;
    private final SchemaStore store;

    public DatasetController(DatasetExecutor executor, SchemaStore store) {
        this.executor = executor;
        this.store = store;
    }

    @PostMapping
    public Map<String, Object> register(@RequestBody DatasetSchema schema) {
        store.putDataset(schema);
        return java.util.Collections.singletonMap("ok", true);
    }

    @PostMapping("/{id}/preview")
    public QueryResult preview(@PathVariable("id") String id) {
        DatasetSchema schema = store.requireDataset(id);
        return executor.execute(schema);
    }
}
