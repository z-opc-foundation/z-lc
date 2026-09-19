package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcHttpRequestType 单元测试
 *
 * @author zifang
 */
class ZLcHttpRequestTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcHttpRequestType.values()).hasSize(2);
    }

    @Test
    void shouldHaveGetCode() {
        assertThat(ZLcHttpRequestType.GET.getCode()).isEqualTo("get");
    }

    @Test
    void shouldHavePostCode() {
        assertThat(ZLcHttpRequestType.POST.getCode()).isEqualTo("post");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcHttpRequestType.fromCode("get")).isEqualTo(ZLcHttpRequestType.GET);
        assertThat(ZLcHttpRequestType.fromCode("post")).isEqualTo(ZLcHttpRequestType.POST);
    }

    @Test
    void shouldFromCodeBeCaseInsensitive() {
        assertThat(ZLcHttpRequestType.fromCode("GET")).isEqualTo(ZLcHttpRequestType.GET);
        assertThat(ZLcHttpRequestType.fromCode("POST")).isEqualTo(ZLcHttpRequestType.POST);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcHttpRequestType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcHttpRequestType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcHttpRequestType.valueOf("GET")).isEqualTo(ZLcHttpRequestType.GET);
        assertThat(ZLcHttpRequestType.valueOf("POST")).isEqualTo(ZLcHttpRequestType.POST);
    }
}
