package com.zifang.z.lc.core.domain;

/**
 * 服务项 DO — 蒸馏自 ace-platform-core {@code ServiceItemDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code service_item} 表 — 服务集成（HTTP / 开放平台）的持久化对象.
 *
 * <p>{@link #httpInterfaceConfig} / {@link #openPlatformInterfaceConfig} 字段以 JSON 字符串
 * 存储（对应 {@code z-lc-common/dto/tree/HttpInterfaceConfig} /
 * {@code OpenPlatformInterfaceConfig} 反序列化结果）.
 *
 * @author zifang
 */
public class ServiceItemDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 服务名称.
     */
    private String serviceName;

    /**
     * 服务 code（业务唯一）.
     */
    private String serviceCode;

    /**
     * 服务描述.
     */
    private String serviceDesc;

    /**
     * 服务类型（HTTP / OPEN_PLATFORM / INNER）.
     */
    private String serviceType;

    /**
     * HTTP 接口配置（JSON 字符串 — {@code HttpInterfaceConfig}）.
     */
    private String httpInterfaceConfig;

    /**
     * 开放平台接口配置（JSON 字符串 — {@code OpenPlatformInterfaceConfig}）.
     */
    private String openPlatformInterfaceConfig;

    /**
     * 入参列表（JSON 字符串 — {@code List<HttpRequestParam>}）.
     */
    private String requestParams;

    /**
     * 出参定义（JSON 字符串）.
     */
    private String responseSchema;

    /**
     * 树节点 code.
     */
    private String treeNodeCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
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

    public String getHttpInterfaceConfig() {
        return httpInterfaceConfig;
    }

    public void setHttpInterfaceConfig(String httpInterfaceConfig) {
        this.httpInterfaceConfig = httpInterfaceConfig;
    }

    public String getOpenPlatformInterfaceConfig() {
        return openPlatformInterfaceConfig;
    }

    public void setOpenPlatformInterfaceConfig(String openPlatformInterfaceConfig) {
        this.openPlatformInterfaceConfig = openPlatformInterfaceConfig;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {
        this.responseSchema = responseSchema;
    }

    public String getTreeNodeCode() {
        return treeNodeCode;
    }

    public void setTreeNodeCode(String treeNodeCode) {
        this.treeNodeCode = treeNodeCode;
    }

    /**
     * 是否 HTTP 类型服务.
     */
    public boolean isHttp() {
        return "HTTP".equalsIgnoreCase(serviceType);
    }

    /**
     * 是否开放平台类型服务.
     */
    public boolean isOpenPlatform() {
        return "OPEN_PLATFORM".equalsIgnoreCase(serviceType);
    }

    /**
     * 是否内置服务.
     */
    public boolean isInner() {
        return "INNER".equalsIgnoreCase(serviceType);
    }
}
