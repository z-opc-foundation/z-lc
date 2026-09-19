package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelAuthObjectType 单元测试
 *
 * @author zifang
 */
class ZLcLabelAuthObjectTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelAuthObjectType.values()).hasSize(2);
    }

    @Test
    void shouldHaveStaffCode() {
        assertThat(ZLcLabelAuthObjectType.STAFF.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveStaffDescription() {
        assertThat(ZLcLabelAuthObjectType.STAFF.getDescription()).isEqualTo("人员");
    }

    @Test
    void shouldHaveDeptCode() {
        assertThat(ZLcLabelAuthObjectType.DEPT.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveDeptDescription() {
        assertThat(ZLcLabelAuthObjectType.DEPT.getDescription()).isEqualTo("部门");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelAuthObjectType.fromCode(1)).isEqualTo(ZLcLabelAuthObjectType.STAFF);
        assertThat(ZLcLabelAuthObjectType.fromCode(2)).isEqualTo(ZLcLabelAuthObjectType.DEPT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelAuthObjectType.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelAuthObjectType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelAuthObjectType.valueOf("STAFF")).isEqualTo(ZLcLabelAuthObjectType.STAFF);
        assertThat(ZLcLabelAuthObjectType.valueOf("DEPT")).isEqualTo(ZLcLabelAuthObjectType.DEPT);
    }
}
