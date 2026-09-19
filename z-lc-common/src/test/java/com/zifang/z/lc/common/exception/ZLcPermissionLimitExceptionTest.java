package com.zifang.z.lc.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPermissionLimitException 单元测试
 *
 * @author zifang
 */
class ZLcPermissionLimitExceptionTest {

    @Test
    void shouldExtendRuntimeException() {
        assertThat(ZLcPermissionLimitException.class.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    void shouldCreateWithMessage() {
        String message = "权限受限";
        ZLcPermissionLimitException exception = new ZLcPermissionLimitException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithNullMessage() {
        ZLcPermissionLimitException exception = new ZLcPermissionLimitException(null);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        String message = "权限受限";
        Throwable cause = new RuntimeException("无权执行");
        ZLcPermissionLimitException exception = new ZLcPermissionLimitException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithNullMessageAndCause() {
        ZLcPermissionLimitException exception = new ZLcPermissionLimitException(null, null);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }
}
