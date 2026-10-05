package com.zifang.z.lc.sdk.spi.approve;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 同意后置处理.
 * <p>
 * 设计哲学: 引擎在 <b>审批同意后, 流程流转前 — 业务方异步任务触发</b>.
 * <p>
 * 调用时机: 审批同意后, 流程流转前 — 业务方异步任务触发
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="审批", code="AgreePostService") 自动索引.
 * <p>
 * z-camuda 模块应实现这些 SPI 以桥接工作流引擎.
 */
@InterfaceMapping(name = "同意后置处理", code = "AgreePostService", group = "审批")
public interface AgreePostService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (processInstanceId / workflowDefinitionKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Map<String, Object>> postAgree(ExtensionServiceContext context, Map<String, Object> data);
}
