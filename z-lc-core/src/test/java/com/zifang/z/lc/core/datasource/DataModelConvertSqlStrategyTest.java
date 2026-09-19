package com.zifang.z.lc.core.datasource;

import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;
import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelConvertSqlStrategy 单元测试
 *
 * 测试接口定义本身的元数据 (方法签名、返回类型等).
 */
public class DataModelConvertSqlStrategyTest {

    @Test
    public void shouldBeInterface() {
        assertTrue("DataModelConvertSqlStrategy 应当是 interface",
                DataModelConvertSqlStrategy.class.isInterface());
    }

    @Test
    public void shouldBePublic() {
        assertTrue("DataModelConvertSqlStrategy 应当是 public",
                java.lang.reflect.Modifier.isPublic(DataModelConvertSqlStrategy.class.getModifiers()));
    }

    @Test
    public void shouldHaveFiveDeclaredMethods() throws Exception {
        Method[] methods = DataModelConvertSqlStrategy.class.getDeclaredMethods();
        // 接口默认方法不算, 这里只算抽象方法
        int abstractCount = 0;
        for (Method m : methods) {
            if (java.lang.reflect.Modifier.isAbstract(m.getModifiers())) {
                abstractCount++;
            }
        }
        assertEquals("接口应当声明 5 个方法", 5, abstractCount);
    }

    @Test
    public void tryConnectShouldReturnBoolean() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("tryConnect", DatasourceSaveDTO.class);
        assertEquals(Boolean.class, m.getReturnType());
    }

    @Test
    public void fetchDataSourceShouldReturnDataSource() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("fetchDataSource", DataSourceDO.class);
        assertEquals(DataSource.class, m.getReturnType());
    }

    @Test
    public void fetchTableInfoShouldReturnList() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("fetchTableInfo", DataSource.class, String.class);
        assertEquals(List.class, m.getReturnType());
        assertEquals(DataSourceTableDTO.class, ((java.lang.reflect.ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0]);
    }

    @Test
    public void fetchTableColumnInfoShouldReturnList() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("fetchTableColumnInfo",
                DataSource.class, String.class, String.class);
        assertEquals(List.class, m.getReturnType());
        assertEquals(DataSourceTableColumnDTO.class,
                ((java.lang.reflect.ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0]);
    }

    @Test
    public void fetchTableInfoWithTableNameShouldReturnSingle() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("fetchTableInfo",
                DataSource.class, String.class, String.class);
        assertEquals(DataSourceTableDTO.class, m.getReturnType());
    }

    @Test
    public void allMethodsShouldBeAbstract() throws Exception {
        Method[] methods = DataModelConvertSqlStrategy.class.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isSynthetic() || m.isBridge()) {
                continue;
            }
            if (m.isDefault()) {
                continue;
            }
            assertTrue("方法应当是抽象: " + m.getName(),
                    java.lang.reflect.Modifier.isAbstract(m.getModifiers()));
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.datasource",
                DataModelConvertSqlStrategy.class.getPackage().getName());
    }

    @Test
    public void shouldHaveNoSuperInterfaces() {
        Class<?>[] superIfaces = DataModelConvertSqlStrategy.class.getInterfaces();
        assertEquals("顶层接口应当无父接口", 0, superIfaces.length);
    }

    @Test
    public void tryConnectShouldDeclareException() throws Exception {
        Method m = DataModelConvertSqlStrategy.class.getMethod("tryConnect", DatasourceSaveDTO.class);
        // 接口方法不能直接抛 checked exception, 这里仅校验返回类型
        assertEquals(Boolean.class, m.getReturnType());
    }

    @Test
    public void methodsShouldBePublic() throws Exception {
        Method[] methods = DataModelConvertSqlStrategy.class.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isSynthetic()) continue;
            assertTrue("接口方法必须是 public: " + m.getName(),
                    java.lang.reflect.Modifier.isPublic(m.getModifiers()));
        }
    }

    @Test
    public void anonymousImplCanSatisfyInterface() {
        DataModelConvertSqlStrategy impl = new DataModelConvertSqlStrategy() {
            @Override
            public Boolean tryConnect(DatasourceSaveDTO dto) {
                return true;
            }

            @Override
            public DataSource fetchDataSource(DataSourceDO dataSourceDO) {
                return null;
            }

            @Override
            public List<DataSourceTableDTO> fetchTableInfo(DataSource dataSource, String schemaMark) {
                return null;
            }

            @Override
            public List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName) {
                return null;
            }

            @Override
            public DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName) {
                return null;
            }
        };
        assertNotNull(impl);
        assertTrue(impl instanceof DataModelConvertSqlStrategy);
    }

    @Test
    public void anonymousImplMethodsShouldBeCallable() {
        DataModelConvertSqlStrategy impl = new DataModelConvertSqlStrategy() {
            @Override
            public Boolean tryConnect(DatasourceSaveDTO dto) {
                return true;
            }

            @Override
            public DataSource fetchDataSource(DataSourceDO dataSourceDO) {
                return null;
            }

            @Override
            public List<DataSourceTableDTO> fetchTableInfo(DataSource dataSource, String schemaMark) {
                return null;
            }

            @Override
            public List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName) {
                return null;
            }

            @Override
            public DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName) {
                return null;
            }
        };
        DatasourceSaveDTO dto = new DatasourceSaveDTO();
        Boolean result = impl.tryConnect(dto);
        assertNotNull(result);
        assertTrue(result);
    }
}