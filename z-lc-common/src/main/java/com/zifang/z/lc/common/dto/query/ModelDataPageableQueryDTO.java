package com.zifang.z.lc.common.dto.query;

import java.io.Serializable;
import java.util.List;

/**
 * 模型数据分页查询 DTO.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataPageableQueryDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>相比 {@link ModelDataQueryDTO} 增加了：
 * <ul>
 *   <li>{@link #pageCode} — 关联的页面 code（用于权限校验 / 字段过滤）</li>
 *   <li>{@link #queryCondition} — 递归查询条件（与扁平 queryBeans 并存 — 前者高级筛选 / 后者基础）</li>
 *   <li>{@link #selectFieldCodes} — 仅返回指定字段（性能优化）</li>
 *   <li>{@link #keysetPagination} / {@link #keysetAfterPk} — 主键游标分页（避免深度 offset 性能问题）</li>
 *   <li>{@link #queryOffset} — 断点续导起点（用于大数据导出场景）</li>
 * </ul>
 *
 * <p>注意：本 DTO 不继承任何分页基类（避免外部依赖），由 z-lc-web Controller 在入参时
 * 把 {@code current / size} 提取到 {@code PageRequest} 中 — 调用方按需填充.
 *
 * @author zifang
 */
public class ModelDataPageableQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页（从 1 开始）— 由 Controller 入参填充. */
    private Long current = 1L;

    /** 每页大小. */
    private Long size = 20L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型 code.
     */
    private String modelCode;

    /**
     * 页面 code（用于权限校验 / 字段过滤）.
     */
    private String pageCode;

    /**
     * 字段等值条件列表（扁平 — 类似 AND 组合）.
     */
    private List<ModelDataQueryBean> queryBeans;

    /**
     * 字段顺序.
     */
    private ModelDataOrderBean orderBean;

    /**
     * 递归查询条件（与 queryBeans 并存 — 二选一或同时使用）.
     */
    private ModelDataQueryCondition queryCondition;

    /**
     * select 字段（不指定时返回所有字段 — 性能优化场景用）.
     */
    private List<String> selectFieldCodes;

    /**
     * 为 true 时使用主键游标分页（{@code limit size} 配合 {@link #keysetAfterPk}），
     * 避免深度 {@code offset} 性能问题.
     */
    private Boolean keysetPagination;

    /**
     * 上一页最后一条的主键值；首页为 null.
     */
    private Object keysetAfterPk;

    /**
     * 指定 offset 分页起点；设置后优先于 {@link #current} 计算 {@code limit offset}.
     * 主要用于「断点续导」场景.
     */
    private Long queryOffset;

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public List<ModelDataQueryBean> getQueryBeans() {
        return queryBeans;
    }

    public void setQueryBeans(List<ModelDataQueryBean> queryBeans) {
        this.queryBeans = queryBeans;
    }

    public ModelDataOrderBean getOrderBean() {
        return orderBean;
    }

    public void setOrderBean(ModelDataOrderBean orderBean) {
        this.orderBean = orderBean;
    }

    public ModelDataQueryCondition getQueryCondition() {
        return queryCondition;
    }

    public void setQueryCondition(ModelDataQueryCondition queryCondition) {
        this.queryCondition = queryCondition;
    }

    public List<String> getSelectFieldCodes() {
        return selectFieldCodes;
    }

    public void setSelectFieldCodes(List<String> selectFieldCodes) {
        this.selectFieldCodes = selectFieldCodes;
    }

    public Boolean getKeysetPagination() {
        return keysetPagination;
    }

    public void setKeysetPagination(Boolean keysetPagination) {
        this.keysetPagination = keysetPagination;
    }

    public Object getKeysetAfterPk() {
        return keysetAfterPk;
    }

    public void setKeysetAfterPk(Object keysetAfterPk) {
        this.keysetAfterPk = keysetAfterPk;
    }

    public Long getQueryOffset() {
        return queryOffset;
    }

    public void setQueryOffset(Long queryOffset) {
        this.queryOffset = queryOffset;
    }
}
