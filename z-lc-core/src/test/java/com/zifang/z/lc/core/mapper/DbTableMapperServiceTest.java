package com.zifang.z.lc.core.mapper;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.core.datasource.LcDataSourceCatalogItem;
import com.zifang.z.lc.core.datasource.LcDataSourceResolver;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        service = new DbTableMapperService(ds, emptyProvider());
    }

    /** 目录缺席的 provider（z-dms 未装配形态）。 */
    private static ObjectProvider<LcDataSourceResolver> emptyProvider() {
        return new ObjectProvider<LcDataSourceResolver>() {
            @Override
            public LcDataSourceResolver getObject() {
                throw new UnsupportedOperationException("no resolver");
            }

            @Override
            public LcDataSourceResolver getObject(Object... args) {
                throw new UnsupportedOperationException("no resolver");
            }

            @Override
            public LcDataSourceResolver getIfAvailable() {
                return null;
            }

            @Override
            public LcDataSourceResolver getIfUnique() {
                return null;
            }
        };
    }

    private static ObjectProvider<LcDataSourceResolver> providerOf(final LcDataSourceResolver resolver) {
        return new ObjectProvider<LcDataSourceResolver>() {
            @Override
            public LcDataSourceResolver getObject() {
                return resolver;
            }

            @Override
            public LcDataSourceResolver getObject(Object... args) {
                return resolver;
            }

            @Override
            public LcDataSourceResolver getIfAvailable() {
                return resolver;
            }

            @Override
            public LcDataSourceResolver getIfUnique() {
                return resolver;
            }
        };
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

    // ===== V1 数据源管理域: 按目录 id 多源化 (2026-10-26, lead 009/013 §六) =====

    /** 与 dev 种子同构的 H2 实例, 两张外部表。 */
    private static JdbcDataSource h2WithTables() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:dsmapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        try (Connection c = h2.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE ds_customer (id INTEGER PRIMARY KEY, name VARCHAR(50), "
                    + "created_at TIMESTAMP, remark TEXT)");
            st.execute("CREATE TABLE other_table (id INTEGER)");
        }
        return h2;
    }

    @Test
    public void scanTablesShouldUseResolverForNamedSource() throws Exception {
        JdbcDataSource h2 = h2WithTables();
        LcDataSourceResolver stub = new LcDataSourceResolver() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public List<LcDataSourceCatalogItem> list() {
                return Collections.singletonList(new LcDataSourceCatalogItem("ext-1", "外部 H2", "JDBC", null));
            }

            @Override
            public DataSource resolve(String id) {
                if (!"ext-1".equals(id)) {
                    throw new IllegalArgumentException("数据源目录里不存在 id: " + id);
                }
                return h2;
            }
        };
        DbTableMapperService svc = new DbTableMapperService(proxyDataSource(), providerOf(stub));

        List<EntityDefDTO> tables = svc.scanTables("ext-1", "ds_", null);
        assertEquals(1, tables.size());
        EntityDefDTO customer = tables.get(0);
        assertEquals("ds_customer", customer.getTableName());
        assertEquals("dsCustomer", customer.getEntityCode());
        assertNotNull(customer.getFields());
        // name / created_at / remark 三列（id 是引擎自建列，映射时按 SYSTEM_COLUMN_CODES 跳过）
        assertEquals(3, customer.getFields().size());
        assertEquals("name", customer.getFields().get(0).getFieldCode());
        assertEquals("TEXT", customer.getFields().get(0).getFieldType()); // VARCHAR(50) 恰在 TEXT 边界
        assertEquals("created_at", customer.getFields().get(1).getFieldCode());
        assertEquals("DATETIME", customer.getFields().get(1).getFieldType());

        // 单表路径
        EntityDefDTO one = svc.mapTable("ext-1", "ds_customer", null);
        assertEquals("dsCustomer", one.getEntityCode());
        assertEquals(3, one.getFields().size());

        // 目录里不存在的 id → 400 语义 (IllegalArgumentException 上抛)
        try {
            svc.scanTables("nope", "", null);
            fail("unknown id 应当抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void namedSourceShouldFailFastWhenCatalogNotInstalled() {
        DbTableMapperService svc = new DbTableMapperService(proxyDataSource(), emptyProvider());
        try {
            svc.scanTables("ext-1", "", null);
            fail("目录未装配应当抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void nullSourceIdShouldKeepPrimaryBehavior() {
        // 代理 DataSource 的 getConnection() 返回 null → 主库路径异常被吞、返回空列表 (历史行为不变)
        assertTrue(service.scanTables("anything", null).isEmpty());
        List<EntityDefDTO> viaNullId = service.scanTables(null, "x", null);
        assertTrue(viaNullId.isEmpty());
    }

    private static DataSource proxyDataSource() {
        return (DataSource) java.lang.reflect.Proxy.newProxyInstance(
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
    }
}