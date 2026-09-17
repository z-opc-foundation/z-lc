package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.List;

/**
 * 远程服务树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code RemoteServiceTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「外部服务集成」树节点 — 把第三方 HTTP/OpenPlatform 接口包装成模型可调用服务.
 *
 * @author zifang
 */
public class RemoteServiceTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 服务编码（业务唯一标识）.
     */
    private String serviceCode;

    /**
     * 服务名称.
     */
    private String serviceName;

    /**
     * 服务描述.
     */
    private String serviceDesc;

    /**
     * 服务类型（HTTP / OPEN_PLATFORM / INNER）.
     */
    private String serviceType;

    /**
     * 入参列表（描述每个入参的 code / type / required / defaultValue）.
     */
    private List<HttpRequestParam> requestParams;

    /**
     * HTTP 接口配置（serviceType=HTTP 时使用）.
     */
    private HttpInterfaceConfig httpInterfaceConfig;

    /**
     * 开放平台接口配置（serviceType=OPEN_PLATFORM 时使用）.
     */
    private OpenPlatformInterfaceConfig openPlatformInterfaceConfig;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDesc() {
        return serviceDesc;
    }

    public void setServiceDesc(String serviceDesc) {
        this.serviceDesc = serviceDesc;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public List<HttpRequestParam> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(List<HttpRequestParam> requestParams) {
        this.requestParams = requestParams;
    }

    public HttpInterfaceConfig getHttpInterfaceConfig() {
        return httpInterfaceConfig;
    }

    public void setHttpInterfaceConfig(HttpInterfaceConfig httpInterfaceConfig) {
        this.httpInterfaceConfig = httpInterfaceConfig;
    }

    public OpenPlatformInterfaceConfig getOpenPlatformInterfaceConfig() {
        return openPlatformInterfaceConfig;
    }

    public void setOpenPlatformInterfaceConfig(OpenPlatformInterfaceConfig openPlatformInterfaceConfig) {
        this.openPlatformInterfaceConfig = openPlatformInterfaceConfig;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }
}
