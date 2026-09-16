package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 表单初始化.
 * <p>
 * 设计哲学: 引擎在 <b>表单发起时 (含纯表单 + 流程表单), 业务方对初始数据加工 (默认值/字段注入)</b>.
 * <p>
 * 调用时机: 表单发起时 (含纯表单 + 流程表单), 业务方对初始数据加工 (默认值/字段注入)
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> (或 @Service) + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="表单", code="FormDataInitService") 自动索引.
 */
@InterfaceMapping(name = "表单初始化", code = "FormDataInitService", group = "表单")
public interface FormDataInitService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (appCode / modelCode / workflowKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Map<String, Object>> init(ExtensionServiceContext context, Map<String, Object> data);
}
