package com.zifang.z.lc.sdk.spi.model;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

import java.util.Map;

/**
 * 模型增删改查扩展.
 * <p>
 * 设计哲学: 模型 CRUD 扩展点, 业务方可在 save/delete 前置后置扩展.
 * <p>
 * 实现规范: 实现类必须标 <code>@Component</code> + <code>@InterfaceMapping</code>
 * — 引擎通过 (group="模型", code="ModelService") 自动索引.
 * <p>
 * 业务模块 (z-task / z-ctc / z-meta) 应实现这些 SPI 以桥接 z-lc.
 */
@InterfaceMapping(name = "模型增删改查扩展", code = "ModelService", group = "模型")
public interface ModelService {

    /**
     * 引擎回调入口.
     *
     * @param context SPI 调用上下文
     * @return 处理结果
     */
    Result<Object> handler(ExtensionServiceContext context, String action, Map<String, Object> data);
}
