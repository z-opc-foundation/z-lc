package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.List;

/**
 * HTTP 接口配置.
 *
 * <p>蒸馏自 ace-platform-client {@code HttpInterfaceConfig}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「外部 HTTP 服务」调用配置 — requestUrl + requestType (GET/POST) +
 * requestBody (POST 时使用) + requestParamList (URL query 参数列表).
 *
 * @author lufei.lhw (distilled by zifang)
 */
public class HttpInterfaceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接口 URL.
     */
    private String requestUrl;

    /**
     * 请求类型（GET / POST）.
     */
    private String requestType;

    /**
     * 请求体（POST 时使用 — 通常是 JSON 模板字符串，支持 {@code {{paramName}}} 占位符）.
     */
    private String requestBody;

    /**
     * 请求参数列表（URL query 参数）.
     */
    private List<HttpRequestParam> requestParamList;

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public List<HttpRequestParam> getRequestParamList() {
        return requestParamList;
    }

    public void setRequestParamList(List<HttpRequestParam> requestParamList) {
        this.requestParamList = requestParamList;
    }
}
