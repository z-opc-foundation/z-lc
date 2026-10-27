package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 页面更新请求 DTO (V2 页面域)。id 必填；pageCode/appCode 不可改（页面身份键）。
 */
public class PageUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String pageName;

    /** 页面 schema JSON（保存前经 PageSchemaValidator 校验） */
    private String schema;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
