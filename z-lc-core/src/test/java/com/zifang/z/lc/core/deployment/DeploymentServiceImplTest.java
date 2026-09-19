package com.zifang.z.lc.core.deployment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.core.deployment.entity.DeploymentEntity;
import com.zifang.z.lc.mapper.deployment.DeploymentMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DeploymentServiceImpl 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 DeploymentMapper, 无需 Mockito / 数据库.
 */
public class DeploymentServiceImplTest {

    private DeploymentServiceImpl service;
    private Map<Long, DeploymentEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new DeploymentServiceImpl();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    DeploymentEntity e = (DeploymentEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    DeploymentEntity e = (DeploymentEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), e);
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
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

        Field f = DeploymentServiceImpl.class.getDeclaredField("deploymentMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

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

    @Test
    public void createDeploymentShouldSetPendingStatus() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode("crm");
        req.setDeployType("SQL");
        req.setVersion("v1");

        DeploymentDTO dto = service.createDeployment(req);

        assertNotNull(dto);
        assertEquals("crm", dto.getAppCode());
        assertEquals("SQL", dto.getDeployType());
        assertEquals(DeploymentEntity.STATUS_PENDING, dto.getStatus());
        assertEquals("default", dto.getTenantCode());
        assertNotNull(dto.getCreateTime());
        assertEquals(1, store.size());
    }

    @Test
    public void createDeploymentShouldKeepExplicitTenant() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setTenantCode("t1");

        DeploymentDTO dto = service.createDeployment(req);

        assertEquals("t1", dto.getTenantCode());
    }

    @Test
    public void updateDeploymentStatusShouldReturnNullForMissing() {
        assertNull(service.updateDeploymentStatus(999L, "RUNNING", null));
    }

    @Test
    public void updateDeploymentStatusShouldMergeFields() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        DeploymentDTO created = service.createDeployment(req);

        DeploymentDTO updated = service.updateDeploymentStatus(
                created.getId(), DeploymentEntity.STATUS_SUCCESS, "executed 5 ddl");

        assertNotNull(updated);
        assertEquals(DeploymentEntity.STATUS_SUCCESS, updated.getStatus());
        assertEquals("executed 5 ddl", updated.getDeployLog());
    }

    @Test
    public void updateDeploymentStatusShouldKeepLogWhenNull() {
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.getAppCode();
        DeploymentDTO created = service.createDeployment(req);
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
        DeploymentCreateReq req = new DeploymentCreateReq();
        req.setAppCode("app-a");
        DeploymentDTO created = service.createDeployment(req);

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