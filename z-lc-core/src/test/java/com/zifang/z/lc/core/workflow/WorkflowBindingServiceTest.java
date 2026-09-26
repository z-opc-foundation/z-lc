package com.zifang.z.lc.core.workflow;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowBindingMapper;
import com.zifang.z.lc.mapper.workflow.WorkflowFireMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * WorkflowBindingService 单元测试.
 * <p>
 * 两个 mapper 用 JDK 动态代理模拟，并<b>按 {@code QueryWrapper} 里真实存在的谓词过滤</b>：
 * 缺陷 #61 之后这里的账全是租户、自动提单、查重、删除结局这些只有按谓词过滤才证得出的形状。
 * 替身自己先被 {@link #fixtureReallyAppliesWrapperPredicates()} 打一遍 —— 否则下面
 * "看不见跨租户的行"那几条都可能是替身忽略 wrapper 造成的假绿（同 {@code SchemaAdminBizServiceTest}）。
 * <p>
 * {@code selectById} 返回的是<b>脱离库存的副本</b>：真库里读出来的是一个新对象，替身若返回同一个
 * 引用，"更新不能改写租户"这一类断言就会因为测试自己改的正是那一行而永远绿。
 */
public class WorkflowBindingServiceTest {

    private static final String TENANT = "default";
    private static final String APP = "crm";
    private static final String ENTITY = "order";
    private static final String PROCESS = "expense-approval";

    private WorkflowBindingService service;
    private List<WorkflowBindingEntity> bindingRows;
    private List<WorkflowFireEntity> fireRows;
    /** 记下每一次 updateById 写了谁：删除/更新那几条要看真的走到了落库这一步。 */
    private List<String> updated;
    private AtomicLong bindingIds;

    @Before
    public void setUp() throws Exception {
        service = new WorkflowBindingService();
        bindingRows = Collections.synchronizedList(new ArrayList<WorkflowBindingEntity>());
        fireRows = Collections.synchronizedList(new ArrayList<WorkflowFireEntity>());
        updated = Collections.synchronizedList(new ArrayList<String>());
        bindingIds = new AtomicLong(0);
        AtomicLong fireIds = new AtomicLong(0);

        WorkflowBindingMapper bindingMapper = (WorkflowBindingMapper) Proxy.newProxyInstance(
                WorkflowBindingMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowBindingMapper.class, BaseMapper.class},
                new BindingHandler(bindingRows, bindingIds, updated));
        WorkflowFireMapper fireMapper = (WorkflowFireMapper) Proxy.newProxyInstance(
                WorkflowFireMapper.class.getClassLoader(),
                new Class<?>[]{WorkflowFireMapper.class, BaseMapper.class},
                new FireHandler(fireRows, fireIds));

        inject(service, "workflowBindingMapper", bindingMapper);
        inject(service, "workflowFireMapper", fireMapper);
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private WorkflowBindingEntity binding(String tenant, String app, String entity,
                                          String event, String processKey) {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        e.setTenantCode(tenant);
        e.setAppCode(app);
        e.setEntityCode(entity);
        e.setTriggerEvent(event);
        e.setProcessDefinitionKey(processKey);
        return e;
    }

    private WorkflowBindingEntity validBinding() {
        return binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS);
    }

    /**
     * 直接塞一行进"库"（补主键、补齐 create 会写的那些列），用来种旧数据/跨租户的行 ——
     * 这些形状走不过写入口的闸，却是今天库里真实存在的东西。
     */
    private WorkflowBindingEntity seed(WorkflowBindingEntity e) {
        e.setId(bindingIds.incrementAndGet());
        if (e.getDeleted() == null) {
            e.setDeleted(0);
        }
        if (e.getAutoSubmit() == null) {
            e.setAutoSubmit(1);
        }
        bindingRows.add(e);
        return e;
    }

    private WorkflowFireEntity seedFire(String tenant, String entity, Long recordId, String status) {
        WorkflowFireEntity f = new WorkflowFireEntity();
        f.setId((long) (fireRows.size() + 1));
        f.setTenantCode(tenant);
        f.setAppCode(APP);
        f.setEntityCode(entity);
        f.setRecordId(recordId);
        f.setStatus(status);
        f.setDeleted(0);
        fireRows.add(f);
        return f;
    }

    /** 复制一份"用户提交上来的改动"：断言要看的是库存的那一行，不是这只副本。 */
    private WorkflowBindingEntity edit(WorkflowBindingEntity stored) {
        WorkflowBindingEntity c = binding(stored.getTenantCode(), stored.getAppCode(),
                stored.getEntityCode(), stored.getTriggerEvent(), stored.getProcessDefinitionKey());
        c.setId(stored.getId());
        c.setAutoSubmit(stored.getAutoSubmit());
        c.setCreateTime(stored.getCreateTime());
        c.setUpdateTime(stored.getUpdateTime());
        c.setDeleted(stored.getDeleted());
        return c;
    }

    private WorkflowBindingEntity storedRow(int index) {
        return bindingRows.get(index);
    }

    private void expectBadRequest(Runnable call, String mustMention) {
        try {
            call.run();
            fail("写入口应当拒掉，好让一份兑现不了的绑定不出生（消息里要点名 [" + mustMention + "]）");
        } catch (IllegalArgumentException ex) {
            assertTrue("拒绝消息要点名 [" + mustMention + "]，实际: " + ex.getMessage(),
                    ex.getMessage() != null && ex.getMessage().contains(mustMention));
        }
    }

    // ---------------- 替身自证 ----------------

    @Test
    public void fixtureReallyAppliesWrapperPredicates() {
        seed(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "keep"));
        seed(binding("other", APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "foreign-tenant"));
        seed(binding(TENANT, "erp", ENTITY, WorkflowTriggers.AFTER_CREATE, "foreign-app"));
        seed(binding(TENANT, APP, "task", WorkflowTriggers.AFTER_CREATE, "foreign-entity"));
        seed(binding(TENANT, APP, ENTITY, "AFTER_DELETE", "foreign-event"));
        seed(autoSubmitOff(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "auto-submit-off")));
        seed(softDeleted(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "soft-deleted")));

        assertEquals("夹具: 挂接点上只该留下同租户/同应用/同实体/同事件、未删除且 auto_submit=1 的那一行",
                Collections.singletonList("keep"),
                keysOf(service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE)));
        assertEquals("夹具: 按实体查（管理面那张表的全部绑定）该看得见旧行与别的事件的行，"
                + "只有租户/应用/deleted 参与过滤",
                Arrays.asList("auto-submit-off", "foreign-event", "keep"),
                keysOf(service.listByEntity(TENANT, APP, ENTITY)));
        assertEquals("夹具: 按实体查的实体谓词没生效（换一张表要看得见另一行）",
                Collections.singletonList("foreign-entity"),
                keysOf(service.listByEntity(TENANT, APP, "task")));
        List<String> appKeys = keysOf(service.listByApp(TENANT, APP));
        assertEquals("夹具: listByApp 的租户/应用/deleted 谓词没生效，读到的是 " + appKeys,
                Arrays.asList("auto-submit-off", "foreign-event", "foreign-entity", "keep"), appKeys);
        assertEquals("夹具: orderByDesc(id) 没生效（最后种下的那行该在最前）: " + appKeys,
                "auto-submit-off", appKeys.get(0));
    }

    private static WorkflowBindingEntity autoSubmitOff(WorkflowBindingEntity e) {
        e.setAutoSubmit(0);
        return e;
    }

    private static WorkflowBindingEntity softDeleted(WorkflowBindingEntity e) {
        e.setDeleted(1);
        return e;
    }

    private static List<String> keysOf(List<WorkflowBindingEntity> rows) {
        List<String> out = new ArrayList<>();
        for (WorkflowBindingEntity r : rows) {
            out.add(r.getProcessDefinitionKey());
        }
        return out;
    }

    // ---------------- 读侧：每个查询都按租户钉住 ----------------

    @Test
    public void listByEventShouldSeeNothingForAnotherTenant() {
        seed(binding("other", APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS));
        assertEquals("跨租户的绑定不能出现在这个租户的挂接点上", 0,
                service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE).size());
        assertEquals("反过来那一行要看得见", 1,
                service.listByEvent("other", APP, ENTITY, WorkflowTriggers.AFTER_CREATE).size());
    }

    @Test
    public void listByEventShouldSkipRowsWithAutoSubmitOff() {
        seed(autoSubmitOff(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS)));
        assertEquals("闸存在之前落的旧行（auto_submit=0）不能被当「已启用」，否则一条永不发单的绑定会混进结局账",
                0, service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE).size());
    }

    @Test
    public void listByEventShouldReturnEnabledRowsInRegistrationOrder() {
        service.create(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "first"));
        service.create(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "second"));
        service.create(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "third"));
        assertEquals("同一个挂接点上的多条绑定按登记先后发起（orderByAsc(id)）: ",
                Arrays.asList("first", "second", "third"),
                keysOf(service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE)));
    }

    @Test
    public void readsShouldNotReturnSoftDeletedRows() {
        seed(softDeleted(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS)));
        assertEquals(0, service.listByApp(TENANT, APP).size());
        assertEquals(0, service.listByEntity(TENANT, APP, ENTITY).size());
        assertEquals(0, service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE).size());
    }

    // ---------------- 写入口：兑现不了的形态不生出绑定行 ----------------

    @Test
    public void createShouldStoreWithDefaultsAndStamps() {
        WorkflowBindingEntity e = validBinding();
        WorkflowBindingEntity result = service.create(e);

        assertSame(e, result);
        assertNotNull(result.getId());
        assertEquals(Integer.valueOf(0), result.getDeleted());
        assertEquals("autoSubmit 缺省应为 1（显式给 0 会在写入口被拒，见 refuseAutoSubmitOff）",
                Integer.valueOf(1), result.getAutoSubmit());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(1, bindingRows.size());
        assertEquals(WorkflowTriggers.AFTER_CREATE, storedRow(0).getTriggerEvent());
    }

    @Test
    public void createShouldTrimTheEventAndProcessKey() {
        WorkflowBindingEntity e = binding(TENANT, APP, ENTITY, "  AFTER_CREATE  ", "  " + PROCESS + " ");
        service.create(e);
        assertEquals(WorkflowTriggers.AFTER_CREATE, e.getTriggerEvent());
        assertEquals(PROCESS, e.getProcessDefinitionKey());
    }

    @Test
    public void createShouldRefuseEveryTriggerTheEngineCannotHonor() {
        for (final String event : WorkflowTriggers.unimplementedReasons().keySet()) {
            expectBadRequest(new Runnable() {
                @Override
                public void run() {
                    service.create(binding(TENANT, APP, ENTITY, event, PROCESS));
                }
            }, event);
            assertEquals("被拒的 [" + event + "] 不该留下一行: ", 0, bindingRows.size());
        }
    }

    @Test
    public void createShouldRefuseAnUnknownEventAndPointAtWhatIsAvailable() {
        try {
            service.create(binding(TENANT, APP, ENTITY, "ON_SUNDAY", PROCESS));
            fail("不认识的事件必须拒掉");
        } catch (IllegalArgumentException ex) {
            assertTrue("要点名来路不明的事件: " + ex.getMessage(), ex.getMessage().contains("ON_SUNDAY"));
            assertTrue("还要说清现在能登记什么: " + ex.getMessage(),
                    ex.getMessage().contains(WorkflowTriggers.AFTER_CREATE));
        }
        assertEquals(0, bindingRows.size());
    }

    @Test
    public void createShouldRefuseMissingIdentityColumns() {
        WorkflowBindingEntity noApp = validBinding();
        noApp.setAppCode("  ");
        expectBadRequest(newCreate(noApp), "appCode");
        WorkflowBindingEntity noEntity = validBinding();
        noEntity.setEntityCode(null);
        expectBadRequest(newCreate(noEntity), "entityCode");
        WorkflowBindingEntity noKey = validBinding();
        noKey.setProcessDefinitionKey("");
        expectBadRequest(newCreate(noKey), "processDefinitionKey");
        assertEquals("被拒的三条一条都不该落库: ", 0, bindingRows.size());
    }

    private Runnable newCreate(final WorkflowBindingEntity e) {
        return new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        };
    }

    @Test
    public void refuseAutoSubmitOffBecauseNothingWouldHappen() {
        expectBadRequest(newCreate(autoSubmitOff(validBinding())), "自动提单");
        assertEquals(0, bindingRows.size());
    }

    @Test
    public void createShouldRefuseADuplicateOfTheSameEventAndProcessKey() {
        service.create(validBinding());
        expectBadRequest(newCreate(validBinding()), "已经绑定过这个流程");
        assertEquals("重复登记会留两行 ⇒ 一条记录写成功就并行发起两个流程实例", 1, bindingRows.size());
    }

    @Test
    public void createShouldAllowTheSameProcessOnADifferentEntityOrTenant() {
        service.create(validBinding());
        service.create(binding(TENANT, APP, "task", WorkflowTriggers.AFTER_CREATE, PROCESS));
        service.create(binding("other", APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS));
        assertEquals("查重是 (租户, 应用, 实体, 事件, KEY) 五元组，不是 KEY 全局唯一",
                3, bindingRows.size());
    }

    // ---------------- update：改绑定不能绕过创建时那道闸 ----------------

    @Test
    public void updateShouldRequireAnExistingRow() {
        try {
            service.update(validBinding());
            fail("没有 id 的更新必须拒绝");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("必须带 id"));
        }
        WorkflowBindingEntity missing = validBinding();
        missing.setId(4242L);
        try {
            service.update(missing);
            fail("改一条不存在的绑定要说话，不能报「改好了」");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("不存在"));
        }
        assertEquals(0, bindingRows.size());
    }

    @Test
    public void updateShouldRefuseASoftDeletedRow() {
        seed(softDeleted(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS)));
        try {
            service.update(edit(storedRow(0)));
            fail("已删除的绑定不是「可以顺手复活的空行」");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("已删除"));
        }
        assertEquals(Integer.valueOf(1), storedRow(0).getDeleted());
    }

    @Test
    public void updateShouldNotLetAValidRowBeTurnedIntoAnUnhonorableOne() {
        service.create(validBinding());
        WorkflowBindingEntity doomed = edit(storedRow(0));
        doomed.setTriggerEvent("AFTER_UPDATE");
        try {
            service.update(doomed);
            fail("先建一条合法的、再把它改成合法的以外，就是绕过创建闸的那条路");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("AFTER_UPDATE"));
        }
        assertEquals("被拒的更新不能落到库里: ", WorkflowTriggers.AFTER_CREATE,
                storedRow(0).getTriggerEvent());
    }

    @Test
    public void updateShouldRefuseDuplicates() {
        service.create(validBinding());
        service.create(binding(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE, "second"));
        WorkflowBindingEntity collide = edit(storedRow(1));
        collide.setProcessDefinitionKey(PROCESS);
        expectBadRequest(newUpdate(collide), "已经绑定过这个流程");
        assertEquals("被拒的改动不能已经落到那一行上: ", "second", storedRow(1).getProcessDefinitionKey());
    }

    @Test
    public void updateShouldKeepTheStoredTenantAndCreateStamp() {
        service.create(validBinding());
        Date created = storedRow(0).getCreateTime();
        WorkflowBindingEntity tampered = edit(storedRow(0));
        tampered.setTenantCode("attacker");
        tampered.setCreateTime(new Date(0L));
        service.update(tampered);
        assertEquals("更新不该改写租户（那是创建时定下的）: ", TENANT, storedRow(0).getTenantCode());
        assertEquals("创建时间要被保留: ", created, storedRow(0).getCreateTime());
        assertNotNull(storedRow(0).getUpdateTime());
        assertEquals(Integer.valueOf(0), storedRow(0).getDeleted());
        assertTrue("更新要真的走到落库: " + updated,
                updated.contains("binding:" + storedRow(0).getId()));
    }

    private Runnable newUpdate(final WorkflowBindingEntity e) {
        return new Runnable() {
            @Override
            public void run() {
                service.update(e);
            }
        };
    }

    @Test
    public void updateShouldDefaultAutoSubmitTo1SoTheRowStillFires() {
        service.create(validBinding());
        WorkflowBindingEntity blank = edit(storedRow(0));
        blank.setAutoSubmit(null);
        service.update(blank);
        assertEquals("留空的 autoSubmit 若落成 null/0，运行期那条 listByEvent(…, auto_submit=1) 就再也看不见它: ",
                Integer.valueOf(1), storedRow(0).getAutoSubmit());
    }

    // ---------------- delete：删 0 行要说话 ----------------

    @Test
    public void deleteShouldSoftDeleteAndReportTheAffectedRow() {
        WorkflowBindingEntity stored = service.create(validBinding());
        assertEquals(1, service.delete(stored.getId()));
        assertEquals(Integer.valueOf(1), storedRow(0).getDeleted());
        assertTrue("删除要走 updateById（软删）而不是硬删: " + updated,
                updated.contains("binding:" + stored.getId()));
        assertEquals("软删之后运行期读不到它: ", 0,
                service.listByEvent(TENANT, APP, ENTITY, WorkflowTriggers.AFTER_CREATE).size());
    }

    @Test
    public void deleteShouldReturnZeroForMissingAndAlreadyDeleted() {
        WorkflowBindingEntity stored = service.create(validBinding());
        assertEquals(1, service.delete(stored.getId()));
        assertEquals("同一条删第二次必须是 0，否则界面会把「什么都没删」报成「已删除」", 0,
                service.delete(stored.getId()));
        assertEquals(0, service.delete(9999L));
        try {
            service.delete(null);
            fail("delete(null) 必须拒绝，而不是当成「删掉了」");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("必须带 id"));
        }
    }

    // ---------------- 结局账（z_lc_workflow_fire） ----------------

    @Test
    public void listFiresShouldBeTenantPinnedAndNewestFirst() {
        seedFire(TENANT, ENTITY, 7L, WorkflowFireEntity.STATUS_STARTED);
        seedFire("other", ENTITY, 8L, WorkflowFireEntity.STATUS_STARTED);
        seedFire(TENANT, ENTITY, 9L, WorkflowFireEntity.STATUS_FAILED);
        seedFire(TENANT, "task", 10L, WorkflowFireEntity.STATUS_STARTED);
        seedFire(TENANT, ENTITY, 11L, WorkflowFireEntity.STATUS_STARTED).setDeleted(1);

        List<WorkflowFireEntity> mine = service.listFires(TENANT, APP, ENTITY, null);
        assertEquals("跨租户/跨实体/已删除的结局行不能进这个列表: " + statusesOf(mine),
                Arrays.asList(9L, 7L), recordIdsOf(mine));
        assertEquals("最新的结局在前: ", WorkflowFireEntity.STATUS_FAILED, mine.get(0).getStatus());

        List<WorkflowFireEntity> oneRecord = service.listFires(TENANT, APP, ENTITY, 7L);
        assertEquals("按记录号过滤不能把别的记录带进来: " + statusesOf(oneRecord),
                Collections.singletonList(7L), recordIdsOf(oneRecord));
    }

    private static List<String> statusesOf(List<WorkflowFireEntity> rows) {
        List<String> out = new ArrayList<>();
        for (WorkflowFireEntity r : rows) {
            out.add(r.getStatus() + "@" + r.getTenantCode() + "#" + r.getRecordId());
        }
        return out;
    }

    private static List<Long> recordIdsOf(List<WorkflowFireEntity> rows) {
        List<Long> out = new ArrayList<>();
        for (WorkflowFireEntity r : rows) {
            out.add(r.getRecordId());
        }
        return out;
    }

    @Test
    public void listFiresShouldCapAtTheLimitTheQueryCarries() {
        for (long i = 1; i <= 250; i++) {
            seedFire(TENANT, ENTITY, i, WorkflowFireEntity.STATUS_STARTED);
        }
        assertEquals("读侧必须带上 LIMIT 200（替身按 wrapper 里的 LIMIT 截断）—— 谁把 LIMIT 摘掉这条就红，"
                + "没有上限的结局读会把整张表拉进内存", 200,
                service.listFires(TENANT, APP, ENTITY, null).size());
    }

    // ---------------- 类形状 ----------------

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("WorkflowBindingService 应当标注 @Service",
                WorkflowBindingService.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.workflow",
                WorkflowBindingService.class.getPackage().getName());
    }

    // ---------------- 替身 ----------------

    @SuppressWarnings("unchecked")
    private abstract static class AbstractFakeMapper implements InvocationHandler {
        protected final List<Object> rows;
        private final AtomicLong ids;

        AbstractFakeMapper(List<?> rows, AtomicLong ids) {
            this.rows = (List<Object>) rows;
            this.ids = ids;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("insert".equals(name)) {
                Object row = args[0];
                if (idOf(row) == null) {
                    setId(row, ids.incrementAndGet());
                } else if (idOf(row) >= ids.get()) {
                    ids.set(idOf(row));
                }
                rows.add(row);
                return 1;
            }
            if ("updateById".equals(name)) {
                Object row = args[0];
                for (int i = 0; i < rows.size(); i++) {
                    Object hit = rows.get(i);
                    if (hit == row || (idOf(row) != null && idOf(row).equals(idOf(hit)))) {
                        rows.set(i, row);
                        return 1;
                    }
                }
                return 0;
            }
            if ("selectById".equals(name)) {
                for (Object row : rows) {
                    if (idOf(row) != null && idOf(row).equals(args[0])) {
                        return copy(row);
                    }
                }
                return null;
            }
            if ("selectList".equals(name)) {
                List<Object> kept = new ArrayList<>();
                for (Object row : rows) {
                    if (row != null && matches(row, args[0])) {
                        kept.add(row);
                    }
                }
                sort(kept, args[0]);
                Integer limit = limitOf(args[0]);
                if (limit != null && kept.size() > limit) {
                    return new ArrayList<Object>(kept.subList(0, limit));
                }
                return kept;
            }
            if ("selectCount".equals(name)) {
                return Long.valueOf(0L);
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
            if (Map.class.isAssignableFrom(rt)) {
                return new java.util.concurrent.ConcurrentHashMap<>();
            }
            return null;
        }

        protected abstract boolean matches(Object row, Object wrapper);

        protected abstract Long idOf(Object row);

        protected abstract void setId(Object row, Long id);

        /** 读出来的是新对象（真库如此），否则"改写租户"这类断言会因为改的正是库存那一行而常绿。 */
        protected abstract Object copy(Object row);
    }

    private static final class BindingHandler extends AbstractFakeMapper {
        private final List<String> updated;

        BindingHandler(List<WorkflowBindingEntity> rows, AtomicLong ids, List<String> updated) {
            super(rows, ids);
            this.updated = updated;
        }

        @Override
        protected boolean matches(Object row, Object wrapper) {
            WorkflowBindingEntity e = (WorkflowBindingEntity) row;
            return eq(wrapper, "tenant_code", e.getTenantCode())
                    && eq(wrapper, "app_code", e.getAppCode())
                    && eq(wrapper, "entity_code", e.getEntityCode())
                    && eq(wrapper, "trigger_event", e.getTriggerEvent())
                    && eq(wrapper, "auto_submit", e.getAutoSubmit())
                    && eq(wrapper, "deleted", e.getDeleted());
        }

        @Override
        protected Long idOf(Object row) {
            return ((WorkflowBindingEntity) row).getId();
        }

        @Override
        protected void setId(Object row, Long id) {
            ((WorkflowBindingEntity) row).setId(id);
        }

        @Override
        protected Object copy(Object row) {
            WorkflowBindingEntity s = (WorkflowBindingEntity) row;
            WorkflowBindingEntity c = new WorkflowBindingEntity();
            c.setId(s.getId());
            c.setTenantCode(s.getTenantCode());
            c.setAppCode(s.getAppCode());
            c.setEntityCode(s.getEntityCode());
            c.setTriggerEvent(s.getTriggerEvent());
            c.setProcessDefinitionKey(s.getProcessDefinitionKey());
            c.setAutoSubmit(s.getAutoSubmit());
            c.setCreateTime(s.getCreateTime());
            c.setUpdateTime(s.getUpdateTime());
            c.setDeleted(s.getDeleted());
            return c;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("updateById".equals(method.getName()) && args != null && args.length == 1) {
                updated.add("binding:" + idOf(args[0]));
            }
            return super.invoke(proxy, method, args);
        }
    }

    private static final class FireHandler extends AbstractFakeMapper {
        FireHandler(List<WorkflowFireEntity> rows, AtomicLong ids) {
            super(rows, ids);
        }

        @Override
        protected boolean matches(Object row, Object wrapper) {
            WorkflowFireEntity f = (WorkflowFireEntity) row;
            return eq(wrapper, "tenant_code", f.getTenantCode())
                    && eq(wrapper, "app_code", f.getAppCode())
                    && eq(wrapper, "entity_code", f.getEntityCode())
                    && eq(wrapper, "record_id", f.getRecordId())
                    && eq(wrapper, "deleted", f.getDeleted());
        }

        @Override
        protected Long idOf(Object row) {
            return ((WorkflowFireEntity) row).getId();
        }

        @Override
        protected void setId(Object row, Long id) {
            ((WorkflowFireEntity) row).setId(id);
        }

        @Override
        protected Object copy(Object row) {
            WorkflowFireEntity s = (WorkflowFireEntity) row;
            WorkflowFireEntity c = new WorkflowFireEntity();
            c.setId(s.getId());
            c.setTenantCode(s.getTenantCode());
            c.setAppCode(s.getAppCode());
            c.setEntityCode(s.getEntityCode());
            c.setRecordId(s.getRecordId());
            c.setBindingId(s.getBindingId());
            c.setTriggerEvent(s.getTriggerEvent());
            c.setProcessDefinitionKey(s.getProcessDefinitionKey());
            c.setStatus(s.getStatus());
            c.setInstanceId(s.getInstanceId());
            c.setDetail(s.getDetail());
            c.setCreateTime(s.getCreateTime());
            c.setUpdateTime(s.getUpdateTime());
            c.setDeleted(s.getDeleted());
            return c;
        }
    }

    /** wrapper 里 {@code column = #{ew.paramNameValuePairs.X}} 绑的那个值；谓词不存在返回 null。 */
    private static Object predicateValue(Object wrapper, String column) {
        if (!(wrapper instanceof AbstractWrapper)) {
            return null;
        }
        AbstractWrapper<?, ?, ?> aw = (AbstractWrapper<?, ?, ?>) wrapper;
        Matcher m = Pattern.compile(column + "\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}")
                .matcher(String.valueOf(aw.getSqlSegment()));
        if (!m.find()) {
            return null;
        }
        return aw.getParamNameValuePairs().get(m.group(1));
    }

    /** 这一列上没有谓词就等于不筛这一列。 */
    private static boolean eq(Object wrapper, String column, Object actual) {
        Object wanted = predicateValue(wrapper, column);
        return wanted == null || String.valueOf(wanted).equals(String.valueOf(actual));
    }

    private static void sort(List<Object> rows, Object wrapper) {
        if (!(wrapper instanceof AbstractWrapper)) {
            return;
        }
        String sql = String.valueOf(((AbstractWrapper<?, ?, ?>) wrapper).getSqlSegment());
        Matcher m = Pattern.compile("ORDER BY\\s+(\\w+)(\\s+(ASC|DESC))?", Pattern.CASE_INSENSITIVE)
                .matcher(sql);
        if (!m.find()) {
            return;
        }
        final boolean desc = m.group(3) != null && "DESC".equalsIgnoreCase(m.group(3));
        Collections.sort(rows, new java.util.Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                long la = idOfAny(a);
                long lb = idOfAny(b);
                return desc ? Long.compare(lb, la) : Long.compare(la, lb);
            }
        });
    }

    private static long idOfAny(Object row) {
        Long id = row instanceof WorkflowBindingEntity
                ? ((WorkflowBindingEntity) row).getId()
                : ((WorkflowFireEntity) row).getId();
        if (id == null) {
            throw new AssertionError("替身里的行没有主键 ⇒ 排序相关的断言都不能信: " + row);
        }
        return id;
    }

    /** {@code last("LIMIT 200")} 会落进 sqlSegment；替身按它截断，好让"摘掉 LIMIT"这件事被测得出来。 */
    private static Integer limitOf(Object wrapper) {
        if (!(wrapper instanceof AbstractWrapper)) {
            return null;
        }
        Matcher m = Pattern.compile("LIMIT\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
                .matcher(String.valueOf(((AbstractWrapper<?, ?, ?>) wrapper).getSqlSegment()));
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
}
