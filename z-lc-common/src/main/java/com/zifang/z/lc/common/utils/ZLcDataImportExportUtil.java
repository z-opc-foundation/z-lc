package com.zifang.z.lc.common.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 数据导入导出工具 — 蒸馏自 ace-platform-core
 * {@code DataImporAndExportUtil} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>提供 Excel 数据格式化、表头构建等通用能力.
 * 蒸馏时移除了 ace 对 hutool DateUtil / EasyExcel ListUtils / BusinessException 的依赖,
 * 改为纯 JDK + Jackson 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>导入数据时按字段类型做格式化 (数字/日期/文本)</li>
 *   <li>导出数据时构建 Excel 表头</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcDataImportExportUtil {

    private ZLcDataImportExportUtil() {
    }

    /** Jackson ObjectMapper (复用实例). */
    public static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /** 支持的日期格式 (按优先级排列). */
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyyMMddHHmmssSSS",
            "yyyy-MM-dd HH:mm:ss",
            "yyyyMMddHHmmss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyyMMdd",
            "yyyy/MM/dd",
            "yyyy-MM",
            "yyyyMM",
            "MM/yyyy"
    };

    private static final DateTimeFormatter NORM_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 根据字段类型格式化值.
     *
     * @param fieldType 字段类型 (Number/Time/Text)
     * @param fieldVal  原始值
     * @return 格式化后的值
     * @throws IllegalArgumentException 数字或日期解析失败时
     */
    public static Object fieldDataFormat(String fieldType, Object fieldVal) {
        if (fieldVal == null) {
            return null;
        }

        if ("Number".equals(fieldType)) {
            try {
                String str = fieldVal.toString();
                // 负数括号表示法: (100) → -100
                if (str.startsWith("(") && str.endsWith(")")) {
                    str = "-" + str.substring(1, str.length() - 1);
                }
                // 移除千分位逗号
                str = str.replace(",", "");
                // 百分比转换
                if (str.endsWith("%")) {
                    BigDecimal bd = new BigDecimal(str.substring(0, str.length() - 1));
                    return bd.divide(new BigDecimal(100), 4, BigDecimal.ROUND_HALF_UP);
                }
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("数字/金额类型解析异常，解析对象: " + fieldVal);
            }
        }

        if ("Time".equals(fieldType)) {
            String str = fieldVal.toString();
            for (String pattern : DATE_PATTERNS) {
                try {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern(pattern);
                    java.time.temporal.TemporalAccessor temporal = fmt.parse(str);
                    LocalDateTime ldt;
                    // 处理只有日期没有时间的情况
                    if (pattern.contains("HH")) {
                        ldt = LocalDateTime.from(temporal);
                    } else {
                        ldt = java.time.LocalDate.from(temporal).atStartOfDay();
                    }
                    return ldt.format(NORM_DATETIME);
                } catch (DateTimeParseException ignored) {
                    // 继续尝试下一个格式
                }
            }
            throw new IllegalArgumentException("日期类型解析异常，解析对象: " + str);
        }

        return fieldVal;
    }

    /**
     * 构建 Excel 表头 (使用字段描述).
     *
     * @param fieldDescList 字段描述列表
     * @return EasyExcel 格式的表头 (List&lt;List&lt;String&gt;&gt;)
     */
    public static List<List<String>> buildSimpleExcelHead(List<String> fieldDescList) {
        List<List<String>> result = new ArrayList<>();
        if (fieldDescList != null) {
            for (String desc : fieldDescList) {
                List<String> head = new ArrayList<>();
                head.add(desc);
                result.add(head);
            }
        }
        return result;
    }

    /**
     * 构建 Excel 表头 (支持自定义列标题覆盖).
     *
     * @param fieldCodeList   字段编码列表 (位点)
     * @param fieldDescMap    字段编码 → 字段描述映射
     * @param requiredSet     必填字段编码集合
     * @param customTitles    自定义表头 (位点 → 标题); 未包含的位点用默认描述
     * @return EasyExcel 格式的表头
     */
    public static List<List<String>> buildExcelHead(List<String> fieldCodeList,
                                                    Map<String, String> fieldDescMap,
                                                    Set<String> requiredSet,
                                                    Map<String, String> customTitles) {
        List<List<String>> result = new ArrayList<>();
        if (fieldCodeList == null) {
            return result;
        }
        for (String fieldCode : fieldCodeList) {
            String title;
            if (customTitles != null && customTitles.containsKey(fieldCode)) {
                title = customTitles.get(fieldCode);
            } else {
                String desc = fieldDescMap != null ? fieldDescMap.get(fieldCode) : fieldCode;
                if (desc == null) {
                    desc = fieldCode;
                }
                boolean required = requiredSet != null && requiredSet.contains(fieldCode);
                title = required ? desc + "（必填）" : desc;
            }
            List<String> head = new ArrayList<>();
            head.add(title);
            result.add(head);
        }
        return result;
    }
}
