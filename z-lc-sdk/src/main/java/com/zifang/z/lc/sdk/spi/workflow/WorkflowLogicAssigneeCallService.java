package com.zifang.z.lc.sdk.spi.workflow;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.List;
import java.util.Map;

/**
 * 工作流审批人回调.
 * <p>
 * 设计哲学: 引擎在 <b>节点审批人确定后, 业务方补充逻辑 (加签/委托/转办)</b>.
 * <p>
 * 调用时机: 节点审批人确定后, 业务方补充逻辑 (加签/委托/转办)
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="工作流", code="WorkflowLogicAssigneeCallService") 自动索引.
 * <p>
 * z-camuda 模块应实现这些 SPI 以桥接工作流引擎.
 */
@InterfaceMapping(name = "工作流审批人回调", code = "WorkflowLogicAssigneeCallService", group = "工作流")
public interface WorkflowLogicAssigneeCallService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文
     * @return 处理结果
     */
    Result<List<String>> callAssignees(ExtensionServiceContext context, List<String> defaultAssignees, Map<String, Object> nodeConfig);
}
