package com.zifang.z.lc.web.it;

import com.zifang.z.lc.report.common.schema.DataSourceDef;
import com.zifang.z.lc.report.common.schema.DataSourceType;
import com.zifang.z.lc.report.common.schema.FieldType;
import com.zifang.z.lc.report.dataset.CachingDatasetExecutor;
import com.zifang.z.lc.report.dataset.DatasetExecutor;
import com.zifang.z.lc.report.datasource.impl.MemoryDataSource;
import com.zifang.z.lc.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.lc.report.render.EChartsRenderService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V2 页面域 HTTP 契约集成测试 (lead 013 §四): CRUD + 校验 400 + 渲染全链路。
 * <p>
 * 渲染链在测试上下文用报表域真实组件装配（内存源 + 缓存执行器 + ECharts 渲染服务），
 * 与 z-lc-admin 生产装配同构；FORM 字段清单来自 data.sql 里 demo/order 的事件回放。
 */
@SpringBootTest(classes = LcTestApplication.class)
@AutoConfigureMockMvc
@Import(PageHttpContractTest.ReportChainTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PageHttpContractTest {

    /** 页面 schema (对象形态, 便于阅读); 请求体里以字符串化 JSON 传输 (schema 列是 TEXT) */
    private static final String SCHEMA = "{"
            + "\"version\":1,"
            + "\"datasets\":{\"ds1\":{\"sourceId\":\"mem\",\"baseTable\":\"orders\"}},"
            + "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4},"
            + "             {\"widgetId\":\"w2\",\"x\":6,\"y\":0,\"w\":6,\"h\":4}],"
            + "\"widgets\":[{\"id\":\"w1\",\"type\":\"TABLE\",\"title\":\"订单\",\"datasetId\":\"ds1\"},"
            + "             {\"id\":\"w2\",\"type\":\"FORM\",\"entityCode\":\"order\"}]}";

    private static final String PAGE = "{"
            + "\"appCode\":\"demo\",\"pageCode\":\"p_main\",\"pageName\":\"主页\","
            + "\"schema\":" + jsonString(SCHEMA) + "}";

    @Resource
    private MockMvc mockMvc;


    /** JSON 字符串字面量转义 (schema 以字符串化 JSON 嵌进请求体) */
    private static String jsonString(String raw) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : raw.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append('\\').append('n');
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    @Test
    @Order(1)
    void create_list_get() throws Exception {
        mockMvc.perform(post("/api/lc/page/create").contentType(MediaType.APPLICATION_JSON).content(PAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageCode").value("p_main"))
                .andExpect(jsonPath("$.data.schema").isNotEmpty());

        mockMvc.perform(get("/api/lc/page/list").param("appCode", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pageCode").value("p_main"));

        mockMvc.perform(get("/api/lc/page/demo/p_main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageName").value("主页"));
    }

    @Test
    @Order(2)
    void duplicatePageCodeRejected() throws Exception {
        mockMvc.perform(post("/api/lc/page/create").contentType(MediaType.APPLICATION_JSON).content(PAGE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void invalidSchemaRejected() throws Exception {
        // widget 缺 datasetId → 400 (PageSchemaValidator)
        String badSchema = SCHEMA.replace("\"type\":\"TABLE\",\"title\":\"订单\",\"datasetId\":\"ds1\"",
                "\"type\":\"TABLE\",\"title\":\"订单\"");
        String bad = "{\"appCode\":\"demo\",\"pageCode\":\"p_bad\",\"pageName\":\"x\","
                + "\"schema\":" + jsonString(badSchema) + "}";
        mockMvc.perform(post("/api/lc/page/create").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void render_fullChain() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lc/page/demo/p_main/render"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layout[0].widgetId").value("w1"))
                // TABLE 部件: 报表执行链真读了内存源
                .andExpect(jsonPath("$.data.options.w1._widgetType").value("TABLE"))
                .andExpect(jsonPath("$.data.options.w1.rows").isArray())
                // FORM 部件: 事件回放出 order 的字段清单
                .andExpect(jsonPath("$.data.forms.w2.entityCode").value("order"))
                .andExpect(jsonPath("$.data.forms.w2.fields").isArray())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"fieldCode\"") || body.contains("\"code\":\"name\""),
                "FORM 字段清单应包含实体字段, 实际: " + body);
    }

    @Test
    @Order(5)
    void render_unknownPage404() throws Exception {
        mockMvc.perform(get("/api/lc/page/demo/p_nope/render"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void update_then_delete() throws Exception {
        mockMvc.perform(post("/api/lc/page/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":null,\"pageName\":\"主页改\"}"))
                .andExpect(status().isBadRequest()); // id 缺失 → 400

        MvcResult list = mockMvc.perform(get("/api/lc/page/list").param("appCode", "demo"))
                .andExpect(status().isOk()).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(list.getResponse().getContentAsString(),
                "$.data[0].id").toString();

        mockMvc.perform(post("/api/lc/page/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"pageName\":\"主页改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageName").value("主页改"));

        mockMvc.perform(post("/api/lc/page/delete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lc/page/demo/p_main/render"))
                .andExpect(status().isNotFound());
    }

    /** 报表渲染链测试装配: 与 z-lc-admin 生产同构 (内存源 + 缓存执行器 + ECharts 渲染) */
    @TestConfiguration
    static class ReportChainTestConfig {

        @Bean
        public DataSourceRegistry reportDataSourceRegistry() {
            DataSourceRegistry registry = new DataSourceRegistry();
            Map<String, java.util.List<Map<String, Object>>> tables = new LinkedHashMap<>();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", "o-1");
            row.put("amount", 12.5);
            tables.put("orders", new java.util.ArrayList<>(java.util.Collections.singletonList(row)));
            Map<String, Map<String, FieldType>> colTypes = new LinkedHashMap<>();
            Map<String, FieldType> cols = new LinkedHashMap<>();
            cols.put("name", FieldType.STRING);
            cols.put("amount", FieldType.DOUBLE);
            colTypes.put("orders", cols);
            registry.register(new MemoryDataSource(
                    new DataSourceDef("mem", "内存源", DataSourceType.MEMORY, new LinkedHashMap<>()),
                    tables, colTypes));
            return registry;
        }

        @Bean
        public DatasetExecutor datasetExecutor(DataSourceRegistry registry) {
            return new CachingDatasetExecutor(
                    (sourceId, table) -> registry.require(sourceId).read(table), 60_000L, 128);
        }

        @Bean
        public EChartsRenderService eChartsRenderService() {
            return new EChartsRenderService();
        }
    }
}
