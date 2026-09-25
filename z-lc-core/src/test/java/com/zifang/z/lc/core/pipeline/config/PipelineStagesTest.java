package com.zifang.z.lc.core.pipeline.config;

import com.zifang.z.lc.core.pipeline.FieldProcessor;
import com.zifang.z.lc.core.pipeline.processor.DictResolveProcessor;
import com.zifang.z.lc.core.pipeline.processor.RefCheckProcessor;
import com.zifang.z.lc.core.pipeline.processor.RequiredCheckProcessor;
import com.zifang.z.lc.core.pipeline.processor.TypeConvertProcessor;
import com.zifang.z.lc.core.pipeline.processor.ValueValidateProcessor;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link PipelineStages} 的口径测试 —— 也就是"配置页能承诺什么"的唯一来源。
 * <p>
 * 每条拒绝都配一条相反方向的接受用例: 只测拒绝的话, 把整张表写成永远抛异常也能全绿。
 */
public class PipelineStagesTest {

    private static final String ALL_FIVE = "[{\"type\":\"DICT_RESOLVE\",\"order\":1},"
            + "{\"type\":\"REF_CHECK\",\"order\":2},"
            + "{\"type\":\"REQUIRED_CHECK\",\"order\":3},"
            + "{\"type\":\"TYPE_CONVERT\",\"order\":4},"
            + "{\"type\":\"VALUE_VALIDATE\",\"order\":5}]";

    private static List<String> resolve(String trigger, String stages) {
        return PipelineStages.validateAndResolve(trigger, stages);
    }

    /** 断言拒绝, 并核消息里点名了哪个词 —— "只说不合法"的消息不算合格。 */
    private static String expectReject(String trigger, String stages, String... mustName) {
        try {
            resolve(trigger, stages);
            fail("应当被拒: trigger=" + trigger + " stages=" + stages);
            return null;
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            for (String needle : mustName) {
                assertTrue("拒绝消息要点名 [" + needle + "], 实际: " + msg, msg.contains(needle));
            }
            return msg;
        }
    }

    // ===== 阶段词表与容器里真实的处理器名对得上 =====

    @Test
    public void everySupportedTypeMapsToARealProcessorName() {
        // 登记在配置词表里、但容器里没有同名 bean 的阶段 = 配了不跑的第二次翻车
        assertEquals("DictResolve", PipelineStages.processorNameFor(PipelineStages.DICT_RESOLVE));
        assertEquals("RefCheck", PipelineStages.processorNameFor(PipelineStages.REF_CHECK));
        assertEquals("RequiredCheck", PipelineStages.processorNameFor(PipelineStages.REQUIRED_CHECK));
        assertEquals("TypeConvert", PipelineStages.processorNameFor(PipelineStages.TYPE_CONVERT));
        assertEquals("ValueValidate", PipelineStages.processorNameFor(PipelineStages.VALUE_VALIDATE));

        FieldProcessor[] real = new FieldProcessor[]{
                new DictResolveProcessor(), new RefCheckProcessor(), new RequiredCheckProcessor(),
                new TypeConvertProcessor(), new ValueValidateProcessor()};
        for (FieldProcessor p : real) {
            assertTrue("处理器 [" + p.name() + "] 没有被登记进配置词表, 界面就配不出它",
                    PipelineStages.supportedTypes().contains(nameOf(p)));
        }
    }

    private static String nameOf(FieldProcessor p) {
        for (String type : PipelineStages.supportedTypes()) {
            if (p.name().equals(PipelineStages.processorNameFor(type))) {
                return type;
            }
        }
        return "UNMAPPED:" + p.name();
    }

    @Test
    public void defaultOrderIsTheHistoricalProcessorChain() {
        // 默认链 = 处理器名按字典序 (Pipeline 历史上就是这么排的)。改动这一格等于改动
        // "没有配置的实体"的运行时行为, 所以钉住。
        assertEquals(Arrays.asList(PipelineStages.DICT_RESOLVE, PipelineStages.REF_CHECK,
                PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                PipelineStages.VALUE_VALIDATE), PipelineStages.defaultOrder());
    }

    // ===== 接受的方向 =====

    @Test
    public void acceptsFullChainInAnySupportedOrder() {
        assertEquals(Arrays.asList(PipelineStages.DICT_RESOLVE, PipelineStages.REF_CHECK,
                PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                PipelineStages.VALUE_VALIDATE), resolve(PipelineStages.BEFORE_CREATE, ALL_FIVE));
    }

    @Test
    public void sortOrderWinsOverArrayPosition() {
        // 界面用 order 表达"第几步", 数组位置只是草稿态; 两者冲突时 order 说话
        String stages = "[{\"type\":\"VALUE_VALIDATE\",\"order\":9},"
                + "{\"type\":\"REQUIRED_CHECK\",\"order\":1},"
                + "{\"type\":\"TYPE_CONVERT\",\"order\":5}]";
        assertEquals(Arrays.asList(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                PipelineStages.VALUE_VALIDATE), resolve(PipelineStages.BEFORE_UPDATE, stages));
    }

    @Test
    public void absentOrderFallsBackToArrayPosition() {
        String stages = "[{\"type\":\"REQUIRED_CHECK\"},{\"type\":\"TYPE_CONVERT\"},"
                + "{\"type\":\"VALUE_VALIDATE\"},{\"type\":\"REF_CHECK\"}]";
        assertEquals(Arrays.asList(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                PipelineStages.VALUE_VALIDATE, PipelineStages.REF_CHECK),
                resolve(PipelineStages.BEFORE_CREATE, stages));
    }

    @Test
    public void theTwoWriteNoOpStagesAreOptional() {
        // 必填三闸齐了就该收下 —— 把 DICT_RESOLVE / REF_CHECK 变成必需, 等于逼用户配空跑的闸
        String stages = "[{\"type\":\"REQUIRED_CHECK\",\"order\":1},{\"type\":\"TYPE_CONVERT\",\"order\":2},"
                + "{\"type\":\"VALUE_VALIDATE\",\"order\":3}]";
        assertEquals(3, resolve(PipelineStages.BEFORE_CREATE, stages).size());
    }

    // ===== 拒绝的方向 =====

    @Test
    public void rejectsStageTypesWithoutAnExecutor() {
        // 这两个正是配置页历史上列出来、而后端根本没有任何实现的阶段
        for (String ghost : Arrays.asList("WEBHOOK", "SCRIPT")) {
            String stages = "[{\"type\":\"REQUIRED_CHECK\",\"order\":1},{\"type\":\"TYPE_CONVERT\",\"order\":2},"
                    + "{\"type\":\"VALUE_VALIDATE\",\"order\":3},{\"type\":\"" + ghost + "\",\"order\":4}]";
            expectReject(PipelineStages.BEFORE_CREATE, stages, ghost, "没有执行器", PipelineStages.TYPE_CONVERT);
        }
    }

    @Test
    public void rejectsTriggerEventsWithoutAHookPoint() {
        for (String event : Arrays.asList("AFTER_CREATE", "AFTER_UPDATE", "AFTER_DELETE", "CREATE", "BEFORE_DELETE")) {
            expectReject(event, ALL_FIVE, event, "没有挂接点", PipelineStages.BEFORE_CREATE);
        }
    }

    @Test
    public void rejectsMissingMandatoryStagesAndNamesEachOne() {
        String msg = expectReject(PipelineStages.BEFORE_CREATE,
                "[{\"type\":\"DICT_RESOLVE\",\"order\":1}]",
                PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT, PipelineStages.VALUE_VALIDATE);
        assertTrue("消息要说明为什么不能摘: " + msg, msg.contains("必填阶段"));
    }

    @Test
    public void rejectsValueValidateBeforeTypeConvert() {
        // 值校验的长度规则只对字符串单元格生效: 放到转换之前会把合法数字按字符长度拒掉
        String stages = "[{\"type\":\"VALUE_VALIDATE\",\"order\":1},{\"type\":\"REQUIRED_CHECK\",\"order\":2},"
                + "{\"type\":\"TYPE_CONVERT\",\"order\":3}]";
        expectReject(PipelineStages.BEFORE_CREATE, stages, "VALUE_VALIDATE", "TYPE_CONVERT", "之后");
    }

    @Test
    public void rejectsDuplicateStageInsteadOfRunningItTwice() {
        String stages = "[{\"type\":\"REQUIRED_CHECK\",\"order\":1},{\"type\":\"TYPE_CONVERT\",\"order\":2},"
                + "{\"type\":\"VALUE_VALIDATE\",\"order\":3},{\"type\":\"TYPE_CONVERT\",\"order\":4}]";
        expectReject(PipelineStages.BEFORE_CREATE, stages, "TYPE_CONVERT", "重复");
    }

    @Test
    public void rejectsUnparseableStageChains() {
        expectReject(PipelineStages.BEFORE_CREATE, "not json", "JSON");
        expectReject(PipelineStages.BEFORE_CREATE, "", "不能为空");
        expectReject(PipelineStages.BEFORE_CREATE, null, "不能为空");
        expectReject(PipelineStages.BEFORE_CREATE, "{}", "非空 JSON 数组");
        expectReject(PipelineStages.BEFORE_CREATE, "[]", "非空 JSON 数组");
        expectReject(PipelineStages.BEFORE_CREATE, "[\"TYPE_CONVERT\"]", "不是对象");
        expectReject(PipelineStages.BEFORE_CREATE,
                "[{\"order\":1},{\"type\":\"TYPE_CONVERT\"},{\"type\":\"REQUIRED_CHECK\"},"
                        + "{\"type\":\"VALUE_VALIDATE\"}]", "没有 type");
        expectReject(PipelineStages.BEFORE_CREATE,
                "[{\"type\":\"REQUIRED_CHECK\",\"order\":\"first\"},{\"type\":\"TYPE_CONVERT\"},"
                        + "{\"type\":\"VALUE_VALIDATE\"}]", "必须是整数");
    }

    @Test
    public void nullTriggerIsRejectedWithTheSupportedList() {
        expectReject(null, ALL_FIVE, "不能为空", PipelineStages.BEFORE_CREATE);
        expectReject("   ", ALL_FIVE, "不能为空", PipelineStages.BEFORE_CREATE);
    }

    @Test
    public void triggerMatchingIgnoresSurroundingBlank() {
        assertEquals(5, resolve("  BEFORE_CREATE  ", ALL_FIVE).size());
        assertTrue(PipelineStages.isSupportedTrigger(" BEFORE_UPDATE"));
    }

    // ===== 界面要照抄的那份"哪些阶段在写路径上什么都不做" =====

    @Test
    public void recordsWhichStagesDoNothingOnWrite() {
        assertEquals(Arrays.asList(PipelineStages.DICT_RESOLVE, PipelineStages.REF_CHECK),
                PipelineStages.noOpOnWriteTypes());
        assertEquals(Arrays.asList(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                PipelineStages.VALUE_VALIDATE), PipelineStages.mandatoryTypes());
        assertFalse("REF_CHECK 只做 log.debug, 不许被标成一道闸",
                PipelineStages.mandatoryTypes().contains(PipelineStages.REF_CHECK));
    }

    @Test
    public void supportedVocabularyListsEveryOptionSoMessagesCanTeach() {
        assertEquals(2, PipelineStages.supportedTriggers().size());
        assertTrue(PipelineStages.supportedTriggers().containsAll(
                Arrays.asList(PipelineStages.BEFORE_CREATE, PipelineStages.BEFORE_UPDATE)));
        assertEquals(5, PipelineStages.supportedTypes().size());
        assertEquals("BEFORE_CREATE / BEFORE_UPDATE", PipelineStages.join(
                new java.util.ArrayList<String>(PipelineStages.supportedTriggers())));
    }

    @Test
    public void supportedTypeCheckIsCaseSensitiveOnPurpose() {
        // 阶段类型是配置面与执行面的合同词, 放宽成大小写不敏感会把 type_convert 存成幽灵行
        assertFalse(PipelineStages.isSupportedType("type_convert"));
        assertFalse(PipelineStages.isSupportedType(null));
        assertTrue(PipelineStages.isSupportedType(" TYPE_CONVERT "));
    }
}
