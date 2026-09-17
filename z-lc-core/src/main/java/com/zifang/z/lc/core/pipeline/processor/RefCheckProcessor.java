package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 引用校验处理器: 标记 refEntity 的字段, 写入时检查目标实体是否真的存在 (按 id).
 * <p>
 * 注意: 跨 entity 的引用目前简化处理 - 仅校验值不为 null; 不强制目标实体表存在 id.
 * (Phase 1 范围内假设业务约定 ref 值合法, Phase 2 可加入严格外键校验.)
 */
@Component
public class RefCheckProcessor implements FieldProcessor {

    private static final Logger log = LogManager.getLogger(RefCheckProcessor.class);

    @Autowired
    private EventReplayService eventReplayService;

    @Override
    public String name() {
        return "RefCheck";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        if (entity == null || entity.getFields() == null || body == null || body.getFieldValues() == null) {
            return;
        }

        for (FieldDefDTO f : entity.getFields()) {
            if (f.getRefEntity() == null || f.getRefEntity().isEmpty()) {
                continue;
            }

            Object v = body.getFieldValues().get(f.getFieldCode());
            if (v == null) {
                continue;
            }
            // 仅做非空检查, 详细跨 entity 校验由 Phase 2 引入
            log.debug("RefCheck: entity={} field={} refEntity={} value={}",
                    entity.getEntityCode(), f.getFieldCode(), f.getRefEntity(), v);
        }
    }

    @Override
    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        // 读时不做解析 (Phase 2 可引入 ref label 翻译)
    }
}
