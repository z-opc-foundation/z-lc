package com.zifang.z.report.dataset;

import com.zifang.z.report.dataset.engine.Table;

/**
 * 源内数据单元解析函数 (依赖倒置缝): dataset 模块不依赖 datasource 模块,
 * 由装配层把 "sourceId+table → 内存 Table" 注入进来 (JDBC 源即 read)。
 */
@FunctionalInterface
public interface TableSourceResolver {

    /** 解析数据源 sourceId 内的数据单元 table 为内存 Table */
    Table resolve(String sourceId, String table);
}
