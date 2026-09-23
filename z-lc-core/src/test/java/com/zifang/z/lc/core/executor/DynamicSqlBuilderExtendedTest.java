package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.junit.Assert.*;

/**
 * DynamicSqlBuilder 扩展边界测试
 * <p>
 * 补充原有 DynamicSqlBuilderTest 未覆盖的边界路径:
 * jdbcType 映射全矩阵、coerce 类型转换边界、filter 操作符全路径、
 * dict/refEntity JOIN 生成、空字段防御、表名非法校验等.
 */
public class DynamicSqlBuilderExtendedTest {

    private DynamicSqlBuilder builder;

    @Before
    public void setUp() {
        builder = new DynamicSqlBuilder();
    }

    private EntityDefDTO entity(String tableName, FieldDefDTO... fields) {
        EntityDefDTO e = new EntityDefDTO();
        e.setTableName(tableName);
        e.setEntityCode("test");
        e.setTenantCode("t1");
        e.setFields(new ArrayList<>(Arrays.asList(fields)));
        return e;
    }

    private FieldDefDTO field(String code, String type) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setFieldName(code);
        f.setFieldType(type);
        return f;
    }

    // ===== jdbcType 静态方法全矩阵 =====

    @Test
    public void jdbcTypeShouldReturnVarcharForNullFieldType() {
        assertEquals("VARCHAR(255)", DynamicSqlBuilder.jdbcType(null, null, null));
    }

    @Test
    public void jdbcTypeShouldMapIntLongRefToBigint() {
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("INT", 10, 0));
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("LONG", 20, 0));
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("REF", 19, 0));
    }

    @Test
    public void jdbcTypeShouldMapDecimalWithDefaults() {
        assertEquals("DECIMAL(18,2)", DynamicSqlBuilder.jdbcType("DECIMAL", null, null));
        assertEquals("DECIMAL(10,0)", DynamicSqlBuilder.jdbcType("DECIMAL", 10, 0));
    }

    @Test
    public void jdbcTypeShouldMapBooleanToTinyInt() {
        assertEquals("TINYINT(1)", DynamicSqlBuilder.jdbcType("BOOLEAN", null, null));
    }

    @Test
    public void jdbcTypeShouldMapDateTypes() {
        assertEquals("DATE", DynamicSqlBuilder.jdbcType("DATE", null, null));
        assertEquals("DATETIME", DynamicSqlBuilder.jdbcType("DATETIME", null, null));
    }

    @Test
    public void jdbcTypeShouldMapTextAndJson() {
        assertEquals("TEXT", DynamicSqlBuilder.jdbcType("TEXT", null, null));
        assertEquals("JSON", DynamicSqlBuilder.jdbcType("JSON", null, null));
    }

    @Test
    public void jdbcTypeShouldMapStringWithLength() {
        assertEquals("VARCHAR(50)", DynamicSqlBuilder.jdbcType("STRING", 50, null));
        assertEquals("VARCHAR(255)", DynamicSqlBuilder.jdbcType("STRING", null, null));
    }

    @Test
    public void jdbcTypeShouldClampLengthToMinimum1() {
        assertEquals("VARCHAR(1)", DynamicSqlBuilder.jdbcType("STRING", 0, null));
        assertEquals("VARCHAR(1)", DynamicSqlBuilder.jdbcType("STRING", -5, null));
    }

    @Test
    public void jdbcTypeShouldClampScaleToMinimum0() {
        assertEquals("DECIMAL(10,0)", DynamicSqlBuilder.jdbcType("DECIMAL", 10, -3));
    }

    @Test
    public void jdbcTypeShouldFallbackToVarcharForUnknown() {
        assertEquals("VARCHAR(100)", DynamicSqlBuilder.jdbcType("UNKNOWN_TYPE", 100, null));
    }

    // ===== coerce 类型转换边界 =====

    @Test
    public void coerceShouldReturnNullForNullInput() {
        assertNull(builder.coerce(null, "STRING"));
    }

    @Test
    public void coerceShouldReturnRawForNullFieldType() {
        assertEquals("hello", builder.coerce("hello", null));
    }

    @Test
    public void coerceShouldConvertNumberToLongForIntType() {
        assertEquals(Long.valueOf(42L), builder.coerce(42, "INT"));
        assertEquals(Long.valueOf(42L), builder.coerce(42.5, "LONG"));
        assertEquals(Long.valueOf(42L), builder.coerce("42", "REF"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coerceShouldThrowForNonNumericIntType() {
        builder.coerce("abc", "INT");
    }

    @Test
    public void coerceShouldConvertNumberToDoubleForDecimalType() {
        assertEquals(Double.valueOf(3.14), builder.coerce(3.14, "DECIMAL"));
        assertEquals(Double.valueOf(3.14), builder.coerce("3.14", "DECIMAL"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coerceShouldThrowForNonNumericDecimalType() {
        builder.coerce("xyz", "DECIMAL");
    }

    @Test
    public void coerceShouldConvertToBoolean() {
        assertEquals(Boolean.TRUE, builder.coerce(true, "BOOLEAN"));
        assertEquals(Boolean.TRUE, builder.coerce("true", "BOOLEAN"));
        assertEquals(Boolean.FALSE, builder.coerce("false", "BOOLEAN"));
        assertEquals(Boolean.FALSE, builder.coerce("abc", "BOOLEAN"));
    }

    @Test
    public void coerceShouldConvertDateTypes() {
        assertNotNull(builder.coerce("2026-09-19", "DATE"));
        assertNotNull(builder.coerce("2026-09-19 10:30:00", "DATETIME"));
        // Date 对象直接返回
        java.util.Date now = new java.util.Date();
        assertEquals(now, builder.coerce(now, "DATE"));
        assertEquals(now, builder.coerce(now, "DATETIME"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coerceShouldThrowForInvalidDateFormat() {
        builder.coerce("not-a-date", "DATE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void coerceShouldThrowForInvalidDateTimeFormat() {
        builder.coerce("not-a-datetime", "DATETIME");
    }

    @Test
    public void coerceShouldConvertToStringTypes() {
        assertEquals("42", builder.coerce(42, "STRING"));
        assertEquals("hello", builder.coerce("hello", "TEXT"));
        assertEquals("{}", builder.coerce(new HashMap<>(), "JSON"));
    }

    // ===== validateTable 边界 =====

    @Test(expected = IllegalArgumentException.class)
    public void buildListSqlShouldRejectNullEntity() {
        builder.buildListSql(null, new RuntimeQueryDTO());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildListSqlShouldRejectEmptyTableName() {
        EntityDefDTO e = entity("");
        builder.buildListSql(e, new RuntimeQueryDTO());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildListSqlShouldRejectInvalidTableName() {
        EntityDefDTO e = entity("DROP TABLE users");
        builder.buildListSql(e, new RuntimeQueryDTO());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildListSqlShouldRejectNullFields() {
        EntityDefDTO e = new EntityDefDTO();
        e.setTableName("lc_test");
        e.setFields(null);
        builder.buildListSql(e, new RuntimeQueryDTO());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildListSqlShouldRejectEmptyFields() {
        EntityDefDTO e = new EntityDefDTO();
        e.setTableName("lc_test");
        e.setFields(new ArrayList<>());
        builder.buildListSql(e, new RuntimeQueryDTO());
    }

    // ===== buildInsertSql 边界 =====

    @Test
    public void buildInsertSqlShouldHandleNullFieldValues() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(e, body, "testUser");

        assertNotNull(result.sql);
        assertTrue(result.sql.startsWith("INSERT INTO"));
        // fieldValues 为空时，tenant_code 和 create_time 仍会被自动注入
        assertTrue(result.sql.contains("`tenant_code`"));
        assertTrue(result.sql.contains("`create_time`"));
    }

    @Test
    public void buildInsertSqlShouldSkipNonWhitelistFields() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "test");
        values.put("hacker_field", "DROP TABLE"); // 不在白名单
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(e, body, "testUser");
        assertFalse("不应包含黑客字段", result.sql.contains("hacker_field"));
        assertTrue(result.sql.contains("`name`"));
    }

    // ===== buildUpdateSql 边界 =====

    @Test
    public void buildUpdateSqlShouldHandleEmptyFieldValues() {
        // update_time 会自动注入，所以不会抛异常
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeCrudDTO body = new RuntimeCrudDTO();

        DynamicSqlBuilder.SqlAndParams result = builder.buildUpdateSql(e, 1L, body, "testUser");
        assertNotNull(result.sql);
        assertTrue(result.sql.contains("UPDATE"));
        assertTrue(result.sql.contains("update_time"));
    }

    // ===== filter 操作符全路径 =====

    @Test
    public void filterShouldSupportGteAndLte() {
        EntityDefDTO e = entity("lc_test", field("amount", "DECIMAL"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("amount:gte", 100);
        filters.put("amount:lte", 999);
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains(">="));
        assertTrue(result.sql.contains("<="));
        // 2 个 filter 值 + 1 个 tenant_code
        assertEquals(3, result.params.size());
    }

    @Test
    public void filterShouldSupportInWithList() {
        EntityDefDTO e = entity("lc_test", field("status", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status:in", Arrays.asList("A", "B", "C"));
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("IN"));
        // 3 个 IN 值 + 1 个 tenant_code
        assertEquals(4, result.params.size());
    }

    @Test
    public void filterShouldSupportInWithEmptyCollection() {
        EntityDefDTO e = entity("lc_test", field("status", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status:in", Collections.emptyList());
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("NULL"));
    }

    @Test
    public void filterShouldFallbackToEqForUnknownOperator() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name:unknown_op", "value");
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("="));
    }

    @Test
    public void filterShouldSkipNullValues() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", null);
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertFalse("null 值不应生成条件", result.sql.contains("name = ?"));
    }

    @Test
    public void filterShouldSkipNonWhitelistFields() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("hacker_field", "bad");
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertFalse(result.sql.contains("hacker_field"));
    }

    // ===== buildExtraSelectColumns / buildJoinClauses =====

    @Test
    public void buildExtraSelectColumnsShouldGenerateDictLabel() {
        FieldDefDTO f = field("status", "STRING");
        f.setDictCode("order_status");
        EntityDefDTO e = entity("lc_test", f);

        String cols = builder.buildExtraSelectColumns(e);
        assertTrue(cols.contains("item_label"));
        assertTrue(cols.contains("`status_label`"));
    }

    @Test
    public void buildExtraSelectColumnsShouldGenerateRefName() {
        FieldDefDTO f = field("customer_id", "REF");
        f.setRefEntity("customer");
        EntityDefDTO e = entity("lc_test", f);

        String cols = builder.buildExtraSelectColumns(e);
        assertTrue(cols.contains("entity_name"));
        assertTrue(cols.contains("`customer_id_name`"));
    }

    @Test
    public void buildExtraSelectColumnsShouldReturnEmptyForNullEntity() {
        assertEquals("", builder.buildExtraSelectColumns(null));
    }

    @Test
    public void buildJoinClausesShouldGenerateDictJoin() {
        FieldDefDTO f = field("status", "STRING");
        f.setDictCode("order_status");
        EntityDefDTO e = entity("lc_test", f);

        String join = builder.buildJoinClauses(e);
        assertTrue(join.contains("LEFT JOIN (SELECT"));
        assertTrue(join.contains("FROM z_lc_dict_item i"));
        assertTrue(join.contains("d_status"));
        assertTrue("重复字典项必须在 join 前被折叠掉", join.contains("x.id < i.id"));
    }

    @Test
    public void buildJoinClausesShouldGenerateRefJoin() {
        FieldDefDTO f = field("customer_id", "REF");
        f.setRefEntity("customer");
        EntityDefDTO e = entity("lc_test", f);

        String join = builder.buildJoinClauses(e);
        assertTrue(join.contains("LEFT JOIN `customer`"));
        assertTrue(join.contains("r_customer_id"));
    }

    @Test
    public void buildJoinClausesShouldReturnEmptyForNullEntity() {
        assertEquals("", builder.buildJoinClauses(null));
    }

    @Test
    public void buildJoinClausesShouldReturnEmptyForNullFields() {
        EntityDefDTO e = entity("lc_test");
        e.setFields(null);
        assertEquals("", builder.buildJoinClauses(e));
    }

    // ===== orderBy 边界 =====

    @Test
    public void orderByShouldDefaultToIdDescWhenNull() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("ORDER BY id DESC"));
    }

    @Test
    public void orderByShouldSupportAscDirection() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("name asc");

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("ORDER BY `name` ASC"));
    }

    @Test
    public void orderByShouldDefaultToAscForInvalidDirection() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("name invalid");

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("ORDER BY `name` ASC"));
    }

    // ===== pagination 边界 =====

    @Test
    public void paginationShouldDefaultToPage1Size20() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(null);
        query.setSize(null);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("LIMIT 0,20"));
    }

    @Test
    public void paginationShouldClampSizeTo200() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(1);
        query.setSize(500);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("LIMIT 0,200"));
    }

    @Test
    public void paginationShouldClampSizeToMinimum1() {
        EntityDefDTO e = entity("lc_test", field("name", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(1);
        query.setSize(0);

        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(e, query);
        assertTrue(result.sql.contains("LIMIT 0,20"));
    }

    @Test
    public void buildCountSqlShouldRejectNullEntity() {
        try {
            builder.buildCountSql(null, new RuntimeQueryDTO());
            fail("null entity 应抛异常");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void buildCountSqlShouldSupportFilters() {
        EntityDefDTO e = entity("lc_test", field("status", "STRING"));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status:like", "pend");
        query.setFilters(filters);

        DynamicSqlBuilder.SqlAndParams result = builder.buildCountSql(e, query);
        assertTrue(result.sql.contains("COUNT(*)"));
        assertTrue(result.sql.contains("LIKE"));
    }

    // ===== SqlAndParams 结构 =====

    @Test
    public void sqlAndParamsShouldStoreFields() {
        DynamicSqlBuilder.SqlAndParams sp = new DynamicSqlBuilder.SqlAndParams("SELECT 1", Arrays.asList(1));
        assertEquals("SELECT 1", sp.sql);
        assertEquals(1, sp.params.size());
    }

    // ===== 结构契约 =====

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull(DynamicSqlBuilder.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.executor", DynamicSqlBuilder.class.getPackage().getName());
    }
}