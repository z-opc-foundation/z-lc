package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDefaultValueStrategy 单元测试
 *
 * @author zifang
 */
class ZLcDefaultValueStrategyTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcDefaultValueStrategy.values()).hasSize(8);
    }

    @Test
    void shouldHaveDefaultStrategies() {
        assertThat(ZLcDefaultValueStrategy.DEFAULT_STRATEGIES).hasSize(8);
    }

    @Test
    void shouldHaveCurrentTimeMillis() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_TIME_MILLIS.getName()).isEqualTo("当前时间戳");
        assertThat(ZLcDefaultValueStrategy.CURRENT_TIME_MILLIS.getScript()).isEqualTo("${CURRENT_TIME_MILLIS}");
    }

    @Test
    void shouldHaveCurrentTime() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_TIME.getName()).isEqualTo("当前时间");
        assertThat(ZLcDefaultValueStrategy.CURRENT_TIME.getScript()).isEqualTo("${CURRENT_TIME}");
    }

    @Test
    void shouldHaveOperator() {
        assertThat(ZLcDefaultValueStrategy.OPERATOR.getName()).isEqualTo("当前操作者");
        assertThat(ZLcDefaultValueStrategy.OPERATOR.getScript()).isEqualTo("${OPERATOR}");
    }

    @Test
    void shouldHaveOrgId() {
        assertThat(ZLcDefaultValueStrategy.ORG_ID.getName()).isEqualTo("当前操作者机构");
        assertThat(ZLcDefaultValueStrategy.ORG_ID.getScript()).isEqualTo("${ORG_ID}");
    }

    @Test
    void shouldHaveCurrentCampusId() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_CAMPUS_ID.getName()).isEqualTo("当前操作者院区");
        assertThat(ZLcDefaultValueStrategy.CURRENT_CAMPUS_ID.getScript()).isEqualTo("${CURRENT_CAMPUS_ID}");
    }

    @Test
    void shouldHaveCurrentAppCode() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_APP_CODE.getName()).isEqualTo("当前操作的应用标识");
        assertThat(ZLcDefaultValueStrategy.CURRENT_APP_CODE.getScript()).isEqualTo("${CURRENT_APP_CODE}");
    }

    @Test
    void shouldHaveCurrentModelCode() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_MODEL_CODE.getName()).isEqualTo("当前操作的数据模型标识");
        assertThat(ZLcDefaultValueStrategy.CURRENT_MODEL_CODE.getScript()).isEqualTo("${CURRENT_MODEL_CODE}");
    }

    @Test
    void shouldHaveCurrentTenantCode() {
        assertThat(ZLcDefaultValueStrategy.CURRENT_TENANT_CODE.getName()).isEqualTo("当前租户");
        assertThat(ZLcDefaultValueStrategy.CURRENT_TENANT_CODE.getScript()).isEqualTo("${CURRENT_TENANT_CODE}");
    }

    @Test
    void shouldFromName() {
        assertThat(ZLcDefaultValueStrategy.fromName("当前时间戳")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_TIME_MILLIS);
        assertThat(ZLcDefaultValueStrategy.fromName("当前时间")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_TIME);
        assertThat(ZLcDefaultValueStrategy.fromName("当前操作者")).isEqualTo(ZLcDefaultValueStrategy.OPERATOR);
        assertThat(ZLcDefaultValueStrategy.fromName("当前操作者机构")).isEqualTo(ZLcDefaultValueStrategy.ORG_ID);
        assertThat(ZLcDefaultValueStrategy.fromName("当前操作者院区")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_CAMPUS_ID);
        assertThat(ZLcDefaultValueStrategy.fromName("当前操作的应用标识")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_APP_CODE);
        assertThat(ZLcDefaultValueStrategy.fromName("当前操作的数据模型标识")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_MODEL_CODE);
        assertThat(ZLcDefaultValueStrategy.fromName("当前租户")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_TENANT_CODE);
    }

    @Test
    void shouldReturnNullForUnknownFromName() {
        assertThat(ZLcDefaultValueStrategy.fromName("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromName() {
        assertThat(ZLcDefaultValueStrategy.fromName(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcDefaultValueStrategy.valueOf("CURRENT_TIME_MILLIS")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_TIME_MILLIS);
        assertThat(ZLcDefaultValueStrategy.valueOf("CURRENT_TIME")).isEqualTo(ZLcDefaultValueStrategy.CURRENT_TIME);
        assertThat(ZLcDefaultValueStrategy.valueOf("OPERATOR")).isEqualTo(ZLcDefaultValueStrategy.OPERATOR);
    }
}
