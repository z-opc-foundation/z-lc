package com.zifang.z.lc.common.enums;

/**
 * 页面模板操作记录类型 — 蒸馏自 ace-platform-core
 * {@code Enums.PageTemplateOperateRecordType} ({@code com.c2f.ace.core.common}).
 *
 * <p>用于低代码平台页面模板的操作审计日志 — 区分不同类型的页面操作.
 *
 * <ul>
 *   <li>{@link #PRINT} — 打印操作</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcPageTemplateOperateRecordType {

    /** 打印操作. */
    PRINT("print");

    private final String type;

    ZLcPageTemplateOperateRecordType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ZLcPageTemplateOperateRecordType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcPageTemplateOperateRecordType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}