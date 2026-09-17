package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;

import java.util.List;
import java.util.Map;

/**
 * 字段处理器: 对 CRUD 入参 (preWrite) 或读出 (postRead) 做转换/校验/丰富
 * <p>
 * 顺序执行, 任何一个抛异常就中断链路 (异常由 Controller 转为 4xx 错误).
 */
public interface FieldProcessor {

    /**
     * 处理器名
     */
    String name();

    /**
     * 写入前处理 (校验 + 类型转换 + 默认值). 可改写 body.fieldValues.
     */
    void preWrite(EntityDefDTO entity, RuntimeCrudDTO body);

    /**
     * 读出后处理 (值翻译, 例如 dict code → label). 可改写 row.
     */
    void postRead(EntityDefDTO entity, Map<String, Object> row);

    /**
     * 列表读出后处理
     */
    default void postReadList(EntityDefDTO entity, List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            postRead(entity, row);
        }
    }
}
