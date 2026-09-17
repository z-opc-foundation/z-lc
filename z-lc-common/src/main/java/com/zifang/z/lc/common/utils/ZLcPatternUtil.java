package com.zifang.z.lc.common.utils;

import java.util.regex.Pattern;

/**
 * 正则匹配工具 — 蒸馏自 ace-platform-core
 * {@code PatternUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台常用的字符串格式校验:
 * 纯数字判断、模型字段名格式校验等.
 *
 * <p>典型场景：
 * <ul>
 *   <li>模型字段名合法性校验 (仅允许小写字母+数字+下划线)</li>
 *   <li>表单输入值类型判断 (数字/文本)</li>
 *   <li>流程变量名格式校验</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcPatternUtil {

    private ZLcPatternUtil() {
    }

    /** 模型字段名正则: 小写字母开头 + 小写字母/数字/下划线. */
    private static final Pattern MODEL_FIELD_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_]*$");

    /** 纯数字正则. */
    private static final Pattern NUMERIC_PATTERN =
            Pattern.compile("^[0-9]+$");

    /**
     * 判断字符串是否为纯数字.
     *
     * @param str 待校验字符串
     * @return 是否为纯数字
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(str).matches();
    }

    /**
     * 判断字符串是否符合模型字段名格式.
     * 规则: 小写字母开头, 仅包含小写字母、数字、下划线.
     *
     * @param str 待校验字符串
     * @return 是否为合法模型字段名
     */
    public static boolean isModelField(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return MODEL_FIELD_PATTERN.matcher(str).matches();
    }
}
