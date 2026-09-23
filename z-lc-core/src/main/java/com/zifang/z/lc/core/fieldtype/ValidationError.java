package com.zifang.z.lc.core.fieldtype;

import java.io.Serializable;

/**
 * 字段校验的结构化错误项 (一条错误 = fieldCode + errorType + message).
 * <p>
 * 设计: 校验不抛首个异常, 而是返回错误列表, 供前端一次性展示全部问题.
 *
 * @author zifang
 */
public class ValidationError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被校验字段编码
     */
    private final String fieldCode;

    /**
     * 错误类别: required / maxLength / invalidNumber / invalidDecimal / invalidBoolean /
     * invalidDate / outOfRange / config ...
     */
    private final String errorType;

    /**
     * 可读错误描述
     */
    private final String message;

    public ValidationError(String fieldCode, String errorType, String message) {
        this.fieldCode = fieldCode;
        this.errorType = errorType;
        this.message = message;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationError{fieldCode='" + fieldCode + "', errorType='" + errorType + "', message='" + message + "'}";
    }
}
