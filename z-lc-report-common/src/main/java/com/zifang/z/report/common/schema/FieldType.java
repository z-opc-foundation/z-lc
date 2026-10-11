package com.zifang.z.report.common.schema;

/**
 * 字段数据类型 (契约层与引擎层共用)。
 * <p>
 * 引擎层 (z-report-dataset Table) 直接使用本枚举作为列类型,
 * 避免契约与引擎两套类型漂移。
 */
public enum FieldType {
    STRING,
    INT,
    DOUBLE,
    BOOL,
    DATE
}
