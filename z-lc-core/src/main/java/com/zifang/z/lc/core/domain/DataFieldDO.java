package com.zifang.z.lc.core.domain;

/**
 * 数据字段 DO — 蒸馏自 ace-platform-core {@code DataFieldDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus 依赖.
 *
 * <p>对应 {@code data_field} 表的内存表示 — 包含父类 8 个通用字段 + 字段特有的 12 个属性.
 *
 * <p>与 z-lc 已有的 {@code z-lc-common/dto/FieldDefDTO} 互补：
 * <ul>
 *   <li>{@code FieldDefDTO} — 业务运行时字段定义（轻量）</li>
 *   <li>{@code DataFieldDO} — 持久化对象（重量，含物理列名/类型/约束）</li>
 * </ul>
 *
 * @author zifang
 */
public class DataFieldDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 归属模型 code.
     */
    private String modelCode;

    /**
     * 字段名称（中文）.
     */
    private String fieldName;

    /**
     * 字段 code（业务唯一）.
     */
    private String fieldCode;

    /**
     * 字段是否必填.
     * <ul>
     *   <li>0 — 非必填</li>
     *   <li>1 — 必填</li>
     * </ul>
     */
    private Integer fieldIsRequired;

    /**
     * 字段校验规则（正则 / JSON schema / 自定义表达式）.
     */
    private String fieldValidationRule;

    /**
     * 字段描述.
     */
    private String fieldDesc;

    /**
     * 字段类型.
     * <p>对应 z-lc FieldDefDTO.fieldType 取值：
     * {@code STRING / INT / LONG / DECIMAL / BOOLEAN / DATE / DATETIME / TEXT / JSON / REF}.
     */
    private String fieldType;

    /**
     * 关联的子模型 code（Object / Array 字段关联子模型时填写）.
     */
    private String relateTo;

    /**
     * 关联关系（一对一 / 一对多 / 多对多）.
     */
    private String relateType;

    /**
     * 字段其他属性（JSON 字符串 — 下拉选项 / 默认值等）.
     */
    private String fieldOptions;

    /**
     * 绑定的字典 code.
     */
    private String bondDictCode;

    /**
     * 物理表列名（physicalFlag=1 时使用）.
     */
    private String columnName;

    /**
     * 物理表列类型（physicalFlag=1 时使用 — 如 varchar / bigint / datetime）.
     */
    private String columnType;

    /**
     * 物理表列长度（physicalFlag=1 时使用 — 如 255 / 18,2）.
     */
    private String columnLength;

    /**
     * 是否主键（physicalFlag=1 时使用）.
     */
    private Boolean primaryFlag;

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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public Integer getFieldIsRequired() {
        return fieldIsRequired;
    }

    public void setFieldIsRequired(Integer fieldIsRequired) {
        this.fieldIsRequired = fieldIsRequired;
    }

    public String getFieldValidationRule() {
        return fieldValidationRule;
    }

    public void setFieldValidationRule(String fieldValidationRule) {
        this.fieldValidationRule = fieldValidationRule;
    }

    public String getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldDesc(String fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getRelateTo() {
        return relateTo;
    }

    public void setRelateTo(String relateTo) {
        this.relateTo = relateTo;
    }

    public String getRelateType() {
        return relateType;
    }

    public void setRelateType(String relateType) {
        this.relateType = relateType;
    }

    public String getFieldOptions() {
        return fieldOptions;
    }

    public void setFieldOptions(String fieldOptions) {
        this.fieldOptions = fieldOptions;
    }

    public String getBondDictCode() {
        return bondDictCode;
    }

    public void setBondDictCode(String bondDictCode) {
        this.bondDictCode = bondDictCode;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnLength() {
        return columnLength;
    }

    public void setColumnLength(String columnLength) {
        this.columnLength = columnLength;
    }

    public Boolean getPrimaryFlag() {
        return primaryFlag;
    }

    public void setPrimaryFlag(Boolean primaryFlag) {
        this.primaryFlag = primaryFlag;
    }

    public boolean isRequired() {
        return fieldIsRequired != null && fieldIsRequired == 1;
    }

    public boolean isPrimary() {
        return primaryFlag != null && primaryFlag;
    }
}
