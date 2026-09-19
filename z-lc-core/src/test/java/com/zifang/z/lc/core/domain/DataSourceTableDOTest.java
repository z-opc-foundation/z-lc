package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DataSourceTableDO 单元测试
 *
 * @author zifang
 */
public class DataSourceTableDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DataSourceTableDO table = new DataSourceTableDO();
        assertNotNull(table);
    }

    @Test
    public void shouldExtendBaseDTO() {
        DataSourceTableDO table = new DataSourceTableDO();
        assertTrue(table instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        DataSourceTableDO table = new DataSourceTableDO();
        assertTrue(table instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setTenantCode("tenant-001");
        assertEquals("tenant-001", table.getTenantCode());
    }

    @Test
    public void shouldSetAndGetDatasourceCode() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setDatasourceCode("ds-001");
        assertEquals("ds-001", table.getDatasourceCode());
    }

    @Test
    public void shouldSetAndGetTableName() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setTableName("z_lc_user");
        assertEquals("z_lc_user", table.getTableName());
    }

    @Test
    public void shouldSetAndGetDescriptions() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setDescriptions("用户表");
        assertEquals("用户表", table.getDescriptions());
    }

    @Test
    public void shouldSetAndGetTableType() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setTableType("biz");
        assertEquals("biz", table.getTableType());
    }

    @Test
    public void shouldSupportDifferentTableTypes() {
        DataSourceTableDO bizTable = new DataSourceTableDO();
        bizTable.setTableType("biz");

        DataSourceTableDO dictTable = new DataSourceTableDO();
        dictTable.setTableType("dict");

        DataSourceTableDO sysTable = new DataSourceTableDO();
        sysTable.setTableType("system");

        assertEquals("biz", bizTable.getTableType());
        assertEquals("dict", dictTable.getTableType());
        assertEquals("system", sysTable.getTableType());
    }

    @Test
    public void shouldInheritBaseDTOBehavior() {
        DataSourceTableDO table = new DataSourceTableDO();
        table.setId(100L);

        assertEquals(Long.valueOf(100L), table.getId());
    }

    @Test
    public void shouldHandleNullValues() {
        DataSourceTableDO table = new DataSourceTableDO();
        assertNull(table.getTenantCode());
        assertNull(table.getDatasourceCode());
        assertNull(table.getTableName());
    }
}