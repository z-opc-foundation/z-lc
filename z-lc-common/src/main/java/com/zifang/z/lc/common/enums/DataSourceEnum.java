package com.zifang.z.lc.common.enums;

/**
 * 数据源方言枚举.
 *
 * <p>蒸馏自 ace-platform-core {@code DataSourceEnum}
 * （{@code com.c2f.ace.core.enums}），字段语义完全对齐.
 *
 * <p>用于 {@code DataModelConvertSqlDispatch} 路由：根据数据源 type 字段
 * 决定调用哪个方言的策略实现.
 *
 * @author xuhf (distilled by zifang)
 */
public enum DataSourceEnum {

    /**
     * MySQL / MariaDB.
     */
    DATA_SOURCE_MYSQL("mysql", "mysql"),

    /**
     * Apache Doris（OLAP 列存）.
     */
    DATA_SOURCE_DORIS("doris", "doris"),

    /**
     * 达梦数据库（国产）.
     */
    DATA_SOURCE_DM("dm", "达梦数据库"),

    /**
     * StarRocks（OLAP）.
     */
    DATA_SOURCE_STARROCKS("sr", "starrocks"),

    /**
     * 人大金仓（国产）.
     */
    DATA_SOURCE_KINGBASE("kingbase", "人大金仓");

    /**
     * 数据库类型字符串（{@code data_source.type} 字段值）.
     */
    private final String type;

    /**
     * 中文显示名称.
     */
    private final String name;

    DataSourceEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 按 type 字符串查找枚举值（未匹配返回 null）.
     */
    public static DataSourceEnum getEnumByType(String type) {
        if (type == null) {
            return null;
        }
        for (DataSourceEnum e : values()) {
            if (e.getType().equals(type)) {
                return e;
            }
        }
        return null;
    }
}
