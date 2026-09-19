package com.zifang.z.lc.common.dto.sign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuerySignResultExtendDTO 单元测试
 *
 * @author zifang
 */
class QuerySignResultExtendDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSignDataId() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        dto.setSignDataId("sign-123");
        assertThat(dto.getSignDataId()).isEqualTo("sign-123");
    }

    @Test
    void shouldHandleNullValues() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        assertThat(dto.getSignDataId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyString() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        dto.setSignDataId("");
        assertThat(dto.getSignDataId()).isEmpty();
    }

    @Test
    void shouldSetNullString() {
        QuerySignResultExtendDTO dto = new QuerySignResultExtendDTO();
        dto.setSignDataId(null);
        assertThat(dto.getSignDataId()).isNull();
    }
}