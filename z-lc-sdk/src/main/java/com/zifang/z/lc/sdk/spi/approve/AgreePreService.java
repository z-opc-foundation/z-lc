package com.zifang.z.lc.sdk.spi.approve;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 同意前置处理.
 * <p>
 * 设计哲学: 引擎在 <b>审批人点击同意按钮后, 业务方对审批意见/数据加工 (加签/转办预处理)</b>.
 * <p>
 * 调用时机: 审批人点击同意按钮后, 业务方对审批意见/数据加工 (加签/转办预处理)
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="审批", code="AgreePreService") 自动索引.
 * <p>
 * z-camuda 模块应实现这些 SPI 以桥接工作流引擎.
 */
@InterfaceMapping(name = "同意前置处理", code = "AgreePreService", group = "审批")
public interface AgreePreService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (processInstanceId / workflowDefinitionKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Map<String, Object>> preAgree(ExtensionServiceContext context, Map<String, Object> data);
}
