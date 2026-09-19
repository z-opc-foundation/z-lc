package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPlatformStatusCode 单元测试
 *
 * @author zifang
 */
class ZLcPlatformStatusCodeTest {

    @Test
    void shouldImplementStatusCode() {
        assertThat(StatusCode.class.isAssignableFrom(ZLcPlatformStatusCode.class)).isTrue();
    }

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcPlatformStatusCode.values()).hasSize(11);
    }

    @Test
    void shouldHaveBizExceptionCode() {
        assertThat(ZLcPlatformStatusCode.BIZ_EXCEPTION.getCode()).isEqualTo(1001);
        assertThat(ZLcPlatformStatusCode.BIZ_EXCEPTION.getMessage()).isEqualTo("业务异常");
    }

    @Test
    void shouldHaveParamExceptionCode() {
        assertThat(ZLcPlatformStatusCode.PARAM_EXCEPTION.getCode()).isEqualTo(1002);
        assertThat(ZLcPlatformStatusCode.PARAM_EXCEPTION.getMessage()).isEqualTo("参数异常");
    }

    @Test
    void shouldHaveTooBusyExceptionCode() {
        assertThat(ZLcPlatformStatusCode.TOO_BUSY_EXCEPTION.getCode()).isEqualTo(2001);
        assertThat(ZLcPlatformStatusCode.TOO_BUSY_EXCEPTION.getMessage()).isEqualTo("系统繁忙，请稍后再试");
    }

    @Test
    void shouldHaveValidateExceptionCode() {
        assertThat(ZLcPlatformStatusCode.VALIDATE_EXCEPTION.getCode()).isEqualTo(3001);
        assertThat(ZLcPlatformStatusCode.VALIDATE_EXCEPTION.getMessage()).isEqualTo("校验不通过");
    }

    @Test
    void shouldHaveEntityNotFoundCode() {
        assertThat(ZLcPlatformStatusCode.ENTITY_NOT_FOUND.getCode()).isEqualTo(1003);
        assertThat(ZLcPlatformStatusCode.ENTITY_NOT_FOUND.getMessage()).isEqualTo("实体未找到");
    }

    @Test
    void shouldHaveDatasourceConnectFailedCode() {
        assertThat(ZLcPlatformStatusCode.DATASOURCE_CONNECT_FAILED.getCode()).isEqualTo(4001);
        assertThat(ZLcPlatformStatusCode.DATASOURCE_CONNECT_FAILED.getMessage()).isEqualTo("数据源连接失败");
    }

    @Test
    void shouldHaveFieldTypeMismatchCode() {
        assertThat(ZLcPlatformStatusCode.FIELD_TYPE_MISMATCH.getCode()).isEqualTo(5001);
        assertThat(ZLcPlatformStatusCode.FIELD_TYPE_MISMATCH.getMessage()).isEqualTo("字段类型不匹配");
    }

    @Test
    void shouldHaveModelDefinitionConflictCode() {
        assertThat(ZLcPlatformStatusCode.MODEL_DEFINITION_CONFLICT.getCode()).isEqualTo(6001);
        assertThat(ZLcPlatformStatusCode.MODEL_DEFINITION_CONFLICT.getMessage()).isEqualTo("模型定义冲突");
    }

    @Test
    void shouldHavePermissionDeniedCode() {
        assertThat(ZLcPlatformStatusCode.PERMISSION_DENIED.getCode()).isEqualTo(8001);
        assertThat(ZLcPlatformStatusCode.PERMISSION_DENIED.getMessage()).isEqualTo("权限校验失败");
    }

    @Test
    void shouldHaveUnauthorizedCode() {
        assertThat(ZLcPlatformStatusCode.UNAUTHORIZED.getCode()).isEqualTo(8002);
        assertThat(ZLcPlatformStatusCode.UNAUTHORIZED.getMessage()).isEqualTo("未授权访问");
    }

    @Test
    void shouldHaveNotFoundCode() {
        assertThat(ZLcPlatformStatusCode.NOT_FOUND.getCode()).isEqualTo(8003);
        assertThat(ZLcPlatformStatusCode.NOT_FOUND.getMessage()).isEqualTo("资源不存在");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcPlatformStatusCode.fromCode(1001)).isEqualTo(ZLcPlatformStatusCode.BIZ_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.fromCode(1002)).isEqualTo(ZLcPlatformStatusCode.PARAM_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.fromCode(2001)).isEqualTo(ZLcPlatformStatusCode.TOO_BUSY_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.fromCode(3001)).isEqualTo(ZLcPlatformStatusCode.VALIDATE_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.fromCode(1003)).isEqualTo(ZLcPlatformStatusCode.ENTITY_NOT_FOUND);
        assertThat(ZLcPlatformStatusCode.fromCode(4001)).isEqualTo(ZLcPlatformStatusCode.DATASOURCE_CONNECT_FAILED);
        assertThat(ZLcPlatformStatusCode.fromCode(5001)).isEqualTo(ZLcPlatformStatusCode.FIELD_TYPE_MISMATCH);
        assertThat(ZLcPlatformStatusCode.fromCode(6001)).isEqualTo(ZLcPlatformStatusCode.MODEL_DEFINITION_CONFLICT);
        assertThat(ZLcPlatformStatusCode.fromCode(8001)).isEqualTo(ZLcPlatformStatusCode.PERMISSION_DENIED);
        assertThat(ZLcPlatformStatusCode.fromCode(8002)).isEqualTo(ZLcPlatformStatusCode.UNAUTHORIZED);
        assertThat(ZLcPlatformStatusCode.fromCode(8003)).isEqualTo(ZLcPlatformStatusCode.NOT_FOUND);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcPlatformStatusCode.fromCode(9999)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcPlatformStatusCode.valueOf("BIZ_EXCEPTION")).isEqualTo(ZLcPlatformStatusCode.BIZ_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.valueOf("PARAM_EXCEPTION")).isEqualTo(ZLcPlatformStatusCode.PARAM_EXCEPTION);
        assertThat(ZLcPlatformStatusCode.valueOf("TOO_BUSY_EXCEPTION")).isEqualTo(ZLcPlatformStatusCode.TOO_BUSY_EXCEPTION);
    }
}
