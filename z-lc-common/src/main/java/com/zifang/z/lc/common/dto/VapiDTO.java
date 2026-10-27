package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 虚拟接口 vApi DTO (V3 接口/脚本域, lead 013 §四)。
 * <p>
 * vApi = 应用级虚拟接口注册表条目：appCode + method + path 定位, 实现即脚本
 * （dslType + sourceCode 直存——孵化期自包含; 未来接 z-script 注册表时经
 * scriptRef 桥接, 形态不变）。分发由 z-lc-web 通用分发 Controller
 * {@code /api/lc/vapi/{appCode}/**} 按注册表路由执行。
 */
public class VapiDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appCode;
    private String vapiCode;
    /** HTTP 方法, 大写: GET/POST/PUT/DELETE */
    private String method;
    /** 应用内相对路径, 如 order/submit */
    private String path;
    /** 脚本 DSL 类型: EL/GROOVY/LUA/SQL/MOCK (z-script ScriptEngine 契约) */
    private String dslType;
    /** 脚本源码 (孵化期直存; 出参 DTO 不脱敏——脚本本身即用户资产) */
    private String sourceCode;
    /** 参数 schema (JSON, 预留) */
    private String paramSchema;
    /** 1=启用 0=停用 */
    private Integer status;
    private String description;
    private java.util.Date createTime;
    private java.util.Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getVapiCode() {
        return vapiCode;
    }

    public void setVapiCode(String vapiCode) {
        this.vapiCode = vapiCode;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getParamSchema() {
        return paramSchema;
    }

    public void setParamSchema(String paramSchema) {
        this.paramSchema = paramSchema;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

    public java.util.Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }
}
