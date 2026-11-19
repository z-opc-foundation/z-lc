package com.zifang.z.lc.core.vapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.util.Date;

/**
 * 虚拟接口注册表实体 (V3, lead 013 §四)。
 * <p>
 * 直连 CRUD（与 Page 同款决策：vApi 不是 app schema 的一部分，不进事件回放链；
 * 接口调用留痕由执行日志承接，属 V3+ 扩展面）。
 */
@BizModel(name = "虚拟接口注册表实体 (V3, lead 013 §四)", module = "z-lc", description = "虚拟接口注册表实体 (V3, lead 013 §四)。 直连 CRUD（与 Page 同款决策：vApi 不是 app schema 的一部分，不进事件回放链； 接口调用留痕由执行日志承接，属 V3+ 扩展面）。")
@TableName("z_lc_vapi")
public class VapiEntity {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "vapiCode", type = BizField.FieldType.STRING)
    private String vapiCode;
    /** HTTP 方法大写: GET/POST/PUT/DELETE */
    @BizField(name = "method", type = BizField.FieldType.STRING)
    private String method;
    /** 应用内相对路径, 归一化存储: 无首尾斜杠 */
    @BizField(name = "path", type = BizField.FieldType.STRING)
    private String path;
    /** z-script ScriptEngine DSL: EL/GROOVY/LUA/SQL/MOCK */
    @BizField(name = "dslType", type = BizField.FieldType.STRING)
    private String dslType;
    /** 脚本源码 (孵化期直存; 未来接 z-script 注册表时改为 scriptRef) */
    @BizField(name = "sourceCode", type = BizField.FieldType.STRING)
    private String sourceCode;
    @BizField(name = "paramSchema", type = BizField.FieldType.STRING)
    private String paramSchema;
    /** 1=启用 0=停用 */
    @BizField(name = "status", type = BizField.FieldType.INTEGER)
    private Integer status;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "deleted", type = BizField.FieldType.INTEGER)
    private Integer deleted;
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;
    @BizField(name = "updateTime", type = BizField.FieldType.DATETIME)
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



