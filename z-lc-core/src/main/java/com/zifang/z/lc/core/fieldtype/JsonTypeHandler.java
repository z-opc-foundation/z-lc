package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON → JSON. 半结构化列: 不可排序/分组/行内编辑, 仅 eq/ne/空判过滤.
 *
 * @author zifang
 */
public class JsonTypeHandler extends AbstractFieldTypeHandler {

    private static final List<String> OPS = Collections.unmodifiableList(Arrays.asList(
            "eq", "ne", "isNull", "isNotNull"));

    @Override
    public String fieldType() {
        return "JSON";
    }

    @Override
    public CellValueType cellValueType() {
        return CellValueType.STRING;
    }

    @Override
    public String label() {
        return "JSON 对象";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "JSON";
    }

    @Override
    public Object coerce(Object raw) {
        // 保持历史行为: 任意对象 toString (Map/List 走其文本表示)
        return raw.toString();
    }

    @Override
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        // JSON 无额外配置要求
    }

    @Override
    public List<String> operators() {
        return OPS;
    }

    @Override
    public boolean sortable() {
        return false;
    }

    @Override
    public boolean groupable() {
        return false;
    }

    @Override
    public boolean filterable() {
        return true;
    }

    @Override
    public boolean inlineEditable() {
        return false;
    }

    @Override
    public String widget() {
        return "json";
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
