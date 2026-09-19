package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * RuntimeCrudExecutor 单元测试
 * <p>
 * 覆盖参数校验 fail-fast 路径与 rawQuery 的 SELECT-only 安全约束.
 * SQL 真实执行路径 (JdbcTemplate) 由集成测试覆盖.
 */
public class RuntimeCrudExecutorTest {

    private RuntimeCrudExecutor executor;

    @Before
    public void setUp() {
        DataSource ds = (DataSource) java.lang.reflect.Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        Class<?> rt = method.getReturnType();
                        if (rt == int.class) {
                            return 0;
                        }
                        if (rt == boolean.class) {
                            return false;
                        }
                        return null;
                    }
                });
        executor = new RuntimeCrudExecutor(ds);
    }

    private EntityDefDTO entity() {
        EntityDefDTO e = new EntityDefDTO();
        e.setEntityCode("order");
        e.setTableName("lc_crm_order");
        return e;
    }

    // ===== fail-fast 参数校验 =====

    @Test(expected = IllegalArgumentException.class)
    public void listShouldRejectNullEntity() {
        executor.list(null, new com.zifang.z.lc.common.dto.RuntimeQueryDTO());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getShouldRejectNullId() {
        executor.get(entity(), null, "t1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateShouldRejectNullId() {
        executor.update(entity(), null, new RuntimeCrudDTO(), "user1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteShouldRejectNullId() {
        executor.delete(entity(), null, "t1");
    }

    // ===== rawQuery SELECT-only 安全约束 =====

    @Test
    public void rawQueryShouldRejectNonSelect() {
        assertRawQueryRejected("DROP TABLE users");
        assertRawQueryRejected("DELETE FROM lc_crm_order");
        assertRawQueryRejected("UPDATE lc_crm_order SET a=1");
        assertRawQueryRejected("INSERT INTO lc_crm_order VALUES (1)");
    }

    @Test
    public void rawQueryShouldRejectNullSql() {
        try {
            executor.rawQuery(null, null);
            fail("null SQL 应被拒绝");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void rawQueryShouldAcceptLeadingWhitespaceSelect() {
        try {
            // " select" 开头 (trim 后合法), 走到 JdbcTemplate 才失败 (mock 返回 null 会 NPE/异常)
            // 这里仅验证校验通过 (异常不是 IllegalArgumentException)
            executor.rawQuery("  select 1", Collections.emptyList());
            fail("mock 数据源下执行应抛非 IAE 异常");
        } catch (IllegalArgumentException e) {
            fail("SELECT 语句不应被校验层拒绝");
        } catch (Exception expected) {
            // JdbcTemplate 层异常, 校验已通过
        }
    }

    private void assertRawQueryRejected(String sql) {
        try {
            executor.rawQuery(sql, null);
            fail("非 SELECT 语句应被拒绝: " + sql);
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    // ===== 结构契约 =====

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("RuntimeCrudExecutor 应当标注 @Component",
                RuntimeCrudExecutor.class.getAnnotation(Component.class));
    }

    @Test
    public void longOfShouldHandleNullAndValue() throws Exception {
        Method m = RuntimeCrudExecutor.class.getDeclaredMethod("longOf", Integer.class, int.class);
        m.setAccessible(true);
        assertEquals(20L, m.invoke(null, (Integer) null, 20));
        assertEquals(5L, m.invoke(null, 5, 20));
    }

    @Test
    public void shouldInstantiateWithMockedDataSource() {
        assertTrue(executor != null);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.executor",
                RuntimeCrudExecutor.class.getPackage().getName());
    }
}