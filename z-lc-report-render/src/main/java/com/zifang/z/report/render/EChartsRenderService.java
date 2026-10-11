package com.zifang.z.report.render;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;
import com.zifang.z.report.render.impl.BarOptionBuilder;
import com.zifang.z.report.render.impl.KpiOptionBuilder;
import com.zifang.z.report.render.impl.LineOptionBuilder;
import com.zifang.z.report.render.impl.PieOptionBuilder;
import com.zifang.z.report.render.impl.RawOptionBuilder;
import com.zifang.z.report.render.impl.TableOptionBuilder;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 渲染服务: QueryResult (明细) → 聚合/组织 → echarts option (或渲染器约定结构)。
 * <p>
 * builder 按 ChartType 注册, M1 内置五件套 + RAW (对象整形语言产出的任意结构);
 * 扩展图表新增 builder 即可。
 */
public class EChartsRenderService {

    private final Map<ChartType, ChartOptionBuilder> builders = new EnumMap<>(ChartType.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public EChartsRenderService() {
        register(new LineOptionBuilder());
        register(new BarOptionBuilder());
        register(new PieOptionBuilder());
        register(new TableOptionBuilder());
        register(new KpiOptionBuilder());
        register(new RawOptionBuilder());
    }

    public void register(ChartOptionBuilder builder) {
        builders.put(builder.type(), builder);
    }

    public ObjectNode render(WidgetSpec widget, QueryResult data) {
        ChartOptionBuilder builder = builders.get(widget.getType());
        if (builder == null) {
            throw new IllegalArgumentException("no option builder for chart type: " + widget.getType());
        }
        return builder.build(widget, toTable(data), mapper);
    }

    /** QueryResult → Table (列类型取自契约; 值缺失按 STRING 兜底) */
    private Table toTable(QueryResult result) {
        LinkedHashMap<String, FieldType> defs = new LinkedHashMap<>();
        if (result.getColumns() != null) {
            for (com.zifang.z.report.common.schema.FieldDef f : result.getColumns()) {
                defs.put(f.getName(), f.getType() == null ? FieldType.STRING : f.getType());
            }
        }
        return Table.fromRows(result.getRows(), defs);
    }
}
