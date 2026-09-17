package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 页面模板树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code PageTemplateTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「页面设计器」树节点的附加信息 — pageType / pageCode / pageName /
 * 关联流程模板 key / 自定义标签 / icon / 创建时间.
 *
 * @author zifang
 */
public class PageTemplateTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型标识.
     */
    private String modelCode;

    /**
     * 页面类型（list / form / dashboard 等）.
     */
    private String pageType;

    /**
     * 页面 code.
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
     * 流程模板 key（关联 Flowable 工作流）.
     */
    private String workflowDefinitionKey;

    /**
     * 自定义标签.
     */
    private List<String> customTags;

    /**
     * 图标地址.
     */
    private String imageUrl;

    /**
     * 创建时间.
     */
    private LocalDateTime createTime;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

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

    public List<String> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(List<String> customTags) {
        this.customTags = customTags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }
}
