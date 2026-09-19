package com.zifang.z.lc.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExportSuspendException 单元测试
 *
 * @author zifang
 */
class ZLcExportSuspendExceptionTest {

    @Test
    void shouldExtendRuntimeException() {
        assertThat(ZLcExportSuspendException.class.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        String message = "导出任务挂起";
        Throwable cause = new RuntimeException("分页重试耗尽");
        ZLcExportSuspendException exception = new ZLcExportSuspendException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithNullMessage() {
        Throwable cause = new RuntimeException("分页重试耗尽");
        ZLcExportSuspendException exception = new ZLcExportSuspendException(null, cause);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithNullCause() {
        String message = "导出任务挂起";
        ZLcExportSuspendException exception = new ZLcExportSuspendException(message, null);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithNullMessageAndCause() {
        ZLcExportSuspendException exception = new ZLcExportSuspendException(null, null);
        
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }
}
