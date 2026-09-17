package com.zifang.z.lc.common.enums;

/**
 * 打印模板类型枚举 — 蒸馏自 ace-platform-core
 * {@code PrintTemplateTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「打印模板」配置时选择模板类型：
 * <ul>
 *   <li>{@link #FORM} — 表单打印（按字段布局）</li>
 *   <li>{@link #TABLE} — 表格打印（按列表布局）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcPrintTemplateTypeEnum {

    /** 表单打印模板. */
    FORM(1, "表单"),

    /** 表格打印模板. */
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

    /**
     * 按 code 反查中文描述（未匹配返回 null）.
     */
    public static String getValueByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcPrintTemplateTypeEnum t : values()) {
            if (t.code.equals(code)) {
                return t.value;
            }
        }
        return null;
    }

    public static ZLcPrintTemplateTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcPrintTemplateTypeEnum t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
