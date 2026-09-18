package com.zifang.z.lc.common.enums;

/**
 * 打印模板类型枚举 — 蒸馏自 ace-platform-core
 * {@code PrintTemplateTypeEnum} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义打印模板类型: 表单、表格.
 *
 * @author zifang
 */
public enum ZLcPrintTemplateTypeEnum {

    FORM(1, "表单"),
    TABLE(2, "表格");

    private final Integer code;
    private final String value;

    ZLcPrintTemplateTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcPrintTemplateTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcPrintTemplateTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
