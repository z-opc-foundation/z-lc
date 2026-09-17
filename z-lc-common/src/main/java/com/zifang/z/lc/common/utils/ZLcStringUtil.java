package com.zifang.z.lc.common.utils;

/**
 * 字符串工具 — 蒸馏自 ace-platform-core
 * {@code StringUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台常用的字符串转换能力:
 * 下划线 ↔ 驼峰命名互转、空值判断、字符串修剪等.
 *
 * <p>典型场景：
 * <ul>
 *   <li>数据库字段名 (snake_case) → Java 属性名 (camelCase)</li>
 *   <li>模型字段校验时的空值判断</li>
 *   <li>表单数据处理时的字符串修剪</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcStringUtil {

    private ZLcStringUtil() {
    }

    /** 下划线字符. */
    private static final char UNDERLINE = '_';

    /**
     * 下划线命名转驼峰命名.
     *
     * @param param              原始字符串 (如 "user_name")
     * @param capitalLettersFlag 首字母是否大写 (true → "UserName", false → "userName")
     * @return 驼峰命名字符串
     */
    public static String underlineToCamel(String param, Boolean capitalLettersFlag) {
        if (param == null || param.isEmpty()) {
            return param;
        }
        if (!param.contains("_")) {
            return param;
        }

        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(param.charAt(i));
            if (capitalLettersFlag && i == 0) {
                c = Character.toUpperCase(c);
            }
            if (c == UNDERLINE) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 驼峰命名转下划线命名.
     *
     * @param camelCase 驼峰字符串 (如 "userName")
     * @return 下划线字符串 (如 "user_name")
     */
    public static String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append(UNDERLINE);
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 修剪字符串值 — 非 String 类型直接返回.
     */
    public static Object trim(Object o) {
        if (o instanceof String) {
            return ((String) o).trim();
        }
        return o;
    }

    /**
     * 判断字符串是否为空 (null 或 "").
     */
    public static boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否非空.
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
