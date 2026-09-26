package com.zifang.z.lc.core.permission;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * PermissionService 单元测试 —— 只量"这一层自己拼出来的条件"，行为契约在真 HTTP + 真库的
 * {@code LcHttpContractTest} 那一层（缺陷 #48 的六条都在那里被逐条证过牙）。
 * <p>
 * 这一支原来是用 JDK 动态代理 mock {@code PermissionMapper}，把 {@code revoke} 的
 * {@code deleteById} 当成期望钉下来（"revokeShouldDelegateToDeleteById"），于是"谁的 id 都删得掉"
 * 这件事在测试层是绿的。签名改了（三个列表查询与判定都带租户、回收要回报受影响行数），
 * 那条期望连同它保护的错误行为一起删掉。
 */
public class PermissionServiceTest {

    private PermissionService service;

    /** 最近一次传给 mapper 的 wrapper，用来量"条件里真出现了哪些列"。 */
    private final AtomicReference<Wrapper<?>> lastSelect = new AtomicReference<>();
    private final AtomicReference<Wrapper<?>> lastDelete = new AtomicReference<>();
    private final AtomicReference<PermissionEntity> lastInserted = new AtomicReference<>();
    private final AtomicReference<PermissionEntity> selectOneResult = new AtomicReference<>();
    private final AtomicInteger selectCountResult = new AtomicInteger(0);

    @Before
    public void setUp() throws Exception {
        service = new PermissionService();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectOne".equals(name) || "selectCount".equals(name) || "selectList".equals(name)) {
                    lastSelect.set(firstWrapper(args));
                }
                if ("selectOne".equals(name)) {
                    return selectOneResult.get();
                }
                if ("selectCount".equals(name)) {
                    return (long) selectCountResult.get();
                }
                if ("selectList".equals(name)) {
                    return new ArrayList<PermissionEntity>();
                }
                if ("insert".equals(name)) {
                    lastInserted.set((PermissionEntity) args[0]);
                    return 1;
                }
                if ("delete".equals(name)) {
                    lastDelete.set(firstWrapper(args));
                    return 1;
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                if (rt == long.class || rt == Long.class) {
                    return 0L;
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

    private static Wrapper<?> firstWrapper(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Wrapper) {
                return (Wrapper<?>) arg;
            }
        }
        return null;
    }

    private static String sql(Wrapper<?> wrapper) {
        assertNotNull("mapper 没收到查询条件", wrapper);
        String segment = wrapper.getSqlSegment();
        assertNotNull("条件是空的 —— 那等于不加任何过滤", segment);
        return segment.toUpperCase(Locale.ROOT);
    }

    private static PermissionEntity grantee(String app, String role, String permission, String entity) {
        PermissionEntity e = new PermissionEntity();
        e.setAppCode(app);
        e.setRoleCode(role);
        e.setPermission(permission);
        e.setEntityCode(entity);
        return e;
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("PermissionService 应当标注 @Service",
                PermissionService.class.getAnnotation(Service.class));
    }

    /* ---------------- 词表：唯一一份，写入口当场拒 ---------------- */

    @Test
    public void registryIsTheSingleSourceAndCoversTheMatrixColumns() {
        List<String> all = PermissionKeys.all();
        assertEquals("词表就是矩阵的列顺序，一项不能少", "[VIEW, CREATE, UPDATE, DELETE, EXPORT]", all.toString());
        for (String key : all) {
            assertTrue(key + " 必须认得自己", PermissionKeys.isKnown(key));
            assertEquals(key, PermissionKeys.canonical("  " + key.toLowerCase(Locale.ROOT) + " "));
        }
    }

    @Test
    public void grantRejectsUnknownPermissionAndNamesWhatIsAllowed() {
        try {
            service.grant(grantee("app1", "editor", "WIBBLE", null));
            fail("不在词表里的权限项必须被写入口拒掉");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("未知的权限项"));
            // 光说"不合法"不够 —— 用户得知道能填什么（这一族文案的既有口径）
            assertTrue(ex.getMessage(), ex.getMessage().contains("VIEW"));
            assertTrue(ex.getMessage(), ex.getMessage().contains("EXPORT"));
        }
        assertNull("被拒的授权不该已经落库", lastInserted.get());
    }

    @Test
    public void grantStoresTheCanonicalFormSoTheCheckCanMatchIt() {
        service.grant(grantee("app1", "editor", "view", null));
        assertNotNull("授权应当落库", lastInserted.get());
        assertEquals("库里存的必须是规范形态（存 'view' 而 check 问 'VIEW' 会永远答拒绝）",
                "VIEW", lastInserted.get().getPermission());
        // 租户由入口钉死；service 只在没给的时候兜 default
        assertEquals("default", lastInserted.get().getTenantCode());
    }

    /* ---------------- 作用范围：空值是一种语义，不是"没写" ---------------- */

    @Test
    public void appWideGrantDedupeUsesIsNullNotEqualsNull() {
        service.grant(grantee("app1", "editor", "VIEW", "   "));
        String where = sql(lastSelect.get());
        // 旧写法是 eq("entity_code", null) → SQL 里的 `entity_code = NULL` 恒为 unknown，
        // 于是"整个应用"那一支每点一次多一行，而矩阵页的注释写着服务端幂等。
        assertTrue("应用级查重必须用 IS NULL: " + where, where.contains("ENTITY_CODE IS NULL"));
        assertFalse("不许再拿 = ? 去比一个空范围: " + where, where.contains("ENTITY_CODE ="));
        assertNull("空串要归一成 null，否则两种写法在库里是两行", lastInserted.get().getEntityCode());
    }

    @Test
    public void entityScopedGrantComparesTheEntityColumn() {
        service.grant(grantee("app1", "editor", "VIEW", "orders"));
        String where = sql(lastSelect.get());
        assertTrue(where.contains("ENTITY_CODE"));
        assertFalse("实体级不能退化成 IS NULL: " + where, where.contains("ENTITY_CODE IS NULL"));
    }

    @Test
    public void grantReturnsTheExistingRowWithoutInsertingAgain() {
        PermissionEntity existing = grantee("app1", "editor", "VIEW", null);
        existing.setId(7L);
        selectOneResult.set(existing);
        PermissionEntity result = service.grant(grantee("app1", "editor", "VIEW", null));
        assertSame("命中查重时必须回原来那一行，而不是又造一行", existing, result);
        assertNull("重复授权不许再插一行", lastInserted.get());
    }

    /* ---------------- 租户：三个列表查询与判定都参与 ---------------- */

    @Test
    public void everyReadPathFiltersOnTenant() {
        // 四条读路径各钉一句: 少了消息就分不清是哪一条丢了租户条件（四条的错法互不相同）
        service.listByApp("t1", "app1");
        assertTrue("listByApp 少了租户条件: ", sql(lastSelect.get()).contains("TENANT_CODE"));
        service.listByEntity("t1", "app1", "orders");
        assertTrue("listByEntity 少了租户条件: ", sql(lastSelect.get()).contains("TENANT_CODE"));
        service.listByRole("t1", "app1", "editor");
        assertTrue("listByRole 少了租户条件: ", sql(lastSelect.get()).contains("TENANT_CODE"));
        selectCountResult.set(0);
        service.hasPermission("t1", "app1", "orders", "editor", "VIEW");
        assertTrue("hasPermission 少了租户条件: ", sql(lastSelect.get()).contains("TENANT_CODE"));
    }

    @Test
    public void blankTenantOrKeyIsRejectedRatherThanSilentlyDroppingTheFilter() {
        try {
            service.listByApp("  ", "app1");
            fail("空租户不能被当成\"不过滤\"");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("tenantCode"));
        }
        try {
            service.hasPermission("t1", "app1", null, "  ", "VIEW");
            fail("空角色不能被当成\"任意角色\"");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("roleCode"));
        }
    }

    /* ---------------- 判定：作用范围两种语义各一支 ---------------- */

    @Test
    public void checkForAConcreteEntityAcceptsTheAppWideRowToo() {
        selectCountResult.set(1);
        assertTrue(service.hasPermission("t1", "app1", "orders", "clerk", "VIEW"));
        String where = sql(lastSelect.get());
        assertTrue("具体实体必须同时认应用级那一行: " + where, where.contains("IS NULL"));
        assertTrue(where.contains("ENTITY_CODE"));
    }

    @Test
    public void checkWithoutAnEntityAsksOnlyTheAppWideRow() {
        selectCountResult.set(1);
        assertTrue(service.hasPermission("t1", "app1", null, "clerk", "VIEW"));
        String where = sql(lastSelect.get());
        assertTrue("整个应用那一档必须只比 ENTITY_CODE IS NULL: ", where.contains("ENTITY_CODE IS NULL"));
        assertFalse("某个实体的授权不能冒充\"整个应用都可以\": ", where.contains(" OR "));
    }

    @Test
    public void checkRejectsAnUnknownPermissionInsteadOfAnsweringFalse() {
        // "role 有没有 WIBBLE 权限" 是个问不出口的问题：答 false 会让调用方以为"没授过"，
        // 而真相是那一行根本不可能存在。
        try {
            service.hasPermission("t1", "app1", "orders", "clerk", "WIBBLE");
            fail("未知权限项应当抛，而不是回一句拒绝");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("未知的权限项"));
        }
    }

    @Test
    public void revokeReportsHowManyRowsItTouchedAndScopedItsDelete() {
        assertEquals(1, service.revoke("t1", 42L));
        String where = sql(lastDelete.get());
        assertTrue("回收必须按租户圈定: " + where, where.contains("TENANT_CODE"));
        assertTrue(where.contains("ID"));
        try {
            service.revoke("t1", null);
            fail("没有 id 的回收问的是\"删哪一条\"，不能整个放过");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("id"));
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.permission",
                PermissionService.class.getPackage().getName());
        assertFalse(PermissionKeys.class.isInterface());
    }

    /** QueryWrapper 的 toString 也用来调试；这里确认测试拿到的确实是新构造的那一个。 */
    @Test
    public void capturedWrapperIsAQueryWrapper() {
        service.listByApp("t1", "app1");
        assertTrue(lastSelect.get() instanceof QueryWrapper);
    }
}
