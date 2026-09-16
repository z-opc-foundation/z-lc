package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 应用更新请求 DTO (F035 T1)
 */
public class AppUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String appName;
    private String description;
    private String icon;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
