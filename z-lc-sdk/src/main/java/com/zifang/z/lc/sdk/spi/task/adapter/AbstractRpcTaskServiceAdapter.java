package com.zifang.z.lc.sdk.spi.task.adapter;

import com.zifang.z.lc.sdk.spi.task.AbstractTaskService;

/**
 * RPC 任务服务适配器抽象基类 — 蒸馏自 ace-platform-engine {@code AbstractRpcTaskServiceAdapter}
 * （{@code com.c2f.ace.engine.adapter}），行为完全对齐.
 *
 * <p>包含：
 * <ul>
 *   <li>{@link #group} — RPC 服务分组（默认 {@code "z-lc-extensions"}）</li>
 *   <li>{@link #version(String)} — 根据 identityCode 生成版本号（{@code "1.0.0_<identityCode>"}）</li>
 * </ul>
 *
 * <p>子类（{@link ZLcRpcTaskServiceAdapterProvider} / {@link ZLcRpcTaskServiceAdapterInvoker}）
 * 继承此基类获得 group + version 模板.
 *
 * @author zifang
 */
public abstract class AbstractRpcTaskServiceAdapter implements TaskServiceAdapter {

    /** RPC 服务分组. */
    protected String group = "z-lc-extensions";

    /** 版本号模板（{@code %s} 占位符为 identityCode） */
    private static final String VERSION_TEMPLATE = "1.0.0_%s";

    /**
     * 根据 identityCode 生成版本号.
     *
     * @param identityCode {@link com.zifang.z.lc.sdk.spi.task.TaskServiceInfo#identityCode()}
     * @return 版本号字符串
     */
    public static String version(String identityCode) {
        return String.format(VERSION_TEMPLATE, identityCode);
    }
}
