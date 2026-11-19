package com.zifang.z.lc.core.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

@BizModel(name = "z_lc_permission", module = "z-lc", description = "z_lc_permission")
@TableName("z_lc_permission")
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class PermissionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    /** 授权范围；null / 空串 = 整个应用 (写入口统一归成 null，见 {@code PermissionService#grant})。 */
    @BizField(name = "entityCode", type = BizField.FieldType.STRING)
    private String entityCode;
    @BizField(name = "roleCode", type = BizField.FieldType.STRING)
    private String roleCode;
    /**
     * 权限项，取值只能是 {@link com.zifang.z.lc.core.permission.PermissionKeys} 里登记的那几个。
     * <p>
     * 这里以前注着 "READ / WRITE / DELETE / ADMIN" —— 一份和前端矩阵的列头
     * (VIEW / CREATE / UPDATE / DELETE / EXPORT) 平行、而且没有任何一处会校验的说法。
     */
    @BizField(name = "permission", type = BizField.FieldType.STRING)
    private String permission;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;

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

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}



