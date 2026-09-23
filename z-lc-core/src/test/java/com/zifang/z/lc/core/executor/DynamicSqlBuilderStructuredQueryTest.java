package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.QueryConditionDTO;
import com.zifang.z.lc.common.dto.QuerySortDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DynamicSqlBuilder 结构化查询 (conjunction/conditions/sorts) + SQL 注入防护测试.
 * <p>
 * 守护 Task 1/3 的核心安全承诺:
 * <ul>
 *   <li>历史 {@code orderBy.split("\\s+")} 注入面 — 恶意 orderBy 被结构性拒绝, 绝不落入 SQL</li>
 *   <li>结构化 sorts/conditions — 未知字段 / 非标识符 / 非法操作符 / 非法方向 → IllegalArgumentException
 *       (ControllerAdvice → 400), 且异常抛出时机在建 SQL 之前, 不产生任何用户输入片段</li>
 *   <li>合法结构化输入 — 组装为反引号包裹的多字段 ORDER BY 与 AND/OR 分组的 WHERE</li>
 * </ul>
 */
public class DynamicSqlBuilderStructuredQueryTest {

    private final DynamicSqlBuilder builder = new DynamicSqlBuilder();

    private EntityDefDTO entity() {
        EntityDefDTO e = new EntityDefDTO();
        e.setEntityCode("order");
        e.setTableName("lc_demo_order");
        e.setFields(new ArrayList<>(Arrays.asList(
                field("name", "STRING"), field("amount", "DECIMAL"), field("status", "STRING"))));
        return e;
    }

    private FieldDefDTO field(String code, String type) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setFieldName(code);
        f.setFieldType(type);
        return f;
    }

    private QueryConditionDTO cond(String code, String op, Object val) {
        QueryConditionDTO c = new QueryConditionDTO();
        c.setFieldCode(code);
        c.setOperator(op);
        c.setValue(val);
        return c;
    }

    private QuerySortDTO sort(String code, String dir) {
        QuerySortDTO s = new QuerySortDTO();
        s.setFieldCode(code);
        s.setDir(dir);
        return s;
    }

    // ===== 历史 orderBy 注入面 =====

    @Test
    public void maliciousOrderByMustNeverReachSql() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setOrderBy("id; drop table z_lc_app");
        DynamicSqlBuilder.SqlAndParams r = builder.buildListSql(entity(), q);
        String sql = r.sql.toLowerCase();
        assertFalse("drop must not reach SQL: " + r.sql, sql.contains("drop"));
        assertFalse("semicolon injection must not reach SQL", sql.contains(";"));
        // 结构性预检失败 → 静默跳过, 不回退默认排序, 也不抛 (向后兼容 legacy 行为)
        assertFalse(r.sql.contains("z_lc_app"));
    }

    @Test
    public void stackedStatementOrderByIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setOrderBy("name; DELETE FROM z_lc_event --");
        DynamicSqlBuilder.SqlAndParams r = builder.buildListSql(entity(), q);
        String sql = r.sql.toLowerCase();
        assertFalse(sql.contains("delete from"));
        assertFalse(sql.contains("--"));
    }

    @Test
    public void legitimateOrderByStillWorks() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setOrderBy("name desc");
        assertTrue(builder.buildListSql(entity(), q).sql.contains("ORDER BY `name` DESC"));
    }

    // ===== 结构化 sorts 严格校验 =====

    @Test
    public void structuredSortWithNonIdentifierFieldIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setSorts(Collections.singletonList(sort("1=1", "asc")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void structuredSortUnknownFieldIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setSorts(Collections.singletonList(sort("evil_column", "asc")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void structuredSortIllegalDirectionIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setSorts(Collections.singletonList(sort("name", "; drop table x")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void multiFieldValidSortProducesQuotedOrderBy() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setSorts(Arrays.asList(sort("name", "asc"), sort("amount", "DESC")));
        String sql = builder.buildListSql(entity(), q).sql;
        assertTrue(sql.contains("ORDER BY `name` ASC, `amount` DESC"));
    }

    // ===== 结构化 conditions 严格校验 =====

    @Test
    public void unknownConditionFieldIsRejectedBeforeSql() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Collections.singletonList(cond("not_declared", "eq", 1)));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void illegalOperatorForTypeIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        // DECIMAL 不支持 like
        q.setConditions(Collections.singletonList(cond("amount", "like", "1%")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void totallyUnknownOperatorIsRejected() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Collections.singletonList(cond("name", "= 1 OR 1=1", "x")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q));
    }

    @Test
    public void validConditionsAreParameterisedAndQuoted() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Arrays.asList(cond("name", "like", "abc"), cond("amount", "gte", 10)));
        DynamicSqlBuilder.SqlAndParams r = builder.buildListSql(entity(), q);
        assertTrue(r.sql.contains("(`name` LIKE ? AND `amount` >= ?)"));
        assertTrue(r.params.contains("%abc%"));
        assertTrue(r.params.contains(10));
    }

    @Test
    public void orConjunctionGroupsConditionsWithOr() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConjunction("OR");
        q.setConditions(Arrays.asList(cond("name", "eq", "a"), cond("status", "eq", "B")));
        String sql = builder.buildListSql(entity(), q).sql;
        assertTrue(sql.contains("(`name` = ? OR `status` = ?)"));
    }

    @Test
    public void inOperatorExpandsPlaceholdersAndEmptyInIsAlwaysFalse() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Collections.singletonList(cond("status", "in", Arrays.asList("A", "B"))));
        DynamicSqlBuilder.SqlAndParams r = builder.buildListSql(entity(), q);
        assertTrue(r.sql.contains("`status` IN (?,?)"));

        RuntimeQueryDTO q2 = new RuntimeQueryDTO();
        q2.setConditions(Collections.singletonList(cond("status", "in", Collections.emptyList())));
        assertTrue(builder.buildListSql(entity(), q2).sql.contains("1 = 0"));

        // in 但值不是集合 → 拒绝
        RuntimeQueryDTO q3 = new RuntimeQueryDTO();
        q3.setConditions(Collections.singletonList(cond("status", "in", "notAList")));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), q3));
    }

    @Test
    public void isNullNeedsNoValueAndEqNeedsValue() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Collections.singletonList(cond("name", "isNull", null)));
        assertTrue(builder.buildListSql(entity(), q).sql.contains("`name` IS NULL"));

        RuntimeQueryDTO bad = new RuntimeQueryDTO();
        bad.setConditions(Collections.singletonList(cond("name", "eq", null)));
        assertThrowsIllegalArgument(() -> builder.buildListSql(entity(), bad));
    }

    @Test
    public void countSqlAlsoEnforcesStructuredValidation() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setConditions(Collections.singletonList(cond("ghost", "eq", 1)));
        assertThrowsIllegalArgument(() -> builder.buildCountSql(entity(), q));
    }

    @Test
    public void legacyFiltersAndStructuredConditionsAreAndedTogether() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setFilters(Collections.<String, Object>singletonMap("name", "Alice"));
        q.setConditions(Collections.singletonList(cond("status", "eq", "OK")));
        String sql = builder.buildListSql(entity(), q).sql;
        // legacy `name` = ? 在前, 结构化组在后用 AND 连接
        assertTrue(sql.contains("`name` = ?"));
        assertTrue(sql.contains("AND (`status` = ?)"));
    }

    // ===== helpers =====

    private interface ThrowingRunnable {
        void run();
    }

    private static void assertThrowsIllegalArgument(ThrowingRunnable r) {
        try {
            r.run();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
