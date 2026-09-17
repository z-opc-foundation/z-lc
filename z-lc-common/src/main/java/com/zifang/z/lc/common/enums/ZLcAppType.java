package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 应用类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.AppType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于区分低代码平台应用与外部系统的来源：
 * <ul>
 *   <li>{@link #LC} — 低代码平台自建应用</li>
 *   <li>{@link #NC} — 外部 NC 系统（用友等）同步过来的应用</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcAppType {

    /** 低代码平台自建应用 */
    LC("LC"),

    /** 外部 NC 系统（用友等）同步过来的应用 */
    NC("NC");

    private final String code;

    ZLcAppType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 所有支持的应用类型. */
    public static final List<String> ALL_TYPES = Arrays.asList(LC.code, NC.code);

    /** 判断给定 code 是否是合法的应用类型. */
    public static boolean contains(String code) {
        return code != null && ALL_TYPES.contains(code);
    }

    public static ZLcAppType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcAppType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
