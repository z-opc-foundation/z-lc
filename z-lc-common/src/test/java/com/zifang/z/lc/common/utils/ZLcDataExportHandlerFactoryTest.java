package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.enums.ZLcDataFileTypeEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataExportHandlerFactory 单元测试
 *
 * @author zifang
 */
class ZLcDataExportHandlerFactoryTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcDataExportHandlerFactory> constructor = ZLcDataExportHandlerFactory.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullFileType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(null);
        assertThat(handler).isNull();
    }

    @Test
    void shouldReturnNullForExcelFileType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(String.valueOf(ZLcDataFileTypeEnum.EXCEL.getCode()));
        assertThat(handler).isNull();
    }

    @Test
    void shouldReturnNullForCsvFileType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(String.valueOf(ZLcDataFileTypeEnum.CSV.getCode()));
        assertThat(handler).isNull();
    }

    @Test
    void shouldReturnNullForUnknownFileType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create("unknown");
        assertThat(handler).isNull();
    }
}