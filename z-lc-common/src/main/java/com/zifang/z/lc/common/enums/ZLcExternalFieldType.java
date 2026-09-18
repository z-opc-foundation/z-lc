package com.zifang.z.lc.common.enums;

import java.util.Objects;

/**
 * 外部模型字段类型枚举 — 蒸馏自 ace-platform-core
 * {@code ExternalFieldType} ({@code com.c2f.ace.core.model}).
 *
 * <p>定义低代码平台外部模型的字段类型, 包含类型编码、名称和数据库字段长度映射.
 * 蒸馏时移除了 ace 对 Lombok / AceStatusCode / BusinessException 的依赖,
 * 改为手写 getter + RuntimeException 实现.
 *
 * @author zifang
 */
public enum ZLcExternalFieldType {

    TEXT(1, "文本", "varchar(255)"),
    NUMBER(2, "数值", "bigint(20)"),
    DATE(3, "日期", "datetime"),
    DECIMAL(4, "金额", "decimal(22,2)");

    private final Integer code;
    private final String name;
    private final String fieldLength;

    ZLcExternalFieldType(Integer code, String name, String fieldLength) {
        this.code = code;
        this.name = name;
        this.fieldLength = fieldLength;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFieldLength() {
        return fieldLength;
    }

    /**
     * 根据名称获取枚举值.
     *
     * @param name 类型名称
     * @return 对应枚举值
     * @throws IllegalArgumentException 找不到时抛出
     */
    public static ZLcExternalFieldType getByName(String name) {
        for (ZLcExternalFieldType value : values()) {
            if (Objects.equals(value.getName(), name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("错误的字段类型: " + name);
    }

    /**
     * 根据编码获取枚举值.
     *
     * @param code 类型编码
     * @return 对应枚举值; 找不到时返回 TEXT
     */
    public static ZLcExternalFieldType getByCode(Integer code) {
        for (ZLcExternalFieldType value : values()) {
            if (Objects.equals(value.getCode(), code)) {
                return value;
            }
        }
        return TEXT;
    }
}
