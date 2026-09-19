package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcJobParam 单元测试
 */
class ZLcJobParamTest {

    @Test
    void shouldCreateEmptyInstanceWithDefaultConstructor() {
        ZLcJobParam param = new ZLcJobParam();

        assertThat(param).isNotNull();
        assertThat(param.getTaskCode()).isNull();
        assertThat(param.getOrgId()).isNull();
        assertThat(param.getStaffId()).isNull();
        assertThat(param.getJobAccountNo()).isNull();
    }

    @Test
    void shouldCreateInstanceWithAllArgsConstructor() {
        ZLcJobParam param = new ZLcJobParam("TASK-001", 100L, 200L, "acc-001");

        assertThat(param.getTaskCode()).isEqualTo("TASK-001");
        assertThat(param.getOrgId()).isEqualTo(100L);
        assertThat(param.getStaffId()).isEqualTo(200L);
        assertThat(param.getJobAccountNo()).isEqualTo("acc-001");
    }

    @Test
    void shouldSetAndGetTaskCode() {
        ZLcJobParam param = new ZLcJobParam();
        param.setTaskCode("TASK-XYZ");

        assertThat(param.getTaskCode()).isEqualTo("TASK-XYZ");
    }

    @Test
    void shouldSetAndGetOrgId() {
        ZLcJobParam param = new ZLcJobParam();
        param.setOrgId(999L);

        assertThat(param.getOrgId()).isEqualTo(999L);
    }

    @Test
    void shouldSetAndGetStaffId() {
        ZLcJobParam param = new ZLcJobParam();
        param.setStaffId(12345L);

        assertThat(param.getStaffId()).isEqualTo(12345L);
    }

    @Test
    void shouldSetAndGetJobAccountNo() {
        ZLcJobParam param = new ZLcJobParam();
        param.setJobAccountNo("test-account");

        assertThat(param.getJobAccountNo()).isEqualTo("test-account");
    }

    @Test
    void shouldAllowSettingAllFieldsToNull() {
        ZLcJobParam param = new ZLcJobParam("TASK", 1L, 2L, "acc");

        param.setTaskCode(null);
        param.setOrgId(null);
        param.setStaffId(null);
        param.setJobAccountNo(null);

        assertThat(param.getTaskCode()).isNull();
        assertThat(param.getOrgId()).isNull();
        assertThat(param.getStaffId()).isNull();
        assertThat(param.getJobAccountNo()).isNull();
    }

    @Test
    void shouldSupportOverwritingAllFields() {
        ZLcJobParam param = new ZLcJobParam("OLD", 1L, 2L, "OLD-ACC");

        param.setTaskCode("NEW");
        param.setOrgId(100L);
        param.setStaffId(200L);
        param.setJobAccountNo("NEW-ACC");

        assertThat(param.getTaskCode()).isEqualTo("NEW");
        assertThat(param.getOrgId()).isEqualTo(100L);
        assertThat(param.getStaffId()).isEqualTo(200L);
        assertThat(param.getJobAccountNo()).isEqualTo("NEW-ACC");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcJobParam param = new ZLcJobParam();
        assertThat(param).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHaveSerialVersionUid() throws NoSuchFieldException {
        java.lang.reflect.Field field = ZLcJobParam.class.getDeclaredField("serialVersionUID");
        assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void shouldHandleEmptyStrings() {
        ZLcJobParam param = new ZLcJobParam("", 0L, 0L, "");

        assertThat(param.getTaskCode()).isEmpty();
        assertThat(param.getJobAccountNo()).isEmpty();
    }

    @Test
    void shouldHandleNegativeIds() {
        ZLcJobParam param = new ZLcJobParam("TASK", -1L, -1L, "ACC");

        assertThat(param.getOrgId()).isEqualTo(-1L);
        assertThat(param.getStaffId()).isEqualTo(-1L);
    }

    @Test
    void shouldHandleLargeIds() {
        ZLcJobParam param = new ZLcJobParam("TASK", Long.MAX_VALUE, Long.MIN_VALUE, "ACC");

        assertThat(param.getOrgId()).isEqualTo(Long.MAX_VALUE);
        assertThat(param.getStaffId()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void shouldAllowModifyingInstanceAfterConstruction() {
        ZLcJobParam param = new ZLcJobParam("INIT", 1L, 1L, "INIT-ACC");
        param.setTaskCode("MODIFIED");

        assertThat(param.getTaskCode()).isEqualTo("MODIFIED");
        assertThat(param.getOrgId()).isEqualTo(1L);
        assertThat(param.getStaffId()).isEqualTo(1L);
        assertThat(param.getJobAccountNo()).isEqualTo("INIT-ACC");
    }

    @Test
    void shouldBeIndependentBetweenInstances() {
        ZLcJobParam a = new ZLcJobParam("A", 1L, 1L, "A");
        ZLcJobParam b = new ZLcJobParam("B", 2L, 2L, "B");

        a.setTaskCode("A-MODIFIED");

        assertThat(a.getTaskCode()).isEqualTo("A-MODIFIED");
        assertThat(b.getTaskCode()).isEqualTo("B");
    }
}