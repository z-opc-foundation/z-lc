package com.zifang.z.lc.common.enums;

/**
 * 数据文件类型枚举 — 蒸馏自 ace-platform-core
 * {@code DataFileTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「数据导出」时选择输出文件格式 —
 * EXCEL / CSV / PDF / 打印模板.
 *
 * @author zifang
 */
public enum ZLcDataFileTypeEnum {

    /** Excel 文件 (xlsx). */
    EXCEL(1, "xlsx"),

    /** CSV 文件. */
    CSV(2, "csv"),

    /** PDF 文件. */
    PDF(3, "pdf"),

    /** 打印模板（输出走打印机） */
    PRINT(4, "打印");

    private final Integer code;
    private final String value;

    ZLcDataFileTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcDataFileTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcDataFileTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
