package com.zifang.z.lc.core.materialize.dto;

import java.util.List;

/**
 * 触发代码物化的请求体.
 */
public class MaterializationReq {

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 用户指定的导出根路径 (绝对路径)
     */
    private String materializationPath;

    /**
     * 导出的 entity 列表; null 或空 = 全量
     */
    private List<String> entityCodes;

    /**
     * 备注
     */
    private String description;

    /**
     * 触发来源: USER / AGENT / SYSTEM, 默认 USER
     */
    private String triggerSource;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String v) {
        this.appCode = v;
    }

    public String getMaterializationPath() {
        return materializationPath;
    }

    public void setMaterializationPath(String v) {
        this.materializationPath = v;
    }

    public List<String> getEntityCodes() {
        return entityCodes;
    }

    public void setEntityCodes(List<String> v) {
        this.entityCodes = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String v) {
        this.triggerSource = v;
    }
}
