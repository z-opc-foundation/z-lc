package com.zifang.z.lc.core.page;

import com.zifang.util.json.JsonCursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 页面 schema 校验器单测 (V2 页面域): 合法结构放行, 结构/对齐/越界各拦一道。
 */
class PageSchemaValidatorTest {

    private static final String VALID = "{"
            + "\"version\":1,"
            + "\"datasets\":{\"ds1\":{\"sourceId\":\"dev-h2\",\"baseTable\":\"z_lc_app\"}},"
            + "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4},"
            + "             {\"widgetId\":\"w2\",\"x\":6,\"y\":0,\"w\":6,\"h\":4}],"
            + "\"widgets\":[{\"id\":\"w1\",\"type\":\"TABLE\",\"title\":\"清单\",\"datasetId\":\"ds1\"},"
            + "             {\"id\":\"w2\",\"type\":\"FORM\",\"entityCode\":\"app\"}]}";

    @Test
    void validSchemaPasses() {
        JsonCursor root = PageSchemaValidator.validate(VALID);
        assertNotNull(root);
        assertEquals(2, root.path("widgets").size());
    }

    @Test
    void emptyAndBrokenJsonRejected() {
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate("  "));
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate("{not json"));
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate("[1,2]"));
    }

    @Test
    void widgetsArrayRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> PageSchemaValidator.validate("{\"layout\":[]}"));
        assertThrows(IllegalArgumentException.class,
                () -> PageSchemaValidator.validate("{\"widgets\":{},\"layout\":[]}"));
    }

    @Test
    void widgetIdMustBeUnique() {
        String dup = VALID.replace("\"id\":\"w2\",\"type\":\"FORM\"", "\"id\":\"w1\",\"type\":\"FORM\"")
                .replace("{\"widgetId\":\"w2\",\"x\":6", "{\"widgetId\":\"w1\",\"x\":6");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(dup));
    }

    @Test
    void unknownTypeRejected() {
        String bad = VALID.replace("\"type\":\"TABLE\"", "\"type\":\"MAP\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(bad));
    }

    @Test
    void formRequiresEntityCode() {
        String bad = VALID.replace("\"type\":\"FORM\",\"entityCode\":\"app\"", "\"type\":\"FORM\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(bad));
    }

    @Test
    void nonFormRequiresKnownDataset() {
        String noBinding = VALID.replace("\"type\":\"TABLE\",\"title\":\"清单\",\"datasetId\":\"ds1\"",
                "\"type\":\"TABLE\",\"title\":\"清单\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(noBinding));

        String unknownDs = VALID.replace("\"datasetId\":\"ds1\"", "\"datasetId\":\"ds_x\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(unknownDs));

        String dsNoTable = VALID.replace("\"baseTable\":\"z_lc_app\"", "\"baseTable\":\"\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(dsNoTable));
    }

    @Test
    void layoutMustPairWithWidgets() {
        // widget 有两个, layout 只放一个 → w2 缺 layout 单元
        String missingCell = VALID.replace(
                "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4},"
                        + "             {\"widgetId\":\"w2\",\"x\":6,\"y\":0,\"w\":6,\"h\":4}]",
                "\"layout\":[{\"widgetId\":\"w1\",\"x\":0,\"y\":0,\"w\":6,\"h\":4}]");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(missingCell));

        // layout 引用不存在的 widget
        String ghost = VALID.replace("{\"widgetId\":\"w2\"", "{\"widgetId\":\"w9\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(ghost));
    }

    @Test
    void layoutBoundsEnforced() {
        String overflow = VALID.replace("\"x\":6,\"y\":0,\"w\":6", "\"x\":8,\"y\":0,\"w\":6");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(overflow));

        String badH = VALID.replace("\"w\":6,\"h\":4}", "\"w\":6,\"h\":0}");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(badH));
    }

    @Test
    void illegalAggRejected() {
        String bad = VALID.replace("\"type\":\"TABLE\"", "\"type\":\"TABLE\",\"agg\":\"MEDIAN\"");
        assertThrows(IllegalArgumentException.class, () -> PageSchemaValidator.validate(bad));
    }
}
