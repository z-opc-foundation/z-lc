package com.zifang.z.lc.common.utils;

/**
 * 数据导出处理器接口 — 蒸馏自 ace-platform-core
 * {@code IDataExportHandler} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>定义数据导出任务的统一处理接口.
 * 具体实现类 (如 ExcelExportHandler) 负责执行导出逻辑并返回文件 URL.
 *
 * @author zifang
 */
public interface ZLcIDataExportHandler {

    /**
     * 执行导出处理.
     *
     * @return 导出后的文件 URL
     */
    String handle();
}
