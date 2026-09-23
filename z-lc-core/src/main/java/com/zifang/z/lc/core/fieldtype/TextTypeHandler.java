package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.List;

/**
 * TEXT → TEXT. 长文本: 不可排序 (数据库对 TEXT 排序代价高), 可过滤/分组/行内编辑.
 *
 * @author zifang
 */
public class TextTypeHandler extends StringTypeHandler {

    @Override
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        // TEXT 物理类型为 TEXT, 不依赖 fieldLength, 无需 STRING 的 fieldLength 提示
    }

    @Override
    public String fieldType() {
        return "TEXT";
    }

    @Override
    public String label() {
        return "多行长文本";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "TEXT";
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
    public String widget() {
        return "textarea";
    }
}
