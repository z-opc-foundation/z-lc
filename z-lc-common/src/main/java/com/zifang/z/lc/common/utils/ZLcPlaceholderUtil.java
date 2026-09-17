package com.zifang.z.lc.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符替换工具 — 蒸馏自 ace-platform-core
 * {@code PlaceholderUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>用于 BPMN 流程变量替换: 将字符串中的 {@code ${variableName}}
 * 占位符替换为 {@code Map} 中对应的值.
 *
 * <p>典型场景：
 * <ul>
 *   <li>GenericHttpServiceCallListener 中 URL / RequestBody 变量替换</li>
 *   <li>GenericMsgNotifyListener 中消息标题 / 正文变量替换</li>
 *   <li>审批人逻辑中动态计算审批人表达式替换</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcPlaceholderUtil {

    private ZLcPlaceholderUtil() {
    }

    /** 匹配 ${...} 占位符. */
    private static final Pattern PATTERN = Pattern.compile("\\$\\{(.*?)}");

    /**
     * 替换字符串中的 {@code ${key}} 占位符为 context 中对应的值.
     *
     * @param str     原始字符串 (可含 {@code ${xxx}} 占位符)
     * @param context 占位符上下文 (key → value)
     * @return 替换后的字符串; 若 str 为 null 则返回 null
     */
    public static String markReplace(String str, Map<String, Object> context) {
        if (str == null || context == null || context.isEmpty()) {
            return str;
        }
        Matcher m = PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = context.get(key);
            if (val != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(val)));
            }
            // key 不存在时保留原始占位符
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
