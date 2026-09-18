package com.zifang.z.lc.common.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注解类单元测试
 *
 * @author zifang
 */
class ZLcAnnotationTest {

    @Test
    void infoAnnotation_shouldBeAnnotation() {
        assertThat(ZLcInfo.class.isAnnotation()).isTrue();
    }

    @Test
    void operationRecordAnnotation_shouldBeAnnotation() {
        assertThat(ZLcOperationRecord.class.isAnnotation()).isTrue();
    }

    @Test
    void retryAnnotation_shouldBeAnnotation() {
        assertThat(ZLcRetry.class.isAnnotation()).isTrue();
    }
}
