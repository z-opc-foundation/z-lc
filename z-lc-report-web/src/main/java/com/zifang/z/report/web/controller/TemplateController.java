package com.zifang.z.report.web.controller;

import com.zifang.z.report.common.schema.AggType;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.common.schema.ViewSchema;
import com.zifang.z.report.common.schema.WidgetEncode;
import com.zifang.z.report.common.schema.WidgetLayout;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.datasource.impl.MemoryDataSource;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.web.store.SchemaStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置模板 API (M2): 开箱即用的示例报表书。
 * <p>
 * apply 幂等约定: demo 源/数据集/视图按固定 id 复用 (已存在不重建), 报表书每次新建
 * (用户可基于模板衍生多本书)。模板数据走 MemoryDataSource, 与真实数据源隔离。
 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    public static final String DEMO_SOURCE_ID = "demo-mem";
    public static final String DEMO_DATASET_ID = "demo-ds-orders";
    public static final String DEMO_VIEW_ID = "demo-view-sales";

    private final DataSourceRegistry registry;
    private final SchemaStore store;
    private final com.zifang.z.report.book.BookService bookService;

    public TemplateController(DataSourceRegistry registry, SchemaStore store,
                              com.zifang.z.report.book.BookService bookService) {
        this.registry = registry;
        this.store = store;
        this.bookService = bookService;
    }

    /** 模板清单 (静态声明, 前端模板画廊用) */
    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> sales = new LinkedHashMap<>();
        sales.put("id", "sales-overview");
        sales.put("title", "销售概览");
        sales.put("description", "内存演示数据: 城市销售额柱图 + 总额 KPI + 明细表");
        sales.put("datasetId", DEMO_DATASET_ID);
        out.add(sales);
        return out;
    }

    /** 应用模板: 幂等装配 demo 源/数据集/视图, 并创建一本挂好节点的报表书 */
    @PostMapping("/{tplId}/apply")
    public Map<String, Object> apply(@PathVariable("tplId") String tplId) {
        if (!"sales-overview".equals(tplId)) {
            throw new IllegalArgumentException("unknown template: " + tplId);
        }
        ensureDemoSource();
        ensureDemoDataset();
        ensureDemoView();

        String bookId = bookService.createBook("销售概览 (示例书)");
        String pageId = bookService.addNode(bookId, null,
                com.zifang.z.report.common.schema.BookNodeType.PAGE, "销售概览页", DEMO_VIEW_ID);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bookId", bookId);
        out.put("pageId", pageId);
        out.put("viewId", DEMO_VIEW_ID);
        out.put("datasetId", DEMO_DATASET_ID);
        out.put("sourceId", DEMO_SOURCE_ID);
        return out;
    }

    private void ensureDemoSource() {
        if (registry.get(DEMO_SOURCE_ID) != null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("hangzhou", "线上", 150, 3));
        rows.add(row("hangzhou", "线下", 80, 2));
        rows.add(row("shanghai", "线上", 200, 5));
        rows.add(row("shanghai", "线下", 120, 4));
        rows.add(row("beijing", "线上", 90, 2));
        rows.add(row("beijing", "线下", 60, 1));
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        tables.put("orders", rows);
        registry.register(new MemoryDataSource(
                new DataSourceDef(DEMO_SOURCE_ID, "内置演示数据 (内存)", DataSourceType.MEMORY, null),
                tables, null));
    }

    private Map<String, Object> row(String city, String channel, int amount, int qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("city", city);
        m.put("channel", channel);
        m.put("amount", amount);
        m.put("qty", qty);
        return m;
    }

    private void ensureDemoDataset() {
        if (store.getDataset(DEMO_DATASET_ID) != null) {
            return;
        }
        DatasetSchema ds = new DatasetSchema();
        ds.setId(DEMO_DATASET_ID);
        ds.setName("演示订单");
        ds.setSourceId(DEMO_SOURCE_ID);
        ds.setBaseTable("orders");
        ds.setFields(Arrays.asList(
                field("city", FieldType.STRING),
                field("channel", FieldType.STRING),
                field("amount", FieldType.INT),
                field("qty", FieldType.INT)));
        store.putDataset(ds);
    }

    private FieldDef field(String name, FieldType type) {
        FieldDef f = new FieldDef();
        f.setName(name);
        f.setType(type);
        return f;
    }

    private void ensureDemoView() {
        if (store.getView(DEMO_VIEW_ID) != null) {
            return;
        }
        ViewSchema view = new ViewSchema();
        view.setId(DEMO_VIEW_ID);
        view.setTitle("销售概览");

        WidgetSpec bar = new WidgetSpec();
        bar.setId("w-bar");
        bar.setType(ChartType.BAR);
        bar.setTitle("各城市销售额");
        bar.setDatasetId(DEMO_DATASET_ID);
        bar.setAgg(AggType.SUM);
        WidgetEncode barEnc = new WidgetEncode();
        barEnc.setX("city");
        barEnc.setY("amount");
        bar.setEncode(barEnc);

        WidgetSpec kpi = new WidgetSpec();
        kpi.setId("w-kpi");
        kpi.setType(ChartType.KPI);
        kpi.setTitle("销售总额");
        kpi.setDatasetId(DEMO_DATASET_ID);
        kpi.setAgg(AggType.SUM);
        WidgetEncode kpiEnc = new WidgetEncode();
        kpiEnc.setValue("amount");
        kpi.setEncode(kpiEnc);

        WidgetSpec table = new WidgetSpec();
        table.setId("w-table");
        table.setType(ChartType.TABLE);
        table.setTitle("订单明细");
        table.setDatasetId(DEMO_DATASET_ID);
        WidgetEncode tblEnc = new WidgetEncode();
        tblEnc.setColumns(Arrays.asList("city", "channel", "amount", "qty"));
        table.setEncode(tblEnc);

        view.setWidgets(Arrays.asList(kpi, bar, table));
        view.setLayout(Arrays.asList(
                layout("w-kpi", 0, 0, 4, 4),
                layout("w-bar", 4, 0, 8, 8),
                layout("w-table", 0, 8, 12, 8)));
        store.putView(view);
    }

    private WidgetLayout layout(String widgetId, int x, int y, int w, int h) {
        WidgetLayout l = new WidgetLayout();
        l.setWidgetId(widgetId);
        l.setX(x);
        l.setY(y);
        l.setW(w);
        l.setH(h);
        return l;
    }
}
