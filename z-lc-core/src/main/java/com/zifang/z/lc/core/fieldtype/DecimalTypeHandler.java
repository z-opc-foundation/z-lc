package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * DECIMAL → DECIMAL(p,s) (默认 DECIMAL(18,2), 与历史 jdbcType 一致).
 * 写入强转 Double; 可带 fieldLength=精度 / scale=小数位 配置校验.
 *
 * @author zifang
 */
public class DecimalTypeHandler extends AbstractFieldTypeHandler {

    @Override
    public String fieldType() {
        return "DECIMAL";
    }

    @Override
    public CellValueType cellValueType() {
        return CellValueType.NUMBER;
    }

    @Override
    public String label() {
        return "高精度小数";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        int p = length == null ? 18 : Math.max(1, length);
        int s = scale == null ? 2 : Math.max(0, scale);
        return "DECIMAL(" + p + "," + s + ")";
    }

    @Override
    public Object coerce(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }

        try {
            return Double.valueOf(raw.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Field requires decimal: " + raw);
        }
    }

    @Override
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        Integer len = def.getFieldLength();
        Integer scale = def.getScale();
        if (len != null && (len < 1 || len > 65)) {
            errors.add(new ValidationError(def.getFieldCode(), "config",
                    "DECIMAL 精度须在 [1,65], 实际: " + len));
        }

        if (scale != null && scale < 0) {
            errors.add(new ValidationError(def.getFieldCode(), "config",
                    "DECIMAL scale 不能为负, 实际: " + scale));
        }

        if (len != null && scale != null && scale > len) {
            errors.add(new ValidationError(def.getFieldCode(), "config",
                    "DECIMAL scale(" + scale + ") 不能大于精度(" + len + ")"));
        }
    }

    @Override
    protected List<ValidationError> checkValue(FieldDefDTO def, Object cellValue) {
        List<ValidationError> errors = new ArrayList<>();
        String code = def == null ? null : def.getFieldCode();
        double d;
        if (cellValue instanceof Number) {
            d = ((Number) cellValue).doubleValue();
        } else {
            try {
                d = Double.parseDouble(cellValue.toString());
            } catch (NumberFormatException e) {
                errors.add(new ValidationError(code, "invalidDecimal",
                        "字段 (" + code + ") 需要小数, 实际值: " + cellValue));
                return errors;
            }
        }

        if (Double.isNaN(d) || Double.isInfinite(d)) {
            errors.add(new ValidationError(code, "outOfRange",
                    "字段 (" + code + ") 不是有限小数: " + cellValue));
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
