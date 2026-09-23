package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FieldTypeHandler 抽象基类: 沉淀共性逻辑 (required 判空 / 长度校验 / describe 组装 /
 * 通用操作符集合), 子类只声明差异.
 *
 * @author zifang
 */
public abstract class AbstractFieldTypeHandler implements FieldTypeHandler {

    /**
     * 标识符白名单正则 (与 DynamicSqlBuilder 对 fieldCode 的约束一致).
     */
    protected static final String CODE_REGEX = "^[A-Za-z][A-Za-z0-9_]*$";

    /**
     * 文本类可用的比较操作符 (eq/like 系 + 范围 + in).
     */
    protected static final List<String> TEXT_OPS = Collections.unmodifiableList(Arrays.asList(
            "eq", "ne", "like", "notLike", "in", "notIn", "isNull", "isNotNull"));

    /**
     * 数值/日期类可用的比较操作符 (范围比较, 无 like).
     */
    protected static final List<String> COMPARE_OPS = Collections.unmodifiableList(Arrays.asList(
            "eq", "ne", "gt", "gte", "lt", "lte", "in", "notIn", "isNull", "isNotNull"));

    /**
     * 布尔类操作符.
     */
    protected static final List<String> BOOL_OPS = Collections.unmodifiableList(Arrays.asList(
            "eq", "ne", "isNull", "isNotNull"));

    /**
     * 日期类操作符 (范围比较 + 空判, 一般不做 in).
     */
    protected static final List<String> DATE_OPS = Collections.unmodifiableList(Arrays.asList(
            "eq", "ne", "gt", "gte", "lt", "lte", "isNull", "isNotNull"));

    /**
     * required 判空语义 (与历史 RequiredCheckProcessor 完全一致):
     * null / 空串 / 空集合 视为空.
     */
    public static boolean isEmptyValue(Object v) {
        if (v == null) {
            return true;
        }

        if (v instanceof String) {
            return ((String) v).isEmpty();
        }

        if (v instanceof java.util.Collection) {
            return ((java.util.Collection<?>) v).isEmpty();
        }

        return false;
    }

    @Override
    public List<ValidationError> validate(FieldDefDTO def) {
        List<ValidationError> errors = new ArrayList<>();
        if (def == null) {
            return errors;
        }

        if (def.getFieldCode() == null || !def.getFieldCode().matches(CODE_REGEX)) {
            errors.add(new ValidationError(def.getFieldCode(), "config", "fieldCode 必须匹配 " + CODE_REGEX));
        }

        validateConfig(def, errors);
        return errors;
    }

    /**
     * 子类扩展点: 追加类型专属的配置校验.
     */
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        // 默认无额外配置校验
    }

    @Override
    public List<ValidationError> validateValue(FieldDefDTO def, Object cellValue) {
        List<ValidationError> errors = new ArrayList<>();
        String fieldCode = def == null ? null : def.getFieldCode();
        boolean required = def != null && Boolean.TRUE.equals(def.getRequired());

        if (isEmptyValue(cellValue)) {
            if (required) {
                errors.add(new ValidationError(fieldCode, "required",
                        "字段 [" + (def == null ? "" : def.getFieldName()) + "] (" + fieldCode + ") 为必填"));
            }

            return errors;
        }
        // 非必填但给了非法类型值, 子类继续检查
        errors.addAll(checkValue(def, cellValue));
        // 通用最大长度校验 (仅对字符串值按 fieldLength 判断)
        Integer maxLen = def == null ? null : def.getFieldLength();
        if (maxLen != null && maxLen > 0 && cellValue instanceof String) {
            String s = (String) cellValue;
            if (s.length() > maxLen) {
                errors.add(new ValidationError(fieldCode, "maxLength",
                        "字段 (" + fieldCode + ") 长度 " + s.length() + " 超过上限 " + maxLen));
            }
        }

        return errors;
    }

    /**
     * 子类扩展点: 类型专属的值检查 (数值解析/范围/布尔/日期格式).
     */
    protected List<ValidationError> checkValue(FieldDefDTO def, Object cellValue) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fieldType", fieldType());
        m.put("cellValueType", cellValueType().getCode());
        m.put("label", label());
        m.put("dbType", dbType(null, null));
        m.put("widget", widget());
        m.put("sortable", sortable());
        m.put("groupable", groupable());
        m.put("filterable", filterable());
        m.put("inlineEditable", inlineEditable());
        m.put("operators", operators());
        return m;
    }
}
