package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 视图配置更新请求 DTO (F035 T5: View Config)
 */
public class ViewConfigUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String viewType;

    /**
     * 视图配置 JSON
     */
    private String config;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
