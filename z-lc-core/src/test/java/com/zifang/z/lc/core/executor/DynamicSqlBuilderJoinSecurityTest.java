package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link DynamicSqlBuilder#buildJoinClauses} 的标识符安全。
 *
 * <p>背景：{@code quote()} 原本只把标识符包进反引号、<b>不做校验</b>，而它被用来拼
 * {@code refEntity}（LEFT JOIN 的表名）。{@code refEntity} 是实体定义元数据，
 * 写入闸 {@code SchemaAdminBizService.validateFieldCodes} 当时只校验 {@code fieldCode}，
 * 于是含反引号的 {@code refEntity} 能闭合引号逃逸：
 *
 * <pre>
 *   refEntity = x` JOIN z_lc_app a ON 1=1 --
 *   ⇒ LEFT JOIN `x` JOIN z_lc_app a ON 1=1 --` r_ref1 ON ...
 * </pre>
 *
 * 末尾的 {@code --} 还会把紧随其后的 {@code t.deleted = 0} 一起注释掉，
 * 即"定义一个实体"等价于往 list/count SQL 注入任意 SQL，且顺带绕过软删除过滤。
 */
public class DynamicSqlBuilderJoinSecurityTest {

    private final DynamicSqlBuilder builder = new DynamicSqlBuilder();

    private static FieldDefDTO refField(String fieldCode, String refEntity) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(fieldCode);
        f.setFieldType("REF");
        f.setRefEntity(refEntity);
        return f;
    }

    private static EntityDefDTO entityOf(FieldDefDTO... fields) {
        EntityDefDTO e = new EntityDefDTO();
        e.setEntityCode("order");
        e.setTableName("z_lc_order");
        e.setFields(new ArrayList<FieldDefDTO>(Arrays.asList(fields)));
        return e;
    }

    /** JUnit 4 手写 assertThrows：给出明确的失败信息。 */
    private void assertRejected(String refEntity, String what) {
        try {
            String join = builder.buildJoinClauses(entityOf(refField("ref1", refEntity)));
            fail(what + " 应被拒绝，但拼出了: " + join);
        } catch (IllegalArgumentException expected) {
            assertTrue("失败信息应指向非法标识符，实际: " + expected.getMessage(),
                    expected.getMessage() != null
                            && expected.getMessage().contains("Illegal SQL identifier"));
        }
    }

    @Test
    public void validRefEntityStillJoins() {
        String join = builder.buildJoinClauses(entityOf(refField("ref1", "z_lc_customer")));
        assertEquals(" LEFT JOIN `z_lc_customer` r_ref1 ON r_ref1.id = t.`ref1`"
                + " AND r_ref1.deleted = 0", join);
    }

    /** 回归：refEntity 含反引号曾逃逸出可执行注入。 */
    @Test
    public void backtickInRefEntityIsRejected() {
        assertRejected("x` JOIN z_lc_app a ON 1=1 --", "含反引号的 refEntity");
    }

    @Test
    public void sqlStructureCharsInRefEntityRejected() {
        // 注意：空串不在此列 —— buildJoinClauses 的判空是 !isEmpty()，
        // 空串压根不进 JOIN 分支，语义是"无引用字段"（见 noRefEntityMeansNoJoin）。
        String[] bad = {"t1, t2", "t1; DROP TABLE z_lc_app", "t1 t2", "t1--",
                "t1/*x*/", "(SELECT 1)", "t1 ON 1=1", "1=1", "   ",
                "z_lc_app`", "`z_lc_app", " z_lc_app ", "z_lc_app "};
        for (String b : bad) {
            assertRejected(b, "含 SQL 结构字符的 refEntity [" + b + "]");
        }
    }

    @Test
    public void noRefEntityMeansNoJoin() {
        assertEquals("", builder.buildJoinClauses(entityOf(refField("plain1", null))));
        assertEquals("", builder.buildJoinClauses(entityOf(refField("plain1", ""))));
    }

    /** 纵深防御：fieldCode 在写入门已拦，这里 quote 再拦一道。 */
    @Test
    public void backtickInFieldCodeIsRejected() {
        try {
            String join = builder.buildJoinClauses(
                    entityOf(refField("ref1` , (SELECT 1) x --", "z_lc_customer")));
            fail("含反引号的 fieldCode 应被拒绝，但拼出了: " + join);
        } catch (IllegalArgumentException expected) {
            assertTrue("失败信息应指向非法标识符，实际: " + expected.getMessage(),
                    expected.getMessage().contains("Illegal SQL identifier"));
        }
    }

    /** dictCode 走 ? 占位符，不受标识符校验影响。 */
    @Test
    public void dictCodeUsesPlaceholderAndIsNotIdentifierQuoted() {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("status");
        f.setFieldType("DICT");
        f.setDictCode("ORDER_STATUS");
        String join = builder.buildJoinClauses(entityOf(f));
        assertTrue("dictCode 必须是绑定参数而不是拼进 SQL，实际: " + join,
                join.contains("dict_code = ?"));
    }

    @Test
    public void emptyFieldsYieldEmptyJoin() {
        assertEquals("", builder.buildJoinClauses(null));
        assertEquals("", builder.buildJoinClauses(new EntityDefDTO()));
    }
}
