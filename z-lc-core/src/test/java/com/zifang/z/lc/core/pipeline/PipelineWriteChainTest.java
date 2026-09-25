package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.pipeline.config.PipelineConfigService;
import com.zifang.z.lc.core.pipeline.config.PipelineStages;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 执行面: 配置到底有没有决定运行时跑什么。
 * <p>
 * 这一族的缺陷 #41 在于"配置页写着按 order 执行, 而 {@code Pipeline} 永远跑全部 bean 并按类名排序"
 * —— {@code PipelineConfigService.listByEvent} 在生产代码里零调用者就是这个脱节的硬证据。
 * 所以这里全部用**记录调用顺序的桩处理器**, 直接量"哪几个跑了、按什么顺序跑", 而不是量错误消息。
 */
public class PipelineWriteChainTest {

    private static final String APP = "crm";
    private static final String ENTITY = "order";

    private List<String> calls;
    private Map<String, FieldProcessor> container;
    private StubConfigService configs;

    @Before
    public void setUp() {
        calls = new ArrayList<String>();
        container = new HashMap<String, FieldProcessor>();
        for (String name : Arrays.asList("DictResolve", "RefCheck", "RequiredCheck", "TypeConvert", "ValueValidate")) {
            container.put(name, new Recording(name, calls));
        }
        configs = new StubConfigService();
    }

    private Pipeline pipelineWith(FieldProcessor... processors) {
        Pipeline pipeline = new Pipeline(new ArrayList<FieldProcessor>(Arrays.asList(processors)));
        pipeline.setConfigService(configs);
        return pipeline;
    }

    private Pipeline defaultPipeline() {
        return pipelineWith(container.values().toArray(new FieldProcessor[0]));
    }

    private static PipelineConfigEntity config(long id, String trigger, String stages, int enabled) {
        PipelineConfigEntity e = new PipelineConfigEntity();
        e.setId(id);
        e.setAppCode(APP);
        e.setEntityCode(ENTITY);
        e.setTriggerEvent(trigger);
        e.setStages(stages);
        e.setEnabled(enabled);
        return e;
    }

    private static String stagesOf(String... types) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"type\":\"").append(types[i]).append("\",\"order\":").append(i + 1).append('}');
        }
        return sb.append(']').toString();
    }

    private static EntityDefDTO entity() {
        EntityDefDTO e = new EntityDefDTO();
        e.setAppCode(APP);
        e.setEntityCode(ENTITY);
        e.setTenantCode("t1");
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("name");
        f.setFieldName("名称");
        f.setFieldType("VARCHAR");
        e.setFields(new ArrayList<FieldDefDTO>(Collections.singletonList(f)));
        return e;
    }

    private static RuntimeCrudDTO body(Object... kv) {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            values.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        dto.setFieldValues(values);
        return dto;
    }

    // ===== 没有配置 = 保持现状 =====

    @Test
    public void withoutConfigTheFullChainRunsInNameOrder() {
        Pipeline pipeline = defaultPipeline();
        pipeline.preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("DictResolve", "RefCheck", "RequiredCheck", "TypeConvert", "ValueValidate"), calls);
    }

    @Test
    public void configServiceIsNeverConsultedForTheReadPath() {
        // 配置只描述写前挂接点; 读路径要是也去查一次配置表, 每次列表都要多一趟查询
        defaultPipeline().postRead(entity(), new HashMap<String, Object>());
        defaultPipeline().postReadList(entity(), new ArrayList<Map<String, Object>>());
        assertEquals(0, configs.lookups);
    }

    // ===== 有配置 = 配置说话 =====

    @Test
    public void configuredSubsetRunsExactlyThoseStages() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("RequiredCheck", "TypeConvert", "ValueValidate"), calls);
        assertFalse("没被点名的阶段不许跑: DictResolve 混进来说明链还是硬编码的那条",
                calls.contains("DictResolve"));
    }

    @Test
    public void configuredOrderIsTheExecutionOrder() {
        // 默认链是 RequiredCheck 在 TypeConvert 之前; 反过来配就必须反过来跑 ——
        // 这一条是"按 order 顺序执行"这句宣传唯一的兑现证据。
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.TYPE_CONVERT, PipelineStages.REQUIRED_CHECK,
                        PipelineStages.VALUE_VALIDATE), 1));
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("TypeConvert", "RequiredCheck", "ValueValidate"), calls);
    }

    @Test
    public void unreadStageParamsWarnButDoNotBlockWrites() {
        // 在写入口上"参数没人读就拒"这道闸之前存下的老配置行, 运行期不许因此把该实体的写入
        // 全按住; 但链必须照配置跑 —— "看见不认识的东西就退回默认链"是另一种装死。
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                "[{\"type\":\"REQUIRED_CHECK\",\"order\":1},"
                        + "{\"type\":\"TYPE_CONVERT\",\"config\":{\"trimStrings\":true},\"order\":2},"
                        + "{\"type\":\"VALUE_VALIDATE\",\"order\":3}]", 1));
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("RequiredCheck", "TypeConvert", "ValueValidate"), calls);
    }

    @Test
    public void configIsLookedUpPerTriggerPointNotPerRow() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        Pipeline.Chain chain = defaultPipeline().writeChain(APP, entity(), Pipeline.BEFORE_CREATE);
        for (int i = 0; i < 2000; i++) {
            chain.run(entity(), body("name", "row" + i));
        }
        assertEquals("整批只该查一次配置表 (2000 行 = 2000 次查询是回归)", 1, configs.lookups);
        assertEquals(2000 * 3, calls.size());
    }

    @Test
    public void beforeUpdateAndBeforeCreateAreDifferentChains() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        configs.rows.add(config(2L, Pipeline.BEFORE_UPDATE,
                stagesOf(PipelineStages.DICT_RESOLVE, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE, PipelineStages.REQUIRED_CHECK), 1));
        Pipeline pipeline = defaultPipeline();
        pipeline.preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("RequiredCheck", "TypeConvert", "ValueValidate"), calls);
        calls.clear();
        pipeline.preWrite(APP, entity(), body("name", "b"), Pipeline.BEFORE_UPDATE);
        assertEquals(Arrays.asList("DictResolve", "TypeConvert", "ValueValidate", "RequiredCheck"), calls);
    }

    @Test
    public void disabledConfigLeavesTheDefaultChainAlone() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 0));
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(5, calls.size());
    }

    @Test
    public void configOfAnotherAppOrEntityDoesNotLeakIn() {
        PipelineConfigEntity otherApp = config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK), 1);
        otherApp.setAppCode("hr");
        PipelineConfigEntity otherEntity = config(2L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK), 1);
        otherEntity.setEntityCode("customer");
        configs.rows.add(otherApp);
        configs.rows.add(otherEntity);
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals("别的应用/实体的配置不许决定这条链", 5, calls.size());
    }

    // ===== 配置兑现不了的时候: 拒绝, 不许悄悄退回默认链 =====

    @Test
    public void legacyConfigMissingMandatoryStagesFailsLoudInsteadOfFallingBack() {
        // 校验是后加的, 库里可能已有这种行。这时候"退回默认链"= 用户以为自定义链在跑,
        // 实际跑的是另一条 —— 正是 #41 本身, 所以只能抛错。
        configs.rows.add(config(7L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.DICT_RESOLVE), 1));
        try {
            defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
            fail("缺必填阶段的存量配置必须抛错");
        } catch (IllegalArgumentException ex) {
            assertTrue("消息要带得出是哪一条配置: " + ex.getMessage(), ex.getMessage().contains("id=7"));
            assertTrue(ex.getMessage().contains(PipelineStages.REQUIRED_CHECK));
        }
        assertEquals("抛错之前一个阶段都不该跑", 0, calls.size());
    }

    @Test
    public void stageWithoutABeanInContainerFailsLoudInsteadOfBeingSkipped() {
        // 词表里有、容器里没有 = 配了不跑的第二次翻车; 跳过它等于静默少一道闸
        Pipeline pipeline = pipelineWith(
                container.get("RequiredCheck"), container.get("TypeConvert"));
        pipeline.setConfigService(configs);
        configs.rows.add(config(9L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        try {
            pipeline.preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
            fail("容器里没有 ValueValidate 时必须抛错");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("ValueValidate"));
            assertTrue("不能退回默认链: " + ex.getMessage(), ex.getMessage().contains("拒绝"));
        }
        assertEquals("链解析失败就不该有半条链跑过", 0, calls.size());
    }

    @Test
    public void multipleEnabledConfigsPickTheNewestWithoutMerging() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        configs.rows.add(config(2L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.TYPE_CONVERT, PipelineStages.REQUIRED_CHECK,
                        PipelineStages.VALUE_VALIDATE, PipelineStages.DICT_RESOLVE), 1));
        defaultPipeline().preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(Arrays.asList("TypeConvert", "RequiredCheck", "ValueValidate", "DictResolve"), calls);
    }

    @Test
    public void missingAppCodeFallsBackToDefaultChainWithoutLookingUp() {
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK), 1));
        // appCode 拿不到 (调用方没传) 时不查配置: 查了也只会拿到一条不知道属于谁的链
        defaultPipeline().preWrite(null, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
        assertEquals(5, calls.size());
        assertEquals(0, configs.lookups);
    }

    @Test
    public void processorNamesAreWrappedIntoPipelineExceptionWithTheStageThatThrew() {
        // 执行链换了来源之后, 报错还要指得是哪个阶段 —— 界面/日志都靠这个前缀定位
        configs.rows.add(config(1L, Pipeline.BEFORE_CREATE,
                stagesOf(PipelineStages.REQUIRED_CHECK, PipelineStages.TYPE_CONVERT,
                        PipelineStages.VALUE_VALIDATE), 1));
        Pipeline pipeline = new Pipeline(new ArrayList<FieldProcessor>(Arrays.asList(
                container.get("RequiredCheck"), new Throwing("TypeConvert", calls),
                container.get("ValueValidate"))));
        pipeline.setConfigService(configs);
        try {
            pipeline.preWrite(APP, entity(), body("name", "a"), Pipeline.BEFORE_CREATE);
            fail("处理器抛错必须包成 PipelineException");
        } catch (Pipeline.PipelineException ex) {
            assertEquals("TypeConvert", ex.getProcessorName());
            assertTrue(ex.getMessage(), ex.getMessage().contains("boom"));
        }
        assertEquals("抛错之后的链停在坏阶段, 后面的不许再跑",
                Arrays.asList("RequiredCheck", "TypeConvert"), calls);
    }

    // ===== 桩 =====

    private static class Recording implements FieldProcessor {
        private final String name;
        private final List<String> calls;

        private Recording(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
            calls.add(name);
        }

        @Override
        public void postRead(EntityDefDTO entity, Map<String, Object> row) {
            calls.add(name + ":read");
        }
    }

    private static final class Throwing extends Recording {
        private Throwing(String name, List<String> calls) {
            super(name, calls);
        }

        @Override
        public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
            super.preWrite(entity, body);
            throw new IllegalArgumentException("boom");
        }
    }

    /** 只覆盖查询, 不碰数据库: 语义要跟真实的 listByEvent 一致 (enabled=1 + 三元组匹配). */
    private static final class StubConfigService extends PipelineConfigService {
        final List<PipelineConfigEntity> rows = new ArrayList<PipelineConfigEntity>();
        int lookups;

        @Override
        public List<PipelineConfigEntity> listByEvent(String appCode, String entityCode, String triggerEvent) {
            lookups++;
            List<PipelineConfigEntity> out = new ArrayList<PipelineConfigEntity>();
            for (PipelineConfigEntity row : rows) {
                if (equals(appCode, row.getAppCode()) && equals(entityCode, row.getEntityCode())
                        && equals(triggerEvent, row.getTriggerEvent())
                        && Integer.valueOf(1).equals(row.getEnabled())) {
                    out.add(row);
                }
            }
            return out;
        }

        private static boolean equals(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    }
}
