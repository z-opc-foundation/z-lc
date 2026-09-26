package com.zifang.z.lc.core.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

@TableName("z_lc_permission")
public class PermissionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appCode;
    /** 授权范围；null / 空串 = 整个应用 (写入口统一归成 null，见 {@code PermissionService#grant})。 */
    private String entityCode;
    private String roleCode;
    /**
     * 权限项，取值只能是 {@link com.zifang.z.lc.core.permission.PermissionKeys} 里登记的那几个。
     * <p>
     * 这里以前注着 "READ / WRITE / DELETE / ADMIN" —— 一份和前端矩阵的列头
     * (VIEW / CREATE / UPDATE / DELETE / EXPORT) 平行、而且没有任何一处会校验的说法。
     */
    private String permission;
    private String tenantCode;
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
