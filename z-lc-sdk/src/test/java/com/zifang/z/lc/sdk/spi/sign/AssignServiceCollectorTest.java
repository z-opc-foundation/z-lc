package com.zifang.z.lc.sdk.spi.sign;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AssignServiceCollector 单元测试
 */
public class AssignServiceCollectorTest {

    @Test
    public void shouldCreateWithEmptyRegistry() {
        AssignServiceCollector c = new AssignServiceCollector();
        assertEquals(0, c.size());
    }

    @Test
    public void shouldReturnNullForUnknownIdentityCode() {
        AssignServiceCollector c = new AssignServiceCollector();
        assertNull(c.findByIdentityCode("unknown"));
    }

    @Test
    public void shouldReturnNullForNullIdentityCode() {
        AssignServiceCollector c = new AssignServiceCollector();
        assertNull(c.findByIdentityCode(null));
    }

    @Test
    public void shouldReturnEmptySetWhenNothingRegistered() {
        AssignServiceCollector c = new AssignServiceCollector();
        Set<String> codes = c.registeredIdentityCodes();
        assertNotNull(codes);
        assertTrue(codes.isEmpty());
    }

    @Test
    public void shouldRegisterBeanWithAnnotation() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                AssignCollectorConfig.class, SignServiceImpl.class);
        AssignServiceCollector c = ctx.getBean(AssignServiceCollector.class);
        assertEquals(1, c.size());
    }

    @Test
    public void shouldIndexByIdentityCode() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                AssignCollectorConfig.class, SignServiceImpl.class);
        AssignServiceCollector c = ctx.getBean(AssignServiceCollector.class);
        AbstractAssignService svc = c.findByIdentityCode("sign");
        assertNotNull(svc);
        assertTrue(svc instanceof SignServiceImpl);
    }

    @Test
    public void shouldListRegisteredIdentityCodes() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                AssignCollectorConfig.class, SignServiceImpl.class);
        AssignServiceCollector c = ctx.getBean(AssignServiceCollector.class);
        Set<String> codes = c.registeredIdentityCodes();
        assertEquals(1, codes.size());
        assertTrue(codes.contains("sign"));
    }

    @Test
    public void shouldHaveApplicationContextAwareImpl() {
        assertTrue("AssignServiceCollector 应当实现 ApplicationContextAware",
                org.springframework.context.ApplicationContextAware.class.isAssignableFrom(AssignServiceCollector.class));
    }

    @Test
    public void shouldHaveComponentAnnotation() {
        assertTrue(AssignServiceCollector.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.sign",
                AssignServiceCollector.class.getPackage().getName());
    }

    @Test
    public void shouldHaveSetApplicationContextMethod() throws NoSuchMethodException {
        AssignServiceCollector.class.getMethod("setApplicationContext",
                org.springframework.context.ApplicationContext.class);
    }

    @Test
    public void shouldHaveFindByIdentityCodeMethod() throws NoSuchMethodException {
        AssignServiceCollector.class.getMethod("findByIdentityCode", String.class);
    }

    @Test
    public void shouldHaveRegisteredIdentityCodesMethod() throws NoSuchMethodException {
        AssignServiceCollector.class.getMethod("registeredIdentityCodes");
    }

    @Test
    public void shouldHaveSizeMethod() throws NoSuchMethodException {
        AssignServiceCollector.class.getMethod("size");
    }

    @Test
    public void findByIdentityCodeShouldReturnSameInstanceTwice() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                AssignCollectorConfig.class, SignServiceImpl.class);
        AssignServiceCollector c = ctx.getBean(AssignServiceCollector.class);
        AbstractAssignService a = c.findByIdentityCode("sign");
        AbstractAssignService b = c.findByIdentityCode("sign");
        assertTrue(a == b);
    }

    // --- helpers ---

    @org.springframework.context.annotation.Configuration
    static class AssignCollectorConfig {
        @org.springframework.context.annotation.Bean
        public AssignServiceCollector assignServiceCollector() {
            return new AssignServiceCollector();
        }
    }

    @AssignServiceInfo(identityCode = "sign")
    public static class SignServiceImpl extends AbstractAssignService {
        @Override
        public com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO addSignJob(
                com.zifang.z.lc.common.dto.sign.AddSignExtendDTO dto) { return null; }
        @Override
        public com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO querySignResult(
                com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO dto) { return null; }
        @Override
        public Boolean verifySignedData(
                com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO dto) { return true; }
        @Override
        public java.util.Map<String, Object> getSignedData(
                com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO dto) { return null; }
    }
}