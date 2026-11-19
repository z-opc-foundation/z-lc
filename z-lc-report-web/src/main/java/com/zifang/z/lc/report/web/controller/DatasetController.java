package com.zifang.z.lc.report.web.controller;

import com.zifang.z.lc.report.common.schema.DatasetSchema;
import com.zifang.z.lc.report.common.schema.QueryResult;
import com.zifang.z.lc.report.dataset.DatasetExecutor;
import com.zifang.z.lc.report.web.store.SchemaStore;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
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
@RequestMapping("/api/lc/report/dataset")
public class DatasetController {

    private final DatasetExecutor executor;
    private final SchemaStore store;

    public DatasetController(DatasetExecutor executor, SchemaStore store) {
        this.executor = executor;
        this.store = store;
    }

    @PostMapping
    @BizApi(summary = "register", description = "", tags = { "z-lc" })
    public Map<String, Object> register(@RequestBody @BizParam(name = "schema", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DatasetSchema schema) {
        store.putDataset(schema);
        return java.util.Collections.singletonMap("ok", true);
    }

    @PostMapping("/{id}/preview")
    @BizApi(summary = "preview", description = "", tags = { "z-lc" })
    public QueryResult preview(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String id) {
        DatasetSchema schema = store.requireDataset(id);
        return executor.execute(schema);
    }
}



