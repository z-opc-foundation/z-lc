package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 表单暂存校验.
 * <p>
 * 设计哲学: 引擎在 <b>表单暂存 (草稿) 前的校验, 弱校验 — 允许部分字段缺失</b>.
 * <p>
 * 调用时机: 表单暂存 (草稿) 前的校验, 弱校验 — 允许部分字段缺失
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> (或 @Service) + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="表单", code="FormDataTempValidateService") 自动索引.
 */
@InterfaceMapping(name = "表单暂存校验", code = "FormDataTempValidateService", group = "表单")
public interface FormDataTempValidateService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文 (appCode / modelCode / workflowKey ...)
     * @return 处理结果. Result.success() = 通过; Result.error() = 中断流程
     */
    Result<Boolean> validate(ExtensionServiceContext context, Map<String, Object> data);
}
