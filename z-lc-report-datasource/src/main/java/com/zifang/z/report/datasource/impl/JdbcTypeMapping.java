package com.zifang.z.report.datasource.impl;

import com.zifang.util.db.dialect.SqlType;
import com.zifang.z.report.common.schema.FieldType;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * z-util-jdbc 归一类型 → 引擎 FieldType，以及行值归一化。
 * <p>
 * 引擎只认 5 种列类型，比 JDBC 类型粗：TIMESTAMP/TIME/BINARY/OTHER 一律按字符串呈现，
 * DECIMAL 与浮点同为 DOUBLE（数值化由引擎按 Number 处理）。
 * <p>
 * 归一化约定 (引擎 Table 宽松存取的值域):
 * - 整型 → Long; 浮点/DECIMAL → 原样 (BigDecimal/Double, 引擎按 Number 数值化)
 * - DATE → LocalDate (FieldType.DATE 语义)
 * - DATETIME/TIMESTAMP/TIME → ISO 字符串 (STRING 语义, M1 不引入 LocalDateTime 维度)
 * - BLOB/BINARY → "[binary]" 占位
 */
final class JdbcTypeMapping {

    private JdbcTypeMapping() {
    }

    static FieldType toFieldType(SqlType sqlType) {
        switch (sqlType) {
            case INTEGER:
            case LONG:
                return FieldType.INT;
            case DOUBLE:
            case DECIMAL:
                return FieldType.DOUBLE;
            case BOOLEAN:
                return FieldType.BOOL;
            case DATE:
                return FieldType.DATE;
            default:
                return FieldType.STRING;
        }
    }

    /** ResultSet 取出的原始值 → 引擎值域 */
    static Object normalize(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDate) {
            return v; // mysql-connector-j DATE 已返回 LocalDate
        }
        if (v instanceof Date) {
            return ((Date) v).toLocalDate();
        }
        if (v instanceof Timestamp) {
            return v.toString();
        }
        if (v instanceof java.sql.Time) {
            return v.toString();
        }
        if (v instanceof byte[]) {
            return "[binary]";
        }
        if (v instanceof BigDecimal) {
            return v; // Number 子类, 引擎按数值处理
        }
        if (v instanceof java.time.LocalDateTime) {
            return v.toString();
        }
        return v;
    }
}
