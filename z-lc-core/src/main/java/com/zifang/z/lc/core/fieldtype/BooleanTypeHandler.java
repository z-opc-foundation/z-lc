package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * BOOLEAN → TINYINT(1).
 * <p>
 * coerce 与 {@link #checkValue} 共用同一套可接受写法 (true/false/1/0/Y/N/yes/no/on/off/t/f),
 * 避免出现"校验认为合法、写入却静默篡改成 false"的自相矛盾.
 *
 * @author zifang
 */
public class BooleanTypeHandler extends AbstractFieldTypeHandler {

    @Override
    public String fieldType() {
        return "BOOLEAN";
    }

    @Override
    public CellValueType cellValueType() {
        return CellValueType.BOOLEAN;
    }

    @Override
    public String label() {
        return "布尔";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "TINYINT(1)";
    }

    @Override
    public Object coerce(Object raw) {
        if (raw instanceof Boolean) {
            return raw;
        }

        // coerce 必须和 checkValue 给出同一个答案: checkValue 认 1/0 是合法布尔,
        // 而 Boolean.valueOf("1") 是 false —— 于是校验放行、写入篡改, `done:1` 被静默存成 0.
        // 这里只放宽"以前会被判成 false"的那些写法, 不改动任何既有合法值, 所以不存在存量兼容问题
        // (coerce 只作用于写入时的入参, 不会重新解释库里已存的行).
        if (raw instanceof Number) {
            return Boolean.valueOf(((Number) raw).doubleValue() != 0d);
        }

        String s = String.valueOf(raw).trim();
        if ("1".equals(s) || "Y".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)
                || "on".equalsIgnoreCase(s) || "t".equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }
        if ("0".equals(s) || "N".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)
                || "off".equalsIgnoreCase(s) || "f".equalsIgnoreCase(s)) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(s);
    }

    @Override
    protected List<ValidationError> checkValue(FieldDefDTO def, Object cellValue) {
        List<ValidationError> errors = new ArrayList<>();
        if (cellValue instanceof Boolean) {
            return errors;
        }

        String s = String.valueOf(cellValue).trim();
        if (!("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s) || "1".equals(s) || "0".equals(s))) {
            errors.add(new ValidationError(def == null ? null : def.getFieldCode(), "invalidBoolean",
                    "字段 (" + (def == null ? "" : def.getFieldCode()) + ") 需要布尔值 true/false/1/0, 实际: " + cellValue));
        }

        return errors;
    }

    @Override
    public List<String> operators() {
        return BOOL_OPS;
    }

    @Override
    public boolean sortable() {
        return true;
    }

    @Override
    public boolean groupable() {
        return true;
    }

    @Override
    public boolean filterable() {
        return true;
    }

    @Override
    public boolean inlineEditable() {
        return true;
    }

    @Override
    public String widget() {
        return "switch";
    }

    @Override
    public String formatForExport(Object cellValue) {
        if (cellValue == null) {
            return "";
        }

        if (cellValue instanceof Boolean) {
            return ((Boolean) cellValue) ? "true" : "false";
        }

        return String.valueOf(cellValue);
    }

    @Override
    public Object parseFromCell(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String s = text.trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return Boolean.TRUE;
        }

        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return Boolean.FALSE;
        }

        throw new IllegalArgumentException("Field requires boolean: " + text);
    }
}
