package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 页面模板类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.PageTemplateType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于区分页面模板的不同形态，对应 PageTemplateDO.pageType 字段.
 *
 * <p>{@link #DEFAULT_PAGE_TYPES} 是低代码平台初始化时自动注册的 7 种基础页面类型.
 *
 * @author zifang
 */
public enum ZLcPageTemplateType {

    /** 列表页 */
    LIST("list", "列表页"),

    /** 表单页 */
    FORM("form", "表单页"),

    /** 移动端表单页 */
    APP_FORM("app_form", "移动端表单页"),

    /** 表单页-新版编辑器 */
    FORM_V2("formV2", "表单页-新版编辑器"),

    /** 列表页-新版编辑器 */
    LIST_V2("listV2", "列表页-新版编辑器"),

    /** 普通页 */
    COMMON("common", "普通页"),

    /** 打印模板页 */
    PRINT("print", "打印模板页");

    private final String pageType;
    private final String pageTypeDesc;

    ZLcPageTemplateType(String pageType, String pageTypeDesc) {
        this.pageType = pageType;
        this.pageTypeDesc = pageTypeDesc;
    }

    public String getPageType() {
        return pageType;
    }

    public String getPageTypeDesc() {
        return pageTypeDesc;
    }

    /** 默认注册的 7 种基础页面类型 — 引擎启动时自动写入. */
    public static final List<ZLcPageTemplateType> DEFAULT_PAGE_TYPES = Arrays.asList(
            LIST, FORM, APP_FORM, FORM_V2, LIST_V2, COMMON, PRINT);

    public static ZLcPageTemplateType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcPageTemplateType t : values()) {
            if (t.pageType.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
