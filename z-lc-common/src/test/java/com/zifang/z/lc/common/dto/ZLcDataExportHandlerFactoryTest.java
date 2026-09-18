package com.zifang.z.lc.common.dto;

import com.zifang.z.lc.common.enums.ZLcDataFileTypeEnum;
import com.zifang.z.lc.common.utils.ZLcDataExportHandlerFactory;
import com.zifang.z.lc.common.utils.ZLcIDataExportHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataExportHandlerFactory 单元测试
 *
 * @author zifang
 */
class ZLcDataExportHandlerFactoryTest {

    @Test
    void create_shouldReturnNull_WhenExcelType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(ZLcDataFileTypeEnum.EXCEL.getCode().toString());
        assertThat(handler).isNull();
    }

    @Test
    void create_shouldReturnNull_WhenCSVType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(ZLcDataFileTypeEnum.CSV.getCode().toString());
        assertThat(handler).isNull();
    }

    @Test
    void create_shouldReturnNull_WhenNull() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create(null);
        assertThat(handler).isNull();
    }

    @Test
    void create_shouldReturnNull_WhenUnknownType() {
        ZLcIDataExportHandler handler = ZLcDataExportHandlerFactory.create("unknown");
        assertThat(handler).isNull();
    }
}
