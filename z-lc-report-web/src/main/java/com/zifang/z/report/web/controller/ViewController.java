package com.zifang.z.report.web.controller;

import com.zifang.z.report.web.store.SchemaStore;
import com.zifang.z.report.common.schema.ViewSchema;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 视图页 API: ViewSchema 注册/读取。
 * schema 是唯一事实源 —— AI 生成、拖拽设计器、模板都产它, 渲染只消费它。
 */
@RestController
@RequestMapping("/api/view")
public class ViewController {

    private final SchemaStore store;

    public ViewController(SchemaStore store) {
        this.store = store;
    }

    @PostMapping
    public Map<String, Object> register(@RequestBody ViewSchema view) {
        store.putView(view);
        return java.util.Collections.singletonMap("ok", true);
    }

    @GetMapping("/{id}")
    public ViewSchema get(@PathVariable("id") String id) {
        return store.requireView(id);
    }
}
