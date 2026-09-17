package com.zifang.z.lc.common.dto.adapter;

import java.io.Serializable;
import java.util.List;

/**
 * 字典项调整 DTO — 把多个字典项从一个位置调整到另一个位置.
 *
 * <p>蒸馏自 ace-platform-core {@code AdapterDictItemDTO}
 * （{@code com.c2f.ace.core.service.app.dto}），字段语义完全对齐.
 *
 * <p>用途：字典管理中「拖拽移动」功能 — 一次请求把多个字典项
 * 从原父节点移动到目标父节点.
 *
 * @author zifang
 */
public class AdapterDictItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待调整的字典项 id 列表.
     */
    private List<Long> sourceDictItemIds;

    /**
     * 目标字典树节点 id（目标父节点）.
     */
    private String targetDictTreeNodeId;

    public List<Long> getSourceDictItemIds() {
        return sourceDictItemIds;
    }

    public void setSourceDictItemIds(List<Long> sourceDictItemIds) {
        this.sourceDictItemIds = sourceDictItemIds;
    }

    public String getTargetDictTreeNodeId() {
        return targetDictTreeNodeId;
    }

    public void setTargetDictTreeNodeId(String targetDictTreeNodeId) {
        this.targetDictTreeNodeId = targetDictTreeNodeId;
    }
}
