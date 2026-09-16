package com.zifang.z.lc.core.adapter;

/**
 * 适配器统一接口: z-lc 通过 Adapter 抽象屏蔽下游模块 (z-meta / z-ctc / z-script ...)
 * <p>
 * 每个 Adapter 负责一个外部能力域, 优先级用于排序 (数字越小越靠前).
 */
public interface Adapter {

    /**
     * 适配器名 (唯一)
     */
    String name();

    /**
     * 优先级 (数字越小越靠前)
     */
    int priority();

    /**
     * 启动时初始化 (HTTP 客户端 / 缓存预热等)
     */
    void init();
}
