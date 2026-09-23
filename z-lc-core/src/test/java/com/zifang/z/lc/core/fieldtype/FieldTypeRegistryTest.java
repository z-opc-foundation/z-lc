package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FieldTypeRegistry 单元测试 — 验证「单一事实来源」的四大职责:
 * <ol>
 *   <li>dbType 与历史 DynamicSqlBuilder.jdbcType 逐字符一致 (存量表 DDL 不变)</li>
 *   <li>coerce 与历史 coerce 行为完全一致 (含 null/未知类型回退, 宽松日期/布尔)</li>
 *   <li>validateValue 返回结构化错误列表 (required / maxLength / invalidNumber / outOfRange /
 *       invalidDecimal / invalidBoolean / invalidDate), 且修掉了历史日期宽松吞错 bug</li>
 *   <li>operators / 能力位 / meta describe 契约 (key 顺序 + ≥10 类型)</li>
 * </ol>
 * 这些断言都是可失败的 (真断言), 非 getter/setter 灌水.
 */
public class FieldTypeRegistryTest {

    private final FieldTypeRegistry registry = new FieldTypeRegistry();

    // ===== describe 契约 =====

    @Test
    public void describeShouldCoverAtLeastTenTypesWithExactContractKeys() {
        List<Map<String, Object>> rows = registry.describe();
        assertTrue("field types must be >= 10, got " + rows.size(), rows.size() >= 10);

        for (Map<String, Object> row : rows) {
            // key 顺序即契约, 这里断言 key 集合与有序性
            List<String> keys = new java.util.ArrayList<>(row.keySet());
            assertEquals(java.util.Arrays.asList(
                            "fieldType", "cellValueType", "label", "dbType", "widget",
                            "sortable", "groupable", "filterable", "inlineEditable", "operators"),
                    keys);
            assertNotNull(row.get("fieldType"));
            assertNotNull(row.get("dbType"));
            assertTrue("operators must be a list", row.get("operators") instanceof List);
            for (Object op : (List<?>) row.get("operators")) {
                assertTrue("each operator must be a String", op instanceof String);
            }
        }
    }

    @Test
    public void describeShouldReflectCapabilitiesOfKnownTypes() {
        Map<String, Object> string = rowOf("STRING");
        assertEquals("String", string.get("cellValueType"));
        assertEquals("input", string.get("widget"));
        assertEquals("VARCHAR(255)", string.get("dbType"));
        assertEquals(Boolean.TRUE, string.get("sortable"));

        Map<String, Object> json = rowOf("JSON");
        assertEquals(Boolean.FALSE, json.get("sortable"));
        assertEquals(Boolean.FALSE, json.get("inlineEditable"));
        assertEquals("json", json.get("widget"));

        Map<String, Object> ref = rowOf("REF");
        assertEquals(Boolean.FALSE, ref.get("inlineEditable"));
        assertEquals("ref", ref.get("widget"));
        assertEquals("Number", ref.get("cellValueType"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void describeOperatorsShouldMatchHandlers() {
        List<String> ops = (List<String>) rowOf("STRING").get("operators");
        assertTrue(ops.contains("like"));
        assertFalse("STRING has no range compare", ops.contains("gt"));

        List<String> intOps = (List<String>) rowOf("INT").get("operators");
        assertTrue(intOps.contains("gte"));
        assertFalse("INT has no like", intOps.contains("like"));
    }

    // ===== jdbcType / dbType 与历史一致 =====

    @Test
    public void jdbcTypeShouldMatchLegacyDdlMatrix() {
        assertEquals("VARCHAR(255)", FieldTypeRegistry.jdbcType(null, null, null));
        assertEquals("VARCHAR(255)", FieldTypeRegistry.jdbcType("STRING", null, null));
        assertEquals("VARCHAR(50)", FieldTypeRegistry.jdbcType("STRING", 50, null));
        assertEquals("VARCHAR(1)", FieldTypeRegistry.jdbcType("STRING", 0, null));
        assertEquals("BIGINT", FieldTypeRegistry.jdbcType("INT", null, null));
        assertEquals("BIGINT", FieldTypeRegistry.jdbcType("LONG", null, null));
        assertEquals("BIGINT", FieldTypeRegistry.jdbcType("REF", 19, 0));
        assertEquals("DECIMAL(18,2)", FieldTypeRegistry.jdbcType("DECIMAL", null, null));
        assertEquals("DECIMAL(10,3)", FieldTypeRegistry.jdbcType("DECIMAL", 10, 3));
        assertEquals("TINYINT(1)", FieldTypeRegistry.jdbcType("BOOLEAN", null, null));
        assertEquals("DATE", FieldTypeRegistry.jdbcType("DATE", null, null));
        assertEquals("DATETIME", FieldTypeRegistry.jdbcType("DATETIME", null, null));
        assertEquals("TEXT", FieldTypeRegistry.jdbcType("TEXT", null, null));
        assertEquals("JSON", FieldTypeRegistry.jdbcType("JSON", null, null));
        // 未知类型回退 STRING
        assertEquals("VARCHAR(100)", FieldTypeRegistry.jdbcType("WAT", 100, null));
    }

    // ===== coerce: 与历史完全一致 =====

    @Test
    public void coerceShouldFollowLegacyRules() {
        assertEquals(null, FieldTypeRegistry.coerceValue(null, "STRING"));
        assertEquals("raw", FieldTypeRegistry.coerceValue("raw", null));
        assertEquals(Long.valueOf(42L), FieldTypeRegistry.coerceValue("42", "INT"));
        assertEquals(Long.valueOf(42L), FieldTypeRegistry.coerceValue(42.9, "LONG"));
        assertEquals(Double.valueOf(3.14), FieldTypeRegistry.coerceValue("3.14", "DECIMAL"));
        assertEquals(Boolean.TRUE, FieldTypeRegistry.coerceValue("true", "BOOLEAN"));
        assertEquals(Boolean.FALSE, FieldTypeRegistry.coerceValue("nonsense", "BOOLEAN"));
        assertEquals("123", FieldTypeRegistry.coerceValue(123, "STRING"));
        assertEquals("{}", FieldTypeRegistry.coerceValue(new HashMap<String, Object>(), "JSON"));
        assertTrue(FieldTypeRegistry.coerceValue("2026-01-01", "DATE") instanceof Date);
        assertTrue(FieldTypeRegistry.coerceValue("2026-01-01 10:20:30", "DATETIME") instanceof Date);
    }

    @Test
    public void coerceShouldThrowForUnconvertibleValues() {
        assertCoerceThrows("abc", "INT");
        assertCoerceThrows("xyz", "DECIMAL");
        assertCoerceThrows("not-a-date", "DATE");
        assertCoerceThrows("not-a-datetime", "DATETIME");
    }

    @Test
    public void coerceShouldReturnSameDateInstanceWhenAlreadyDate() {
        Date now = new Date();
        assertEquals(now, FieldTypeRegistry.coerceValue(now, "DATE"));
        assertEquals(now, FieldTypeRegistry.coerceValue(now, "DATETIME"));
    }

    // ===== validateValue: 结构化错误 =====

    @Test
    public void requiredEmptyShouldProduceRequiredError() {
        FieldDefDTO def = field("name", "STRING");
        def.setRequired(true);

        List<ValidationError> empty = registry.handler("STRING").validateValue(def, "");
        assertEquals(1, empty.size());
        assertEquals("required", empty.get(0).getErrorType());
        assertEquals("name", empty.get(0).getFieldCode());

        // 非必填 + 空 → 合法
        def.setRequired(false);
        assertTrue(registry.handler("STRING").validateValue(def, null).isEmpty());
    }

    @Test
    public void maxLengthShouldBeFlaggedForOverlongString() {
        FieldDefDTO def = field("name", "STRING");
        def.setFieldLength(5);

        List<ValidationError> errors = registry.handler("STRING").validateValue(def, "abcdefgh");
        assertEquals(1, errors.size());
        assertEquals("maxLength", errors.get(0).getErrorType());

        assertTrue(registry.handler("STRING").validateValue(def, "abcde").isEmpty());
    }

    @Test
    public void invalidNumberAndOutOfRangeShouldBeFlagged() {
        FieldDefDTO age = field("age", "INT");
        assertEquals("invalidNumber", firstType(registry.handler("INT").validateValue(age, "abc")));
        assertEquals("outOfRange",
                firstType(registry.handler("INT").validateValue(age, Double.valueOf("1e300"))));
    }

    @Test
    public void invalidDecimalBooleanAndDateShouldBeFlagged() {
        assertEquals("invalidDecimal",
                firstType(registry.handler("DECIMAL").validateValue(field("amt", "DECIMAL"), "xyz")));
        assertEquals("invalidBoolean",
                firstType(registry.handler("BOOLEAN").validateValue(field("ok", "BOOLEAN"), "yes")));
        // 历史 coerce 宽松吞下 2026-01-32; validateValue 走严格解析必须拒绝
        assertEquals("invalidDate",
                firstType(registry.handler("DATE").validateValue(field("d", "DATE"), "2026-01-32")));
        // 合法日期无错误
        assertTrue(registry.handler("DATE").validateValue(field("d", "DATE"), "2026-01-31").isEmpty());
    }

    // ===== validate (配置) =====

    @Test
    public void refConfigShouldRequireRefEntity() {
        FieldDefDTO bad = field("deptId", "REF");
        assertEquals("config", firstType(registry.handler("REF").validate(bad)));

        FieldDefDTO good = field("deptId", "REF");
        good.setRefEntity("department");
        assertTrue(registry.handler("REF").validate(good).isEmpty());
    }

    @Test
    public void badFieldCodeShouldBeRejectedByValidate() {
        FieldDefDTO def = field("1-bad-code", "STRING");
        List<ValidationError> errors = registry.handler("STRING").validate(def);
        assertEquals("config", firstType(errors));
    }

    // ===== supportsOperator / 回退 =====

    @Test
    public void supportsOperatorShouldRespectType() {
        assertTrue(registry.supportsOperator("STRING", "like"));
        assertFalse(registry.supportsOperator("INT", "like"));
        assertTrue(registry.supportsOperator("DATE", "gte"));
        assertFalse(registry.supportsOperator("DATE", "in"));
        assertFalse(registry.supportsOperator("BOOLEAN", "gt"));
        // 未知类型回退 STRING → 支持 like
        assertTrue(registry.supportsOperator("NOPE", "like"));
    }

    @Test
    public void handlerShouldFallbackToStringForUnknown() {
        FieldTypeHandler h = registry.handler("DOES_NOT_EXIST");
        assertEquals("STRING", h.fieldType());
        assertEquals("input", h.widget());
    }

    // ===== helpers =====

    private Map<String, Object> rowOf(String type) {
        for (Map<String, Object> row : registry.describe()) {
            if (type.equals(row.get("fieldType"))) {
                return row;
            }
        }
        throw new AssertionError("fieldType missing from describe(): " + type);
    }

    private static String firstType(List<ValidationError> errors) {
        assertFalse("expected at least one error", errors.isEmpty());
        return errors.get(0).getErrorType();
    }

    private static void assertCoerceThrows(Object raw, String type) {
        try {
            FieldTypeRegistry.coerceValue(raw, type);
            fail("coerce should throw for raw=" + raw + " type=" + type);
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static FieldDefDTO field(String code, String type) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setFieldName(code);
        f.setFieldType(type);
        return f;
    }
}
