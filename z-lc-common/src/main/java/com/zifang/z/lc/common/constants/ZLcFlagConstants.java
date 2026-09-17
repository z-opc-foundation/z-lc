package com.zifang.z.lc.common.constants;

/**
 * 通用布尔标志常量 — 蒸馏自 ace-platform-core
 * {@code FlagConstants} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于业务方在数据库/缓存中存储布尔标志（避免使用 {@code boolean} 类型，
 * 因为部分 DB 不支持 BOOLEAN）.
 *
 * @author zifang
 */
public interface ZLcFlagConstants {

    /** 真 — DB 存储时用 1. */
    Integer TRUE = 1;

    /** 假 — DB 存储时用 0. */
    Integer FALSE = 0;

    /**
     * 工具方法 — 把 Integer 标志转 boolean.
     */
    static boolean asBoolean(Integer flag) {
        return flag != null && TRUE.equals(flag);
    }

    /**
     * 工具方法 — 把 boolean 转 Integer 标志.
     */
    static Integer fromBoolean(boolean flag) {
        return flag ? TRUE : FALSE;
    }
}
