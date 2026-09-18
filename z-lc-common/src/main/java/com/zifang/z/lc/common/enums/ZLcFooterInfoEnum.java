package com.zifang.z.lc.common.enums;

/**
 * 页脚信息枚举 — 蒸馏自 ace-platform-core
 * {@code FooterInfoEnum} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义打印/导出页脚信息类型: 组织名称、账套名称、登录用户.
 *
 * @author zifang
 */
public enum ZLcFooterInfoEnum {

    ORG_NAME(1, "组织名称"),
    ACCOUNT_NAME(2, "账套名称"),
    USER_NAME(3, "登录用户");

    private final Integer code;
    private final String value;

    ZLcFooterInfoEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcFooterInfoEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcFooterInfoEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
