package com.zifang.z.report.common.schema;

/**
 * 数据源类型。M1 支持 MEMORY (演示/测试); JDBC(Mysql)/CSV 随 M1.5 接入。
 */
public enum DataSourceType {
    MEMORY,
    JDBC,
    CSV,
    HTTP,
    /** 内存 SQL 源: 把其它源的表搬进内存后, 数据单元是一条 join/聚合 SELECT */
    SQL
}
