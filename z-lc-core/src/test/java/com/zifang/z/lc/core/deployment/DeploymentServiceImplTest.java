package com.zifang.z.lc.core.deployment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.core.app.AppAdminService;
import com.zifang.z.lc.core.deployment.entity.DeploymentEntity;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.core.materialize.mapper.MaterializationMapper;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import com.zifang.z.lc.mapper.deployment.DeploymentMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DeploymentServiceImpl 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 DeploymentMapper / AppAdminService / SchemaAdminService /
 * MaterializationMapper, 无需 Mockito / 数据库.
 * <p>
 * ⚠ 这一层只证明"这个类自己会这么做"。缺陷 #70 的原始形状恰恰是"整套都在，没人调用"
 * （{@code updateDeploymentStatus} 生产代码零调用者），那种病在这一层<b>看不见</b> ——
 * 真进程边界的证据在 {@code z-lc-web} 的 {@code DeploymentContractTest}。
 */
public class DeploymentServiceImplTest {

    private DeploymentServiceImpl service;
    private Map<Long, DeploymentEntity> store;
    private AtomicLong idGen;
    /** insert 那一刻该行的 status 快照：用来钉"先记账、后执行"这个顺序。 */
    private List<String> insertedStatuses;
    private Set<String> liveApps;
    private List<String> provisionCalls;
    private Map<Long, MaterializationEntity> batches;
    /** 下一次 provision 的产物（null = 给一份全 OK 的报告）。 */
    private ProvisionReport nextReport;
    private RuntimeException nextProvisionThrow;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new DeploymentServiceImpl();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);
        insertedStatuses = new ArrayList<>();
        liveApps = new HashSet<>(Arrays.asList("default|crm", "default|app-a", "t1|crm"));
        provisionCalls = new ArrayList<>();
        batches = new HashMap<>();
        nextReport = null;
        nextProvisionThrow = null;

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    DeploymentEntity e = (DeploymentEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    insertedStatuses.add(e.getStatus());
                    store.put(e.getId(), snapshot(e));
                    return 1;
                }
                if ("updateById".equals(name)) {
                    DeploymentEntity e = (DeploymentEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), snapshot(e));
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    DeploymentEntity hit = store.get((Long) args[0]);
                    return hit == null ? null : snapshot(hit);
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
                    return new ConcurrentHashMap<>();
                }
                return null;
            }
        };

        DeploymentMapper mapper = (DeploymentMapper) Proxy.newProxyInstance(
                DeploymentMapper.class.getClassLoader(),
                new Class<?>[]{DeploymentMapper.class, BaseMapper.class},
                handler);

        MaterializationMapper materializations = (MaterializationMapper) Proxy.newProxyInstance(
                MaterializationMapper.class.getClassLoader(),
                new Class<?>[]{MaterializationMapper.class, BaseMapper.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("selectById".equals(method.getName())) {
                            return batches.get((Long) args[0]);
                        }
                        return null;
                    }
                });

        AppAdminService apps = (AppAdminService) Proxy.newProxyInstance(
                AppAdminService.class.getClassLoader(),
                new Class<?>[]{AppAdminService.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getAppByCode".equals(method.getName())) {
                            return liveApps.contains(args[0] + "|" + args[1]) ? new AppDTO() : null;
                        }
                        Class<?> rt = method.getReturnType();
                        if (rt == int.class) {
                            return 0;
                        }
                        return rt == boolean.class ? Boolean.FALSE : null;
                    }
                });

        SchemaAdminService schema = (SchemaAdminService) Proxy.newProxyInstance(
                SchemaAdminService.class.getClassLoader(),
                new Class<?>[]{SchemaAdminService.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("provisionAllTables".equals(method.getName())) {
                            provisionCalls.add(args[0] + "|" + args[1]);
                            if (nextProvisionThrow != null) {
                                throw nextProvisionThrow;
                            }
                            return nextReport == null ? allOkReport() : nextReport;
                        }
                        Class<?> rt = method.getReturnType();
                        if (rt == int.class) {
                            return 0;
                        }
                        return rt == boolean.class ? Boolean.FALSE : null;
                    }
                });

        set("deploymentMapper", mapper);
        set("materializationMapper", materializations);
        set("appAdminService", apps);
        set("schemaAdminService", schema);
    }

    private void set(String field, Object value) throws Exception {
        Field f = DeploymentServiceImpl.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    /**
     * 库里的行不是内存里那个对象：真 mapper 写的是 SQL，读回来的是新实例。
     * 原先这里直接 {@code store.put(id, e)} ⇒ 服务后面对 {@code row} 的每一处 setField 都"当场进了库"，
     * 把 {@code execute} 末尾那句 {@code updateById} 摘掉（注入 J3）后这一层 0 红 ——
     * 替身把"没回写"模仿成了"回写了"。同一支变异在契约层红 2 条，说明缺的只是这一层的地面。
     */
    private static DeploymentEntity snapshot(DeploymentEntity from) {
        DeploymentEntity to = new DeploymentEntity();
        BeanUtils.copyProperties(from, to);
        return to;
    }

    private static ProvisionReport allOkReport() {
        ProvisionReport report = new ProvisionReport();
        report.setTotal(1);
        report.setCreated(1);
        return report;
    }

    private static DeploymentCreateReq req(String appCode, String deployType) {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode(appCode);
        req.setDeployType(deployType);
        return req;
    }

    /* ------------------------------------------------------------------ */

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("DeploymentServiceImpl 应当标注 @Service",
                DeploymentServiceImpl.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementDeploymentService() {
        assertTrue("DeploymentServiceImpl 应当实现 DeploymentService",
                DeploymentService.class.isAssignableFrom(DeploymentServiceImpl.class));
    }

    /**
     * 缺陷 #70 的本体在这一层的样子：先落一行 PENDING 的账，再当场执行，返回的是执行后的真状态。
     * 修前这一条只走一半 —— insert 完就 return PENDING，没有第二句（{@code // TODO: 异步执行}）。
     */
    @Test
    public void createShouldRecordTheRowThenExecuteAndReturnTheRealOutcome() {
        DeploymentCreateReq req = req("crm", DeploymentTypes.HOT_LOAD);
        req.setVersion("v1");

        DeploymentDTO dto = service.createDeployment(req);

        assertNotNull(dto);
        assertEquals("crm", dto.getAppCode());
        assertEquals(DeploymentTypes.HOT_LOAD, dto.getDeployType());
        assertEquals("记账那一步必须先落 PENDING（执行完再改），否则中途崩了会留下一条看不出跑没跑过的账",
                DeploymentEntity.STATUS_PENDING, insertedStatuses.get(0));
        assertEquals("返回的必须是执行后的状态", DeploymentEntity.STATUS_SUCCESS, dto.getStatus());
        assertEquals("default", dto.getTenantCode());
        assertNotNull(dto.getCreateTime());
        assertNotNull(dto.getUpdateTime());
        assertEquals(1, store.size());
        // 返回值对了还不够：界面读的是库里那一行（注入 J3：执行完不回写 ⇒ 这一条必须红）。
        assertEquals("库里那一行必须是执行后的状态（界面的状态列读的是它，不是返回值）",
                DeploymentEntity.STATUS_SUCCESS, store.get(dto.getId()).getStatus());
        assertEquals("执行器必须被真调用一次", Collections.singletonList("default|crm"), provisionCalls);
        assertTrue("deploy_log 要说清动了什么: " + dto.getDeployLog(),
                dto.getDeployLog().contains("新建 1 张"));
    }

    @Test
    public void createShouldKeepExplicitTenantAndRunItAgainstThatTenant() {
        DeploymentCreateReq req = req("crm", DeploymentTypes.HOT_LOAD);
        req.setTenantCode("t1");

        DeploymentDTO dto = service.createDeployment(req);

        assertEquals("t1", dto.getTenantCode());
        assertEquals("建表必须打在同一个租户上，不能存 t1 而 provision default",
                Collections.singletonList("t1|crm"), provisionCalls);
    }

    @Test
    public void createShouldRefuseADeployTypeTheServerCannotExecute() {
        for (String unsupported : DeploymentTypes.unimplementedReasons().keySet()) {
            try {
                service.createDeployment(req("crm", unsupported));
                fail("兑现不了的部署方式不该出生: " + unsupported);
            } catch (IllegalArgumentException ex) {
                assertTrue("拒绝要点名是哪一种: " + ex.getMessage(), ex.getMessage().contains(unsupported));
                assertTrue("同时要说出当前可用的是哪一种: " + ex.getMessage(),
                        ex.getMessage().contains(DeploymentTypes.HOT_LOAD));
            }
        }
        assertEquals("被拒的部署不该留下一行永远 PENDING 的账", 0, store.size());
        assertEquals("被拒的更不该顺手把表建出来", 0, provisionCalls.size());
    }

    @Test
    public void createShouldRefuseAMissingDeployTypeInsteadOfLeakingANullConstraint() {
        try {
            service.createDeployment(req("crm", null));
            fail("deploy_type 是 NOT NULL：不给值必须在门口拒，而不是让数据库回一句裸 500");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("deployType"));
        }
        assertEquals(0, store.size());
    }

    @Test
    public void createShouldRefuseAnAppThatDoesNotExist() {
        try {
            service.createDeployment(req("no_such_app", DeploymentTypes.HOT_LOAD));
            fail("部署一个不存在的应用：修前是 200 + 一行账");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("no_such_app"));
            assertTrue("要说清是「没有这个应用」而不是别的: " + ex.getMessage(),
                    ex.getMessage().contains("没有应用"));
        }
        assertEquals(0, store.size());
        assertEquals("应用不存在时执行器一次都不该被调", 0, provisionCalls.size());
    }

    @Test
    public void createShouldRefuseAMissingAppCode() {
        try {
            service.createDeployment(req("   ", DeploymentTypes.HOT_LOAD));
            fail("没有 appCode 的部署无从执行");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("appCode"));
        }
    }

    @Test
    public void createShouldRefuseAPhantomOrForeignMaterializationBatch() {
        batches.put(7L, batch(7L, "default", "crm"));
        batches.put(8L, batch(8L, "other-tenant", "other-app"));

        try {
            service.createDeployment(batchReq(999999L));
            fail("指向不存在的批次：修前实测原样进账并回显 999999");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("999999"));
        }
        try {
            service.createDeployment(batchReq(8L));
            fail("别的应用/别的租户的批次不能挂到这条部署上");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("other-app"));
        }
        assertEquals(0, store.size());

        // 阳性对照：本应用本租户的真批次 ⇒ 收下且原样读得回，否则上面两条是常数。
        DeploymentDTO ok = service.createDeployment(batchReq(7L));
        assertEquals(Long.valueOf(7L), ok.getMaterializationId());
        assertEquals(1, store.size());
    }

    private static DeploymentCreateReq batchReq(long batchId) {
        DeploymentCreateReq req = req("crm", DeploymentTypes.HOT_LOAD);
        req.setMaterializationId(Long.valueOf(batchId));
        return req;
    }

    private static MaterializationEntity batch(Long id, String tenantCode, String appCode) {
        MaterializationEntity e = new MaterializationEntity();
        e.setId(id);
        e.setTenantCode(tenantCode);
        e.setAppCode(appCode);
        return e;
    }

    /** 有一支没建成 ⇒ 整次部署判 FAILED，且逐支点名（"部署已创建"这句话不能盖住它）。 */
    @Test
    public void createShouldRecordFailureAndNameTheEntityThatDidNotLand() {
        ProvisionReport report = new ProvisionReport();
        report.setTotal(2);
        report.setCreated(1);
        report.getItems().add(new ProvisionReport.Item("order", "t_crm_order",
                ProvisionReport.CREATED, "CREATE TABLE ...", null));
        report.getItems().add(new ProvisionReport.Item("draft", "t_other",
                ProvisionReport.FAILED, "CREATE TABLE ...", "表 t_other 缺引擎自建列"));
        nextReport = report;

        DeploymentDTO dto = service.createDeployment(req("crm", DeploymentTypes.HOT_LOAD));

        assertEquals("有一支没建成就不能把返回值判成 SUCCESS: " + dto.getStatus(),
                DeploymentEntity.STATUS_FAILED, dto.getStatus());
        assertTrue(dto.getDeployLog(), dto.getDeployLog().contains("没建成 1 张"));
        assertTrue("要点名是哪一支: " + dto.getDeployLog(), dto.getDeployLog().contains("draft"));
        assertEquals("账要留在库里，不能只在返回值里红一下",
                DeploymentEntity.STATUS_FAILED, store.get(dto.getId()).getStatus());
    }

    /** 执行器自己抛 ⇒ 也要红着记账，而不是把异常原样抛成一句裸 500 且账上永远 PENDING。 */
    @Test
    public void createShouldRecordFailureWhenTheEngineThrows() {
        nextProvisionThrow = new RuntimeException("connection refused");

        DeploymentDTO dto = service.createDeployment(req("crm", DeploymentTypes.HOT_LOAD));

        assertEquals(DeploymentEntity.STATUS_FAILED, dto.getStatus());
        assertTrue(dto.getDeployLog(), dto.getDeployLog().contains("connection refused"));
        assertEquals(1, store.size());
    }

    @Test
    public void updateDeploymentStatusShouldReturnNullForMissing() {
        assertNull(service.updateDeploymentStatus(999L, "RUNNING", null));
    }

    @Test
    public void updateDeploymentStatusShouldMergeFields() {
        DeploymentDTO created = service.createDeployment(req("crm", DeploymentTypes.HOT_LOAD));

        DeploymentDTO updated = service.updateDeploymentStatus(
                created.getId(), DeploymentEntity.STATUS_SUCCESS, "executed 5 ddl");

        assertNotNull(updated);
        assertEquals(DeploymentEntity.STATUS_SUCCESS, updated.getStatus());
        assertEquals("executed 5 ddl", updated.getDeployLog());
    }

    @Test
    public void updateDeploymentStatusShouldKeepLogWhenNull() {
        DeploymentDTO created = service.createDeployment(req("crm", DeploymentTypes.HOT_LOAD));
        service.updateDeploymentStatus(created.getId(), "RUNNING", "step1");

        DeploymentDTO updated = service.updateDeploymentStatus(created.getId(), "FAILED", null);

        assertEquals(DeploymentEntity.STATUS_FAILED, updated.getStatus());
        assertEquals("log 不应被 null 覆盖", "step1", updated.getDeployLog());
    }

    @Test
    public void getDeploymentShouldReturnNullForMissing() {
        assertNull(service.getDeployment(999L));
    }

    @Test
    public void getDeploymentShouldReturnDto() {
        DeploymentDTO created = service.createDeployment(req("app-a", DeploymentTypes.HOT_LOAD));

        DeploymentDTO found = service.getDeployment(created.getId());

        assertNotNull(found);
        assertEquals("app-a", found.getAppCode());
    }

    @Test
    public void listDeploymentsByAppShouldReturnEmptyList() {
        List<DeploymentDTO> list = service.listDeploymentsByApp("t1", "crm");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void getLatestDeploymentShouldReturnNullForMissing() {
        assertNull(service.getLatestDeployment("t1", "crm"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.deployment",
                DeploymentServiceImpl.class.getPackage().getName());
    }
}
