package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 必填校验处理器: 字段标记 required=true 时, 入参 null/空 则抛异常.
 */
@Component
public class RequiredCheckProcessor implements FieldProcessor {

    private static boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String) return ((String) v).isEmpty();
        if (v instanceof java.util.Collection) return ((java.util.Collection<?>) v).isEmpty();
        return false;
    }

    @Override
    public String name() {
        return "RequiredCheck";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        if (entity == null || entity.getFields() == null) return;
        Map<String, Object> values = body.getFieldValues() == null ? java.util.Collections.emptyMap() : body.getFieldValues();
        for (FieldDefDTO f : entity.getFields()) {
            if (f.getRequired() == null || !f.getRequired()) continue;
            Object v = values.get(f.getFieldCode());
            if (isEmpty(v)) {
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
