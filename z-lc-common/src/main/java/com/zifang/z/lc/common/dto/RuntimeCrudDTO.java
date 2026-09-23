package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时 CRUD create/update body
 * fieldValues: 字段编码 -> 值
 */
public class RuntimeCrudDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String appCode;
    private String entityCode;

    /**
     * 字段值映射
     */
    private Map<String, Object> fieldValues = new LinkedHashMap<>();

    /**
     * 用于 update: 父事件 id (用于"不允许冲突"校验)
     */
    private String parentEventId;

    /**
     * 更新前的整行现值, **由服务端在 update 路径自己查出来塞进去**, 只给校验用.
     * <p>
     * 存在的理由: RequiredCheck 原来只看提交的 fieldValues, 于是"改一个字段"的部分更新
     * 会因为其它必填字段没出现在请求里而被拒 —— 表格内联编辑、看板拖拽全都撞上.
     * 校验要看的是"改完之后的那一行", 不是"这次提交了哪几列".
     */
    private Map<String, Object> existingValues = new LinkedHashMap<String, Object>();

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public Map<String, Object> getExistingValues() {
        return existingValues;
    }

    public void setExistingValues(Map<String, Object> existingValues) {
        this.existingValues = existingValues == null ? new LinkedHashMap<String, Object>() : existingValues;
    }

    public String getParentEventId() {
        return parentEventId;
    }

    public void setParentEventId(String parentEventId) {
        this.parentEventId = parentEventId;
    }
}
