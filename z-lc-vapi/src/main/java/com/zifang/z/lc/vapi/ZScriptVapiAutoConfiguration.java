package com.zifang.z.lc.vapi;

import com.zifang.z.lc.core.vapi.VapiScriptExecutor;
import com.zifang.z.script.engine.ScriptEngine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * z-lc-vapi 自动装配 (孵化模块, spring.factories 挂载)。
 * <p>
 * 类路径有 z-script ScriptEngine 才装配——宿主不想带脚本引擎时不引本模块即可,
 * 分发侧经 ObjectProvider 检测缺席并以 503 语义显式报因。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ScriptEngine.class)
public class ZScriptVapiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VapiScriptExecutor.class)
    public VapiScriptExecutor zScriptVapiExecutor() {
        return new ZScriptVapiExecutor();
    }
}
