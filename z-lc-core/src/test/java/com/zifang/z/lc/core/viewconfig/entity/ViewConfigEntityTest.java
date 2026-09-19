package com.zifang.z.lc.core.viewconfig.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ViewConfigEntity 单元测试
 *
 * @author zifang
 */
public class ViewConfigEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        ViewConfigEntity entity = new ViewConfigEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        ViewConfigEntity entity = new ViewConfigEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setEntityCode("user");
        assertEquals("user", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetViewType() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setViewType("LIST");
        assertEquals("LIST", entity.getViewType());
    }

    @Test
    public void shouldSetAndGetConfig() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setConfig("{\"columns\":[\"name\",\"age\"]}");
        assertEquals("{\"columns\":[\"name\",\"age\"]}", entity.getConfig());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        ViewConfigEntity entity = new ViewConfigEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetDeleted() {
        ViewConfigEntity entity = new ViewConfigEntity();
        entity.setDeleted(0);
        assertEquals(Integer.valueOf(0), entity.getDeleted());
    }

    @Test
    public void shouldHandleNullValues() {
        ViewConfigEntity entity = new ViewConfigEntity();
        assertNull(entity.getId());
        assertNull(entity.getEntityCode());
        assertNull(entity.getConfig());
        assertNull(entity.getCreateTime());
    }
}