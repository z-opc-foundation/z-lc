package com.zifang.z.lc.core.domain;

/**
 * 页面模板 DO — 蒸馏自 ace-platform-core {@code PageTemplateDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code page_template} 表 — 页面设计态的持久化对象.
 *
 * <p>与 z-lc 已有的 {@code z-lc-design/model/PageTemplate} 互补：
 * <ul>
 *   <li>{@code PageTemplate} — classpath JSON 扫描的设计态模型（轻量）</li>
 *   <li>{@code PageTemplateDO} — 持久化对象（含 pageTemplateId 等数据库字段）</li>
 * </ul>
 *
 * @author zifang
 */
public class PageTemplateDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 关联模型 code.
     */
    private String modelCode;

    /**
     * 页面类型（list / form / dashboard 等）.
     */
    private String pageType;

    /**
     * 页面 code（业务唯一）.
     */
    private String pageCode;

    /**
     * 页面名称.
     */
    private String pageName;

    /**
     * 页面描述.
     */
    private String pageDesc;

    /**
     * 关联流程模板 key.
     */
    private String workflowDefinitionKey;

    /**
     * 关联流程模板 id（Flowable deployment id）.
     */
    private String workflowDefinitionId;

    /**
     * 视图描述（前端设计器生成 — JSON 字符串）.
     */
    private String viewJson;

    /**
     * 状态（0 未发布 / 1 已发布）.
     */
    private Integer status;

    /**
     * 归属树节点 code.
     */
    private String pageTreeNodeCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageDesc() {
        return pageDesc;
    }

    public void setPageDesc(String pageDesc) {
        this.pageDesc = pageDesc;
    }

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getViewJson() {
        return viewJson;
    }

    public void setViewJson(String viewJson) {
        this.viewJson = viewJson;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPageTreeNodeCode() {
        return pageTreeNodeCode;
    }

    public void setPageTreeNodeCode(String pageTreeNodeCode) {
        this.pageTreeNodeCode = pageTreeNodeCode;
    }

    public boolean isPublished() {
        return status != null && status == 1;
    }
}
