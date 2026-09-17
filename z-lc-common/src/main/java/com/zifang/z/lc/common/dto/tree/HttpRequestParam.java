package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * HTTP 接口入参定义.
 *
 * <p>蒸馏自 ace-platform-client {@code HttpRequestParam}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用于描述「HTTP 远程服务」每个入参的 code + defaultValue — Runtime 调用时把
 * 调用方传入的实参按此定义映射到 URL query / POST body 字段.
 *
 * @author zifang
 */
public class HttpRequestParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参数名称.
     */
    private String param;

    /**
     * 默认值（调用方未传时使用）.
     */
    private String defaultValue;

    public HttpRequestParam() {
    }

    public HttpRequestParam(String param, String defaultValue) {
        this.param = param;
        this.defaultValue = defaultValue;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
