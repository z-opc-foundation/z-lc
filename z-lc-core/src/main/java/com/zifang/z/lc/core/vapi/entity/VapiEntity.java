package com.zifang.z.lc.core.vapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 虚拟接口注册表实体 (V3, lead 013 §四)。
 * <p>
 * 直连 CRUD（与 Page 同款决策：vApi 不是 app schema 的一部分，不进事件回放链；
 * 接口调用留痕由执行日志承接，属 V3+ 扩展面）。
 */
@TableName("z_lc_vapi")
public class VapiEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String appCode;
    private String vapiCode;
    /** HTTP 方法大写: GET/POST/PUT/DELETE */
    private String method;
    /** 应用内相对路径, 归一化存储: 无首尾斜杠 */
    private String path;
    /** z-script ScriptEngine DSL: EL/GROOVY/LUA/SQL/MOCK */
    private String dslType;
    /** 脚本源码 (孵化期直存; 未来接 z-script 注册表时改为 scriptRef) */
    private String sourceCode;
    private String paramSchema;
    /** 1=启用 0=停用 */
    private Integer status;
    private String description;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
