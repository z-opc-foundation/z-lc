package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * RefCheckProcessor 单元测试
 *
 * @author zifang
 */
public class RefCheckProcessorTest {

    private final RefCheckProcessor processor = new RefCheckProcessor();

    @Test
    public void shouldReturnProcessorName() {
        assertEquals("RefCheck", processor.name());
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
    public void shouldBeSafeWithEntityWithoutFields() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.preWrite(entity, new RuntimeCrudDTO());
    }

    @Test
    public void shouldBeSafeWithNullFields() {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setFields(null);
        processor.preWrite(entity, new RuntimeCrudDTO());
    }

    @Test
    public void shouldBeSafeWithNullBody() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.preWrite(entity, null);
    }

    @Test
    public void shouldBeSafeWithNullFieldValues() {
        EntityDefDTO entity = new EntityDefDTO();
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(null);
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("userId");
        field.setRefEntity("user");
        entity.setFields(Collections.singletonList(field));
        processor.preWrite(entity, body);
    }

    @Test
    public void shouldSkipFieldsWithoutRefEntity() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("name");
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("name", "Alice"));

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldSkipFieldsWithEmptyRefEntity() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("userId");
        field.setRefEntity("");
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("userId", 1);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldSkipNullRefFieldValue() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("userId");
        field.setRefEntity("user");
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("userId", null);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldHandleMultipleRefFields() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO userField = new FieldDefDTO();
        userField.setFieldCode("userId");
        userField.setRefEntity("user");
        FieldDefDTO deptField = new FieldDefDTO();
        deptField.setFieldCode("deptId");
        deptField.setRefEntity("dept");
        entity.setFields(java.util.Arrays.asList(userField, deptField));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("userId", 1L);
        values.put("deptId", 2L);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldPostReadBeNoOp() {
        processor.postRead(null, null);
        processor.postRead(new EntityDefDTO(), new HashMap<>());
    }
}