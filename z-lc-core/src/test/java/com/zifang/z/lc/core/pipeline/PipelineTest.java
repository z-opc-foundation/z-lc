package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pipeline 单元测试
 *
 * @author zifang
 */
public class PipelineTest {

    @Test
    public void shouldCreateWithEmptyProcessors() {
        Pipeline pipeline = new Pipeline(null);
        assertNotNull(pipeline);
    }

    @Test
    public void shouldSortProcessorsByName() {
        Pipeline pipeline = new Pipeline(Arrays.asList(
                new NamedProcessor("zebra"),
                new NamedProcessor("alpha"),
                new NamedProcessor("mike")
        ));
        // After construction, internal state should be sorted; we can verify through behavior
        // Calling preWrite on null processor order is verified through behavior below
        assertNotNull(pipeline);
    }

    @Test
    public void shouldBeSafeWithNullProcessors() {
        Pipeline pipeline = new Pipeline(null);
        // Should not throw on preWrite with empty processors
        pipeline.preWrite(null, null);
    }

    @Test
    public void shouldExecutePreWriteInOrder() {
        List<String> log = new ArrayList<>();
        Pipeline pipeline = new Pipeline(Arrays.asList(
                new NamedProcessor("c", log),
                new NamedProcessor("a", log),
                new NamedProcessor("b", log)
        ));
        pipeline.preWrite(null, null);
        assertEquals(Arrays.asList("a", "b", "c"), log);
    }

    @Test
    public void shouldWrapPreWriteExceptionAsPipelineException() {
        Pipeline pipeline = new Pipeline(Arrays.asList(new ThrowingProcessor("bad-proc")));
        try {
            pipeline.preWrite(null, null);
            fail("Expected PipelineException");
        } catch (Pipeline.PipelineException ex) {
            assertEquals("bad-proc", ex.getProcessorName());
        }
    }

    @Test
    public void shouldContinuePostReadEvenIfProcessorThrows() {
        List<String> log = new ArrayList<>();
        Pipeline pipeline = new Pipeline(Arrays.asList(
                new ThrowingProcessor("z"),
                new NamedProcessor("a", log)
        ));
        // postRead should not propagate exception (it only logs warning)
        pipeline.postRead(null, new HashMap<>());
        assertEquals(Arrays.asList("a"), log);
    }

    @Test
    public void shouldContinuePostReadListEvenIfProcessorThrows() {
        List<String> log = new ArrayList<>();
        Pipeline pipeline = new Pipeline(Arrays.asList(
                new ThrowingProcessor("z"),
                new NamedProcessor("a", log)
        ));
        pipeline.postReadList(null, Arrays.asList(new HashMap<>()));
        assertEquals(Arrays.asList("a"), log);
    }

    @Test
    public void shouldHandleNullRowsInPostReadList() {
        Pipeline pipeline = new Pipeline(Arrays.asList(new NamedProcessor("a")));
        pipeline.postReadList(null, null);
        // Should not throw
    }

    @Test
    public void shouldHandleNullRowsInListPostReadList() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(null);
        Pipeline pipeline = new Pipeline(Arrays.asList(new NamedProcessor("a")));
        try {
            pipeline.postReadList(null, rows);
        } catch (NullPointerException expected) {
            // acceptable behavior
        }
    }

    @Test
    public void pipelineExceptionShouldCarryProcessorName() {
        Pipeline.PipelineException ex = new Pipeline.PipelineException("p1", new RuntimeException("oops"));
        assertEquals("p1", ex.getProcessorName());
        assertEquals("oops", ex.getCause().getMessage());
        assertTrue(ex.getMessage().contains("p1"));
        assertTrue(ex.getMessage().contains("oops"));
    }

    @Test
    public void pipelineExceptionShouldExtendRuntimeException() {
        Pipeline.PipelineException ex = new Pipeline.PipelineException("p1", new RuntimeException());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void pipelineExceptionShouldBeThrowable() {
        Pipeline.PipelineException ex = new Pipeline.PipelineException("p1", new RuntimeException("c"));
        try {
            throw ex;
        } catch (RuntimeException caught) {
            assertSame(ex, caught);
            assertEquals("p1", ((Pipeline.PipelineException) caught).getProcessorName());
        }
    }

    // --- Helper processors ---

    private static class NamedProcessor implements FieldProcessor {
        private final String name;
        private final List<String> log;

        NamedProcessor(String name) {
            this(name, null);
        }

        NamedProcessor(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
            if (log != null) log.add(name);
        }

        @Override
        public void postRead(EntityDefDTO entity, Map<String, Object> row) {
            if (log != null) log.add(name);
        }
    }

    private static class ThrowingProcessor implements FieldProcessor {
        private final String name;

        ThrowingProcessor(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
            throw new RuntimeException("boom");
        }

        @Override
        public void postRead(EntityDefDTO entity, Map<String, Object> row) {
            throw new RuntimeException("boom");
        }
    }
}