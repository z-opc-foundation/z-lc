package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时 CRUD 查询参数
 * filters: 字段编码 -> 值; operator 通过 key 后缀表达: ":eq" (默认) / ":like" / ":gt" / ":lt" / ":in"
 */
public class RuntimeQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户编码 (从 JWT 上下文自动注入, 也可显式传)
     */
    private String tenantCode;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 过滤条件, key 可以是 "userName" (=) 或 "userName:like" 或 "age:gt"
     */
    private Map<String, Object> filters = new LinkedHashMap<>();

    /**
     * 排序字段, 例如 "create_time desc"
     */
    private String orderBy;

    /**
     * 页码 (从 1 开始)
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;

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

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
