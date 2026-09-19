package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcIDataExportHandler 单元测试
 *
 * @author zifang
 */
class ZLcIDataExportHandlerTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcIDataExportHandler.class.isInterface()).isTrue();
    }

    @Test
    void shouldHaveHandleMethod() throws NoSuchMethodException {
        assertThat(ZLcIDataExportHandler.class.getMethod("handle")).isNotNull();
    }

    @Test
    void shouldImplementHandleMethod() {
        ZLcIDataExportHandler handler = () -> "http://example.com/exported.xlsx";

        String result = handler.handle();
        assertThat(result).isEqualTo("http://example.com/exported.xlsx");
    }

    @Test
    void shouldHandleExceptionInHandler() {
        ZLcIDataExportHandler handler = () -> {
            throw new RuntimeException("Export failed");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(handler::handle)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Export failed");
    }
}