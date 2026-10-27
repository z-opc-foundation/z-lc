package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/** vApi 创建请求 (V3)。 */
public class VapiCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appCode;
    private String vapiCode;
    private String method;
    private String path;
    private String dslType;
    private String sourceCode;
    private String paramSchema;
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
