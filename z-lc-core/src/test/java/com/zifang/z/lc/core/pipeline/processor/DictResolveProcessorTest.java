package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.core.adapter.MetaAdapter;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DictResolveProcessor 单元测试
 */
public class DictResolveProcessorTest {

    private DictResolveProcessor processor;
    private MetaAdapter metaAdapter;

    @Before
    public void setUp() throws Exception {
        processor = new DictResolveProcessor();
        metaAdapter = new MetaAdapter() {
            @Override
            public List<DictItemDTO> listDictItems(String tenantCode, String dictCode) {
                return new ArrayList<>();
            }
        };
        Field f = DictResolveProcessor.class.getDeclaredField("metaAdapter");
        f.setAccessible(true);
        f.set(processor, metaAdapter);
    }

    @Test
    public void shouldReturnProcessorName() {
        assertEquals("DictResolve", processor.name());
    }

    @Test
    public void shouldImplementFieldProcessor() {
        assertNotNull(processor);
        assertTrue(processor instanceof com.zifang.z.lc.core.pipeline.FieldProcessor);
    }

    @Test
    public void preWriteShouldBeNoOp() {
        processor.preWrite(null, null);
        processor.preWrite(new EntityDefDTO(), new com.zifang.z.lc.common.dto.RuntimeCrudDTO());
    }

    @Test
    public void postReadShouldBeSafeWithNullEntity() {
        processor.postRead(null, new HashMap<>());
    }

    @Test
    public void postReadShouldBeSafeWithNullFields() {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setFields(null);
        processor.postRead(entity, new HashMap<>());
    }

    @Test
    public void postReadShouldBeSafeWithNullRow() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.postRead(entity, null);
    }

    @Test
    public void postReadShouldResolveDictValueToLabel() {
        setUpMetaAdapterWithItems("STATUS_DICT", item("ACTIVE", "1", "启用"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS_DICT"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertEquals("启用", row.get("status_label"));
    }

    @Test
    public void postReadShouldSkipFieldsWithoutDictCode() {
        EntityDefDTO entity = newEntity("t1", field("name", null));
        Map<String, Object> row = new HashMap<>();
        row.put("name", "Alice");

        processor.postRead(entity, row);
        assertFalse("未指定 dictCode 的字段不应产生 label", row.containsKey("name_label"));
    }

    @Test
    public void postReadShouldSkipFieldsWithEmptyDictCode() {
        EntityDefDTO entity = newEntity("t1", field("status", ""));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void postReadShouldSkipNullValues() {
        setUpMetaAdapterWithItems("STATUS", item("A", "1", "启用"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", null);

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void postReadShouldHandleMissingDictItems() {
        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void postReadShouldHandleNullDictItems() {
        metaAdapter = new MetaAdapter() {
            @Override
            public List<DictItemDTO> listDictItems(String tenantCode, String dictCode) {
                return null;
            }
        };
        try {
            Field f = DictResolveProcessor.class.getDeclaredField("metaAdapter");
            f.setAccessible(true);
            f.set(processor, metaAdapter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void postReadShouldMatchByCode() {
        setUpMetaAdapterWithItems("STATUS", item("ACTIVE", "wrong_value", "启用"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "ACTIVE");

        processor.postRead(entity, row);
        assertEquals("启用", row.get("status_label"));
    }

    @Test
    public void postReadShouldNotMatchAnyItem() {
        setUpMetaAdapterWithItems("STATUS", item("OTHER", "99", "其他"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void postReadListShouldBeSafeWithNullEntity() {
        processor.postReadList(null, Collections.<Map<String, Object>>emptyList());
    }

    @Test
    public void postReadListShouldBeSafeWithNullFields() {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setFields(null);
        processor.postReadList(entity, Collections.<Map<String, Object>>emptyList());
    }

    @Test
    public void postReadListShouldBeSafeWithNullRows() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.postReadList(entity, null);
    }

    @Test
    public void postReadListShouldResolveDictForAllRows() {
        setUpMetaAdapterWithItems("STATUS", item("A", "1", "启用"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));

        Map<String, Object> row1 = new HashMap<>();
        row1.put("status", "1");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("status", "2");

        processor.postReadList(entity, Arrays.asList(row1, row2));
        assertEquals("启用", row1.get("status_label"));
        assertFalse("未匹配的行不应有 label", row2.containsKey("status_label"));
    }

    @Test
    public void postReadListShouldBatchDictLookups() {
        // 重设支持多 dict 的 stub
        metaAdapter = new MetaAdapter() {
            @Override
            public List<DictItemDTO> listDictItems(String tenantCode, String dictCode) {
                if ("STATUS".equals(dictCode)) {
                    DictItemDTO i = new DictItemDTO();
                    i.setItemCode("A");
                    i.setItemValue("1");
                    i.setItemLabel("启用");
                    return Collections.singletonList(i);
                } else if ("CATEGORY".equals(dictCode)) {
                    DictItemDTO i = new DictItemDTO();
                    i.setItemCode("X");
                    i.setItemValue("X");
                    i.setItemLabel("X 类");
                    return Collections.singletonList(i);
                }
                return Collections.emptyList();
            }
        };
        try {
            Field f = DictResolveProcessor.class.getDeclaredField("metaAdapter");
            f.setAccessible(true);
            f.set(processor, metaAdapter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EntityDefDTO entity = new EntityDefDTO();
        entity.setTenantCode("t1");
        FieldDefDTO f1 = new FieldDefDTO();
        f1.setFieldCode("status");
        f1.setDictCode("STATUS");
        FieldDefDTO f2 = new FieldDefDTO();
        f2.setFieldCode("category");
        f2.setDictCode("CATEGORY");
        entity.setFields(Arrays.asList(f1, f2));

        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");
        row.put("category", "X");

        processor.postReadList(entity, Collections.singletonList(row));
        assertEquals("启用", row.get("status_label"));
        assertEquals("X 类", row.get("category_label"));
    }

    @Test
    public void postReadListShouldHandleEmptyRows() {
        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        processor.postReadList(entity, Collections.<Map<String, Object>>emptyList());
    }

    @Test
    public void postReadListShouldHandleRowWithNullValue() {
        setUpMetaAdapterWithItems("STATUS", item("A", "1", "启用"));

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", null);

        processor.postReadList(entity, Collections.singletonList(row));
        assertFalse(row.containsKey("status_label"));
    }

    @Test
    public void shouldHandleItemWithBothValueAndCodeNull() {
        DictItemDTO it = new DictItemDTO();
        it.setItemValue(null);
        it.setItemCode(null);
        it.setItemLabel("空项");
        setUpMetaAdapterWithItems("STATUS", it);

        EntityDefDTO entity = newEntity("t1", field("status", "STATUS"));
        Map<String, Object> row = new HashMap<>();
        row.put("status", "1");

        processor.postRead(entity, row);
        assertFalse(row.containsKey("status_label"));
    }

    private EntityDefDTO newEntity(String tenantCode, FieldDefDTO field) {
        EntityDefDTO e = new EntityDefDTO();
        e.setTenantCode(tenantCode);
        e.setFields(Collections.singletonList(field));
        return e;
    }

    private FieldDefDTO field(String code, String dictCode) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setDictCode(dictCode);
        return f;
    }

    private DictItemDTO item(String code, String value, String label) {
        DictItemDTO i = new DictItemDTO();
        i.setItemCode(code);
        i.setItemValue(value);
        i.setItemLabel(label);
        return i;
    }

    private void setUpMetaAdapterWithItems(final String dictCode, final DictItemDTO item) {
        metaAdapter = new MetaAdapter() {
            @Override
            public List<DictItemDTO> listDictItems(String tenantCode, String dc) {
                if (dictCode.equals(dc)) {
                    return Collections.singletonList(item);
                }
                return Collections.emptyList();
            }
        };
        try {
            Field f = DictResolveProcessor.class.getDeclaredField("metaAdapter");
            f.setAccessible(true);
            f.set(processor, metaAdapter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}