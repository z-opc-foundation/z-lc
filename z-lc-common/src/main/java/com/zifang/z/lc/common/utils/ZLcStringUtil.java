package com.zifang.z.lc.common.utils;

/**
 * 字符串工具 — 蒸馏自 ace-platform-core
 * {@code StringUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台常用的字符串转换能力:
 * 下划线 ↔ 驼峰命名互转、字符串修剪.
 *
 * <p>典型场景：
 * <ul>
 *   <li>数据库字段名 (snake_case) → Java 属性名 (camelCase)</li>
 *   <li>表单数据处理时的字符串修剪</li>
 * </ul>
 *
 * <p><b>已收口到 z-util</b>：本类原有 isBlank / isNotBlank 与
 * {@code com.zifang.util.core.lang.StringUtil#isEmpty(String)} /
 * {@code isNotEmpty(String)} 逐输入实测等价
 * （注意是 isEmpty 而不是同名 isBlank —— z-util 的 isBlank 把纯空白也算空,
 * 本地 isBlank 只判 null 与 ""）, 已删除, 调用点直指 z-util.
 *
 * <p><b>不能收口的部分</b>（与 z-util 实测有语义差异, 差异由
 * {@code ZLcUtilDedupEquivalenceTest} 锁定）：
 * <ul>
 *   <li>underlineToCamel：无下划线时原样返回（z-util
 *       underlineToLittleCamelCase 会整体改大小写, "userName"→"username"）;
 *       null 入参返回 null（z-util 抛 IllegalArgumentException）;
 *       前导/连续下划线行为不同（"_name"→"Name" vs "name"）.</li>
 *   <li>camelToUnderline：每个大写前插分隔符（z-util toUnderScoreCase
 *       对连续大写合并, "userID"→"user_i_d" vs "user_id"）.</li>
 *   <li>trim(Object)：z-util 无对应方法.</li>
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
     * <p>z-util 无对应方法, 保留在本地.
     */
    public static Object trim(Object o) {
        if (o instanceof String) {
            return ((String) o).trim();
        }
        return o;
    }
}
