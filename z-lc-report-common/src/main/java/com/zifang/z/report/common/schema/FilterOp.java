package com.zifang.z.report.common.schema;

/**
 * 过滤比较符。LIKE 在 M1 引擎中退化为字符串 contains 语义。
 */
public enum FilterOp {
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    IN,
    LIKE
}
