package com.zifang.z.lc.sdk.spi.workflow;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 工作流上下文注入.
 * <p>
 * 设计哲学: 引擎在 <b>流程启动时, 业务方把业务数据注入流程变量 (供后续节点使用)</b>.
 * <p>
 * 调用时机: 流程启动时, 业务方把业务数据注入流程变量 (供后续节点使用)
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="工作流", code="WorkflowContextInjectService") 自动索引.
 * <p>
 * z-camuda 模块应实现这些 SPI 以桥接工作流引擎.
 */
@InterfaceMapping(name = "工作流上下文注入", code = "WorkflowContextInjectService", group = "工作流")
public interface WorkflowContextInjectService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文
     * @return 处理结果
     */
    Result<Map<String, Object>> inject(ExtensionServiceContext context, Map<String, Object> data);
}
