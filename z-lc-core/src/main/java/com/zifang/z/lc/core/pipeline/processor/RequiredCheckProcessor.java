package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.fieldtype.FieldTypeRegistry;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 必填校验处理器: 字段标记 required=true 时, **改完之后那一行**的值为空则抛异常.
 * 部分更新 (inline 编辑 / 看板拖拽只提交一列) 时, 未提交的列以库里现值参与判定.
 * <p>
 * 判空语义下沉到 {@link FieldTypeRegistry#isEmptyValue(Object)}, 与 FieldTypeHandler.validateValue
 * 的 required 分支保持唯一来源, 不再各自实现.
 */
@Component
public class RequiredCheckProcessor implements FieldProcessor {

    @Override
    public String name() {
        return "RequiredCheck";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        if (entity == null || entity.getFields() == null) {
            return;
        }

        Map<String, Object> values = body.getFieldValues() == null ? java.util.Collections.emptyMap() : body.getFieldValues();
        // 更新路径下 body.existingValues 由服务端填; 提交里没出现的列沿用库里现值.
        // 提交里出现了但值为空, 仍然算"用户想清空", 该拒还是拒.
        Map<String, Object> existing = body.getExistingValues();
        boolean partialUpdate = existing != null && !existing.isEmpty();
        for (FieldDefDTO f : entity.getFields()) {
            if (f.getRequired() == null || !f.getRequired()) {
                continue;
            }

            String code = f.getFieldCode();
            Object v = values.get(code);
            if (partialUpdate && !values.containsKey(code)) {
                v = existing.get(code);
            }
            if (FieldTypeRegistry.isEmptyValue(v)) {
                throw new IllegalArgumentException("字段 [" + f.getFieldName() + "] ("
                        + f.getFieldCode() + ") 为必填");
            }
        }
    }

    @Override
    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        // 不在读路径做处理
    }
}
