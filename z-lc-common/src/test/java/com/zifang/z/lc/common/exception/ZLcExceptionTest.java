package com.zifang.z.lc.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常类单元测试
 *
 * @author zifang
 */
class ZLcExceptionTest {

    @Test
    void permissionLimitException_shouldHaveMessage() {
        ZLcPermissionLimitException ex = new ZLcPermissionLimitException("无权限");
        assertThat(ex.getMessage()).isEqualTo("无权限");
    }

    @Test
    void permissionLimitException_shouldAcceptCause() {
        RuntimeException cause = new RuntimeException("root");
        ZLcPermissionLimitException ex = new ZLcPermissionLimitException("无权限", cause);
        assertThat(ex.getMessage()).isEqualTo("无权限");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void findNoAssigneeException_shouldHaveMessage() {
        ZLcFindNoAssigneeException ex = new ZLcFindNoAssigneeException("未找到处理人");
        assertThat(ex.getMessage()).isEqualTo("未找到处理人");
    }

    @Test
    void findNoAssigneeException_shouldAcceptCause() {
        RuntimeException cause = new RuntimeException("root");
        ZLcFindNoAssigneeException ex = new ZLcFindNoAssigneeException("未找到处理人", cause);
        assertThat(ex.getMessage()).isEqualTo("未找到处理人");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void exportSuspendException_shouldHaveMessageAndCause() {
        RuntimeException cause = new RuntimeException("checkpoint已保存");
        ZLcExportSuspendException ex = new ZLcExportSuspendException("导出暂停", cause);
        assertThat(ex.getMessage()).isEqualTo("导出暂停");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
