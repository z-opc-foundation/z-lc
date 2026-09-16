package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 表单暂存后置处理.
 * <p>
 * 设计哲学: 引擎在 <b>表单暂存后, 落库前 — 业务方可以二次加工或写入第三方</b>.
 * <p>
 * 调用时机: 表单暂存后, 落库前 — 业务方可以二次加工或写入第三方
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> (或 @Service) + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="表单", code="FormDataTempPostHandlerService") 自动索引.
 */
@InterfaceMapping(name = "表单暂存后置处理", code = "FormDataTempPostHandlerService", group = "表单")
public interface FormDataTempPostHandlerService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (appCode / modelCode / workflowKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Map<String, Object>> postHandler(ExtensionServiceContext context, Map<String, Object> data);
}
