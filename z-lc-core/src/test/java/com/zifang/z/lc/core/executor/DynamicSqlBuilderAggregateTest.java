package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.AggregateQueryDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * buildAggregateSql 的时间分桶 (timeGroup) 契约测试。
 * <p>
 * 关注两件在图表场景最容易出事的事:
 * <ul>
 *   <li>分桶表达式必须出现在 GROUP BY / ORDER BY 里而不是只出现在 SELECT ——
 *       只 SELECT 不 GROUP BY 在 ONLY_FULL_GROUP_BY 下直接报错;</li>
 *   <li>非法组合 (timeGroup 配非日期列 / 配不上 groupField) 必须抛错,
 *       不能静默退化成"无分组总量", 否则图表会显示一个看着正常的假数字。</li>
 * </ul>
 */
public class DynamicSqlBuilderAggregateTest {

    private final DynamicSqlBuilder builder = new DynamicSqlBuilder();

    private static EntityDefDTO taskEntity() {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setEntityCode("task");
        entity.setTableName("t_task");
        FieldDefDTO name = field("name", "STRING");
        FieldDefDTO stage = field("stage", "STRING");
        stage.setDictCode("ui_stage");
        entity.setFields(new ArrayList<FieldDefDTO>(
                Arrays.asList(field("id", "LONG"), name, stage, field("amount", "LONG"),
                        field("due", "DATE"), field("closed_at", "DATETIME"))));
        return entity;
    }

    private static FieldDefDTO field(String code, String type) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setFieldType(type);
        return f;
    }

    private static AggregateQueryDTO query(String groupField, String timeGroup) {
        AggregateQueryDTO q = new AggregateQueryDTO();
        q.setGroupField(groupField);
        q.setTimeGroup(timeGroup);
        return q;
    }

    @Test
    public void monthBucketEmitsYearAndMonthInSelectGroupByAndOrderBy() {
        DynamicSqlBuilder.SqlAndParams sp = builder.buildAggregateSql(taskEntity(), query("due", "MONTH"));

        assertTrue(sp.sql, sp.sql.contains("EXTRACT(YEAR FROM t.`due`) AS bucket_year"));
        assertTrue(sp.sql, sp.sql.contains("EXTRACT(MONTH FROM t.`due`) AS bucket_month"));
        assertFalse("MONTH 粒度不该带出日", sp.sql.contains("bucket_day"));
        // 时间轴要按时间正序, 而不是按 count 倒序 —— 否则折线图会画成锯齿
        assertTrue(sp.sql, sp.sql.contains(
                "GROUP BY EXTRACT(YEAR FROM t.`due`), EXTRACT(MONTH FROM t.`due`)"));
        assertTrue(sp.sql, sp.sql.endsWith(
                "ORDER BY EXTRACT(YEAR FROM t.`due`) ASC, EXTRACT(MONTH FROM t.`due`) ASC LIMIT 100"));
        assertFalse(sp.sql, sp.sql.contains("group_count DESC"));
        assertFalse("分桶行没有 group_key, 前端按 bucket_* 拼标签", sp.sql.contains("AS group_key"));
    }

    @Test
    public void dayAndYearBucketsScaleTheParts() {
        String day = builder.buildAggregateSql(taskEntity(), query("due", "day")).sql;
        assertTrue("粒度大小写不敏感", day.contains("bucket_day"));
        assertTrue(day, day.contains("EXTRACT(DAY FROM t.`due`)"));

        String year = builder.buildAggregateSql(taskEntity(), query("due", "YEAR")).sql;
        assertTrue(year, year.contains("bucket_year"));
        assertFalse(year, year.contains("bucket_month"));
        assertFalse(year, year.contains("bucket_day"));
    }

    @Test
    public void timeBucketsDropRowsWithoutADate() {
        String sql = builder.buildAggregateSql(taskEntity(), query("closed_at", "DAY")).sql;
        assertTrue(sql, sql.contains("WHERE t.deleted = 0 AND t.`closed_at` IS NOT NULL"));
    }

    @Test
    public void timeGroupOnANonDateFieldIsRejected() {
        try {
            builder.buildAggregateSql(taskEntity(), query("amount", "MONTH"));
            fail("按数值列做时间分桶必须报错, 不能返回一份全量总量");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("DATE / DATETIME"));
            assertTrue(expected.getMessage(), expected.getMessage().contains("amount"));
        }
    }

    @Test
    public void timeGroupWithoutGroupFieldIsRejected() {
        try {
            builder.buildAggregateSql(taskEntity(), query(null, "MONTH"));
            fail("没有分组列时无从分桶");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("groupField"));
        }
    }

    @Test
    public void unknownTimeGroupIsRejected() {
        try {
            builder.buildAggregateSql(taskEntity(), query("due", "WEEK"));
            fail("WEEK 没有跨数据库可移植的 EXTRACT 写法, 不能假装支持");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("DAY / MONTH / YEAR"));
        }
    }

    @Test
    public void plainGroupByShapeIsUntouched() {
        DynamicSqlBuilder.SqlAndParams sp = builder.buildAggregateSql(taskEntity(), query("stage", null));
        assertTrue(sp.sql, sp.sql.contains("t.`stage` AS group_key"));
        assertTrue(sp.sql, sp.sql.contains("AS group_label"));
        assertTrue(sp.sql, sp.sql.contains("GROUP BY t.`stage`"));
        assertFalse(sp.sql, sp.sql.contains("EXTRACT"));
        assertFalse(sp.sql, sp.sql.contains("IS NOT NULL"));
    }

    /**
     * 字典 join 的 `?` 由 appendTenantParam 预置入参队列, 顺序不能动:
     * 多一个或少一个占位符都会让参数错位成"查询条件串到别的列上"。
     */
    @Test
    public void timeGroupKeepsDictJoinPlaceholderOrder() {
        EntityDefDTO entity = taskEntity();
        AggregateQueryDTO q = query("due", "DAY");
        q.getAggregations().put("amount", java.util.Arrays.asList("SUM"));
        DynamicSqlBuilder.SqlAndParams sp = builder.buildAggregateSql(entity, q);

        List<Object> before = builder.buildAggregateSql(entity, query("due", null)).params;
        assertEquals("每个 dictCode 字段一个占位符", 1, countChar(sp.sql, '?'));
        assertEquals(before.size(), sp.params.size());
        assertTrue(sp.sql, sp.sql.contains("SUM(t.`amount`) AS sum_amount"));
    }

    private static int countChar(String text, char target) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                n++;
            }
        }
        return n;
    }
}
