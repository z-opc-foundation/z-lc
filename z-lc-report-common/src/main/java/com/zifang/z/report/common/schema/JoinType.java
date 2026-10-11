package com.zifang.z.report.common.schema;

/**
 * Join 类型。M1 引擎支持 INNER / LEFT;
 * RIGHT 可由 LEFT 交换两表获得, FULL 暂不支持。
 */
public enum JoinType {
    INNER,
    LEFT
}
