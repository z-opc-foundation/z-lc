package com.zifang.z.lc.sdk.spi.task;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TaskServiceCollector 单元测试
 *
 * 用 Spring 容器测试 setApplicationContext 行为.
 */
public class TaskServiceCollectorTest {

    @Test
    public void shouldCreateWithEmptyRegistry() {
        TaskServiceCollector c = new TaskServiceCollector();
        assertEquals(0, c.size());
    }

    @Test
    public void shouldReturnNullForUnknownIdentityCode() {
        TaskServiceCollector c = new TaskServiceCollector();
        assertNull(c.findByIdentityCode("unknown"));
    }

    @Test
    public void shouldReturnNullForNullIdentityCode() {
        TaskServiceCollector c = new TaskServiceCollector();
        assertNull(c.findByIdentityCode(null));
    }

    @Test
    public void shouldReturnEmptySetWhenNothingRegistered() {
        TaskServiceCollector c = new TaskServiceCollector();
        Set<String> codes = c.registeredIdentityCodes();
        assertNotNull(codes);
        assertTrue(codes.isEmpty());
    }

    @Test
    public void shouldRegisterBeanWithAnnotation() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                TaskCollectorConfig.class, TodoServiceImpl.class);
        TaskServiceCollector c = ctx.getBean(TaskServiceCollector.class);
        assertEquals(1, c.size());
    }

    @Test
    public void shouldIndexByIdentityCode() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                TaskCollectorConfig.class, TodoServiceImpl.class);
        TaskServiceCollector c = ctx.getBean(TaskServiceCollector.class);
        AbstractTaskService svc = c.findByIdentityCode("todo");
        assertNotNull(svc);
        assertTrue(svc instanceof TodoServiceImpl);
    }

    @Test
    public void shouldListRegisteredIdentityCodes() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                TaskCollectorConfig.class, TodoServiceImpl.class);
        TaskServiceCollector c = ctx.getBean(TaskServiceCollector.class);
        Set<String> codes = c.registeredIdentityCodes();
        assertEquals(1, codes.size());
        assertTrue(codes.contains("todo"));
    }

    @Test
    public void shouldHaveApplicationContextAwareImpl() {
        assertTrue("TaskServiceCollector 应当实现 ApplicationContextAware",
                org.springframework.context.ApplicationContextAware.class.isAssignableFrom(TaskServiceCollector.class));
    }

    @Test
    public void shouldHaveComponentAnnotation() {
        assertTrue(TaskServiceCollector.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.task",
                TaskServiceCollector.class.getPackage().getName());
    }

    @Test
    public void shouldHaveSetApplicationContextMethod() throws NoSuchMethodException {
        TaskServiceCollector.class.getMethod("setApplicationContext",
                org.springframework.context.ApplicationContext.class);
    }

    @Test
    public void shouldHaveFindByIdentityCodeMethod() throws NoSuchMethodException {
        TaskServiceCollector.class.getMethod("findByIdentityCode", String.class);
    }

    @Test
    public void shouldHaveRegisteredIdentityCodesMethod() throws NoSuchMethodException {
        TaskServiceCollector.class.getMethod("registeredIdentityCodes");
    }

    @Test
    public void shouldHaveSizeMethod() throws NoSuchMethodException {
        TaskServiceCollector.class.getMethod("size");
    }

    @Test
    public void shouldThrowWhenBeanMissingAnnotation() {
        try {
            new AnnotationConfigApplicationContext(
                    TaskCollectorConfig.class, NoAnnotationServiceImpl.class);
            org.junit.Assert.fail("应当抛异常 (BeanCreationException / IllegalStateException)");
        } catch (IllegalStateException expected) {
            assertTrue("异常消息应包含注解缺失",
                    expected.getMessage().contains("@TaskServiceInfo"));
        } catch (Exception other) {
            // Spring 启动期失败抛 BeanCreationException, 包含 IllegalStateException 作为 cause
            Throwable cause = other.getCause();
            assertNotNull("异常应当有 cause", cause);
            assertTrue("cause 应当是 IllegalStateException 或消息包含注解缺失",
                    cause instanceof IllegalStateException
                            || (cause.getMessage() != null && cause.getMessage().contains("@TaskServiceInfo")));
        }
    }

    @Test
    public void shouldThrowOnDuplicateIdentityCode() {
        try {
            new AnnotationConfigApplicationContext(
                    TaskCollectorConfig.class,
                    TodoServiceImpl.class,
                    DuplicateServiceImpl.class);
            org.junit.Assert.fail("应当抛 IllegalStateException (重复 identityCode)");
        } catch (IllegalStateException expected) {
            assertTrue("异常消息应包含重复 identityCode",
                    expected.getMessage().contains("相同的 TaskService.identityCode"));
        } catch (Exception other) {
            // 也可接受 Spring 启动失败 (BeanCreationException 包装)
            Throwable cause = other.getCause();
            assertNotNull(cause);
            assertTrue("cause 应当包含重复 identityCode 消息",
                    cause.getMessage() != null && cause.getMessage().contains("相同的 TaskService.identityCode"));
        }
    }

    @Test
    public void findByIdentityCodeShouldReturnSameInstanceTwice() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                TaskCollectorConfig.class, TodoServiceImpl.class);
        TaskServiceCollector c = ctx.getBean(TaskServiceCollector.class);
        AbstractTaskService a = c.findByIdentityCode("todo");
        AbstractTaskService b = c.findByIdentityCode("todo");
        assertTrue(a == b);
    }

    // --- helpers ---

    @org.springframework.context.annotation.Configuration
    static class TaskCollectorConfig {
        @org.springframework.context.annotation.Bean
        public TaskServiceCollector taskServiceCollector() {
            return new TaskServiceCollector();
        }
    }

    @TaskServiceInfo(identityCode = "todo")
    public static class TodoServiceImpl extends AbstractTaskService {
        @Override
        public void sendTodoMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendDoneMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendRevokeMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
    }

    public static class NoAnnotationServiceImpl extends AbstractTaskService {
        @Override
        public void sendTodoMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendDoneMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendRevokeMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
    }

    @TaskServiceInfo(identityCode = "todo") // same code as TodoServiceImpl
    public static class DuplicateServiceImpl extends AbstractTaskService {
        @Override
        public void sendTodoMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendDoneMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
        @Override
        public void sendRevokeMsg(com.zifang.z.lc.common.dto.task.MessageDTO messageDTO) {}
    }
}