package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.executor.DynamicSqlBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TypeConvertProcessor 单元测试
 */
public class TypeConvertProcessorTest {

    private TypeConvertProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new TypeConvertProcessor();
        // 注入真实 DynamicSqlBuilder (它的 coerce 是 stateless 的)
        java.lang.reflect.Field sqlBuilderField = TypeConvertProcessor.class.getDeclaredField("sqlBuilder");
        sqlBuilderField.setAccessible(true);
        sqlBuilderField.set(processor, new DynamicSqlBuilder());
    }

    @Test
    public void shouldReturnProcessorName() {
        assertEquals("TypeConvert", processor.name());
    }

    @Test
    public void shouldImplementFieldProcessor() {
        assertNotNull(processor);
        assertTrue(processor instanceof com.zifang.z.lc.core.pipeline.FieldProcessor);
    }

    @Test
    public void shouldBeSafeWithNullEntity() {
        processor.preWrite(null, new RuntimeCrudDTO());
    }

    @Test
    public void shouldBeSafeWithNullBody() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.preWrite(entity, null);
    }

    @Test
    public void shouldBeSafeWithNullFields() {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setFields(null);
        processor.preWrite(entity, new RuntimeCrudDTO());
    }

    @Test
    public void shouldBeSafeWithNullFieldValues() {
        EntityDefDTO entity = new EntityDefDTO();
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(null);
        processor.preWrite(entity, body);
    }

    @Test
    public void shouldSkipNullFieldValues() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("age");
        f.setFieldType("INT");
        entity.setFields(Collections.singletonList(f));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("age", null);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
        // 没有异常抛出, body.getFieldValues().get("age") 应保持 null
        assertNull(body.getFieldValues().get("age"));
    }

    @Test
    public void shouldSkipFieldsWithNullCode() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(null);
        f.setFieldType("INT");
        entity.setFields(Collections.singletonList(f));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("unknown", 42);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
        // 不应修改 body 中的任何值
        assertEquals(42, body.getFieldValues().get("unknown"));
    }

    @Test
    public void shouldConvertStringToLongForIntType() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("age");
        f.setFieldName("年龄");
        f.setFieldType("INT");
        entity.setFields(Collections.singletonList(f));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("age", "42");
        body.setFieldValues(values);

        processor.preWrite(entity, body);
        assertEquals(Long.valueOf(42L), body.getFieldValues().get("age"));
    }

    @Test
    public void shouldPassThroughNullTypeConvert() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("name");
        f.setFieldType("STRING");
        entity.setFields(Collections.singletonList(f));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("name", "alice");
        body.setFieldValues(values);

        processor.preWrite(entity, body);
        assertEquals("alice", body.getFieldValues().get("name"));
    }

    @Test
    public void shouldThrowIllegalArgumentForUnconvertibleString() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode("age");
        f.setFieldName("年龄");
        f.setFieldType("INT");
        entity.setFields(Collections.singletonList(f));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("age", "not_a_number");
        body.setFieldValues(values);

        try {
            processor.preWrite(entity, body);
            org.junit.Assert.fail("应当抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("异常消息应包含字段信息",
                    expected.getMessage().contains("年龄"));
        }
    }

    @Test
    public void shouldProcessMultipleFields() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO intField = new FieldDefDTO();
        intField.setFieldCode("age");
        intField.setFieldType("INT");
        FieldDefDTO stringField = new FieldDefDTO();
        stringField.setFieldCode("name");
        stringField.setFieldType("STRING");
        entity.setFields(java.util.Arrays.asList(intField, stringField));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("age", "30");
        values.put("name", "Alice");
        body.setFieldValues(values);

        processor.preWrite(entity, body);
        assertEquals(Long.valueOf(30L), body.getFieldValues().get("age"));
        assertEquals("Alice", body.getFieldValues().get("name"));
    }

    @Test
    public void postReadShouldBeNoOp() {
        processor.postRead(null, null);
        processor.postRead(new EntityDefDTO(), new HashMap<>());
    }

    @Test
    public void postReadListShouldBeNoOp() {
        processor.postReadList(null, null);
        processor.postReadList(new EntityDefDTO(), Collections.<Map<String, Object>>emptyList());
    }
}