package com.zifang.z.lc.common.enums;

import java.util.Objects;

/**
 * 数据导入模式枚举 — 蒸馏自 ace-platform-core
 * {@code DataImportModeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「数据导入」操作时选择导入策略：
 * <ul>
 *   <li>{@link #FULL_UPDATE} — 全量更新（先清空目标表再插入）</li>
 *   <li>{@link #INCREMENTAL_UPDATE} — 增量更新（按主键 upsert）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcDataImportModeEnum {

    /** 全量更新. */
    FULL_UPDATE(1, "全量更新"),

    /** 增量更新. */
    INCREMENTAL_UPDATE(2, "增量更新");

    private final Integer code;
    private final String value;

    ZLcDataImportModeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcDataImportModeEnum getByCode(Integer code) {
        for (ZLcDataImportModeEnum v : values()) {
            if (Objects.equals(v.getCode(), code)) {
                return v;
            }
        }
        return null;
    }
}
