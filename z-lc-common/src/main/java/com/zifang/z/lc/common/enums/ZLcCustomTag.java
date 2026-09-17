package com.zifang.z.lc.common.enums;

/**
 * 自定义标签枚举 — 蒸馏自 ace-platform-core {@code CustomTag} ({@code com.c2f.ace.core.common}}.
 *
 * <p>用于标识低代码平台中"可由业务方自定义扩展"的核心模块，便于在管理后台 / 菜单 / 权限中
 * 按标签聚合显示.
 *
 * <ul>
 *   <li>{@link #PAGE_TEMPLATE} — 页面管理（业务方可自定义扩展页面模板）</li>
 *   <li>{@link #PRINT_TEMPLATE} — 打印模版管理（业务方可自定义扩展打印模板）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcCustomTag {

    /** 页面管理. */
    PAGE_TEMPLATE("page_template", "页面管理"),

    /** 打印模版管理. */
    PRINT_TEMPLATE("print_template", "打印模版管理");

    private final String code;
    private final String message;

    ZLcCustomTag(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ZLcCustomTag fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcCustomTag v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}