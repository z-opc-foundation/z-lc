package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWorkflowRuntimeStatusCode 单元测试
 *
 * @author zifang
 */
class ZLcWorkflowRuntimeStatusCodeTest {

    @Test
    void getCode_shouldReturn1001() {
        assertThat(ZLcWorkflowRuntimeStatusCode.BIZ_EXCEPTION.getCode()).isEqualTo(1001);
    }

    @Test
    void getMessage_shouldReturnCorrectValue() {
        assertThat(ZLcWorkflowRuntimeStatusCode.BIZ_EXCEPTION.getMessage()).isEqualTo("流程业务异常");
    }

    @Test
    void shouldImplementStatusCode() {
        assertThat(ZLcWorkflowRuntimeStatusCode.BIZ_EXCEPTION).isInstanceOf(StatusCode.class);
    }

    @Test
    void allEnumValues_shouldHaveUniqueCodes() {
        ZLcWorkflowRuntimeStatusCode[] values = ZLcWorkflowRuntimeStatusCode.values();
        long uniqueCodes = java.util.Arrays.stream(values)
                .mapToLong(ZLcWorkflowRuntimeStatusCode::getCode)
                .distinct()
                .count();
        assertThat(uniqueCodes).isEqualTo(values.length);
    }
}
