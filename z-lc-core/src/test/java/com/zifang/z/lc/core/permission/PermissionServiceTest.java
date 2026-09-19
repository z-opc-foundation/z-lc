package com.zifang.z.lc.core.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import com.zifang.z.lc.mapper.permission.PermissionMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * PermissionService 单元测试
 * <p>
 * 通过 JDK 动态代理 mock PermissionMapper (BaseMapper 接口), 无需 Mockito.
 */
public class PermissionServiceTest {

    private PermissionService service;

    /** 记录最后一次 selectOne 查询条件的引用 */
    private AtomicReference<Object> lastQueryWrapper = new AtomicReference<>();
    private AtomicReference<PermissionEntity> lastInserted = new AtomicReference<>();
    private AtomicReference<Long> lastDeletedId = new AtomicReference<>();
    private AtomicInteger selectCountResult = new AtomicInteger(0);
    /** selectOne 命中时返回的实体 (模拟已存在) */
    private AtomicReference<PermissionEntity> selectOneResult = new AtomicReference<>(null);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new PermissionService();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectOne".equals(name)) {
                    lastQueryWrapper.set(args != null && args.length > 0 ? args[0] : null);
                    return selectOneResult.get();
                }
                if ("insert".equals(name)) {
                    lastInserted.set((PermissionEntity) args[0]);
                    return 1;
                }
                if ("deleteById".equals(name)) {
                    lastDeletedId.set((Long) args[0]);
                    return 1;
                }
                if ("selectCount".equals(name)) {
                    return (long) selectCountResult.get();
                }
                if ("selectList".equals(name)) {
                    return new ArrayList<>();
                }
                // BaseMapper 其余默认方法返回安全值
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                return null;
            }
        };

        PermissionMapper mapper = (PermissionMapper) Proxy.newProxyInstance(
                PermissionMapper.class.getClassLoader(),
                new Class<?>[]{PermissionMapper.class, BaseMapper.class},
                handler);

        Field f = PermissionService.class.getDeclaredField("permissionMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("PermissionService 应当标注 @Service",
                PermissionService.class.getAnnotation(Service.class));
    }

    @Test
    public void grantShouldDefaultTenantCode() {
        PermissionEntity entity = new PermissionEntity();
        entity.setAppCode("app1");
        entity.setEntityCode("order");
        entity.setRoleCode("admin");
        entity.setPermission("read");

        PermissionEntity result = service.grant(entity);

        assertSame(entity, result);
        assertEquals("default", result.getTenantCode());
        assertNotNull(result.getCreateTime());
        assertNotNull("insert 应被调用", lastInserted.get());
    }

    @Test
    public void grantShouldReturnExistingWhenDuplicated() {
        PermissionEntity existing = new PermissionEntity();
        existing.setAppCode("app1");
        existing.setEntityCode("order");
        existing.setRoleCode("admin");
        existing.setPermission("read");
        existing.setTenantCode("t1");
        selectOneResult.set(existing);

        PermissionEntity result = service.grant(existing);

        assertSame("重复授权应返回已存在记录", existing, result);
        assertEquals("重复授权不应触发 insert", null, lastInserted.get());
    }

    @Test
    public void revokeShouldDelegateToDeleteById() {
        int n = service.revoke(42L);
        assertEquals(1, n);
        assertEquals(Long.valueOf(42L), lastDeletedId.get());
    }

    @Test
    public void hasPermissionShouldReturnTrueWhenCountPositive() {
        selectCountResult.set(1);
        assertTrue(service.hasPermission("app1", "order", "admin", "read"));
    }

    @Test
    public void hasPermissionShouldReturnFalseWhenCountZero() {
        selectCountResult.set(0);
        assertFalse(service.hasPermission("app1", "order", "admin", "read"));
    }

    @Test
    public void listByAppShouldReturnEmptyListInitially() {
        List<PermissionEntity> list = service.listByApp("app1");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void listByEntityShouldReturnEmptyListInitially() {
        List<PermissionEntity> list = service.listByEntity("app1", "order");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void listByRoleShouldReturnEmptyListInitially() {
        List<PermissionEntity> list = service.listByRole("app1", "admin");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.permission",
                PermissionService.class.getPackage().getName());
    }
}