package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcEngineStatusCode 单元测试
 *
 * @author zifang
 */
class ZLcEngineStatusCodeTest {

    @Test
    void shouldImplementStatusCode() {
        assertThat(StatusCode.class.isAssignableFrom(ZLcEngineStatusCode.class)).isTrue();
    }

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcEngineStatusCode.values()).hasSize(9);
    }

    @Test
    void shouldHaveBizExceptionCode() {
        assertThat(ZLcEngineStatusCode.BIZ_EXCEPTION.getCode()).isEqualTo(1001);
        assertThat(ZLcEngineStatusCode.BIZ_EXCEPTION.getMessage()).isEqualTo("业务异常");
    }

    @Test
    void shouldHaveTooBusyExceptionCode() {
        assertThat(ZLcEngineStatusCode.TOO_BUSY_EXCEPTION.getCode()).isEqualTo(2001);
        assertThat(ZLcEngineStatusCode.TOO_BUSY_EXCEPTION.getMessage()).isEqualTo("系统繁忙，请稍后再试");
    }

    @Test
    void shouldHaveValidateExceptionCode() {
        assertThat(ZLcEngineStatusCode.VALIDATE_EXCEPTION.getCode()).isEqualTo(3001);
        assertThat(ZLcEngineStatusCode.VALIDATE_EXCEPTION.getMessage()).isEqualTo("校验不通过");
    }

    @Test
    void shouldHaveEntityNotFoundCode() {
        assertThat(ZLcEngineStatusCode.ENTITY_NOT_FOUND.getCode()).isEqualTo(1002);
        assertThat(ZLcEngineStatusCode.ENTITY_NOT_FOUND.getMessage()).isEqualTo("实体未找到");
    }

    @Test
    void shouldHaveDatasourceConnectFailedCode() {
        assertThat(ZLcEngineStatusCode.DATASOURCE_CONNECT_FAILED.getCode()).isEqualTo(4001);
        assertThat(ZLcEngineStatusCode.DATASOURCE_CONNECT_FAILED.getMessage()).isEqualTo("数据源连接失败");
    }

    @Test
    void shouldHaveFieldTypeMismatchCode() {
        assertThat(ZLcEngineStatusCode.FIELD_TYPE_MISMATCH.getCode()).isEqualTo(5001);
        assertThat(ZLcEngineStatusCode.FIELD_TYPE_MISMATCH.getMessage()).isEqualTo("字段类型不匹配");
    }

    @Test
    void shouldHaveModelDefinitionConflictCode() {
        assertThat(ZLcEngineStatusCode.MODEL_DEFINITION_CONFLICT.getCode()).isEqualTo(6001);
        assertThat(ZLcEngineStatusCode.MODEL_DEFINITION_CONFLICT.getMessage()).isEqualTo("模型定义冲突");
    }

    @Test
    void shouldHaveWorkflowDefinitionErrorCode() {
        assertThat(ZLcEngineStatusCode.WORKFLOW_DEFINITION_ERROR.getCode()).isEqualTo(7001);
        assertThat(ZLcEngineStatusCode.WORKFLOW_DEFINITION_ERROR.getMessage()).isEqualTo("流程定义错误");
    }

    @Test
    void shouldHavePermissionDeniedCode() {
        assertThat(ZLcEngineStatusCode.PERMISSION_DENIED.getCode()).isEqualTo(8001);
        assertThat(ZLcEngineStatusCode.PERMISSION_DENIED.getMessage()).isEqualTo("权限校验失败");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcEngineStatusCode.fromCode(1001)).isEqualTo(ZLcEngineStatusCode.BIZ_EXCEPTION);
        assertThat(ZLcEngineStatusCode.fromCode(2001)).isEqualTo(ZLcEngineStatusCode.TOO_BUSY_EXCEPTION);
        assertThat(ZLcEngineStatusCode.fromCode(3001)).isEqualTo(ZLcEngineStatusCode.VALIDATE_EXCEPTION);
        assertThat(ZLcEngineStatusCode.fromCode(1002)).isEqualTo(ZLcEngineStatusCode.ENTITY_NOT_FOUND);
        assertThat(ZLcEngineStatusCode.fromCode(4001)).isEqualTo(ZLcEngineStatusCode.DATASOURCE_CONNECT_FAILED);
        assertThat(ZLcEngineStatusCode.fromCode(5001)).isEqualTo(ZLcEngineStatusCode.FIELD_TYPE_MISMATCH);
        assertThat(ZLcEngineStatusCode.fromCode(6001)).isEqualTo(ZLcEngineStatusCode.MODEL_DEFINITION_CONFLICT);
        assertThat(ZLcEngineStatusCode.fromCode(7001)).isEqualTo(ZLcEngineStatusCode.WORKFLOW_DEFINITION_ERROR);
        assertThat(ZLcEngineStatusCode.fromCode(8001)).isEqualTo(ZLcEngineStatusCode.PERMISSION_DENIED);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcEngineStatusCode.fromCode(9999)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcEngineStatusCode.valueOf("BIZ_EXCEPTION")).isEqualTo(ZLcEngineStatusCode.BIZ_EXCEPTION);
        assertThat(ZLcEngineStatusCode.valueOf("TOO_BUSY_EXCEPTION")).isEqualTo(ZLcEngineStatusCode.TOO_BUSY_EXCEPTION);
        assertThat(ZLcEngineStatusCode.valueOf("VALIDATE_EXCEPTION")).isEqualTo(ZLcEngineStatusCode.VALIDATE_EXCEPTION);
    }
}
