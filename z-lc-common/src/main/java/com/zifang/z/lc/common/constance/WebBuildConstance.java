package com.zifang.z.lc.common.constance;

/**
 * Web 构建层常量.
 *
 * <p>蒸馏自 ace-platform-client {@code WebBuildConstance}
 * （{@code com.c2f.ace.client.constance}）。
 *
 * <p>集中放「跨模块硬编码字符串」— 避免字符串魔法值散落各处难维护.
 *
 * @author zifang
 */
public final class WebBuildConstance {

    /**
     * 虚拟根节点 id：值为 {@code "0"}，用于树结构中表示「根」节点 —
     * 所有顶层节点的 {@code parentTreeNodeId} 都引用此常量.
     */
    public static final String ROOT_CODE_ID = "0";

    private WebBuildConstance() {
        // 工具类，禁止实例化
    }
}
