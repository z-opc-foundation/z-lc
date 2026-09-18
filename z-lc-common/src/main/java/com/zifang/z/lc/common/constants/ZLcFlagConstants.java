package com.zifang.z.lc.common.constants;

/**
 * 通用布尔标记常量 — 蒸馏自 ace-platform-core
 * {@code FlagConstants} ({@code com.c2f.ace.core.common}).
 *
 * <p>提供 Integer 类型的 TRUE/FALSE 常量,
 * 用于数据库 boolean 字段的存取 (如 is_deleted, is_enabled 等).
 *
 * @author zifang
 */
public interface ZLcFlagConstants {

    /** 启用/存在. */
    Integer TRUE = 1;

    /** 停用/不存在. */
    Integer FALSE = 0;
}
