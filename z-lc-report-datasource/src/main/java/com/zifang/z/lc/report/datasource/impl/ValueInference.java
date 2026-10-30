package com.zifang.z.lc.report.datasource.impl;

import com.zifang.z.lc.report.common.schema.FieldType;

import java.math.BigDecimal;

/**
 * 值类型推断 (Memory/Csv/内存 SQL 数据源共用): 首行/单值 → FieldType。
 * 规则: Boolean→BOOL, 浮点→DOUBLE, 整数→INT, 其余(含 null)→STRING。
 * BigDecimal 一律→DOUBLE：DECIMAL 基表/DECIMAL 聚合列才产 BigDecimal，聚合语义就是数值列；
 * （2026-10-30：z-util 1.2.11 起整数 SUM 返回 Long，走下面 Number→INT 分支，不再到这。）
 */
final class ValueInference {

    private ValueInference() {
    }

    static FieldType infer(Object v) {
        if (v == null) {
            return FieldType.STRING;
        }
        if (v instanceof Boolean) {
            return FieldType.BOOL;
        }
        if (v instanceof Float || v instanceof Double) {
            return FieldType.DOUBLE;
        }
        if (v instanceof BigDecimal) {
            return FieldType.DOUBLE;
        }
        if (v instanceof Number) {
            return FieldType.INT;
        }
        return FieldType.STRING;
    }

    /** 字符串值的尽力数值化 (CSV 无类型声明: 布尔→Boolean, 纯整数→Long, 浮点→Double, 其余原样) */
    static Object parse(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)) {
            return Boolean.parseBoolean(t);
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException ignore) {
            // fall through
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ignore) {
            // fall through
        }
        return s;
    }
}
