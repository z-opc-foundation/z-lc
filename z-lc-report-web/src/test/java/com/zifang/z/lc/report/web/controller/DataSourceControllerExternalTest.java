package com.zifang.z.lc.report.web.controller;

import com.zifang.z.lc.report.common.schema.DataSourceDef;
import com.zifang.z.lc.report.common.schema.DataSourceType;
import com.zifang.z.lc.report.common.schema.FieldType;
import com.zifang.z.lc.report.datasource.impl.MemoryDataSource;
import com.zifang.z.lc.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.lc.report.datasource.spi.ExternalReportDataSourceResolver;
import com.zifang.z.lc.report.datasource.spi.ReportDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 外部目录源注册端点（2026-10-26 报表域收口）：
 * 目录桥缺席 → 503 语义；在位 → 注册成功且元数据端点可达；id 缺失 → 400。
 * standalone MockMvc，不拉起完整上下文。
 */
class DataSourceControllerExternalTest {

    /** ObjectProvider 替身：只实现 getIfAvailable */
    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() {
                throw new UnsupportedOperationException();
            }

            @Override
            public T getObject(Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }

    /** 内存替身源：def + 单表 */
    private static ReportDataSource memorySource(String id) {
        DataSourceDef def = new DataSourceDef(id, id, DataSourceType.MEMORY, Collections.emptyMap());
        Map<String, java.util.List<Map<String, Object>>> tables = new LinkedHashMap<>();
        tables.put("t1", Collections.singletonList(
                Collections.<String, Object>singletonMap("c1", "v1")));
        Map<String, Map<String, FieldType>> colTypes = new LinkedHashMap<>();
        colTypes.put("t1", Collections.singletonMap("c1", FieldType.STRING));
        return new MemoryDataSource(def, tables, colTypes);
    }

    @Test
    void external_resolverMissing_returns503() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DataSourceController(new DataSourceRegistry(), providerOf(null))).setControllerAdvice(new com.zifang.z.lc.report.web.ApiErrorAdvice()).build();
        mvc.perform(post("/api/lc/report/datasource/external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"dev-h2\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void external_resolverPresent_registersAndProbes() throws Exception {
        ExternalReportDataSourceResolver stub = new ExternalReportDataSourceResolver() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public ReportDataSource resolve(String id, Map<String, String> options) {
                if (!"dev-h2".equals(id)) {
                    throw new IllegalArgumentException("数据源目录里不存在 id: " + id);
                }
                return memorySource(id);
            }
        };
        DataSourceRegistry registry = new DataSourceRegistry();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DataSourceController(registry, providerOf(stub))).setControllerAdvice(new com.zifang.z.lc.report.web.ApiErrorAdvice()).build();

        mvc.perform(post("/api/lc/report/datasource/external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"dev-h2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.sourceId").value("dev-h2"));

        // 注册进的是同一份 registry，元数据端点直接可用
        mvc.perform(get("/api/lc/report/datasource/dev-h2/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("t1"));

        assertNotNull(registry.require("dev-h2"));
    }

    @Test
    void external_unknownId_maps400() throws Exception {
        ExternalReportDataSourceResolver stub = new ExternalReportDataSourceResolver() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public ReportDataSource resolve(String id, Map<String, String> options) {
                throw new IllegalArgumentException("数据源目录里不存在 id: " + id);
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DataSourceController(new DataSourceRegistry(), providerOf(stub))).setControllerAdvice(new com.zifang.z.lc.report.web.ApiErrorAdvice()).build();
        mvc.perform(post("/api/lc/report/datasource/external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"nope\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void external_missingId_maps400() throws Exception {
        ExternalReportDataSourceResolver stub = new ExternalReportDataSourceResolver() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public ReportDataSource resolve(String id, Map<String, String> options) {
                return memorySource(id);
            }
        };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DataSourceController(new DataSourceRegistry(), providerOf(stub))).setControllerAdvice(new com.zifang.z.lc.report.web.ApiErrorAdvice()).build();
        mvc.perform(post("/api/lc/report/datasource/external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
