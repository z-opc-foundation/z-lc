package com.zifang.z.report.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.ViewSchema;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.ExprParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 报表服务 (M3 核心链路): 自然语言 → DatasetSchema / ViewSchema。
 * <p>
 * 两段式产物 (方案 6.2): NL→数据集定义, NL→视图页 schema。
 * 契约校验在本地闭环 (LLM 只产 JSON, 结构合法性/引用合法性/表达式可解析性全部本地校验),
 * LLM 幻觉产物不会流入执行层。
 * <p>
 * LlmClient 为注入 SPI (OpenAI 兼容实现 / 未来 llm-gateway adapter)。
 */
public class AiReportService {

    private final LlmClient llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiReportService(LlmClient llm) {
        this.llm = llm;
    }

    /** 自然语言 → 数据集定义 */
    public DatasetSchema nlToDataset(String requirement, String sourceId,
                                     Map<String, Map<String, String>> tableSchemas) {
        String sys = "你是报表数据集设计器。只输出一个 JSON 对象 (不加任何解释/markdown 代码围栏), "
                + "字段: {\"id\":\"ds-xxx\",\"name\":\"中文名\",\"sourceId\":\"" + sourceId + "\","
                + "\"baseTable\":\"表名\",\"joins\":[],"
                + "\"filters\":[{\"field\":\"列\",\"op\":\"EQ|NE|GT|GE|LT|LE|IN|LIKE\",\"value\":...}],"
                + "\"computedFields\":[{\"name\":\"列名\",\"type\":\"STRING|INT|DOUBLE|BOOL|DATE\","
                + "\"expr\":\"表达式\"}]}\n"
                + "规则:\n"
                + "- baseTable 必须是给定表之一; joins 可为空数组\n"
                + "- 表达式仅可用: 列名, 数字/字符串/true/false 字面量, + - * / 括号, "
                + "比较(> >= < <= = !=), AND OR NOT, IS NULL, IN (...), LIKE; 禁止函数调用\n"
                + "- 计算字段仅在需求确有必要时添加, 可为空数组";
        String user = "需求: " + requirement + "\n可用表与列:\n" + describeTables(tableSchemas);
        String out = llm.chat(java.util.Arrays.asList(LlmMessage.system(sys), LlmMessage.user(user)));
        DatasetSchema schema = readJson(stripFence(out), DatasetSchema.class);
        validateDataset(schema, sourceId, tableSchemas);
        return schema;
    }

    /** 自然语言 → 视图页 schema */
    public ViewSchema nlToView(String requirement, String viewId,
                               Map<String, List<FieldDef>> datasetFields) {
        String sys = "你是报表视图设计器。只输出一个 JSON 对象 (不加任何解释/markdown 代码围栏), "
                + "字段: {\"id\":\"" + viewId + "\",\"title\":\"中文名\",\"version\":1,"
                + "\"widgets\":[{\"id\":\"w1\",\"type\":\"LINE|BAR|PIE|TABLE|KPI\",\"title\":\"图表名\","
                + "\"datasetId\":\"数据集id\",\"encode\":{\"x\":\"维度列\",\"y\":\"度量列\","
                + "\"category\":\"维度列\",\"value\":\"度量列\"},\"agg\":\"SUM|COUNT|AVG|MIN|MAX\"}],"
                + "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4}]}\n"
                + "规则:\n"
                + "- x/category 放维度列 (字符串/日期), y/value 放度量列 (数值); KPI 只需要 value\n"
                + "- layout 网格 12 列, w+h 覆盖全部 widget; 每个widget必须有对应 layout 项\n"
                + "- encode 与 agg 必须给出";
        String user = "需求: " + requirement + "\n可用数据集与列:\n" + describeDatasets(datasetFields);
        String out = llm.chat(java.util.Arrays.asList(LlmMessage.system(sys), LlmMessage.user(user)));
        ViewSchema view = readJson(stripFence(out), ViewSchema.class);
        validateView(view, viewId, datasetFields);
        return view;
    }

    // ==================== 校验 (本地闭环, 拦截幻觉) ====================

    private void validateDataset(DatasetSchema s, String sourceId,
                                 Map<String, Map<String, String>> tableSchemas) {
        if (s.getSourceId() == null || !s.getSourceId().equals(sourceId)) {
            throw new IllegalArgumentException("AI produced wrong sourceId: " + s.getSourceId());
        }
        if (s.getBaseTable() == null || !tableSchemas.containsKey(s.getBaseTable())) {
            throw new IllegalArgumentException("AI produced unknown baseTable: " + s.getBaseTable());
        }
        if (s.getComputedFields() != null) {
            for (com.zifang.z.report.common.schema.ComputedField cf : s.getComputedFields()) {
                // 表达式必须可解析 (白名单语法) 才允许入库
                try {
                    ExprParser.parse(cf.getExpr());
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            "AI produced invalid expr for computedField [" + cf.getName() + "]: " + e.getMessage());
                }
            }
        }
    }

    private void validateView(ViewSchema v, String expectedViewId,
                              Map<String, List<FieldDef>> datasetFields) {
        if (expectedViewId != null && !expectedViewId.equals(v.getId())) {
            v.setId(expectedViewId); // id 以请求为准
        }
        Set<String> widgetIds = new HashSet<>();
        if (v.getWidgets() != null) {
            for (WidgetSpec w : v.getWidgets()) {
                if (w.getDatasetId() == null || !datasetFields.containsKey(w.getDatasetId())) {
                    throw new IllegalArgumentException("AI produced unknown datasetId: " + w.getDatasetId());
                }
                Set<String> cols = new HashSet<>();
                for (FieldDef f : datasetFields.get(w.getDatasetId())) {
                    cols.add(f.getName());
                }
                com.zifang.z.report.common.schema.WidgetEncode enc = w.getEncode();
                if (enc != null) {
                    checkCol(enc.getX(), cols, w.getId());
                    checkCol(enc.getY(), cols, w.getId());
                    checkCol(enc.getCategory(), cols, w.getId());
                    checkCol(enc.getValue(), cols, w.getId());
                }
                widgetIds.add(w.getId());
            }
        }
        if (v.getLayout() != null) {
            for (com.zifang.z.report.common.schema.WidgetLayout l : v.getLayout()) {
                if (!widgetIds.contains(l.getWidgetId())) {
                    throw new IllegalArgumentException("layout references unknown widget: " + l.getWidgetId());
                }
            }
        }
    }

    private void checkCol(String col, Set<String> known, String widgetId) {
        if (col != null && !known.contains(col)) {
            throw new IllegalArgumentException(
                    "AI produced unknown column [" + col + "] in widget " + widgetId);
        }
    }

    // ==================== 工具 ====================

    private String describeTables(Map<String, Map<String, String>> tableSchemas) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> e : tableSchemas.entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ");
            List<String> cols = new ArrayList<>();
            for (Map.Entry<String, String> c : e.getValue().entrySet()) {
                cols.add(c.getKey() + "(" + c.getValue() + ")");
            }
            sb.append(String.join(", ", cols)).append("\n");
        }
        return sb.toString();
    }

    private String describeDatasets(Map<String, List<FieldDef>> datasetFields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<FieldDef>> e : datasetFields.entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ");
            List<String> cols = new ArrayList<>();
            for (FieldDef f : e.getValue()) {
                cols.add(f.getName() + "(" + f.getType() + ")");
            }
            sb.append(String.join(", ", cols)).append("\n");
        }
        return sb.toString();
    }

    /** 剥离 markdown 代码围栏 (宽容 LLM 输出习惯) */
    static String stripFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) {
                t = t.substring(0, lastFence);
            }
            return t.trim();
        }
        return t;
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI output is not valid " + type.getSimpleName()
                    + " JSON: " + e.getMessage());
        }
    }
}
