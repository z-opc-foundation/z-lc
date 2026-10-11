package com.zifang.z.report.datasource.spi;

import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;

import java.util.List;
import java.util.Map;

/**
 * 数据源 SPI: 一切数据入口的统一抽象。
 * <p>
 * 实现约定:
 * - read 返回全量内存 Table (M1 语义); 下推优化 (SQL 聚合/过滤) 由具体实现自行裁剪
 * - table 为源内数据单元标识: JDBC 为表名, CSV 为文件标识, HTTP 为接口标识
 * - 实现必须无状态可复用 (连接管理自行封装)
 */
public interface ReportDataSource {

    /** 数据源定义 (id/name/type/properties) */
    DataSourceDef def();

    /** 源内数据单元清单 (元数据探测, 供 AI 生成/设计器选择) */
    List<String> tables();

    /** 字段元数据探测: 源内数据单元 → 列类型 */
    Map<String, FieldType> schema(String table);

    /** 读取源内数据单元为内存 Table */
    Table read(String table);
}
