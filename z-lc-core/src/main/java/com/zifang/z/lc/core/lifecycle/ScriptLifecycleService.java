package com.zifang.z.lc.core.lifecycle;


import com.zifang.util.json.JsonUtil;
import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import com.zifang.z.lc.sdk.spi.form.FormDataLifecycleService;
import com.zifang.z.script.engine.ApiExecutionResult;
import com.zifang.z.script.engine.DynamicApiExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * z-lc CRUD 生命周期 → z-script 脚本执行 桥接器
 *
 * <p>当低代码模型发生 CREATE / UPDATE / DELETE 时，自动触发关联的脚本逻辑。
 * 业务侧在模型上配置 hookScriptCode，指向 z-script 中注册的业务脚本，
 * 引擎自动完成事件分发和脚本执行。
 *
 * <p>设计哲学：
 * <ul>
 *   <li>z-lc 引擎不直接执行脚本，只负责触发钩子</li>
 *   <li>脚本逻辑由 z-script 引擎执行（解耦）</li>
 *   <li>支持 beforeCreate/afterCreate 等生命周期事件</li>
 * </ul>
 *
 * <p>配置方式：在模型的 customTags 中设置 hookScriptCode=xxx
 */
@InterfaceMapping(name = "脚本生命周期服务", code = "ScriptLifecycleService", group = "表单")
@Component
public class ScriptLifecycleService implements FormDataLifecycleService {

    private static final Logger log = LogManager.getLogger(ScriptLifecycleService.class);

    @Autowired
    private DynamicApiExecutor scriptExecutor;

    /**
     * 从 context.customTags 中解析 hookScriptCode
     * 格式: hookScriptCode=script_xxx
     */
    private String resolveScriptCode(ExtensionServiceContext ctx) {
        if (ctx.getCustomTags() == null) {
            return null;
        }
        for (String tag : ctx.getCustomTags()) {
            if (tag != null && tag.startsWith("hookScriptCode=")) {
                return tag.substring("hookScriptCode=".length());
            }
        }
        return null;
    }

    @Override
    public Result<Boolean> onLifecycle(ExtensionServiceContext ctx, String event, Map<String, Object> data) {
        String scriptCode = resolveScriptCode(ctx);
        if (scriptCode == null || scriptCode.isEmpty()) {
            return Result.success(true); // 没有配置脚本，正常放行
        }

        log.info("[ScriptLifecycle] event={} script={} app={} model={}",
                event, scriptCode, ctx.getAppCode(), ctx.getModelCode());

        try {
            // 通过 z-script 引擎执行脚本
            ApiExecutionResult result = scriptExecutor.executeByCode(scriptCode);

            if (!result.isSuccess()) {
                log.warn("[ScriptLifecycle] script {} failed: {}",
                        scriptCode, result.getError());
                return Result.<Boolean>fail(
                        "脚本执行失败 [" + scriptCode + "]: " + result.getError()).code(500);
            }

            // 脚本可通过返回 body 传递业务判断
            if (result.getBody() != null && !result.getBody().isEmpty()) {
                try {
                    Object body = result.getBodyObject();
                    if (body == null) {
                        // 尝试手动解析
                        body = com.zifang.util.json.JsonUtil.parseObject(result.getBody());
                    }
                    if (body instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) body;
                        Object allow = resultMap.get("allow");
                        if (Boolean.FALSE.equals(allow)) {
                            String message = (String) resultMap.get("message");
                            return Result.<Boolean>fail(message != null ? message : "脚本拒绝操作").code(403);
                        }
                    }
                } catch (Exception e) {
                    log.debug("[ScriptLifecycle] parse body failed, ignore: {}", e.getMessage());
                }
            }

            log.info("[ScriptLifecycle] script {} succeeded for event={}", scriptCode, event);
            return Result.success(true);

        } catch (Exception e) {
            log.error("[ScriptLifecycle] script {} threw exception", scriptCode, e);
            return Result.<Boolean>fail("脚本执行异常: " + e.getMessage()).code(500);
        }
    }
}
