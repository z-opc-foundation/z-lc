package com.zifang.z.lc.common.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据库列类型枚举 — 蒸馏自 ace-platform-core
 * {@code ColumnTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台扫描数据源时识别表字段类型 — 与 {@code DataSourceTableColumnDTO}
 * 对应. 共 25 种标准 SQL 列类型.
 *
 * <p>除了枚举本身，z-lc 蒸馏还提供了 3 个查询辅助：
 * <ul>
 *   <li>{@link #ALL_TYPE_CODES} — 所有列类型字符串（用于 SQL 解析/校验）</li>
 *   <li>{@link #DEFAULT_LENGTH_MAP} — 默认字段长度（int=10 / varchar=255 / decimal=14,2 等）</li>
 *   <li>{@link #NEVER_LENGTH_TYPE} — 不允许指定长度的类型（date/time/json/blob/text 等）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcColumnTypeEnum {

    BIGINT(1, "bigint"),
    BIT(2, "bit"),
    BLOB(3, "blob"),
    CHAR(4, "char"),
    DATE(5, "date"),
    DATETIME(6, "datetime"),
    DECIMAL(7, "decimal"),
    DOUBLE(8, "double"),
    ENUM(9, "enum"),
    FLOAT(10, "float"),
    INT(11, "int"),
    JSON(12, "json"),
    LONGBLOB(13, "longblob"),
    LONGTEXT(14, "longtext"),
    MEDIUMBLOB(15, "mediumblob"),
    MEDIUMTEXT(16, "mediumtext"),
    SET(17, "set"),
    SMALLINT(18, "smallint"),
    TEXT(19, "text"),
    TIME(20, "time"),
    TIMESTAMP(21, "timestamp"),
    TINYINT(22, "tinyint"),
    TINYTEXT(23, "tinytext"),
    VARBINARY(24, "varbinary"),
    VARCHAR(25, "varchar");

    private final int code;
    private final String fieldTypeName;

    ZLcColumnTypeEnum(int code, String fieldTypeName) {
        this.code = code;
        this.fieldTypeName = fieldTypeName;
    }

    public int getCode() {
        return code;
    }

    public String getFieldTypeName() {
        return fieldTypeName;
    }

    /** 所有列类型字符串（不可变列表） */
    public static final List<String> ALL_TYPE_CODES;

    /** 默认字段长度（key = 列类型字符串，value = 默认长度） */
    public static final Map<String, String> DEFAULT_LENGTH_MAP;

    /** 不允许指定长度的类型（key = 列类型字符串，value = true） */
    public static final Set<String> NEVER_LENGTH_TYPE;

    /** 列类型正则（用于解析 {@code varchar(255)} 等带长度的类型字符串） */
    public static final String COLUMN_TYPE_REGEX = "[a-z]+(\\([1-9]+[0]?(,?[1-9]+)?\\))?";

    static {
        List<String> codes = new ArrayList<>();
        for (ZLcColumnTypeEnum e : values()) {
            codes.add(e.getFieldTypeName());
        }
        ALL_TYPE_CODES = Collections.unmodifiableList(codes);

        Map<String, String> lengthMap = new HashMap<>();
        lengthMap.put("int", "10");
        lengthMap.put("varchar", "255");
        lengthMap.put("double", "14,2");
        lengthMap.put("float", "14,2");
        lengthMap.put("bigint", "19");
        lengthMap.put("smallint", "5");
        lengthMap.put("decimal", "14,2");
        DEFAULT_LENGTH_MAP = Collections.unmodifiableMap(lengthMap);

        Set<String> noLength = new HashSet<>(Arrays.asList(
                "enum", "set", "json", "date", "time", "datetime",
                "tinyblob", "blob", "longblob", "mediumblob",
                "tinytext", "text", "mediumtext", "longtext"));
        NEVER_LENGTH_TYPE = Collections.unmodifiableSet(noLength);
    }

    /**
     * 按 code 数值查找（未匹配返回 null）.
     */
    public static ZLcColumnTypeEnum fromCode(int code) {
        for (ZLcColumnTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 按 fieldTypeName 字符串查找（大小写不敏感，未匹配返回 null）.
     */
    public static ZLcColumnTypeEnum fromTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        for (ZLcColumnTypeEnum e : values()) {
            if (e.fieldTypeName.equalsIgnoreCase(typeName)) {
                return e;
            }
        }
        return null;
    }
}
