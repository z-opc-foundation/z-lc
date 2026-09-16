package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 表单生命周期管理.
 * <p>
 * 设计哲学: 引擎在 <b>表单全生命周期事件管理 (init/submit/approve/reject/destroy), 业务方可统一拦截</b>.
 * <p>
 * 调用时机: 表单全生命周期事件管理 (init/submit/approve/reject/destroy), 业务方可统一拦截
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> (或 @Service) + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="表单", code="FormDataLifecycleService") 自动索引.
 */
@InterfaceMapping(name = "表单生命周期管理", code = "FormDataLifecycleService", group = "表单")
public interface FormDataLifecycleService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (appCode / modelCode / workflowKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Boolean> onLifecycle(ExtensionServiceContext context, String event, Map<String, Object> data);
}
