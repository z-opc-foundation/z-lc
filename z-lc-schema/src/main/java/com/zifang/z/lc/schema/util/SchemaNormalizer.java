package com.zifang.z.lc.schema.util;

import com.zifang.z.lc.schema.model.PageSchema;

import java.util.Map;

/**
 * Schema 规范化：反序列化后补齐缺省字段、固化默认版本，保证下游拿到结构完整的对象。
 */
public final class SchemaNormalizer {

    private SchemaNormalizer() {
    }

    public static PageSchema normalize(PageSchema schema) {
        if (schema == null) {
            return new PageSchema();
        }
        if (schema.getVersion() == null || schema.getVersion().isEmpty()) {
            schema.setVersion(PageSchema.CURRENT_VERSION);
        }
        if (schema.getElements() == null) {
            schema.setElements(new java.util.LinkedHashMap<>());
        }
        if (schema.getLayout() == null) {
            schema.setLayout(new PageSchema.Layout());
        }
        if (schema.getImports() == null) {
            schema.setImports(new java.util.ArrayList<>());
        }
        if (schema.getContext() == null) {
            schema.setContext(new com.zifang.z.lc.schema.model.SchemaContext());
        }
        if (schema.getLogic() == null) {
            schema.setLogic(new com.zifang.z.lc.schema.logic.LogicDef());
        }
        if (schema.getMeta() == null) {
            schema.setMeta(new java.util.LinkedHashMap<>());
        }
        // 结构表每个容器补空 NodeStructure
        for (String id : schema.getLayout().getStructure().keySet()) {
            if (schema.getLayout().getStructure().get(id) == null) {
                schema.getLayout().getStructure().put(id, new com.zifang.z.lc.schema.model.NodeStructure());
            }
        }
        return schema;
    }
}
