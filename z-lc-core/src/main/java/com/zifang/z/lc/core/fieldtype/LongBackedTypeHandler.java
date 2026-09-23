package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.List;

/**
 * 整数家族公共基类 (INT/LONG/REF): BIGINT 物理列, Number 单元格, 写入强转 Long.
 * 与历史 DynamicSqlBuilder.coerce 一致: Number → longValue, 字符串 → Long.parseLong.
 *
 * @author zifang
 */
public abstract class LongBackedTypeHandler extends AbstractFieldTypeHandler {

    @Override
    public CellValueType cellValueType() {
        return CellValueType.NUMBER;
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "BIGINT";
    }

    @Override
    public Object coerce(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }

        try {
            return Long.valueOf(raw.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Field requires long: " + raw);
        }
    }

    @Override
    protected List<ValidationError> checkValue(FieldDefDTO def, Object cellValue) {
        List<ValidationError> errors = new java.util.ArrayList<>();
        String code = def == null ? null : def.getFieldCode();
        if (cellValue instanceof Number) {
            double d = ((Number) cellValue).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)
                    || d > (double) Long.MAX_VALUE || d < (double) Long.MIN_VALUE) {
                errors.add(new ValidationError(code, "outOfRange", "字段 (" + code + ") 数值超出 BIGINT 范围: " + cellValue));
            }

            return errors;
        }

        try {
            Long.parseLong(cellValue.toString());
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(code, "invalidNumber", "字段 (" + code + ") 需要整数, 实际值: " + cellValue));
        }

        return errors;
    }

    @Override
    public List<String> operators() {
        return COMPARE_OPS;
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
        return "number";
    }

    @Override
    public String formatForExport(Object cellValue) {
        return cellValue == null ? "" : String.valueOf(cellValue);
    }

    @Override
    public Object parseFromCell(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        return coerce(text);
    }
}
