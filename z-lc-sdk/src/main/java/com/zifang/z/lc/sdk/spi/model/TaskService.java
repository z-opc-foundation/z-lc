package com.zifang.z.lc.sdk.spi.model;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 任务扩展服务.
 * <p>
 * 设计哲学: 任务模型扩展, 业务方可包装 z-task 模块的任务对象.
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="模型", code="TaskService") 自动索引.
 * <p>
 * 业务模块 (z-task / z-ctc / z-meta) 应实现这些 SPI 以桥接 z-lc.
 */
@InterfaceMapping(name = "任务扩展服务", code = "TaskService", group = "模型")
public interface TaskService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文
     * @return 处理结果
     */
    Result<Map<String, Object>> handler(ExtensionServiceContext context, String action, Map<String, Object> data);
}
