package com.zifang.z.lc.vapi;

import com.zifang.z.lc.core.vapi.VapiScriptExecutor;
import com.zifang.z.script.core.domain.entity.Script;
import com.zifang.z.script.engine.ExecutionResult;
import com.zifang.z.script.engine.ScriptEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;

/**
 * z-script 桥实现 (V3): vApi 脚本经 z-script ScriptEngine 执行。
 * <p>
 * ScriptEngine 无状态可单例共享; GROOVY/EL 默认走沙箱（超时/黑白名单）,
 * 与 z-script 平台行为一致。孵化期脚本内嵌 z_lc_vapi 注册表, 组临时 Script
 * 实体喂引擎; 未来接 z-script 注册表时本类替换为 scriptRef 版, 分发链不变。
 */
public class ZScriptVapiExecutor implements VapiScriptExecutor {

    private static final Logger log = LogManager.getLogger(ZScriptVapiExecutor.class);

    private final ScriptEngine scriptEngine;

    public ZScriptVapiExecutor() {
        this(new ScriptEngine());
    }

    public ZScriptVapiExecutor(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public boolean supports(String dslType) {
        return dslType != null
                && !dslType.trim().isEmpty()
                && !"API_BRIDGE".equalsIgnoreCase(dslType.trim());
    }

    @Override
    public Object execute(String dslType, String sourceCode, Map<String, Object> params) {
        Script script = new Script();
        script.setScriptCode("lc-vapi-inline");
        script.setScriptName("z-lc vApi inline script");
        script.setDslType(dslType.trim().toUpperCase(Locale.ROOT));
        script.setSourceCode(sourceCode);

        ExecutionResult result = scriptEngine.execute(script, params);
        if (result == null) {
            throw new IllegalStateException("z-script 引擎返回空结果");
        }
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getErrorMessage() == null
                    ? "脚本执行失败" : result.getErrorMessage());
        }
        log.debug("vApi executed: dsl={} durationMs={}", dslType, result.getDurationMs());
        return result.getData();
    }
}
