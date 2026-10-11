package com.zifang.z.report.render;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.common.schema.WidgetEncode;
import com.zifang.z.report.common.schema.WidgetSpec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EChartsRenderServiceTest {

    private final EChartsRenderService service = new EChartsRenderService();
    private final ObjectMapper mapper = new ObjectMapper();

    private QueryResult data() {
        return QueryResult.of(
                Arrays.asList(FieldDef.of("city", com.zifang.z.report.common.schema.FieldType.STRING),
                        FieldDef.of("amount", com.zifang.z.report.common.schema.FieldType.INT)),
                Arrays.asList(row("city", "hangzhou", "amount", 100),
                        row("city", "shanghai", "amount", 200),
                        row("city", "hangzhou", "amount", 50)));
    }

    private Map<String, Object> row(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private WidgetSpec widget(ChartType type, String dim, String measure) {
        WidgetSpec w = new WidgetSpec();
        w.setId("w1");
        w.setType(type);
        w.setTitle("t");
        WidgetEncode enc = new WidgetEncode();
        enc.setX(dim);
        enc.setY(measure);
        enc.setCategory(dim);
        enc.setValue(measure);
        w.setEncode(enc);
        return w;
    }

    @Test
    void barAggregatesByDimension() {
        JsonNode option = mapper.valueToTree(service.render(widget(ChartType.BAR, "city", "amount"), data()));
        assertEquals("hangzhou", option.path("xAxis").get(0).asText());
        assertEquals("shanghai", option.path("xAxis").get(1).asText());
        assertEquals(150, option.path("series").get(0).path("data").get(0).asInt());
        assertEquals(200, option.path("series").get(0).path("data").get(1).asInt());
        assertEquals("bar", option.path("series").get(0).path("type").asText());
    }

    @Test
    void pieEmitsNameValuePairs() {
        JsonNode option = mapper.valueToTree(service.render(widget(ChartType.PIE, "city", "amount"), data()));
        assertEquals("pie", option.path("series").get(0).path("type").asText());
        assertEquals("hangzhou", option.path("series").get(0).path("data").get(0).path("name").asText());
        assertEquals(150, option.path("series").get(0).path("data").get(0).path("value").asInt());
    }

    @Test
    void kpiAggregatesWholeTableWhenNoDimension() {
        WidgetSpec w = widget(ChartType.KPI, null, "amount");
        w.getEncode().setX(null);
        w.getEncode().setCategory(null);
        // 无维度 → 全表聚合 (KPI 指标卡语义): SUM(amount) = 100+200+50
        JsonNode option = mapper.valueToTree(service.render(w, data()));
        assertEquals("kpi", option.path("type").asText());
        assertEquals(350, option.path("value").asInt());
        assertEquals("t", option.path("label").asText());
    }

    @Test
    void rawWidgetEmitsWhateverTheShapeProgramProduces() {
        WidgetSpec w = new WidgetSpec();
        w.setId("w-raw");
        w.setType(ChartType.RAW);
        w.setShape(Arrays.asList(
                map("op", "group", "by", "city", "items", "lines",
                        "agg", map("total", "SUM(amount)"),
                        "into", map("city", "${city}", "total", "${total}",
                                "lines", map("op", "map", "of", "lines",
                                        "into", map("amount", "${amount}")))),
                map("op", "keyBy", "key", "city")));
        JsonNode option = mapper.valueToTree(service.render(w, data()));
        assertEquals("raw", option.path("type").asText());
        JsonNode hangzhou = option.path("value").path("hangzhou");
        assertEquals(150, hangzhou.path("total").asInt());
        assertEquals(2, hangzhou.path("lines").size());
        assertEquals(100, hangzhou.path("lines").get(0).path("amount").asInt());
    }

    @Test
    void rawWidgetWithoutShapeFailsInsteadOfRenderingEmpty() {
        WidgetSpec w = new WidgetSpec();
        w.setId("w-raw-bad");
        w.setType(ChartType.RAW);
        assertThrows(IllegalArgumentException.class, () -> service.render(w, data()));
    }

    private Map<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
