package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.enums.ZLcDataFileTypeEnum;

/**
 * 数据导出处理器工厂 — 蒸馏自 ace-platform-core
 * {@code DataExportHandlerFactory} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>根据文件类型创建对应的 {@link ZLcIDataExportHandler} 实现.
 * 具体的 Handler 类 (如 ExcelExportHandler) 由业务层实现并注册.
 *
 * <p>典型场景：
 * <ul>
 *   <li>根据用户选择的导出格式 (Excel/CSV/PDF) 创建对应的处理器</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcDataExportHandlerFactory {

    private ZLcDataExportHandlerFactory() {
    }

    /**
     * 创建导出处理器实例.
     *
     * @param fileType 文件类型编码 (Excel/CSV/PDF)
     * @return 对应的处理器; 不支持的类型返回 null
     */
    public static ZLcIDataExportHandler create(String fileType) {
        if (fileType == null) {
            return null;
        }
        // 业务层需根据实际文件类型注册处理器
        // 此处提供默认路由逻辑框架
        if (ZLcDataFileTypeEnum.EXCEL.getCode().equals(fileType)
                || ZLcDataFileTypeEnum.CSV.getCode().equals(fileType)) {
            // Excel/CSV 走同一个处理器
            return null; // 业务层注入实际实现
        }
        return null;
    }
}
