package com.zifang.z.lc.sdk.spi.sign.adapter;

import com.zifang.z.lc.sdk.spi.sign.AbstractAssignService;

/**
 * RPC 签名服务适配器抽象基类 — 蒸馏自 ace-platform-engine
 * {@code AbstractRpcAssignServiceAdapter} （{@code com.c2f.ace.engine.adapter}），
 * 行为完全对齐.
 *
 * <p>包含：
 * <ul>
 *   <li>{@link #group} — RPC 服务分组（默认 {@code "z-lc-extensions"}）</li>
 *   <li>{@link #version(String)} — 根据 identityCode 生成版本号（{@code "1.0.0_<identityCode>"}）</li>
 * </ul>
 *
 * @author zifang
 */
public abstract class AbstractRpcAssignServiceAdapter implements AssignServiceAdapter {

    /** RPC 服务分组. */
    protected String group = "z-lc-extensions";

    /** 版本号模板. */
    private static final String VERSION_TEMPLATE = "1.0.0_%s";

    /**
     * 根据 identityCode 生成版本号.
     */
    public static String version(String identityCode) {
        return String.format(VERSION_TEMPLATE, identityCode);
    }
}
