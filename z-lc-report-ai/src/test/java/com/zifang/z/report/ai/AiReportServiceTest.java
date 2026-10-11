package com.zifang.z.report.ai;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.common.schema.ViewSchema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI 报表服务测试: mock LlmClient, 验证提示词产物解析 + 契约校验拦截幻觉 */
class AiReportServiceTest {

    private static final String GOOD_DATASET_JSON = "{\"id\":\"ds-1\",\"name\":\"订单\","
            + "\"sourceId\":\"ds1\",\"baseTable\":\"orders\","
            + "\"computedFields\":[{\"name\":\"amount_x2\",\"type\":\"DOUBLE\",\"expr\":\"amount * 2\"}]}";

    private static final String GOOD_VIEW_JSON = "{\"id\":\"view1\",\"title\":\"看板\",\"version\":1,"
            + "\"widgets\":[{\"id\":\"w1\",\"type\":\"BAR\",\"title\":\"城市销售额\",\"datasetId\":\"ds-1\","
            + "\"encode\":{\"x\":\"city\",\"y\":\"amount\"},\"agg\":\"SUM\"}],"
            + "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4}]}";

    /** 录制消息 + 返回预置回复的 mock */
    private static LlmClient canned(String reply) {
        return messages -> reply;
    }

    private Map<String, Map<String, String>> tableSchemas() {
        Map<String, Map<String, String>> tables = new LinkedHashMap<>();
        tables.put("orders", new LinkedHashMap<String, String>());
        tables.get("orders").put("uid", "INT");
        tables.get("orders").put("city", "STRING");
        tables.get("orders").put("amount", "INT");
        return tables;
    }

    private Map<String, List<FieldDef>> datasetFields() {
        Map<String, List<FieldDef>> m = new LinkedHashMap<>();
        m.put("ds-1", Arrays.asList(
                FieldDef.of("uid", FieldType.INT),
                FieldDef.of("city", FieldType.STRING),
                FieldDef.of("amount", FieldType.INT)));
        return m;
    }

    @Test
    void nlToDataset_parsesAndRegistersNothing() {
        AiReportService svc = new AiReportService(canned(GOOD_DATASET_JSON));
        DatasetSchema s = svc.nlToDataset("订单数据", "ds1", tableSchemas());
        assertEquals("ds-1", s.getId());
        assertEquals("orders", s.getBaseTable());
        assertEquals(1, s.getComputedFields().size());
    }

    @Test
    void nlToDataset_rejectsUnknownBaseTable() {
        String hallucinated = GOOD_DATASET_JSON.replace("\"orders\"", "\"not_a_table\"");
        AiReportService svc = new AiReportService(canned(hallucinated));
        assertThrows(IllegalArgumentException.class,
                () -> svc.nlToDataset("订单数据", "ds1", tableSchemas()));
    }

    @Test
    void nlToDataset_rejectsInvalidExpr() {
        String bad = GOOD_DATASET_JSON.replace("amount * 2", "ABS(amount)"); // 函数不在白名单
        AiReportService svc = new AiReportService(canned(bad));
        assertThrows(IllegalArgumentException.class,
                () -> svc.nlToDataset("订单数据", "ds1", tableSchemas()));
    }

    @Test
    void nlToDataset_rejectsWrongSource() {
        String wrong = GOOD_DATASET_JSON.replace("\"ds1\"", "\"ds-other\"");
        AiReportService svc = new AiReportService(canned(wrong));
        assertThrows(IllegalArgumentException.class,
                () -> svc.nlToDataset("订单数据", "ds1", tableSchemas()));
    }

    @Test
    void nlToView_parsesAndValidates() {
        AiReportService svc = new AiReportService(canned(GOOD_VIEW_JSON));
        ViewSchema v = svc.nlToView("按城市画柱状图", "view1", datasetFields());
        assertEquals("view1", v.getId());
        assertEquals(1, v.getWidgets().size());
        assertEquals("amount", v.getWidgets().get(0).getEncode().getY());
    }

    @Test
    void nlToView_rejectsUnknownDataset() {
        String bad = GOOD_VIEW_JSON.replace("\"ds-1\"", "\"ds-404\"");
        AiReportService svc = new AiReportService(canned(bad));
        assertThrows(IllegalArgumentException.class,
                () -> svc.nlToView("看板", "view1", datasetFields()));
    }

    @Test
    void nlToView_rejectsUnknownColumn() {
        String bad = GOOD_VIEW_JSON.replace("\"y\":\"amount\"", "\"y\":\"revenue\"");
        AiReportService svc = new AiReportService(canned(bad));
        assertThrows(IllegalArgumentException.class,
                () -> svc.nlToView("看板", "view1", datasetFields()));
    }

    @Test
    void stripFence_tolerant() {
        assertEquals("{\"a\":1}", AiReportService.stripFence("```json\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", AiReportService.stripFence("```\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", AiReportService.stripFence("{\"a\":1}"));
    }

    @Test
    void llmUnconfigured_promptStyleError() {
        LlmClient unconfigured = messages -> {
            throw new IllegalStateException("LLM 未配置");
        };
        AiReportService svc = new AiReportService(unconfigured);
        assertThrows(IllegalStateException.class,
                () -> svc.nlToDataset("需求", "ds1", tableSchemas()));
        assertTrue(true);
    }

    @Test
    void viewIdOverriddenFromRequest() {
        String otherId = GOOD_VIEW_JSON.replace("\"view1\"", "\"view-llm-hallucinated\"");
        AiReportService svc = new AiReportService(canned(otherId));
        ViewSchema v = svc.nlToView("看板", "view1", datasetFields());
        assertEquals("view1", v.getId());
    }
}
