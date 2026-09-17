package com.zifang.z.lc.core.domain;

/**
 * 字典项值 DO — 蒸馏自 ace-platform-core {@code DictItemValueDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code dict_item_value} 表 — 字典条目下的实际值（key-value 形式）.
 *
 * @author zifang
 */
public class DictItemValueDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 归属字典 id（dict_item.id）.
     */
    private Long dictItemId;

    /**
     * 字典 code（冗余字段，便于快速查询）.
     */
    private String dictCode;

    /**
     * 显示名.
     */
    private String name;

    /**
     * 实际值（与 name 配合组成 key-value 对）.
     */
    private String value;

    /**
     * 排序（同一字典下多个值的顺序）.
     */
    private Integer sortOrder;

    /**
     * 是否默认（1 默认 / 0 非默认）.
     */
    private Integer defaultFlag;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public Long getDictItemId() {
        return dictItemId;
    }

    public void setDictItemId(Long dictItemId) {
        this.dictItemId = dictItemId;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(Integer defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    public boolean isDefault() {
        return defaultFlag != null && defaultFlag == 1;
    }
}
