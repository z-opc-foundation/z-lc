package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.List;

/**
 * STRING → VARCHAR(n) (默认 255). 文本类基础 handler.
 *
 * @author zifang
 */
public class StringTypeHandler extends AbstractFieldTypeHandler {

    @Override
    public String fieldType() {
        return "STRING";
    }

    @Override
    public CellValueType cellValueType() {
        return CellValueType.STRING;
    }

    @Override
    public String label() {
        return "单行文本";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        int l = length == null ? 255 : Math.max(1, length);
        return "VARCHAR(" + l + ")";
    }

    @Override
    public Object coerce(Object raw) {
        return raw.toString();
    }

    @Override
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        // 配置校验 (advisory, 不参与既有 DDL 生成路径): STRING 应显式声明 fieldLength
        if (def.getFieldLength() == null || def.getFieldLength() <= 0) {
            errors.add(new ValidationError(def.getFieldCode(), "config",
                    "STRING 类型建议显式声明 fieldLength (>0), 否则按默认 255"));
        }
    }

    @Override
    public List<String> operators() {
        return TEXT_OPS;
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
        return "input";
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

        return text;
    }
}
