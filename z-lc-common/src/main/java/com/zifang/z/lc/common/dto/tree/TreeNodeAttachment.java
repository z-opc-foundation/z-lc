package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * 树节点附件 — 携带节点的业务属性.
 *
 * <p>蒸馏自 ace-platform-client {@code TreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：模型树节点的附件信息（appCode + modelCode + pageType + pageCode + 流程模板 key + tag）—
 * 前端「模型管理」树点击节点时根据这些字段跳转到对应的设计器 / 运行时页面.
 *
 * <p>不同业务节点的附件可继承本类扩展（如 AppDefaultMenuTreeNodeAttachment / DictTreeNodeAttachment 等）.
 *
 * @author zifang
 */
public class TreeNodeAttachment implements Serializable {

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
     * 流程模板 key.
     */
    private String workflowDefinitionKey;

    /**
     * 标签 code（业务自定义 — 用于权限 / 灰度）.
     */
    private String tagCode;

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

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }
}
