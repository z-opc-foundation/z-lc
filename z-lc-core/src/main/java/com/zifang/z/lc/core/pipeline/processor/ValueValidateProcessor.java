package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.fieldtype.FieldTypeHandler;
import com.zifang.z.lc.core.fieldtype.FieldTypeRegistry;
import com.zifang.z.lc.core.fieldtype.ValidationError;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 逐字段的值校验处理器 —— 走 {@link FieldTypeHandler#validateValue}，
 * 也就是长度、数值范围、日期可解析性这些**类型自带**的规则。
 * <p>
 * 为什么单独一个处理器：注册表里早就实现了 maxLength 等校验，但写入链路以前只跑
 * RequiredCheck / TypeConvert / Dict / Ref，从没调用过 validateValue。
 * 结果是一段比 {@code fieldLength} 更长的字符串能顺利通过 preview，
 * 然后在 commit 时才被数据库以 "Value too long" 打回来 —— 正是"预览说能过、真写却失败"，
 * 对批量导入尤其伤：用户在第 3 步才看到本该在第 1 步就报出来的错。
 */
@Component
public class ValueValidateProcessor implements FieldProcessor {

    @Override
    public String name() {
        return "ValueValidate";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        if (entity == null || entity.getFields() == null || body == null) {
            return;
        }
        Map<String, Object> values = body.getFieldValues();
        if (values == null) {
            return;
        }
        FieldTypeRegistry registry = FieldTypeRegistry.getDefault();
        for (FieldDefDTO field : entity.getFields()) {
            if (field == null || field.getFieldCode() == null) {
                continue;
            }
            // 只校验"这次真的带了值"的列；部分更新没提交的列由 RequiredCheck 那套语义负责
            if (!values.containsKey(field.getFieldCode())) {
                continue;
            }
            Object value = values.get(field.getFieldCode());
            if (FieldTypeRegistry.isEmptyValue(value)) {
                continue;
            }
            FieldTypeHandler handler = registry.handler(field.getFieldType());
            if (handler == null) {
                continue;
            }
            List<ValidationError> errors = handler.validateValue(field, value);
            if (errors != null && !errors.isEmpty()) {
                ValidationError first = errors.get(0);
                String label = field.getFieldName() == null ? field.getFieldCode() : field.getFieldName();
                throw new IllegalArgumentException("字段 [" + label + "] "
                        + (first.getMessage() == null ? first.getErrorType() : first.getMessage()));
            }
        }
    }

    @Override
    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        // 读路径不做校验
    }
}
