package com.zifang.z.lc.core.datasource;

import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;
import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelConvertStrategyRegistrar 单元测试
 *
 * @author zifang
 */
public class DataModelConvertStrategyRegistrarTest {

    @Test
    public void shouldHavePrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<DataModelConvertStrategyRegistrar> ctor =
                DataModelConvertStrategyRegistrar.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldDefineZLcDialectAnnotation() {
        java.lang.annotation.Annotation[] annotations = DataModelConvertStrategyRegistrar.ZLcDialect.class.getAnnotations();
        assertNotNull(annotations);
        assertTrue(DataModelConvertStrategyRegistrar.ZLcDialect.class.isAnnotation());
    }

    @Test
    public void shouldRegisterNullSafeWithNullDispatch() {
        // Should not throw with null dispatch
        DataModelConvertStrategyRegistrar.registerAll(null, "com.example");
    }

    @Test
    public void shouldRegisterNullSafeWithNullPackages() {
        DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();
        DataModelConvertStrategyRegistrar.registerAll(dispatch, (String[]) null);
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldRegisterNullSafeWithEmptyPackages() {
        DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();
        DataModelConvertStrategyRegistrar.registerAll(dispatch);
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldRegisterNullSafeWithEmptyStringPackages() {
        DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();
        DataModelConvertStrategyRegistrar.registerAll(dispatch, new String[0]);
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldHandleNonExistentPackage() {
        DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();
        DataModelConvertStrategyRegistrar.registerAll(dispatch, "com.example.nonexistent");
        assertEquals(0, dispatch.registeredDialects().size());
    }

    @Test
    public void shouldRegisterFromExistingPackage() {
        DataModelConvertSqlDispatch dispatch = new DataModelConvertSqlDispatch();
        // com.zifang.z.lc.core.datasource is the package where the registrar lives;
        // there are no @ZLcDialect-annotated classes here yet, but the call should be safe
        DataModelConvertStrategyRegistrar.registerAll(dispatch,
                "com.zifang.z.lc.core.datasource");
        // assert no exception
    }

    @Test
    public void zLcDialectAnnotationShouldHaveValueMethod() throws NoSuchMethodException {
        java.lang.reflect.Method valueMethod = DataModelConvertStrategyRegistrar.ZLcDialect.class.getMethod("value");
        assertEquals(String.class, valueMethod.getReturnType());
    }

    @Test
    public void zLcDialectAnnotationShouldBeRuntimeRetention() {
        java.lang.annotation.Retention retention = DataModelConvertStrategyRegistrar.ZLcDialect.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void zLcDialectAnnotationShouldTargetType() {
        java.lang.annotation.Target target = DataModelConvertStrategyRegistrar.ZLcDialect.class.getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target);
        java.lang.annotation.ElementType[] types = target.value();
        boolean hasType = false;
        for (java.lang.annotation.ElementType t : types) {
            if (t == java.lang.annotation.ElementType.TYPE) {
                hasType = true;
                break;
            }
        }
        assertTrue(hasType);
    }

    @Test
    public void canCreateAnnotationInstance() {
        DataModelConvertStrategyRegistrar.ZLcDialect anno =
                new DataModelConvertStrategyRegistrar.ZLcDialect() {
                    @Override
                    public String value() {
                        return "mock";
                    }

                    @Override
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return DataModelConvertStrategyRegistrar.ZLcDialect.class;
                    }
                };
        assertEquals("mock", anno.value());
    }

    @Test
    public void canApplyAnnotationOnCustomStrategy() throws Exception {
        // Verify the annotation class is usable as an annotation type
        assertNotNull(DataModelConvertStrategyRegistrar.ZLcDialect.class);
        // Verify it can be used as an annotation parameter type
        Class<? extends java.lang.annotation.Annotation> annoType =
                DataModelConvertStrategyRegistrar.ZLcDialect.class;
        assertTrue(annoType.isAnnotation());
    }

    private static class MockStrategyClass {
        // placeholder
    }

    // Stub Strategy for compilation
    private static class StubStrategy implements DataModelConvertSqlStrategy {
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
            return Collections.emptyList();
        }

        @Override
        public List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName) {
            return Collections.emptyList();
        }

        @Override
        public DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName) {
            return null;
        }
    }
}