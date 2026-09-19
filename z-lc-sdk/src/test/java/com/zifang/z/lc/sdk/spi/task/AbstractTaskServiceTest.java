package com.zifang.z.lc.sdk.spi.task;

import com.zifang.z.lc.common.dto.task.MessageDTO;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AbstractTaskService 抽象基类单元测试
 */
public class AbstractTaskServiceTest {

    @Test
    public void shouldBeAbstractClass() {
        assertTrue("AbstractTaskService 应当是 abstract",
                Modifier.isAbstract(AbstractTaskService.class.getModifiers()));
    }

    @Test
    public void shouldImplementTaskService() {
        assertTrue("AbstractTaskService 应当实现 TaskService",
                TaskService.class.isAssignableFrom(AbstractTaskService.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.task",
                AbstractTaskService.class.getPackage().getName());
    }

    @Test
    public void defaultSendTodoMsgShouldBeCallable() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendTodoMsg(new MessageDTO());
        // 不抛异常即视为成功
    }

    @Test
    public void defaultSendTodoMsgShouldHandleNullMessage() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendTodoMsg(null);
        // 不抛异常即视为成功 (内部已做 null-safe)
    }

    @Test
    public void defaultSendDoneMsgShouldBeCallable() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendDoneMsg(new MessageDTO());
    }

    @Test
    public void defaultSendDoneMsgShouldHandleNullMessage() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendDoneMsg(null);
    }

    @Test
    public void defaultSendRevokeMsgShouldBeCallable() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendRevokeMsg(new MessageDTO());
    }

    @Test
    public void defaultSendRevokeMsgShouldHandleNullMessage() {
        AbstractTaskService svc = new AbstractTaskService() {};
        svc.sendRevokeMsg(null);
    }

    @Test
    public void subclassCanOverrideSendTodoMsg() {
        final java.util.List<String> log = new java.util.ArrayList<>();
        AbstractTaskService svc = new AbstractTaskService() {
            @Override
            public void sendTodoMsg(MessageDTO messageDTO) {
                log.add("TODO");
            }
        };
        svc.sendTodoMsg(new MessageDTO());
        assertNotNull(log);
        assertEquals(1, log.size());
        assertTrue(log.contains("TODO"));
    }

    @Test
    public void subclassCanOverrideAllThreeMsgMethods() {
        final java.util.Set<String> sent = new java.util.HashSet<>();
        AbstractTaskService svc = new AbstractTaskService() {
            @Override
            public void sendTodoMsg(MessageDTO messageDTO) { sent.add("TODO"); }
            @Override
            public void sendDoneMsg(MessageDTO messageDTO) { sent.add("DONE"); }
            @Override
            public void sendRevokeMsg(MessageDTO messageDTO) { sent.add("REVOKE"); }
        };
        svc.sendTodoMsg(new MessageDTO());
        svc.sendDoneMsg(new MessageDTO());
        svc.sendRevokeMsg(new MessageDTO());
        assertEquals(3, sent.size());
    }
}