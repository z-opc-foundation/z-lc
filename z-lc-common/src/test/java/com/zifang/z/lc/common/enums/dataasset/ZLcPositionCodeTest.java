package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPositionCode 单元测试
 *
 * @author zifang
 */
class ZLcPositionCodeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcPositionCode.values()).hasSize(3);
    }

    @Test
    void shouldHaveLabelAuthorizationPositionCode() {
        assertThat(ZLcPositionCode.LABEL_AUTHORIZATION_POSITION.getCode()).isEqualTo("label_authorization_position");
    }

    @Test
    void shouldHaveLabelAuthorizationPositionMessage() {
        assertThat(ZLcPositionCode.LABEL_AUTHORIZATION_POSITION.getMessage()).isEqualTo("标签授权岗位");
    }

    @Test
    void shouldHaveLabelEditPositionCode() {
        assertThat(ZLcPositionCode.LABEL_EDIT_POSITION.getCode()).isEqualTo("label_edit_position");
    }

    @Test
    void shouldHaveLabelEditPositionMessage() {
        assertThat(ZLcPositionCode.LABEL_EDIT_POSITION.getMessage()).isEqualTo("标签编辑岗位");
    }

    @Test
    void shouldHaveLabelViewPositionCode() {
        assertThat(ZLcPositionCode.LABEL_VIEW_POSITION.getCode()).isEqualTo("label_view_position");
    }

    @Test
    void shouldHaveLabelViewPositionMessage() {
        assertThat(ZLcPositionCode.LABEL_VIEW_POSITION.getMessage()).isEqualTo("标签查看岗位");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcPositionCode.fromCode("label_authorization_position")).isEqualTo(ZLcPositionCode.LABEL_AUTHORIZATION_POSITION);
        assertThat(ZLcPositionCode.fromCode("label_edit_position")).isEqualTo(ZLcPositionCode.LABEL_EDIT_POSITION);
        assertThat(ZLcPositionCode.fromCode("label_view_position")).isEqualTo(ZLcPositionCode.LABEL_VIEW_POSITION);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcPositionCode.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcPositionCode.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcPositionCode.valueOf("LABEL_AUTHORIZATION_POSITION")).isEqualTo(ZLcPositionCode.LABEL_AUTHORIZATION_POSITION);
        assertThat(ZLcPositionCode.valueOf("LABEL_EDIT_POSITION")).isEqualTo(ZLcPositionCode.LABEL_EDIT_POSITION);
        assertThat(ZLcPositionCode.valueOf("LABEL_VIEW_POSITION")).isEqualTo(ZLcPositionCode.LABEL_VIEW_POSITION);
    }
}
