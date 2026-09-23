package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 结构化查询条件 (单条): {fieldCode, operator, value}
 * <p>
 * operator 取值 ∈ eq, ne, like, notLike, gt, gte, lt, lte, in, notIn, isNull, isNotNull.
 * fieldCode 必须命中实体已声明字段白名单, 否则整体查询被拒绝 (不拼任何 SQL).
 */
public class QueryConditionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段编码 (白名单校验)
     */
    private String fieldCode;

    /**
     * 操作符: eq/ne/like/notLike/gt/gte/lt/lte/in/notIn/isNull/isNotNull
     */
    private String operator;

    /**
     * 比较值 (in/notIn 时为数组; isNull/isNotNull 时忽略)
     */
    private Object value;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
