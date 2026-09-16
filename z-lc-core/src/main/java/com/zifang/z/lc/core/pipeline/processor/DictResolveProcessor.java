package com.zifang.z.lc.core.pipeline.processor;

import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.adapter.MetaAdapter;
import com.zifang.z.lc.core.pipeline.FieldProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典解析处理器: 读出时把 dict 字段的值翻译成 label (附加 {}_label 列).
 * <p>
 * 写入时不做事 (沿用值; 后续可加值合法性校验).
 */
@Component
public class DictResolveProcessor implements FieldProcessor {

    private static final Logger log = LogManager.getLogger(DictResolveProcessor.class);

    @Autowired
    private MetaAdapter metaAdapter;

    @Override
    public String name() {
        return "DictResolve";
    }

    @Override
    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        // 写时不校验值合法性 (留给上层业务规则)
    }

    @Override
    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        if (entity == null || entity.getFields() == null || row == null) return;
        String tenantCode = entity.getTenantCode();
        for (FieldDefDTO f : entity.getFields()) {
            if (f.getDictCode() == null || f.getDictCode().isEmpty()) continue;
            Object val = row.get(f.getFieldCode());
            if (val == null) continue;
            List<DictItemDTO> items = metaAdapter.listDictItems(tenantCode, f.getDictCode());
            if (items == null || items.isEmpty()) continue;
            for (DictItemDTO it : items) {
                if (matches(it, val)) {
                    row.put(f.getFieldCode() + "_label", it.getItemLabel());
                    break;
                }
            }
        }
    }

    @Override
    public void postReadList(EntityDefDTO entity, List<Map<String, Object>> rows) {
        if (entity == null || entity.getFields() == null || rows == null) return;
        // 收集本批涉及到的 dict, 一次性拉, 减少 HTTP 出口
        Map<String, List<DictItemDTO>> dictItems = new HashMap<>();
        String tenantCode = entity.getTenantCode();
        for (FieldDefDTO f : entity.getFields()) {
            if (f.getDictCode() == null || f.getDictCode().isEmpty()) continue;
            if (dictItems.containsKey(f.getDictCode())) continue;
            dictItems.put(f.getDictCode(), metaAdapter.listDictItems(tenantCode, f.getDictCode()));
        }
        for (Map<String, Object> row : rows) {
            for (FieldDefDTO f : entity.getFields()) {
                if (f.getDictCode() == null || f.getDictCode().isEmpty()) continue;
                Object val = row.get(f.getFieldCode());
                if (val == null) continue;
                List<DictItemDTO> items = dictItems.get(f.getDictCode());
                if (items == null || items.isEmpty()) continue;
                for (DictItemDTO it : items) {
                    if (matches(it, val)) {
                        row.put(f.getFieldCode() + "_label", it.getItemLabel());
                        break;
                    }
                }
            }
        }
    }

    private boolean matches(DictItemDTO it, Object val) {
        if (it.getItemValue() != null && it.getItemValue().equals(String.valueOf(val))) return true;
        if (it.getItemCode() != null && it.getItemCode().equals(String.valueOf(val))) return true;
        return false;
    }
}
