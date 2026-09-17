package com.zifang.z.lc.common.enums;

/**
 * HTTP 请求类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.HttpRequestType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于标注页面/表单触发的 HTTP 请求方法类型 —
 * 低代码平台生成页面时按此枚举选择 GET/POST 表单模板.
 *
 * @author zifang
 */
public enum ZLcHttpRequestType {

    /** GET 请求（查询场景） */
    GET("get"),

    /** POST 请求（提交/修改场景） */
    POST("post");

    private final String code;

    ZLcHttpRequestType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ZLcHttpRequestType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcHttpRequestType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
