package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.executor.DynamicSqlBuilder;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 类型转换处理器: 入参强制类型 (复用 DynamicSqlBuilder.coerce). null 跳过.
 */
@Component
public class TypeConvertProcessor implements FieldProcessor {

    @Autowired
    private DynamicSqlBuilder sqlBuilder;

    @Override
    public String name() {
        return "TypeConvert";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        if (entity == null || entity.getFields() == null || body == null || body.getFieldValues() == null) {
            return;
        }

        for (FieldDefDTO f : entity.getFields()) {
            if (f.getFieldCode() == null) {
                continue;
            }

            Object v = body.getFieldValues().get(f.getFieldCode());
            if (v == null) {
                continue;
            }
            try {
                Object coerced = sqlBuilder.coerce(v, f.getFieldType());
                body.getFieldValues().put(f.getFieldCode(), coerced);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("字段 [" + f.getFieldName() + "] ("
                        + f.getFieldCode() + ") 类型转换失败: " + ex.getMessage());
            }
        }
    }

    @Override
    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        // 读出不再做转换 (JdbcTemplate 已按类型映射)
    }
}
