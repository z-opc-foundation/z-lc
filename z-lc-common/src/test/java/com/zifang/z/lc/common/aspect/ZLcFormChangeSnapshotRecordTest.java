package com.zifang.z.lc.common.aspect;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFormChangeSnapshotRecord 单元测试
 *
 * @author zifang
 */
class ZLcFormChangeSnapshotRecordTest {

    @Test
    void shouldHaveRuntimeRetention() {
        assertThat(ZLcFormChangeSnapshotRecord.class.getAnnotation(Retention.class).value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetMethod() {
        Target target = ZLcFormChangeSnapshotRecord.class.getAnnotation(Target.class);
        assertThat(target.value()).hasSize(1);
        assertThat(target.value()[0]).isEqualTo(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void shouldHaveActionTypeMethod() throws NoSuchMethodException {
        assertThat(ZLcFormChangeSnapshotRecord.class.getMethod("actionType")).isNotNull();
    }

    @Test
    void shouldHaveExtractorMethod() throws NoSuchMethodException {
        assertThat(ZLcFormChangeSnapshotRecord.class.getMethod("extractor")).isNotNull();
    }

    @Test
    void shouldActionTypeReturnTypeBeString() throws NoSuchMethodException {
        assertThat(ZLcFormChangeSnapshotRecord.class.getMethod("actionType").getReturnType()).isEqualTo(String.class);
    }

    @Test
    void shouldExtractorReturnTypeBeClass() throws NoSuchMethodException {
        assertThat(ZLcFormChangeSnapshotRecord.class.getMethod("extractor").getReturnType()).isEqualTo(Class.class);
    }
}
