package com.zifang.z.lc.common.enums;

import java.util.Objects;

/**
 * 通用布尔枚举 — 蒸馏自 ace-platform-core
 * {@code BooleanEnum} ({@code com.c2f.ace.core.model}).
 *
 * <p>提供 Integer 类型的 YES/NO 布尔枚举, 用于模型字段的启用/禁用等场景.
 * 蒸馏时移除了 ace 对 Lombok @Getter/@AllArgsConstructor 的依赖,
 * 改为手写 getter 实现.
 *
 * @author zifang
 */
public enum ZLcBooleanEnum {

    YES(1, "是"),
    NO(0, "否");

    private final Integer value;
    private final String name;

    ZLcBooleanEnum(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据名称获取枚举值.
     *
     * @param name 名称
     * @return 对应枚举值; 找不到时返回 NO
     */
    public static ZLcBooleanEnum getByName(String name) {
        return Objects.equals(YES.getName(), name) ? YES : NO;
    }

    /**
     * 根据 value 获取枚举值.
     *
     * @param value 整数值
     * @return 对应枚举值; 找不到时返回 NO
     */
    public static ZLcBooleanEnum getByValue(Integer value) {
        for (ZLcBooleanEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return NO;
    }
}
