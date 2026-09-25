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

    /**
     * 交叉表的原料: 行维度 + 列维度同时出现在 SELECT 和 GROUP BY 里。
     * <p>
     * 只 SELECT 不 GROUP BY 在 ONLY_FULL_GROUP_BY 下直接报错, 而漏掉第二维的 GROUP BY 会让
     * 每个格子拿到"整行第一维的合计"—— 表看着满, 数全是错的。
     */
    @Test
    public void twoDimensionsAreBothSelectedAndBothGrouped() {
        AggregateQueryDTO q = query("stage", null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage", "name")));
        q.getAggregations().put("amount", Arrays.asList("SUM"));
        DynamicSqlBuilder.SqlAndParams sp = builder.buildAggregateSql(taskEntity(), q);

        assertTrue(sp.sql, sp.sql.contains("t.`stage` AS group_key, "));
        assertTrue(sp.sql, sp.sql.contains("t.`name` AS group_key_2"));
        assertTrue(sp.sql, sp.sql.contains("GROUP BY t.`stage`, t.`name`"));
        assertTrue(sp.sql, sp.sql.endsWith("ORDER BY group_count DESC, group_key ASC, group_key_2 ASC LIMIT 100"));
        assertTrue(sp.sql, sp.sql.contains("SUM(t.`amount`) AS sum_amount"));
        // 多维时每一维都保证有非空标签列: 字典维兜住"字典项被删"(否则那些记录会折成一个叫 null 的假列),
        // 非字典维补一列等于原值的标签 (否则透视程序引用 group_label_2 会整条请求 400)。
        assertTrue(sp.sql, sp.sql.contains("COALESCE(MAX(d_stage.item_label), t.`stage`) AS group_label, "));
        assertTrue(sp.sql, sp.sql.contains("COALESCE(t.`name`, t.`name`) AS group_label_2"));
    }

    /** 单维不能因为多维上线多出 COALESCE —— 那是既有调用方 (看板/页脚/图表) 的口径变更。 */
    @Test
    public void singleDimensionKeepsTheUntouchedLabelShape() {
        DynamicSqlBuilder.SqlAndParams sp = builder.buildAggregateSql(taskEntity(), query("stage", null));
        assertTrue(sp.sql, sp.sql.contains("MAX(d_stage.item_label) AS group_label"));
        assertFalse(sp.sql, sp.sql.contains("COALESCE"));
    }

    /**
     * 只有一项的 groupFields 必须与沿用 groupField 逐字节同一条 SQL —— 老的单维调用方
     * (看板分列、表格页脚、图表) 不能因为多维支持上线而换一份口径。
     */
    @Test
    public void oneDimensionInGroupFieldsIsByteIdenticalToGroupField() {
        DynamicSqlBuilder.SqlAndParams legacy = builder.buildAggregateSql(taskEntity(), query("stage", null));
        AggregateQueryDTO q = query(null, null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage")));
        DynamicSqlBuilder.SqlAndParams modern = builder.buildAggregateSql(taskEntity(), q);

        assertEquals(legacy.sql, modern.sql);
        assertEquals(legacy.params.size(), modern.params.size());
        for (int i = 0; i < legacy.params.size(); i++) {
            assertEquals("第 " + i + " 个占位符入参错位", legacy.params.get(i), modern.params.get(i));
        }
    }

    /** 重复维度不是"无害地去重": GROUP BY city, city 会多出一列永远等于行键的假维度。 */
    @Test
    public void duplicateDimensionFailsInsteadOfSilentlyDeduping() {
        AggregateQueryDTO q = query("stage", null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage", "stage")));
        try {
            builder.buildAggregateSql(taskEntity(), q);
            fail("重复维度必须报错, 不能悄悄去重成单维");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Duplicate groupField: stage"));
        }
    }

    /** 第二维查不到字段时报错, 不能"这一维不要了"—— 那会把一张交叉表降级成一张单维统计表。 */
    @Test
    public void unknownSecondDimensionFailsInsteadOfBeingDropped() {
        AggregateQueryDTO q = query("stage", null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage", "nope")));
        try {
            builder.buildAggregateSql(taskEntity(), q);
            fail("未知维度必须报错");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Unknown groupField: nope"));
        }
    }

    /**
     * GROUP BY 的列名不能走占位符, 所以每一维都必须过标识符白名单 —— 漏校验第二维
     * 等于给多维分组开一个注入面, 而这正是多维功能新引入的面。
     */
    @Test
    public void secondDimensionCarriesTheSameInjectionGuard() {
        AggregateQueryDTO q = query("stage", null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage", "name) FROM t_task t2 WHERE 1=1 --")));
        try {
            builder.buildAggregateSql(taskEntity(), q);
            fail("非法标识符必须挡在拼 SQL 之前");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Illegal groupField"));
        }
    }

    /** 空位 = 这一维还没选 (透视表先只选行维度是常态), 此时等价于单维而不是报错也不是多出空列。 */
    @Test
    public void blankSecondDimensionMeansOneDimension() {
        DynamicSqlBuilder.SqlAndParams single = builder.buildAggregateSql(taskEntity(), query("stage", null));
        AggregateQueryDTO q = query("stage", null);
        q.setGroupFields(new ArrayList<String>(Arrays.asList("stage", "  ")));
        assertEquals(single.sql, builder.buildAggregateSql(taskEntity(), q).sql);
    }

    /** 两维 + 时间分桶: 引擎猜不出该分桶哪一维, 只能拒绝, 不能默默只对第一维分桶。 */
    @Test
    public void timeGroupRefusesToGuessWhichDimensionToBucket() {
        AggregateQueryDTO q = query("due", "DAY");
        q.setGroupFields(new ArrayList<String>(Arrays.asList("due", "stage")));
        try {
            builder.buildAggregateSql(taskEntity(), q);
            fail("timeGroup 与多维分组同时出现必须报错");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("timeGroup"));
        }
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
