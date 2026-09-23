package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时 CRUD 查询参数
 * filters: 字段编码 -> 值; operator 通过 key 后缀表达: ":eq" (默认) / ":like" / ":gt" / ":lt" / ":in"
 * <p>
 * 结构化扩展 (加法演进, 旧字段全部保留):
 * <ul>
 *   <li>{@link #conditions} — 结构化条件列表, 与 legacy filters 同时存在时按 AND 合并</li>
 *   <li>{@link #conjunction} — conditions 之间的连接词: AND (默认) / OR</li>
 *   <li>{@link #sorts} — 多字段结构化排序 (与 legacy orderBy 可共存, sorts 优先)</li>
 * </ul>
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
     * 结构化条件之间的连接词: AND (默认) / OR; 非法值按 AND 处理
     */
    private String conjunction = "AND";

    /**
     * 结构化查询条件列表 (白名单严格校验, 未知字段直接拒绝而非静默跳过)
     */
    private List<QueryConditionDTO> conditions = new ArrayList<>();

    /**
     * 结构化排序列表 (多字段; 白名单严格校验; 与 orderBy 共存时 sorts 在前)
     */
    private List<QuerySortDTO> sorts = new ArrayList<>();

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

    public String getConjunction() {
        return conjunction;
    }

    public void setConjunction(String conjunction) {
        this.conjunction = conjunction;
    }

    public List<QueryConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(List<QueryConditionDTO> conditions) {
        this.conditions = conditions;
    }

    public List<QuerySortDTO> getSorts() {
        return sorts;
    }

    public void setSorts(List<QuerySortDTO> sorts) {
        this.sorts = sorts;
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
