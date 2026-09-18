package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 导入模板表头定义 — 蒸馏自 ace-platform-core
 * {@code ImportTemplateExcelHeader} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>定义数据导入模板的 Excel 表头列名, 用于校验上传文件的表头是否正确.
 * 蒸馏时移除了 ace 对 Lombok @Getter / Apache POI / BusinessException 的依赖,
 * 改为纯常量定义. 表头校验逻辑由调用方按需实现.
 *
 * @author zifang
 */
public enum ZLcImportTemplateExcelHeader {

    /** 标签值导入模板表头: 列名 + 标签值. */
    LABEL_VALUE(Arrays.asList("列名", "标签值"));

    private final List<String> headerList;

    ZLcImportTemplateExcelHeader(List<String> headerList) {
        this.headerList = headerList;
    }

    /**
     * 获取表头列名列表.
     *
     * @return 表头列名列表
     */
    public List<String> getHeaderList() {
        return headerList;
    }
}
