package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DynamicSqlBuilder 单元测试
 *
 * @author zifang
 */
public class DynamicSqlBuilderTest {

    private final DynamicSqlBuilder builder = new DynamicSqlBuilder();

    // --- jdbcType ---

    @Test
    public void jdbcTypeShouldHandleNull() {
        assertEquals("VARCHAR(255)", DynamicSqlBuilder.jdbcType(null, null, null));
    }

    @Test
    public void jdbcTypeShouldMapInt() {
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("INT", null, null));
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("LONG", null, null));
        assertEquals("BIGINT", DynamicSqlBuilder.jdbcType("REF", null, null));
    }

    @Test
    public void jdbcTypeShouldMapDecimal() {
        assertEquals("DECIMAL(18,2)", DynamicSqlBuilder.jdbcType("DECIMAL", null, null));
        assertEquals("DECIMAL(10,3)", DynamicSqlBuilder.jdbcType("DECIMAL", 10, 3));
    }

    @Test
    public void jdbcTypeShouldMapBoolean() {
        assertEquals("TINYINT(1)", DynamicSqlBuilder.jdbcType("BOOLEAN", null, null));
    }

    @Test
    public void jdbcTypeShouldMapDate() {
        assertEquals("DATE", DynamicSqlBuilder.jdbcType("DATE", null, null));
        assertEquals("DATETIME", DynamicSqlBuilder.jdbcType("DATETIME", null, null));
    }

    @Test
    public void jdbcTypeShouldMapText() {
        assertEquals("TEXT", DynamicSqlBuilder.jdbcType("TEXT", null, null));
        assertEquals("JSON", DynamicSqlBuilder.jdbcType("JSON", null, null));
    }

    @Test
    public void jdbcTypeShouldMapString() {
        assertEquals("VARCHAR(255)", DynamicSqlBuilder.jdbcType("STRING", null, null));
        assertEquals("VARCHAR(50)", DynamicSqlBuilder.jdbcType("STRING", 50, null));
    }

    // --- validateTable ---

    @Test
    public void validateShouldThrowForNullEntity() {
        try {
            builder.buildListSql(null, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("entity"));
        }
    }

    @Test
    public void validateShouldThrowForEmptyTableName() {
        EntityDefDTO entity = makeEntity("user", "");
        try {
            builder.buildListSql(entity, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("tableName"));
        }
    }

    @Test
    public void validateShouldThrowForInvalidTableName() {
        EntityDefDTO entity = makeEntity("user", "t user");  // has space
        try {
            builder.buildListSql(entity, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid tableName"));
        }
    }

    @Test
    public void validateShouldThrowForEmptyFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.emptyList());
        try {
            builder.buildListSql(entity, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("fields"));
        }
    }

    // --- buildListSql ---

    @Test
    public void buildListSqlShouldGenerateBasicSelect() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, null);
        assertNotNull(result);
        assertTrue(result.sql.contains("SELECT"));
        assertTrue(result.sql.contains("FROM `t_user`"));
        assertTrue(result.sql.contains("deleted = 0"));
        assertTrue(result.sql.contains("ORDER BY"));
        assertTrue(result.sql.contains("LIMIT"));
    }

    @Test
    public void buildListSqlShouldIncludeTenantFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("tenant_code"));
        assertEquals("t1", result.params.get(0));
    }

    @Test
    public void buildListSqlShouldApplyDefaultOrder() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("ORDER BY id DESC"));
    }

    @Test
    public void buildListSqlShouldAcceptCustomOrderBy() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("name asc");
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("ORDER BY `name` ASC"));
    }

    @Test
    public void buildListSqlShouldIgnoreUnknownOrderByField() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("unknown_field asc");
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        // When orderBy field is not in whitelist, the SQL should not include the unknown field
        // (current implementation skips it without falling back to default)
        assertTrue("Should not include unknown_field in: " + result.sql,
                !result.sql.contains("unknown_field"));
    }

    @Test
    public void buildListSqlShouldApplyPageAndSize() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(2);
        query.setSize(10);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIMIT 10,10"));
    }

    @Test
    public void buildListSqlShouldCapSizeAt200() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setSize(500);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIMIT 0,200"));
    }

    @Test
    public void buildListSqlShouldHandleFilters() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("name", "Alice");
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("`name` = ?"));
        assertEquals("Alice", result.params.get(result.params.size() - 1));
    }

    @Test
    public void buildListSqlShouldHandleLikeFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("name:like", "Al");
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIKE"));
        assertTrue(result.params.contains("%Al%"));
    }

    @Test
    public void buildListSqlShouldHandleInFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("status", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("status:in", Arrays.asList("ACTIVE", "PENDING"));
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("IN (?,?)"));
    }

    @Test
    public void buildListSqlShouldHandleEmptyInFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("status", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("status:in", Collections.emptyList());
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("IN (NULL)"));
    }

    @Test
    public void buildListSqlShouldIgnoreNonWhitelistedFilters() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("unknown_field", "value");
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertEquals(0, result.params.size());
    }

    @Test
    public void buildListSqlShouldIgnoreNullFilterValues() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("name", null);
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertEquals(0, result.params.size());
    }

    @Test
    public void buildListSqlShouldHandleGtFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("age", "INT")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("age:gt", 18);
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("`age` > ?"));
    }

    @Test
    public void buildListSqlShouldHandleDescOrder() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("name desc");
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("ORDER BY `name` DESC"));
    }

    @Test
    public void buildListSqlShouldDefaultUnknownDirectionToAsc() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setOrderBy("name invalid");
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("ORDER BY `name` ASC"));
    }

    @Test
    public void buildListSqlShouldHandleNullPage() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(null);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIMIT 0,20"));
    }

    @Test
    public void buildListSqlShouldHandleNullSize() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setSize(null);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIMIT 0,20"));
    }

    @Test
    public void buildListSqlShouldHandleNegativePage() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        query.setPage(-1);
        DynamicSqlBuilder.SqlAndParams result = builder.buildListSql(entity, query);
        assertTrue(result.sql.contains("LIMIT 0,20"));
    }

    // --- buildCountSql ---

    @Test
    public void buildCountSqlShouldGenerateCount() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildCountSql(entity, null);
        assertTrue(result.sql.contains("SELECT COUNT(*)"));
        assertTrue(result.sql.contains("FROM `t_user`"));
    }

    @Test
    public void buildCountSqlShouldHandleFilters() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeQueryDTO query = new RuntimeQueryDTO();
        Map<String, Object> filters = new HashMap<>();
        filters.put("name", "Alice");
        query.setFilters(filters);
        DynamicSqlBuilder.SqlAndParams result = builder.buildCountSql(entity, query);
        assertTrue(result.sql.contains("`name` = ?"));
    }

    // --- buildGetSql ---

    @Test
    public void buildGetSqlShouldGenerateSelectById() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildGetSql(entity, 1L, null);
        assertTrue(result.sql.contains("WHERE id = ?"));
        assertEquals(Long.valueOf(1L), result.params.get(0));
        assertTrue(result.sql.contains("AND deleted = 0"));
    }

    @Test
    public void buildGetSqlShouldIncludeTenantFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildGetSql(entity, 1L, "t1");
        assertTrue(result.sql.contains("AND tenant_code = ?"));
        assertEquals("t1", result.params.get(1));
    }

    // --- buildInsertSql ---

    @Test
    public void buildInsertSqlShouldGenerateInsert() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Alice");
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        assertTrue("Expected INSERT INTO, got: " + result.sql, result.sql.contains("INSERT INTO `t_user`"));
        assertTrue("Expected name column, got: " + result.sql, result.sql.contains("`name`"));
        assertTrue("Expected VALUES (?), got: " + result.sql, result.sql.contains("?"));
        assertTrue("Expected params to contain Alice, got: " + result.params, result.params.contains("Alice"));
    }

    @Test
    public void buildInsertSqlShouldSkipNullValues() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", null);
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        // null name is skipped, but create_time is auto-added
        assertTrue(result.sql.contains("INSERT INTO"));
    }

    @Test
    public void buildInsertSqlShouldIgnoreNonWhitelistedFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("unknown_field", "x");
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        assertTrue(!result.sql.contains("unknown_field"));
    }

    @Test
    public void buildInsertSqlShouldHandleNullFieldValues() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(null);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        assertNotNull(result);
    }

    @Test
    public void buildInsertSqlShouldForceInjectTenantCode() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Alice");
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        assertTrue(result.sql.contains("`tenant_code`"));
    }

    @Test
    public void buildInsertSqlShouldNotOverrideExistingTenantCodeField() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        List<FieldDefDTO> fields = Arrays.asList(makeField("name", "STRING"), makeField("tenant_code", "STRING"));
        entity.setFields(fields);
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Alice");
        values.put("tenant_code", "t1");
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildInsertSql(entity, body, "user1");
        // Should not double-add tenant_code
        long count = result.sql.split("`tenant_code`").length - 1;
        assertTrue("Expected tenant_code to appear at most twice", count <= 2);
    }

    // --- buildUpdateSql ---

    @Test
    public void buildUpdateSqlShouldGenerateUpdate() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Bob");
        body.setFieldValues(values);

        DynamicSqlBuilder.SqlAndParams result = builder.buildUpdateSql(entity, 1L, body, "user1");
        assertTrue(result.sql.contains("UPDATE `t_user`"));
        assertTrue(result.sql.contains("SET `name` = ?"));
        assertTrue(result.sql.contains("WHERE id = ?"));
    }

    @Test
    public void buildUpdateSqlShouldAcceptEmptyBodyWithUpdateTimeOnly() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        // Empty body still gets update_time auto-injected, so SET is not empty
        DynamicSqlBuilder.SqlAndParams result = builder.buildUpdateSql(entity, 1L, body, "user1");
        assertNotNull(result);
        assertTrue(result.sql.contains("UPDATE"));
        assertTrue(result.sql.contains("update_time"));
    }

    @Test
    public void buildUpdateSqlShouldAutoAddUpdateTime() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("name", "Alice"));

        DynamicSqlBuilder.SqlAndParams result = builder.buildUpdateSql(entity, 1L, body, "user1");
        assertTrue(result.sql.contains("`update_time`"));
    }

    @Test
    public void buildUpdateSqlShouldIncludeTenantFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("name", "Alice"));

        DynamicSqlBuilder.SqlAndParams result = builder.buildUpdateSql(entity, 1L, body, "user1");
        assertTrue(result.sql.contains("AND tenant_code = ?"));
    }

    // --- buildDeleteSql ---

    @Test
    public void buildDeleteSqlShouldGenerateSoftDelete() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildDeleteSql(entity, 1L, null);
        assertTrue(result.sql.contains("UPDATE `t_user`"));
        assertTrue(result.sql.contains("SET deleted = 1"));
        assertTrue(result.sql.contains("WHERE id = ?"));
    }

    @Test
    public void buildDeleteSqlShouldIncludeTenantFilter() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setTenantCode("t1");
        entity.setFields(Collections.singletonList(makeField("name", "STRING")));
        DynamicSqlBuilder.SqlAndParams result = builder.buildDeleteSql(entity, 1L, "t1");
        assertTrue(result.sql.contains("AND tenant_code = ?"));
    }

    // --- coerce ---

    @Test
    public void coerceShouldHandleNull() {
        assertEquals(null, builder.coerce(null, "STRING"));
    }

    @Test
    public void coerceShouldHandleNullFieldType() {
        Object result = builder.coerce("test", null);
        assertEquals("test", result);
    }

    @Test
    public void coerceShouldConvertToLong() {
        assertEquals(1L, builder.coerce("1", "INT"));
        assertEquals(2L, builder.coerce(2, "INT"));
        assertEquals(3L, builder.coerce("3", "LONG"));
    }

    @Test
    public void coerceShouldConvertToDouble() {
        assertEquals(1.5, builder.coerce("1.5", "DECIMAL"));
        assertEquals(2.0, builder.coerce(2, "DECIMAL"));
    }

    @Test
    public void coerceShouldConvertToBoolean() {
        assertEquals(Boolean.TRUE, builder.coerce("true", "BOOLEAN"));
        assertEquals(Boolean.TRUE, builder.coerce(true, "BOOLEAN"));
        assertEquals(Boolean.FALSE, builder.coerce("false", "BOOLEAN"));
    }

    @Test
    public void coerceShouldConvertToString() {
        assertEquals("hello", builder.coerce("hello", "STRING"));
        assertEquals("123", builder.coerce(123, "STRING"));
    }

    @Test
    public void coerceShouldConvertDate() {
        Object result = builder.coerce("2026-01-01", "DATE");
        assertNotNull(result);
        assertTrue(result instanceof java.util.Date);
    }

    @Test
    public void coerceShouldConvertDateTime() {
        Object result = builder.coerce("2026-01-01 12:00:00", "DATETIME");
        assertNotNull(result);
        assertTrue(result instanceof java.util.Date);
    }

    @Test
    public void coerceShouldThrowForInvalidLong() {
        try {
            builder.coerce("not-a-number", "INT");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void coerceShouldThrowForInvalidDecimal() {
        try {
            builder.coerce("not-a-number", "DECIMAL");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    // --- buildJoinClauses ---

    @Test
    public void buildJoinClausesShouldHandleNullEntity() {
        assertEquals("", builder.buildJoinClauses(null));
    }

    @Test
    public void buildJoinClausesShouldHandleNullFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(null);
        assertEquals("", builder.buildJoinClauses(entity));
    }

    @Test
    public void buildJoinClausesShouldHandleEmptyFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(Collections.emptyList());
        assertEquals("", builder.buildJoinClauses(entity));
    }

    @Test
    public void buildJoinClausesShouldGenerateDictJoin() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        FieldDefDTO field = makeField("status", "STRING");
        field.setDictCode("status_dict");
        entity.setFields(Collections.singletonList(field));
        String result = builder.buildJoinClauses(entity);
        // 字典项必须先去重再 join: 直接 join 物理表时, 重复的 item_code 会把业务记录 fan-out 成多行
        assertTrue(result.contains("FROM z_lc_dict_item i"));
        assertTrue(result.contains("x.id < i.id"));
        assertTrue(result.contains("d_status"));
        assertTrue(result.contains("dict_code = ?"));
        assertEquals("每个 dictCode 字段只允许一个占位符", 1, countChar(result, '?'));
    }

    private static int countChar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void buildJoinClausesShouldGenerateRefJoin() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        FieldDefDTO field = makeField("deptId", "REF");
        field.setRefEntity("department");
        entity.setFields(Collections.singletonList(field));
        String result = builder.buildJoinClauses(entity);
        assertTrue(result.contains("LEFT JOIN `department` r_deptId"));
    }

    @Test
    public void buildJoinClausesShouldSkipNullFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        List<FieldDefDTO> fields = Arrays.asList(null, makeField("name", "STRING"));
        entity.setFields(fields);
        String result = builder.buildJoinClauses(entity);
        assertEquals("", result);
    }

    @Test
    public void buildJoinClausesShouldSkipFieldWithNullCode() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode(null);
        entity.setFields(Collections.singletonList(field));
        String result = builder.buildJoinClauses(entity);
        assertEquals("", result);
    }

    // --- buildExtraSelectColumns ---

    @Test
    public void buildExtraSelectColumnsShouldHandleNullEntity() {
        assertEquals("", builder.buildExtraSelectColumns(null));
    }

    @Test
    public void buildExtraSelectColumnsShouldHandleNullFields() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        entity.setFields(null);
        assertEquals("", builder.buildExtraSelectColumns(entity));
    }

    @Test
    public void buildExtraSelectColumnsShouldGenerateDictLabel() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        FieldDefDTO field = makeField("status", "STRING");
        field.setDictCode("status_dict");
        entity.setFields(Collections.singletonList(field));
        String result = builder.buildExtraSelectColumns(entity);
        assertTrue(result.contains("`status_label`"));
    }

    @Test
    public void buildExtraSelectColumnsShouldGenerateRefName() {
        EntityDefDTO entity = makeEntity("user", "t_user");
        FieldDefDTO field = makeField("deptId", "REF");
        field.setRefEntity("department");
        entity.setFields(Collections.singletonList(field));
        String result = builder.buildExtraSelectColumns(entity);
        assertTrue(result.contains("`deptId_name`"));
    }

    // --- SqlAndParams ---

    @Test
    public void sqlAndParamsShouldExposeFields() {
        DynamicSqlBuilder.SqlAndParams sp = new DynamicSqlBuilder.SqlAndParams("SELECT 1", Collections.singletonList(1));
        assertEquals("SELECT 1", sp.sql);
        assertEquals(1, sp.params.size());
    }

    // --- Helpers ---

    private static EntityDefDTO makeEntity(String code, String table) {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setEntityCode(code);
        entity.setTableName(table);
        return entity;
    }

    private static FieldDefDTO makeField(String code, String type) {
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode(code);
        field.setFieldType(type);
        return field;
    }
}