package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组聚合查询入参 (看板分列 / 表格页脚统计 / group-by 都用它).
 * <p>
 * 筛选部分刻意与 {@link RuntimeQueryDTO} 同构 (filters / conditions / conjunction),
 * 这样"当前视图的筛选条件"可以直接原样发给聚合接口, 前端不用维护两套口径.
 */
public class AggregateQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String appCode;
    private String entityCode;

    /** 分组字段编码; 为空表示只出一行总计. */
    private String groupField;

    /**
     * 时间分桶粒度: DAY / MONTH / YEAR, 作用于 groupField (必须是 DATE/DATETIME 列).
     * 为空表示按列的原值分组. 图表的时间轴靠它, 否则"按天分组"只能拉到全量记录前端自己数.
     */
    private String timeGroup;

    /** 需要附加的数值聚合: SUM / AVG / MIN / MAX, 键是字段编码. */
    private Map<String, List<String>> aggregations = new LinkedHashMap<String, List<String>>();

    /** 上限, 防止高基数字段一把拉回几万个分组. */
    private Integer limit = 100;

    private Map<String, Object> filters = new LinkedHashMap<String, Object>();
    private String conjunction = "AND";
    private List<QueryConditionDTO> conditions = new ArrayList<QueryConditionDTO>();

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

    public String getGroupField() {
        return groupField;
    }

    public void setGroupField(String groupField) {
        this.groupField = groupField;
    }

    public String getTimeGroup() {
        return timeGroup;
    }

    public void setTimeGroup(String timeGroup) {
        this.timeGroup = timeGroup;
    }

    public Map<String, List<String>> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<String, List<String>> aggregations) {
        this.aggregations = aggregations == null ? new LinkedHashMap<String, List<String>>() : aggregations;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters == null ? new LinkedHashMap<String, Object>() : filters;
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
        this.conditions = conditions == null ? new ArrayList<QueryConditionDTO>() : conditions;
    }
}
