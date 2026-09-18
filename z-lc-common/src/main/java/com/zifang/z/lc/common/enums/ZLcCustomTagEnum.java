package com.zifang.z.lc.common.enums;

/**
 * 自定义标签枚举 — 蒸馏自 ace-platform-core
 * {@code CustomTag} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义低代码平台的自定义标签类型: 页面管理、打印模版管理.
 *
 * @author zifang
 */
public enum ZLcCustomTagEnum {

    PAGE_TEMPLATE("page_template", "页面管理"),
    PRINT_TEMPLATE("print_template", "打印模版管理");

    private final String code;
    private final String message;

    ZLcCustomTagEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ZLcCustomTagEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcCustomTagEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
