package com.zifang.z.lc.core.page;

import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;
import com.zifang.z.lc.schema.util.SchemaCodec;
import com.zifang.z.lc.schema.util.SchemaValidator;
import com.zifang.z.lc.schema.util.WidgetSchemaAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单一 schema 口径的回归测试。
 *
 * <p>要点：旧报表写法不是第二种 schema，而是同一模型的扁平淡法。这里锁两件事：
 * <ol>
 *   <li>设计器产出的 elements 形态能直接过校验（这是保存进库的门槛）</li>
 *   <li>旧 widgets 写法归一化后与前者结构一致（存库/渲染只有一条路径）</li>
 * </ol>
 */
class PageSchemaUnifiedTest {

    /** 设计器真实产出的形态：elements 扁平表 + layout.structure + logic */
    private static final String DESIGNER_SCHEMA = "{"
            + "\"version\":\"1.0\","
            + "\"elements\":{"
            + "\"root\":{\"type\":\"container\",\"props\":{\"direction\":\"column\",\"gap\":12}},"
            + "\"card-1\":{\"type\":\"card\",\"props\":{\"title\":\"工单概览\"}},"
            + "\"input-1\":{\"type\":\"input\",\"props\":{\"label\":\"工单标题\",\"name\":\"title\",\"required\":true}},"
            + "\"table-1\":{\"type\":\"table\",\"props\":{\"title\":\"工单列表\",\"datasetId\":\"ds1\"}}"
            + "},"
            + "\"layout\":{\"root\":\"root\",\"structure\":{"
            + "\"root\":{\"children\":[\"card-1\",\"table-1\"]},"
            + "\"card-1\":{\"children\":[\"input-1\"]},"
            + "\"input-1\":{\"children\":[]},"
            + "\"table-1\":{\"children\":[]}"
            + "}},"
            + "\"datasets\":{\"ds1\":{\"sourceId\":\"dev-h2\",\"baseTable\":\"z_lc_ticket\"}},"
            + "\"logic\":{\"flowBinds\":{\"input-1.onChange\":[\"flow-1\"]},\"flows\":{}}"
            + "}";

    /** 旧报表写法：widgets + 12 栅格 layout 数组 */
    private static final String LEGACY_SCHEMA = "{"
            + "\"version\":1,"
            + "\"datasets\":{\"ds1\":{\"sourceId\":\"dev-h2\",\"baseTable\":\"z_lc_app\"}},"
            + "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4}],"
            + "\"widgets\":[{\"id\":\"w1\",\"type\":\"TABLE\",\"title\":\"清单\",\"datasetId\":\"ds1\"}]}";

    @Test
    void designerSchemaPassesValidation() throws Exception {
        // 过校验（保存进库的门槛）
        PageSchemaValidator.validate(DESIGNER_SCHEMA);

        // 且解码后确实是标准形态
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_SCHEMA);
        assertEquals(4, schema.getElements().size());
        assertEquals("root", schema.getLayout().getRoot());
        assertTrue(SchemaValidator.validate(schema).ok(), SchemaValidator.validate(schema).toString());
    }

    @Test
    void legacyWidgetsFoldIntoTheSameModel() {
        PageSchema schema = WidgetSchemaAdapter.fromLegacyWidgets(
                readTree(LEGACY_SCHEMA));

        // 一个 widget 变成一个带 rect 的节点，挂在根容器下
        assertEquals(2, schema.getElements().size());
        SchemaNode table = schema.getElements().get("w1");
        assertNotNull(table);
        assertEquals("table", table.getType());
        assertEquals("TABLE", table.getProps().get("widgetType"));
        assertNotNull(table.getRect());
        assertEquals(0, table.getRect().getX());
        assertEquals(6, table.getRect().getW());
        assertEquals("w1", schema.getLayout().getStructure().get("root").getChildren().get(0));
        assertEquals(PageSchema.Layout.MODE_ABSOLUTE, schema.getLayout().getMode());

        // 归一化出来的结构必须能过同一套结构校验
        assertTrue(SchemaValidator.validate(schema).ok(), SchemaValidator.validate(schema).toString());
    }

    @Test
    void bothWritingsPassTheSingleEntryPoint() {
        // 同一个校验入口，两种写法都放行
        assertNotNull(PageSchemaValidator.validate(DESIGNER_SCHEMA));
        assertNotNull(PageSchemaValidator.validate(LEGACY_SCHEMA));
    }

    @Test
    void unrecognizableSchemaIsRejected() {
        try {
            PageSchemaValidator.validate("{\"foo\":1}");
            throw new AssertionError("应当拒绝无法识别的结构");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("elements") || expected.getMessage().contains("widgets"),
                    "报错要能指向缺什么: " + expected.getMessage());
        }
    }

    private static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return SchemaCodec.mapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
