package com.zifang.z.lc.common.enums;

/**
 * 数据导出类型枚举 — 蒸馏自 ace-platform-core
 * {@code DataExportTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「数据导出」操作时选择导出范围：
 * <ul>
 *   <li>{@link #CURRENT_PAGE} — 当前页导出（仅导出当前分页的数据）</li>
 *   <li>{@link #ALL} — 全量导出（按查询条件导出全部命中数据）</li>
 *   <li>{@link #CLIENT_PROVIDED} — 客户端数据导出（前端传入具体行 ID）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcDataExportTypeEnum {

    /** 当前页导出. */
    CURRENT_PAGE(1, "当前页导出"),

    /** 全量导出. */
    ALL(2, "全量导出"),

    /** 客户端数据导出. */
    CLIENT_PROVIDED(3, "客户端数据导出");

    private final Integer code;
    private final String value;

    ZLcDataExportTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcDataExportTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcDataExportTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
