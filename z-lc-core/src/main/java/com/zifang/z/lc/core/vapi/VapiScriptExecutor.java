package com.zifang.z.lc.core.vapi;

import java.util.Map;

/**
 * vApi 脚本执行 SPI (V3, 对齐 datasource 域分层: core 零依赖定接口, 实现落孵化模块)。
 * <p>
 * 孵化期实现 {@code z-lc-vapi} 直桥 z-script ScriptEngine（EL/GROOVY/LUA/SQL/MOCK,
 * GROOVY/EL 带沙箱）; 未来接 z-script 独立注册表时换 scriptRef 桥实现, 分发链不变。
 */
public interface VapiScriptExecutor {

    /** 是否支持该 DSL (大小写不敏感)。 */
    boolean supports(String dslType);

    /**
     * 执行脚本。
     *
     * @param dslType    DSL 类型 (EL/GROOVY/LUA/SQL/MOCK)
     * @param sourceCode 脚本源码
     * @param params     输入参数 (query + body 合并; 可为空)
     * @return 执行结果 (执行失败抛异常或返回 null 语义由实现定, 分发侧统一兜 500)
     */
    Object execute(String dslType, String sourceCode, Map<String, Object> params) throws Exception;
}
