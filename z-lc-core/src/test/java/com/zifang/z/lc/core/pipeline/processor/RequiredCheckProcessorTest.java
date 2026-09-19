package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * RequiredCheckProcessor 单元测试
 *
 * @author zifang
 */
public class RequiredCheckProcessorTest {

    private final RequiredCheckProcessor processor = new RequiredCheckProcessor();

    @Test
    public void shouldReturnProcessorName() {
        assertEquals("RequiredCheck", processor.name());
    }

    @Test
    public void shouldImplementFieldProcessor() {
        assertNotNull(processor);
        assertTrue(processor instanceof com.zifang.z.lc.core.pipeline.FieldProcessor);
    }

    @Test
    public void shouldBeSafeWithNullEntity() {
        processor.preWrite(null, new RuntimeCrudDTO());
        // should not throw
    }

    @Test
    public void shouldBeSafeWithEntityWithoutFields() {
        EntityDefDTO entity = new EntityDefDTO();
        processor.preWrite(entity, new RuntimeCrudDTO());
        // should not throw
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
    public void shouldThrowWhenFieldValuesIsNull() {
        EntityDefDTO entity = new EntityDefDTO();
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(null);
        FieldDefDTO field = new FieldDefDTO();
        field.setRequired(true);
        field.setFieldCode("name");
        entity.setFields(Collections.singletonList(field));
        // Null fieldValues becomes emptyMap; missing required value → exception
        try {
            processor.preWrite(entity, body);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("name"));
        }
    }

    @Test
    public void shouldSkipNonRequiredFields() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("nickname");
        field.setRequired(false);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("nickname", null);
        body.setFieldValues(values);

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldSkipFieldsWithNullRequired() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("desc");
        field.setRequired(null);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("desc", null));

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldThrowForMissingRequiredField() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("name");
        field.setFieldName("Name");
        field.setRequired(true);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.emptyMap());

        try {
            processor.preWrite(entity, body);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Name"));
            assertTrue(ex.getMessage().contains("name"));
        }
    }

    @Test
    public void shouldThrowForEmptyRequiredString() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("name");
        field.setFieldName("Name");
        field.setRequired(true);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "");
        body.setFieldValues(values);

        try {
            processor.preWrite(entity, body);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Name"));
        }
    }

    @Test
    public void shouldThrowForEmptyRequiredCollection() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("tags");
        field.setFieldName("Tags");
        field.setRequired(true);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("tags", Collections.emptyList()));

        try {
            processor.preWrite(entity, body);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Tags"));
        }
    }

    @Test
    public void shouldAcceptNonEmptyRequiredField() {
        EntityDefDTO entity = new EntityDefDTO();
        FieldDefDTO field = new FieldDefDTO();
        field.setFieldCode("name");
        field.setRequired(true);
        entity.setFields(Collections.singletonList(field));

        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(Collections.singletonMap("name", "Alice"));

        processor.preWrite(entity, body);
    }

    @Test
    public void shouldPostReadBeNoOp() {
        processor.postRead(null, null);
        processor.postRead(new EntityDefDTO(), new HashMap<>());
    }
}