package com.zifang.z.lc.core.adapter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * MqAdapter 单元测试
 * <p>
 * MqAdapter 是 z-mq 的本地缓冲适配器 (broker 走 Netty TCP), 可直接测试本地 buffer 行为.
 */
public class MqAdapterTest {

    private MqAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MqAdapter();
        Field f = MqAdapter.class.getDeclaredField("brokerUrl");
        f.setAccessible(true);
        f.set(adapter, "tcp://localhost:9876");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("MqAdapter 应当标注 @Component", MqAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("MqAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(MqAdapter.class));
    }

    @Test
    public void nameShouldBeMq() {
        assertEquals("mq", adapter.name());
        assertEquals("mq", MqAdapter.NAME);
    }

    @Test
    public void priorityShouldBeForty() {
        assertEquals(40, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        try {
            adapter.init();
        } catch (Exception ex) {
            assertFalse("init 不应抛异常: " + ex.getMessage(), true);
        }
    }

    @Test
    public void publishAsyncShouldEnqueueMessage() {
        assertTrue(adapter.publishAsync("topic-a", "tag-b", "payload"));
        assertEquals(1, adapter.bufferSize());
    }

    @Test
    public void publishAsyncShouldReturnFalseForNullTopic() {
        assertFalse(adapter.publishAsync(null, "tag", "payload"));
    }

    @Test
    public void publishAsyncShouldReturnFalseForEmptyTopic() {
        assertFalse(adapter.publishAsync("", "tag", "payload"));
    }

    @Test
    public void publishAsyncShouldHandleNullTag() {
        assertTrue(adapter.publishAsync("topic", null, "payload"));
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        assertEquals(1, drained.size());
        assertEquals("", drained.get(0).tag);
    }

    @Test
    public void publishAsyncShouldHandleNullBody() {
        assertTrue(adapter.publishAsync("topic", "tag", null));
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        assertEquals(1, drained.size());
        assertEquals(null, drained.get(0).body);
    }

    @Test
    public void drainSentShouldEmptyBuffer() {
        adapter.publishAsync("t", "g", "x");
        adapter.publishAsync("t", "g", "y");
        assertEquals(2, adapter.bufferSize());
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        assertEquals(2, drained.size());
        assertEquals(0, adapter.bufferSize());
    }

    @Test
    public void drainSentShouldReturnEmptyListForEmptyBuffer() {
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        assertNotNull(drained);
        assertTrue(drained.isEmpty());
    }

    @Test
    public void drainSentShouldReturnUnmodifiableList() {
        adapter.publishAsync("t", "g", "x");
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        try {
            drained.add(new MqAdapter.MqMessage());
            assertFalse("应不可修改", true);
        } catch (UnsupportedOperationException ex) {
            // ok
        }
    }

    @Test
    public void bufferSizeShouldReflectCurrentCount() {
        assertEquals(0, adapter.bufferSize());
        adapter.publishAsync("a", "b", "c");
        assertEquals(1, adapter.bufferSize());
        adapter.publishAsync("d", "e", "f");
        assertEquals(2, adapter.bufferSize());
    }

    @Test
    public void pingShouldReturnTrue() {
        // 当前本地模式, 始终返回 true
        assertTrue(adapter.ping());
    }

    @Test
    public void mqMessageToStringShouldIncludeFields() {
        MqAdapter.MqMessage msg = new MqAdapter.MqMessage();
        msg.topic = "my-topic";
        msg.tag = "my-tag";
        msg.body = "my-body";
        String s = msg.toString();
        assertNotNull(s);
        assertTrue(s.contains("my-topic"));
        assertTrue(s.contains("my-tag"));
        assertTrue(s.contains("my-body"));
    }

    @Test
    public void shouldEnqueueMultipleMessages() {
        Map<String, Object> body = new HashMap<>();
        body.put("key", "value");
        assertTrue(adapter.publishAsync("topic-1", "create", body));
        assertTrue(adapter.publishAsync("topic-2", "update", "string-body"));
        assertTrue(adapter.publishAsync("topic-3", "delete", 42));
        assertEquals(3, adapter.bufferSize());
        List<MqAdapter.MqMessage> drained = adapter.drainSent();
        assertEquals(3, drained.size());
        assertEquals("topic-1", drained.get(0).topic);
        assertEquals("topic-2", drained.get(1).topic);
        assertEquals("topic-3", drained.get(2).topic);
    }
}