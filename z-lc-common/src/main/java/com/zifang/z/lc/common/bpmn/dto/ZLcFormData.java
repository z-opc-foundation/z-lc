package com.zifang.z.lc.common.bpmn.dto;

import java.util.Map;

/**
 * 表单数据 DTO — 蒸馏自 ace-platform-core
 * {@code FormData} ({@code com.c2f.ace.core.bpmn.callable.dto}).
 *
 * <p>用于低代码平台"流程引擎"对外传递 — 表单运行时数据载体.
 * 通常作为 MQ 消息 / WebSocket 推送的载荷部分.
 *
 * @author zifang
 */
public class ZLcFormData {

    /** 应用编码. */
    private String appCode;

    /** 表单编码. */
    private String formCode;

    /** 对应的模型标识. */
    private String modelCode;

    /** 表单实例 ID. */
    private Long formInstanceId;

    /** 当前表单数据 (字段名 → 字段值). */
    private Map<String, Object> data;

    /** 业务主键. */
    private String businessKey;

    /** 页面名称. */
    private String formName;

    public String getAppCode() { return appCode; }
    public void setAppCode(String appCode) { this.appCode = appCode; }

    public String getFormCode() { return formCode; }
    public void setFormCode(String formCode) { this.formCode = formCode; }

    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public Long getFormInstanceId() { return formInstanceId; }
    public void setFormInstanceId(Long formInstanceId) { this.formInstanceId = formInstanceId; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
}