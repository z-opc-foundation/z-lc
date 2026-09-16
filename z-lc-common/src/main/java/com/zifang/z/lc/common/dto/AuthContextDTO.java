package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * JWT 鉴权上下文 (从 z-ctc 拉取)
 */
public class AuthContextDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String userName;
    private String tenantCode;
    private List<String> roles;
    private List<String> permissions;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
