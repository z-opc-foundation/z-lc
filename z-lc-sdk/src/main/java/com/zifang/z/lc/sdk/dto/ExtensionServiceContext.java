package com.zifang.z.lc.sdk.dto;

import java.io.Serializable;
import java.util.List;

/**
 * SPI 调用上下文.
 * <p>
 * 设计哲学:
 * z-lc 引擎回调用户 SPI 时携带的"环境上下文" — 告诉用户 SPI:
 * 当前是哪个应用 / 模型 / 工作流 / 表单 / 页面 / 流程实例.
 * <p>
 * 字段约定: 不强依赖 workflow / form 引擎, 没有相关能力时字段为 null, SPI 自行判断.
 * <p>
 * 手写 getter/setter (不依赖 lombok): 框架层最简实现, 减少对 annotation processor 的依赖.
 */
public class ExtensionServiceContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用标识
     */
    private String appCode;
    /**
     * 模型 code (对应 @DataModel.modelCode)
     */
    private String modelCode;
    /**
     * 工作流定义 key (z-camuda 接入时填充)
     */
    private String workflowDefinitionKey;
    /**
     * 表单 code (z-lc 表单接入时填充, Phase 2C 才用)
     */
    private String formCode;
    /**
     * 页面 code (z-lc-design 页面模板 code)
     */
    private String pageCode;
    /**
     * 流程实例 id (z-camuda 接入时填充)
     */
    private String processInstanceId;
    /**
     * 业务自定义标签 (扩展点)
     */
    private List<String> customTags;

    public ExtensionServiceContext() {
    }

    public ExtensionServiceContext(String appCode, String modelCode, String workflowDefinitionKey,
                                   String formCode, String pageCode, String processInstanceId,
                                   List<String> customTags) {
        this.appCode = appCode;
        this.modelCode = modelCode;
        this.workflowDefinitionKey = workflowDefinitionKey;
        this.formCode = formCode;
        this.pageCode = pageCode;
        this.processInstanceId = processInstanceId;
        this.customTags = customTags;
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public List<String> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(List<String> customTags) {
        this.customTags = customTags;
    }

    public static class Builder {
        private String appCode;
        private String modelCode;
        private String workflowDefinitionKey;
        private String formCode;
        private String pageCode;
        private String processInstanceId;
        private List<String> customTags;

        public Builder appCode(String v) {
            this.appCode = v;
            return this;
        }

        public Builder modelCode(String v) {
            this.modelCode = v;
            return this;
        }

        public Builder workflowDefinitionKey(String v) {
            this.workflowDefinitionKey = v;
            return this;
        }

        public Builder formCode(String v) {
            this.formCode = v;
            return this;
        }

        public Builder pageCode(String v) {
            this.pageCode = v;
            return this;
        }

        public Builder processInstanceId(String v) {
            this.processInstanceId = v;
            return this;
        }

        public Builder customTags(List<String> v) {
            this.customTags = v;
            return this;
        }

        public ExtensionServiceContext build() {
            return new ExtensionServiceContext(appCode, modelCode, workflowDefinitionKey,
                    formCode, pageCode, processInstanceId, customTags);
        }
    }
}
