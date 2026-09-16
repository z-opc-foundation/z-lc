package com.zifang.z.lc.design.model;

import java.io.Serializable;

/**
 * 页面模板元数据 (设计态).
 * <p>
 * 设计哲学:
 * 一个低代码"页面" = 一个 PageTemplate 实例. 包含:
 * <ul>
 *   <li>appCode / modelCode — 关联到 @DataModel 注解的 POJO</li>
 *   <li>pageCode / pageType / pageName — 页面标识 + 类型 + 名称</li>
 *   <li>viewJson — 视图描述 (前端设计器生成, 含组件树/布局/字段绑定)</li>
 *   <li>status — 0=未发布 / 1=已发布</li>
 * </ul>
 * <p>
 * viewJson 格式由前端设计器决定 (z-lc-design 不强约束), 后端只做存储与下发.
 * <p>
 * 手写 getter/setter (不依赖 lombok): 框架层最简实现, 减少对 annotation processor 的依赖.
 */
public class PageTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pageTemplateId;
    private String appCode;
    private String modelCode;
    private String pageType;
    private String pageName;
    private String pageCode;
    private String pageDesc;
    private Integer status;
    private Long treeNodeId;
    private String viewJson;

    public Long getPageTemplateId() {
        return pageTemplateId;
    }

    public void setPageTemplateId(Long v) {
        this.pageTemplateId = v;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String v) {
        this.appCode = v;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String v) {
        this.modelCode = v;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String v) {
        this.pageType = v;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String v) {
        this.pageName = v;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String v) {
        this.pageCode = v;
    }

    public String getPageDesc() {
        return pageDesc;
    }

    public void setPageDesc(String v) {
        this.pageDesc = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        this.status = v;
    }

    public Long getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(Long v) {
        this.treeNodeId = v;
    }

    public String getViewJson() {
        return viewJson;
    }

    public void setViewJson(String v) {
        this.viewJson = v;
    }
}
