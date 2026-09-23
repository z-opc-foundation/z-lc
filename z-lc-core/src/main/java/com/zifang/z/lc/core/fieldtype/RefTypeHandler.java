package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.List;

/**
 * REF → BIGINT (存目标实体 id; 外键扩展由 DynamicSqlBuilder 的 LEFT JOIN 完成).
 * 配置上必须声明 refEntity; 前端用专用选择器, 不支持行内编辑.
 *
 * @author zifang
 */
public class RefTypeHandler extends LongBackedTypeHandler {

    @Override
    public String fieldType() {
        return "REF";
    }

    @Override
    public String label() {
        return "实体引用";
    }

    @Override
    protected void validateConfig(FieldDefDTO def, List<ValidationError> errors) {
        if (def.getRefEntity() == null || def.getRefEntity().isEmpty()) {
            errors.add(new ValidationError(def.getFieldCode(), "config",
                    "REF 字段必须声明 refEntity"));
        }
    }

    @Override
    public boolean inlineEditable() {
        return false;
    }

    @Override
    public String widget() {
        return "ref";
    }
}
