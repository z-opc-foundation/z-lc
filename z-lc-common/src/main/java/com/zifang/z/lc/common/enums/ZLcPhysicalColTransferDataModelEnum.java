package com.zifang.z.lc.common.enums;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 物理列 ↔ 数据模型字段类型 ↔ Java 属性类型映射枚举 — 蒸馏自 ace-platform-core
 * {@code PhysicalColTransferDataModelEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>三段映射关系，用于数据源扫描 → DataModel 字段生成 → Java POJO 生成的链路:
 * <ul>
 *   <li>DB 物理列类型（{@code bigint / varchar / datetime} ...）</li>
 *   <li>DataModel 字段类型（{@code Text / Number / Time}）— 与 {@link ZLcModelFieldType} 对应</li>
 *   <li>Java 属性类型（{@code String / Long / Integer / LocalDate} ...）</li>
 * </ul>
 *
 * <p>25 种映射覆盖 MySQL/PostgreSQL/H2 等主流 DB 的标准列类型 —
 * 引擎扫描到陌生类型时默认按 {@code Text} 处理（与 ace 默认行为一致）.
 *
 * @author zifang
 */
public enum ZLcPhysicalColTransferDataModelEnum {

    BIGINT_UNSIGNED("bigint unsigned", "Number", "Long", Long.class),
    BIGINT("bigint", "Number", "Long", Long.class),
    BIT("bit", "Number", "Integer", Integer.class),
    BLOB("blob", null, null, null),
    CHAR("char", "Text", "String", String.class),
    DATE("date", "Time", "LocalDate", LocalDate.class),
    DATETIME("datetime", "Time", "LocalDateTime", LocalDateTime.class),
    DECIMAL("decimal", "Number", "Double", Double.class),
    DOUBLE("double", "Number", "Double", Double.class),
    ENUM("enum", null, null, null),
    FLOAT("float", "Number", "Float", Float.class),
    INT("int", "Number", "Integer", Integer.class),
    JSON("json", "Text", "String", String.class),
    LONGBLOB("longblob", "Text", "String", String.class),
    LONGTEXT("longtext", "Text", "String", String.class),
    MEDIUMBLOB("mediumblob", "Text", "String", String.class),
    MEDIUMTEXT("mediumtext", "Text", "String", String.class),
    SET("set", "Text", "String", String.class),
    SMALLINT("smallint", "Number", "Integer", Integer.class),
    TEXT("text", "Text", "String", String.class),
    TIME("time", "Time", "LocalDateTime", LocalDateTime.class),
    TIMESTAMP("timestamp", "Time", "LocalDateTime", LocalDateTime.class),
    TINYINT("tinyint", "Number", "Integer", Integer.class),
    TINYTEXT("tinytext", "Text", "String", String.class),
    VARBINARY("varbinary", "Text", "String", String.class),
    VARCHAR("varchar", "Text", "String", String.class),
    BINARY("binary", "Number", "Integer", Integer.class);

    private final String physicalColumnType;
    private final String dataFieldType;
    private final String javaAttribute;
    private final Class<?> clazz;

    ZLcPhysicalColTransferDataModelEnum(String physicalColumnType, String dataFieldType,
                                        String javaAttribute, Class<?> clazz) {
        this.physicalColumnType = physicalColumnType;
        this.dataFieldType = dataFieldType;
        this.javaAttribute = javaAttribute;
        this.clazz = clazz;
    }

    public String getPhysicalColumnType() {
        return physicalColumnType;
    }

    public String getDataFieldType() {
        return dataFieldType;
    }

    public String getJavaAttribute() {
        return javaAttribute;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 按物理列类型字符串反查 DataModel 字段类型（包含匹配 — 兼容带长度的列如 {@code varchar(255)}）.
     * 未匹配默认返回 {@code "Text"} — 与 ace 默认行为一致.
     */
    public static String getDataFieldTypeByPhysicalColumnType(String physicalColumnType) {
        if (physicalColumnType == null) {
            return "Text";
        }
        for (ZLcPhysicalColTransferDataModelEnum e : values()) {
            if (physicalColumnType.toLowerCase().contains(e.getPhysicalColumnType().toLowerCase())) {
                return e.getDataFieldType();
            }
        }
        return "Text";
    }

    /**
     * 按 DataModel 字段类型反查 Java 属性 Class（用于代码生成）.
     * 未匹配默认返回 {@code String.class} — 与 ace 默认行为一致.
     */
    public static Class<?> getClassTypeByModelType(String modelType) {
        if (modelType == null) {
            return String.class;
        }
        for (ZLcPhysicalColTransferDataModelEnum e : values()) {
            if (e.dataFieldType != null && e.dataFieldType.equals(modelType)) {
                return e.getClazz();
            }
        }
        return String.class;
    }

    /**
     * 一次性查询：把物理列类型转为 (DataModel 字段类型 + Java Class) 二元组.
     *
     * @return key = dataFieldType, value = Java Class
     */
    public static Map<String, Class<?>> transfer(String physicalColumnType) {
        Map<String, Class<?>> result = new HashMap<>();
        String dataFieldType = getDataFieldTypeByPhysicalColumnType(physicalColumnType);
        Class<?> clazz = getClassTypeByModelType(dataFieldType);
        result.put(dataFieldType, clazz);
        return result;
    }
}
