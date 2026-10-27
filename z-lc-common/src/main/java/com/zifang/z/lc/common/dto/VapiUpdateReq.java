package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/** vApi 更新请求 (V3): id 定位, 其余字段可部分更新。 */
public class VapiUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String vapiCode;
    private String method;
    private String path;
    private String dslType;
    private String sourceCode;
    private String paramSchema;
    private Integer status;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
