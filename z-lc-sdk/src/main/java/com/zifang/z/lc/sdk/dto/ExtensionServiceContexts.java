package com.zifang.z.lc.sdk.dto;

/**
 * SPI 调用上下文工厂 (兼容旧用法).
 * <p>
 * 业务侧可以用静态方法创建 ExtensionServiceContext, 避免 import Builder 全路径.
 */
public final class ExtensionServiceContexts {

    private ExtensionServiceContexts() {
    }

    public static ExtensionServiceContext of(String appCode, String modelCode) {
        return new ExtensionServiceContext(appCode, modelCode, null, null, null, null, null);
    }

    public static ExtensionServiceContext of(String appCode, String modelCode, String pageCode) {
        return new ExtensionServiceContext(appCode, modelCode, null, null, pageCode, null, null);
    }
}
