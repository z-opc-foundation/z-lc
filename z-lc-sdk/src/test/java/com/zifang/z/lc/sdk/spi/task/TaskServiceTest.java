package com.zifang.z.lc.sdk.spi.task;

import com.zifang.z.lc.common.dto.task.MessageDTO;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TaskService (task) SPI 接口单元测试
 */
public class TaskServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(TaskService.class.isInterface());
    }

    @Test
    public void shouldDeclareSendTodoMsg() throws NoSuchMethodException {
        Method m = TaskService.class.getMethod("sendTodoMsg", MessageDTO.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareSendDoneMsg() throws NoSuchMethodException {
        Method m = TaskService.class.getMethod("sendDoneMsg", MessageDTO.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareSendRevokeMsg() throws NoSuchMethodException {
        Method m = TaskService.class.getMethod("sendRevokeMsg", MessageDTO.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        final java.util.List<String> sentMsgs = new java.util.ArrayList<>();
        TaskService impl = new TaskService() {
            @Override
            public void sendTodoMsg(MessageDTO messageDTO) {
                sentMsgs.add("TODO");
            }

            @Override
            public void sendDoneMsg(MessageDTO messageDTO) {
                sentMsgs.add("DONE");
            }

            @Override
            public void sendRevokeMsg(MessageDTO messageDTO) {
                sentMsgs.add("REVOKE");
            }
        };
        impl.sendTodoMsg(new MessageDTO());
        impl.sendDoneMsg(new MessageDTO());
        impl.sendRevokeMsg(new MessageDTO());

        assertEquals(3, sentMsgs.size());
        assertTrue(sentMsgs.contains("TODO"));
        assertTrue(sentMsgs.contains("DONE"));
        assertTrue(sentMsgs.contains("REVOKE"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.task",
                TaskService.class.getPackage().getName());
    }
}