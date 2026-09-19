package com.zifang.z.lc.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFindNoAssigneeException 单元测试
 *
 * @author zifang
 */
class ZLcFindNoAssigneeExceptionTest {

    @Test
    void shouldExtendRuntimeException() {
        assertThat(ZLcFindNoAssigneeException.class.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    void shouldCreateWithMessage() {
        String message = "找不到审批人";
        ZLcFindNoAssigneeException exception = new ZLcFindNoAssigneeException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithNullMessage() {
        ZLcFindNoAssigneeException exception = new ZLcFindNoAssigneeException(null);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        String message = "找不到审批人";
        Throwable cause = new RuntimeException("查询条件无匹配");
        ZLcFindNoAssigneeException exception = new ZLcFindNoAssigneeException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithNullMessageAndCause() {
        ZLcFindNoAssigneeException exception = new ZLcFindNoAssigneeException(null, null);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }
}
