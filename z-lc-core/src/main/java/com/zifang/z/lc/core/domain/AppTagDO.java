package com.zifang.z.lc.core.domain;

/**
 * 应用标签 DO — 蒸馏自 ace-platform-core
 * {@code AppTagDO} （{@code com.c2f.ace.core.domain.entity}}，
 * 字段语义完全对齐.
 *
 * <p>对应 {@code app_tag} 表 — 应用级别的标签维度，
 * 用于：
 * <ul>
 *   <li>数据分类（如：紧急/普通、已审核/未审核）</li>
 *   <li>权限维度（如：部门标签）</li>
 *   <li>业务流程路由（如：根据 tagType 选择不同审批流）</li>
 * </ul>
 *
 * <p>{@link #tagGroupCode} 用于把多个标签聚合为一组（如「紧急程度」组下有「普通/紧急/特紧急」）.
 *
 * @author zifang
 */
public class AppTagDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 标签名称.
     */
    private String tagName;

    /**
     * 标签 code.
     */
    private String tagCode;

    /**
     * 标签描述.
     */
    private String tagDesc;

    /**
     * 标签性质（如 {@code "category"} / {@code "priority"} / {@code "region"}）.
     */
    private String tagType;

    /**
     * 标签组 code.
     */
    private String tagGroupCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getTagDesc() {
        return tagDesc;
    }

    public void setTagDesc(String tagDesc) {
        this.tagDesc = tagDesc;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public String getTagGroupCode() {
        return tagGroupCode;
    }

    public void setTagGroupCode(String tagGroupCode) {
        this.tagGroupCode = tagGroupCode;
    }

    /**
     * 工厂方法 — 与 ace {@code AppTagDO.of(...)} 行为对齐.
     */
    public static AppTagDO of(String appCode, String tagGroupCode, String tagCode,
                              String tagName, String tagDesc, String tagType) {
        AppTagDO tag = new AppTagDO();
        tag.setAppCode(appCode);
        tag.setTagGroupCode(tagGroupCode);
        tag.setTagCode(tagCode);
        tag.setTagName(tagName);
        tag.setTagDesc(tagDesc);
        tag.setTagType(tagType);
        return tag;
    }
}
