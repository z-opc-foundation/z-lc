package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * FieldProcessor 单元测试 — 验证接口的 default 实现.
 *
 * @author zifang
 */
public class FieldProcessorTest {

    @Test
    public void shouldImplementInterface() {
        FieldProcessor processor = new SimpleProcessor("test");
        assertNotNull(processor);
    }

    @Test
    public void defaultPostReadListShouldIterate() {
        FieldProcessor processor = new SimpleProcessor("test");
        Map<String, Object> row1 = new HashMap<>();
        row1.put("k", "v1");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("k", "v2");

        processor.postReadList(null, java.util.Arrays.asList(row1, row2));
        assertEquals("v1", row1.get("k"));
        assertEquals("v2", row2.get("k"));
    }

    @Test
    public void defaultPostReadListShouldHandleNullRows() {
        FieldProcessor processor = new SimpleProcessor("test");
        processor.postReadList(null, null);
        // should not throw
    }

    @Test
    public void defaultPostReadListShouldHandleEmptyList() {
        FieldProcessor processor = new SimpleProcessor("test");
        processor.postReadList(null, java.util.Collections.emptyList());
        // should not throw
    }

    private static class SimpleProcessor implements FieldProcessor {
        private final String name;

        SimpleProcessor(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
            // no-op
        }

        @Override
        public void postRead(EntityDefDTO entity, Map<String, Object> row) {
            // default does nothing, but we set k to v for testing
            if (row != null && !row.containsKey("k")) {
                row.put("k", "v");
            }
        }
    }
}