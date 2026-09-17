package com.zifang.z.lc.common.constants;

/**
 * Web 构建方案编码前缀常量 — 蒸馏自 ace-platform-core
 * {@code WebBuildSolutionConstant} ({@code com.c2f.ace.core.common}}.
 *
 * <p>低代码平台「Web 应用构建方案」业务编码统一以 {@code SOLUTION} 为前缀，
 * 用于业务编码字典/路由/缓存键等场景，避免与其它业务前缀冲突。
 *
 * @author zifang
 */
public final class ZLcWebBuildSolutionConstant {

    /** Web 构建方案业务编码前缀常量. */
    public static final String CODE_PREFIX = "SOLUTION";

    private ZLcWebBuildSolutionConstant() {
        // constant holder
    }
}