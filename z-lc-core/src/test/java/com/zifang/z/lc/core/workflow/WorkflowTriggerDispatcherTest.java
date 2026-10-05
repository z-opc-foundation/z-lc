package com.zifang.z.lc.core.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.adapter.CamudaAdapter;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowFireMapper;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * WorkflowTriggerDispatcher 单元测试：绑定从「存起来的一张纸」变成真的发一次单（缺陷 #61）.
 * <p>
 * {@code listByEvent} 在生产代码里零调用者就是这一支坏掉的硬证据，所以这里第一层钉的是
 * "记录写成功之后真的去读绑定、真的发起、真的把结局写回 {@code z_lc_workflow_fire}"；
 * 第二层钉三条口径：外部引擎怎样都不能把用户的写入带走（不抛）、等待有上限、结局能回读。
 * 查询本身按不按租户过滤不在这里证 —— 那是 {@link WorkflowBindingServiceTest} 与契约层的事，
 * 这里的替身只按"给它什么绑定就发什么"配合，并记下它被问了什么。
 */
public class WorkflowTriggerDispatcherTest {

    private static final String TENANT = "default";
    private static final String APP = "crm";
    private static final String ENTITY = "order";
    private static final String PROCESS = "expense-approval";
    private static final Long RECORD_ID = 42L;

    private WorkflowTriggerDispatcher dispatcher;
    private FakeBindingService bindings;
    private FakeAdapter adapter;
    private RecordingFireMapper fires;

    @Before
    public void setUp() {
        dispatcher = new WorkflowTriggerDispatcher();
        bindings = new FakeBindingService();
        adapter = new FakeAdapter();
        fires = new RecordingFireMapper();
        dispatcher.setBindingService(bindings);
        dispatcher.setCamudaAdapter(adapter);
        dispatcher.setFireMapper(fires.mapper());
    }

    private WorkflowBindingEntity binding(long id, String processKey) {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        e.setId(id);
        e.setTenantCode(TENANT);
        e.setAppCode(APP);
        e.setEntityCode(ENTITY);
        e.setTriggerEvent(WorkflowTriggers.AFTER_CREATE);
        e.setProcessDefinitionKey(processKey);
        e.setAutoSubmit(1);
        e.setDeleted(0);
        return e;
    }

    private Map<String, Object> fields() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("title", "两万元报销");
        m.put("amount", 20000);
        return m;
    }

    /** 跑一遍真实派发并给出结局行 —— 每条断言都基于同一次调用。 */
    private int dispatch() {
        return dispatcher.afterCreate(TENANT, APP, ENTITY, RECORD_ID, fields(), "u-7");
    }

    private WorkflowFireEntity onlyFire() {
        assertEquals("一次尝试该留下一行结局: ", 1, fires.rows.size());
        return fires.rows.get(0);
    }

    // ---------------- 这条链真的存在 ----------------

    @Test
    public void noBindingsMeansNoRequestAndNoLedgerRow() {
        assertEquals("这个挂接点上没有启用的绑定 ⇒ 一次都不该发", 0, dispatch());
        assertEquals(0, adapter.calls.get());
        assertTrue(fires.rows.isEmpty());
    }

    @Test
    public void aBindingIsActuallyStartedAndTheOutcomeIsReadableBack() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-42");

        assertEquals(1, dispatch());
        WorkflowFireEntity row = onlyFire();
        assertEquals(WorkflowFireEntity.STATUS_STARTED, row.getStatus());
        assertEquals("实例 id 要落库，否则「发起了」这句话没人能验: ", "wf-42", row.getInstanceId());
        assertNull("成功行不该带失败原因: ", row.getDetail());
        assertEquals(Long.valueOf(1L), row.getBindingId());
        assertEquals(TENANT, row.getTenantCode());
        assertEquals(APP, row.getAppCode());
        assertEquals(ENTITY, row.getEntityCode());
        assertEquals(RECORD_ID, row.getRecordId());
        assertEquals(PROCESS, row.getProcessDefinitionKey());
        assertEquals(WorkflowTriggers.AFTER_CREATE, row.getTriggerEvent());
        assertNotNull(row.getCreateTime());
        assertNotNull(row.getUpdateTime());
        assertEquals(Integer.valueOf(0), row.getDeleted());
    }

    @Test
    public void theHookLookedUpIsTheOnesTheEngineHas() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        dispatch();
        List<Object> asked = bindings.lastAsk;
        assertEquals("派发器问了 " + asked + "，四格都该是记录自己的坐标", 4, asked.size());
        assertEquals("发起人来自记录所在租户，不能在这里钉死 default（那样别的租户写数据永远不发单）: ",
                TENANT, asked.get(0));
        assertEquals(APP, asked.get(1));
        assertEquals(ENTITY, asked.get(2));
        assertEquals("挂接点必须是引擎真有的那一个: ", WorkflowTriggers.AFTER_CREATE, asked.get(3));
    }

    @Test
    public void everyBindingOnTheHookGetsItsOwnRequestAndRow() {
        bindings.add(binding(1L, "first-flow"));
        bindings.add(binding(2L, "second-flow"));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-x");

        assertEquals("两条绑定就是两次尝试（返回值是尝试条数，不是成功条数）", 2, dispatch());
        assertEquals(2, adapter.calls.get());
        assertEquals(2, fires.rows.size());
        assertEquals(Arrays.asList(1L, 2L), Arrays.asList(fires.rows.get(0).getBindingId(),
                fires.rows.get(1).getBindingId()));
        assertEquals(Arrays.asList("first-flow", "second-flow"),
                Arrays.asList(fires.rows.get(0).getProcessDefinitionKey(),
                        fires.rows.get(1).getProcessDefinitionKey()));
    }

    // ---------------- 交给引擎的那一单长什么样 ----------------

    @Test
    public void handsTheEngineTheCoordinatesThatIdentifyThisRecord() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-42");
        dispatch();

        Ask ask = adapter.lastAsk;
        assertEquals("businessKey 要能反查回这条记录: ", "crm:order:42", ask.businessKey);
        assertEquals(PROCESS, ask.processKey);
        assertEquals("发起人就是写这条记录的人: ", "u-7", ask.initiator);
        assertEquals("order#42", ask.title);
        assertEquals("流程变量要带整份字段值，审批人才看得见这单在说什么: " + ask.variables,
                "两万元报销", ask.variables.get("title"));
        assertEquals(Integer.valueOf(20000), ask.variables.get("amount"));
        assertEquals(APP, ask.variables.get("lcAppCode"));
        assertEquals(ENTITY, ask.variables.get("lcEntityCode"));
        assertEquals(RECORD_ID, ask.variables.get("lcRecordId"));
    }

    @Test
    public void trimsTheStoredProcessKeyBeforeSendingIt() {
        bindings.add(binding(1L, "  " + PROCESS + " "));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        dispatch();
        assertEquals("绑定里存的 KEY 带空白时不能原样发给引擎: ", PROCESS, adapter.lastAsk.processKey);
    }

    // ---------------- 失败只落账，绝不带走用户的写入 ----------------

    @Test
    public void engineFailureBecomesAFailedRowNotAnException() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.failed("POST /api/approval-center/processes/start http=503");

        try {
            assertEquals("外部引擎挂了也要把「尝试过」这件事如实报出来: ", 1, dispatch());
        } catch (RuntimeException ex) {
            fail("发起失败绝不能抛回业务写路径（用户刚写的数据不该为一台外部引擎陪葬）: " + ex);
        }
        WorkflowFireEntity row = onlyFire();
        assertEquals(WorkflowFireEntity.STATUS_FAILED, row.getStatus());
        assertNull("失败行没有实例 id，界面才不会把它当已发起: ", row.getInstanceId());
        assertTrue("失败原因要能回读: " + row.getDetail(), row.getDetail().contains("http=503"));
    }

    @Test
    public void anExceptionFromTheEngineIsAlsoJustAFailedRow() {
        bindings.add(binding(1L, PROCESS));
        adapter.thrown = new IllegalStateException("socket 断了");
        assertEquals(1, dispatch());
        assertEquals(WorkflowFireEntity.STATUS_FAILED, onlyFire().getStatus());
        assertTrue(onlyFire().getDetail().contains("socket 断了"));
    }

    @Test
    public void waitingIsBoundedSoAHungEngineCannotHoldTheUserWrite() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-late");
        adapter.delayMs = 400L;
        dispatcher.setTimeoutMs(40L);

        long began = System.currentTimeMillis();
        assertEquals(1, dispatch());
        long spent = System.currentTimeMillis() - began;
        WorkflowFireEntity row = onlyFire();
        assertEquals("z-camuda 挂住时必须判 FAILED 而不是等它: ", WorkflowFireEntity.STATUS_FAILED, row.getStatus());
        assertTrue("要说是超时: " + row.getDetail(), row.getDetail().contains("40ms"));
        assertTrue("等待有上限（实测 " + spent + "ms）：共享 http 客户端的读超时是 60s，"
                + "那条 60s 会把用户的「新建记录」按住一分钟", spent < 2_000L);
    }

    @Test
    public void defaultTimeoutIsNotTheSixtySecondHttpClientReadTimeout() {
        assertTrue("默认上限该是秒级而不是共享客户端的 60s: " + WorkflowTriggerDispatcher.DEFAULT_TIMEOUT_MS,
                WorkflowTriggerDispatcher.DEFAULT_TIMEOUT_MS > 0L
                        && WorkflowTriggerDispatcher.DEFAULT_TIMEOUT_MS <= 5_000L);
    }

    @Test
    public void concurrentDispatchSlotsAreBoundedAndRejectHonestly() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        ExecutorService exhausted = Executors.newSingleThreadExecutor();
        exhausted.shutdownNow();
        dispatcher.setWorkers(exhausted);

        try {
            assertEquals("池满也要留下尝试过的一行: ", 1, dispatch());
        } catch (RuntimeException ex) {
            fail("并发额度用尽同样不能抛回写路径: " + ex);
        }
        WorkflowFireEntity row = onlyFire();
        assertEquals(WorkflowFireEntity.STATUS_FAILED, row.getStatus());
        assertTrue("要说清是没排队判的额度上限: " + row.getDetail(),
                row.getDetail().contains("并发发起已达上限"));
        assertEquals("槽位当场拒了就不该有请求发出去: ", 0, adapter.calls.get());
    }

    @Test
    public void ledgerWriteFailureDoesNotEscalateToTheBusinessWrite() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        fires.insertFailure = new IllegalStateException("z_lc_workflow_fire 不存在");
        try {
            assertEquals(1, dispatch());
        } catch (RuntimeException ex) {
            fail("结局账写不进去也不能把用户的写入带走: " + ex);
        }
    }

    @Test
    public void bindingLookupFailureIsOnlyAWarn() {
        bindings.add(binding(1L, PROCESS));
        bindings.failure = new IllegalStateException("数据库连不上");
        try {
            assertEquals("查不到绑定就当作没有绑定，不能抛: ", 0, dispatch());
        } catch (RuntimeException ex) {
            fail("绑定查询失败抛回写路径就是让业务写入为读侧的抖动陪葬: " + ex);
        }
        assertEquals(0, adapter.calls.get());
    }

    @Test
    public void refusesToDispatchWithoutTheCoordinatesOfARealRecord() {
        bindings.add(binding(1L, PROCESS));
        assertEquals(0, dispatcher.afterCreate(TENANT, APP, ENTITY, null, fields(), "u-7"));
        assertEquals(0, dispatcher.afterCreate(TENANT, null, ENTITY, RECORD_ID, fields(), "u-7"));
        assertEquals(0, dispatcher.afterCreate(TENANT, APP, null, RECORD_ID, fields(), "u-7"));
        assertEquals("缺坐标时一次都不该发: ", 0, adapter.calls.get());
        assertTrue(fires.rows.isEmpty());
    }

    @Test
    public void nullFieldValuesStillProducesAUsableRequest() {
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        dispatcher.afterCreate(TENANT, APP, ENTITY, RECORD_ID, null, "u-7");
        assertEquals(1, adapter.calls.get());
        assertEquals("字段值缺席也要留下定位这条记录的三个变量: ", RECORD_ID,
                adapter.lastAsk.variables.get("lcRecordId"));
        assertEquals(WorkflowFireEntity.STATUS_STARTED, onlyFire().getStatus());
    }

    @Test
    public void longFailureReasonIsClippedAndSaysSo() {
        bindings.add(binding(1L, PROCESS));
        char[] blob = new char[800];
        Arrays.fill(blob, '崩');
        adapter.answer = CamudaAdapter.ProcessStart.failed("引擎回了一大段栈: " + new String(blob));
        dispatch();
        String detail = onlyFire().getDetail();
        assertTrue("detail 列只有 512 字符，超长要截断而不是让落库失败: 实际长度 " + detail.length(),
                detail.length() <= 512);
        assertTrue("截断要留得出痕迹: " + detail.substring(detail.length() - 20),
                detail.endsWith("…(截断)"));
        assertTrue(detail.startsWith("引擎回了一大段栈: "));
    }

    @Test
    public void nullBindingRowsAreSkippedNotCounted() {
        bindings.add(null);
        bindings.add(binding(1L, PROCESS));
        adapter.answer = CamudaAdapter.ProcessStart.started("wf-1");
        assertEquals("null 行不该算一次尝试: ", 1, dispatch());
        assertEquals(1, adapter.calls.get());
    }

    // ---------------- 替身 ----------------

    private static final class Ask {
        String processKey;
        String businessKey;
        String initiator;
        String title;
        Map<String, Object> variables;
    }

    private static final class FakeAdapter extends CamudaAdapter {
        CamudaAdapter.ProcessStart answer = CamudaAdapter.ProcessStart.started("wf-stub");
        RuntimeException thrown;
        long delayMs;
        final AtomicInteger calls = new AtomicInteger();
        volatile Ask lastAsk;

        @Override
        public CamudaAdapter.ProcessStart startProcess(String processKey, String businessKey,
                                                   String initiator, String title,
                                                   Map<String, Object> variables) {
            calls.incrementAndGet();
            Ask ask = new Ask();
            ask.processKey = processKey;
            ask.businessKey = businessKey;
            ask.initiator = initiator;
            ask.title = title;
            ask.variables = variables;
            lastAsk = ask;
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待被中断", ie);
                }
            }
            if (thrown != null) {
                throw thrown;
            }
            return answer;
        }
    }

    /** 只回答"这个挂接点上有哪些绑定"，并记下被问了什么；租户/事件过滤本身在 service 层钉。 */
    private static final class FakeBindingService extends WorkflowBindingService {
        private final List<WorkflowBindingEntity> rows = new ArrayList<>();
        RuntimeException failure;
        volatile List<Object> lastAsk;

        void add(WorkflowBindingEntity e) {
            rows.add(e);
        }

        @Override
        public List<WorkflowBindingEntity> listByEvent(String tenantCode, String appCode,
                                                       String entityCode, String triggerEvent) {
            lastAsk = Arrays.<Object>asList(tenantCode, appCode, entityCode, triggerEvent);
            if (failure != null) {
                throw failure;
            }
            return new ArrayList<>(rows);
        }
    }

    private static final class RecordingFireMapper {
        final List<WorkflowFireEntity> rows = new ArrayList<>();
        RuntimeException insertFailure;

        WorkflowFireMapper mapper() {
            return (WorkflowFireMapper) Proxy.newProxyInstance(
                    WorkflowFireMapper.class.getClassLoader(),
                    new Class<?>[]{WorkflowFireMapper.class, BaseMapper.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("insert".equals(name)) {
                                if (insertFailure != null) {
                                    throw insertFailure;
                                }
                                rows.add((WorkflowFireEntity) args[0]);
                                return 1;
                            }
                            Class<?> rt = method.getReturnType();
                            if (rt == int.class) {
                                return 0;
                            }
                            if (rt == boolean.class) {
                                return false;
                            }
                            if (List.class.isAssignableFrom(rt)) {
                                return new ArrayList<>();
                            }
                            return null;
                        }
                    });
        }
    }
}
