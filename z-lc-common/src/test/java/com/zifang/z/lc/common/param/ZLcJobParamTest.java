package com.zifang.z.lc.common.param;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcJobParam (param包版本) 单元测试
 *
 * @author zifang
 */
class ZLcJobParamTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcJobParam param = new ZLcJobParam();
        assertThat(param).isNotNull();
    }

    @Test
    void shouldCreateWithFullConstructor() {
        ZLcJobParam param = new ZLcJobParam("task-001", 100L, 200L, "ACC001");
        assertThat(param.getTaskCode()).isEqualTo("task-001");
        assertThat(param.getOrgId()).isEqualTo(100L);
        assertThat(param.getStaffId()).isEqualTo(200L);
        assertThat(param.getJobAccountNo()).isEqualTo("ACC001");
    }

    @Test
    void shouldSetAndGetTaskCode() {
        ZLcJobParam param = new ZLcJobParam();
        param.setTaskCode("task-001");
        assertThat(param.getTaskCode()).isEqualTo("task-001");
    }

    @Test
    void shouldSetAndGetOrgId() {
        ZLcJobParam param = new ZLcJobParam();
        param.setOrgId(100L);
        assertThat(param.getOrgId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetStaffId() {
        ZLcJobParam param = new ZLcJobParam();
        param.setStaffId(200L);
        assertThat(param.getStaffId()).isEqualTo(200L);
    }

    @Test
    void shouldSetAndGetJobAccountNo() {
        ZLcJobParam param = new ZLcJobParam();
        param.setJobAccountNo("ACC001");
        assertThat(param.getJobAccountNo()).isEqualTo("ACC001");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcJobParam param = new ZLcJobParam();
        assertThat(param.getTaskCode()).isNull();
        assertThat(param.getOrgId()).isNull();
        assertThat(param.getStaffId()).isNull();
        assertThat(param.getJobAccountNo()).isNull();
    }
}