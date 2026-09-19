package com.zifang.z.lc.core.mapper;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DbTableMapperService 单元测试
 * <p>
 * 覆盖 JDBC 类型映射矩阵与命名转换 (去前缀 / 驼峰化).
 * DataSource 通过 JDK 动态代理 mock, 不触碰真实数据库.
 */
public class DbTableMapperServiceTest {

    private DbTableMapperService service;

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
        service = new DbTableMapperService(ds);
    }

    // ===== toLcType 类型映射矩阵 =====

    @Test
    public void toLcTypeShouldMapBooleanTypes() {
        assertEquals("BOOLEAN", DbTableMapperService.toLcType(Types.BIT, 1, 0));
        assertEquals("BOOLEAN", DbTableMapperService.toLcType(Types.BOOLEAN, 1, 0));
    }

    @Test
    public void toLcTypeShouldMapIntegerTypes() {
        assertEquals("INTEGER", DbTableMapperService.toLcType(Types.TINYINT, 3, 0));
        assertEquals("INTEGER", DbTableMapperService.toLcType(Types.SMALLINT, 5, 0));
        assertEquals("INTEGER", DbTableMapperService.toLcType(Types.INTEGER, 10, 0));
    }

    @Test
    public void toLcTypeShouldMapLongType() {
        assertEquals("LONG", DbTableMapperService.toLcType(Types.BIGINT, 19, 0));
    }

    @Test
    public void toLcTypeShouldMapFloatingTypes() {
        assertEquals("FLOAT", DbTableMapperService.toLcType(Types.FLOAT, 10, 4));
        assertEquals("FLOAT", DbTableMapperService.toLcType(Types.REAL, 10, 4));
        assertEquals("DOUBLE", DbTableMapperService.toLcType(Types.DOUBLE, 15, 6));
        assertEquals("DECIMAL", DbTableMapperService.toLcType(Types.DECIMAL, 10, 2));
        assertEquals("DECIMAL", DbTableMapperService.toLcType(Types.NUMERIC, 10, 2));
    }

    @Test
    public void toLcTypeShouldMapCharTypes() {
        assertEquals("BOOLEAN", DbTableMapperService.toLcType(Types.VARCHAR, 1, 0));
        assertEquals("BOOLEAN", DbTableMapperService.toLcType(Types.CHAR, 1, 0));
        assertEquals("VARCHAR", DbTableMapperService.toLcType(Types.VARCHAR, 45, 0));
        assertEquals("VARCHAR", DbTableMapperService.toLcType(Types.CHAR, 32, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.VARCHAR, 255, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.VARCHAR, 4000, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.LONGVARCHAR, 100, 0));
    }

    @Test
    public void toLcTypeShouldMapDateTimeTypes() {
        assertEquals("DATE", DbTableMapperService.toLcType(Types.DATE, 10, 0));
        assertEquals("TIME", DbTableMapperService.toLcType(Types.TIME, 8, 0));
        assertEquals("DATETIME", DbTableMapperService.toLcType(Types.TIMESTAMP, 26, 6));
    }

    @Test
    public void toLcTypeShouldMapBinaryToFile() {
        assertEquals("FILE", DbTableMapperService.toLcType(Types.BINARY, 16, 0));
        assertEquals("FILE", DbTableMapperService.toLcType(Types.VARBINARY, 255, 0));
        assertEquals("FILE", DbTableMapperService.toLcType(Types.BLOB, 65535, 0));
    }

    @Test
    public void toLcTypeShouldMapLobAndNTypesToText() {
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.CLOB, 65535, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.NCLOB, 65535, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.NVARCHAR, 100, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.NCHAR, 50, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.LONGNVARCHAR, 100, 0));
    }

    @Test
    public void toLcTypeShouldFallbackToVarcharForUnknown() {
        assertEquals("VARCHAR", DbTableMapperService.toLcType(Types.OTHER, 0, 0));
        assertEquals("VARCHAR", DbTableMapperService.toLcType(Types.ARRAY, 0, 0));
    }

    // ===== 命名转换 (私有静态方法, 反射测试) =====

    private String invokeStatic(String methodName, Class<?>[] paramTypes, Object[] args)
            throws Exception {
        Method m = DbTableMapperService.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return (String) m.invoke(null, args);
    }

    @Test
    public void removePrefixShouldStripKnownPrefixes() throws Exception {
        Class<?>[] types = {String.class, String[].class};
        assertEquals("customer", invokeStatic("removePrefix", types,
                new Object[]{"lc_customer", new String[]{"lc_", "oc_", "z_", "t_"}}));
        assertEquals("customer", invokeStatic("removePrefix", types,
                new Object[]{"oc_customer", new String[]{"lc_", "oc_", "z_", "t_"}}));
        assertEquals("customer", invokeStatic("removePrefix", types,
                new Object[]{"t_customer", new String[]{"lc_", "oc_", "z_", "t_"}}));
    }

    @Test
    public void removePrefixShouldKeepUnprefixedName() throws Exception {
        Class<?>[] types = {String.class, String[].class};
        assertEquals("customer", invokeStatic("removePrefix", types,
                new Object[]{"customer", new String[]{"lc_", "oc_"}}));
    }

    @Test
    public void toCamelCaseShouldConvertUnderscores() throws Exception {
        Class<?>[] types = {String.class};
        assertEquals("orderItem", invokeStatic("toCamelCase", types, new Object[]{"order_item"}));
        assertEquals("orderItem", invokeStatic("toCamelCase", types, new Object[]{"order-item"}));
        assertEquals("a", invokeStatic("toCamelCase", types, new Object[]{"a"}));
    }

    @Test
    public void toCamelCaseShouldHandleNullAndEmpty() throws Exception {
        Class<?>[] types = {String.class};
        assertEquals(null, invokeStatic("toCamelCase", types, new Object[]{null}));
        assertEquals("", invokeStatic("toCamelCase", types, new Object[]{""}));
    }

    // ===== 结构契约 =====

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("DbTableMapperService 应当标注 @Service",
                DbTableMapperService.class.getAnnotation(Service.class));
    }

    @Test
    public void toLcTypeShouldBeStableForRepeatedCalls() {
        assertEquals(
                DbTableMapperService.toLcType(Types.INTEGER, 10, 0),
                DbTableMapperService.toLcType(Types.INTEGER, 10, 0));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.mapper",
                DbTableMapperService.class.getPackage().getName());
    }

    @Test
    public void varcharBoundaryShouldSeparateVarcharAndText() {
        // size < 50 → VARCHAR, 50 <= size < 2000 → TEXT
        assertEquals("VARCHAR", DbTableMapperService.toLcType(Types.VARCHAR, 49, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.VARCHAR, 50, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.VARCHAR, 1999, 0));
        assertEquals("TEXT", DbTableMapperService.toLcType(Types.VARCHAR, 2000, 0));
    }

    @Test
    public void serviceShouldInstantiateWithMockedDataSource() {
        // setUp 已用代理 DataSource 完成构造, 此处确认实例可用
        assertTrue(service != null);
    }
}