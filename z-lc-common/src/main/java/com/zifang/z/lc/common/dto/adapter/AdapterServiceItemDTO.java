package com.zifang.z.lc.common.dto.adapter;

import java.io.Serializable;
import java.util.List;

/**
 * 服务项调整 DTO — 把多个服务从一个位置调整到另一个位置.
 *
 * <p>蒸馏自 ace-platform-core {@code AdapterServiceItemDTO}
 * （{@code com.c2f.ace.core.service.app.dto}），字段语义完全对齐.
 *
 * <p>用途：「服务集成」中拖拽移动功能 — 一次请求把多个服务从原分组移动到目标分组.
 *
 * @author zifang
 */
public class AdapterServiceItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待调整的服务项 id 列表.
     */
    private List<Long> sourceItemIds;

    /**
     * 目标树节点 id（目标父节点）.
     */
    private String targetTreeNodeId;

    public List<Long> getSourceItemIds() {
        return sourceItemIds;
    }

    public void setSourceItemIds(List<Long> sourceItemIds) {
        this.sourceItemIds = sourceItemIds;
    }

    public String getTargetTreeNodeId() {
        return targetTreeNodeId;
    }

    public void setTargetTreeNodeId(String targetTreeNodeId) {
        this.targetTreeNodeId = targetTreeNodeId;
    }
}
