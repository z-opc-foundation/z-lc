package com.zifang.z.report.common.schema;

/**
 * 聚合类型 (M1: SUM/COUNT/AVG/MIN/MAX)。
 * SUM 对全整型列返回 Long, 含浮点返回 Double; COUNT 返回 Long。
 */
public enum AggType {
    SUM,
    COUNT,
    AVG,
    MIN,
    MAX
}
