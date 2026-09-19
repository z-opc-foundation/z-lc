package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcActionTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcActionTypeEnumTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcActionTypeEnum.values()).hasSize(3);
    }

    @Test
    void shouldHaveCreateAction() {
        ZLcActionTypeEnum action = ZLcActionTypeEnum.CREATE;
        assertThat(action.getActionType()).isEqualTo("create");
        assertThat(action.getActionTypeName()).isEqualTo("新增");
    }

    @Test
    void shouldHaveUpdateAction() {
        ZLcActionTypeEnum action = ZLcActionTypeEnum.UPDATE;
        assertThat(action.getActionType()).isEqualTo("update");
        assertThat(action.getActionTypeName()).isEqualTo("修改");
    }

    @Test
    void shouldHaveDeleteAction() {
        ZLcActionTypeEnum action = ZLcActionTypeEnum.DELETE;
        assertThat(action.getActionType()).isEqualTo("delete");
        assertThat(action.getActionTypeName()).isEqualTo("删除");
    }

    @Test
    void shouldGetNameByType() {
        assertThat(ZLcActionTypeEnum.getNameByType("create")).isEqualTo("新增");
        assertThat(ZLcActionTypeEnum.getNameByType("update")).isEqualTo("修改");
        assertThat(ZLcActionTypeEnum.getNameByType("delete")).isEqualTo("删除");
    }

    @Test
    void shouldReturnNullForUnknownType() {
        assertThat(ZLcActionTypeEnum.getNameByType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullType() {
        assertThat(ZLcActionTypeEnum.getNameByType(null)).isNull();
    }

    @Test
    void shouldFromActionType() {
        assertThat(ZLcActionTypeEnum.fromActionType("create")).isEqualTo(ZLcActionTypeEnum.CREATE);
        assertThat(ZLcActionTypeEnum.fromActionType("update")).isEqualTo(ZLcActionTypeEnum.UPDATE);
        assertThat(ZLcActionTypeEnum.fromActionType("delete")).isEqualTo(ZLcActionTypeEnum.DELETE);
    }

    @Test
    void shouldReturnNullForUnknownFromActionType() {
        assertThat(ZLcActionTypeEnum.fromActionType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromActionType() {
        assertThat(ZLcActionTypeEnum.fromActionType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcActionTypeEnum.valueOf("CREATE")).isEqualTo(ZLcActionTypeEnum.CREATE);
        assertThat(ZLcActionTypeEnum.valueOf("UPDATE")).isEqualTo(ZLcActionTypeEnum.UPDATE);
        assertThat(ZLcActionTypeEnum.valueOf("DELETE")).isEqualTo(ZLcActionTypeEnum.DELETE);
    }
}
