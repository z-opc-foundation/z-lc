package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.VapiDTO;
import com.zifang.z.lc.core.vapi.VapiScriptExecutor;
import com.zifang.z.lc.core.vapi.VapiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * vApi 注册表管理 + 通用分发 Controller (V3 接口/脚本域, lead 013 §四)。
 * <p>
 * 分发面 {@code ANY /api/lc/vapi/{appCode}/**}：按注册表 (app+method+path) 路由，
 * 参数 = query 合并 JSON body，经 {@link VapiScriptExecutor} 执行（孵化模块直桥
 * z-script 引擎；宿主未装配时 503 显式报因）。
 * <p>
 * RuntimeCrud 保留为"零脚本缺省"——纯 CRUD 不写脚本，复杂逻辑才落这里。
 */
@RestController
@RequestMapping("/api/lc/vapi")
public class VapiController {

    private static final Logger log = LogManager.getLogger(VapiController.class);

    @Resource
    private VapiService vapiService;

    private final ObjectProvider<VapiScriptExecutor> executorProvider;

    public VapiController(ObjectProvider<VapiScriptExecutor> executorProvider) {
        this.executorProvider = executorProvider;
    }

    // ---------- 注册表管理面 ----------

    @PostMapping("/create")
    public Result<?> create(@RequestBody com.zifang.z.lc.common.dto.VapiCreateReq request) {
        return Result.success(vapiService.createVapi(request));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody com.zifang.z.lc.common.dto.VapiUpdateReq request) {
        return Result.success(vapiService.updateVapi(request));
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestParam("id") Long id) {
        vapiService.deleteVapi(id);
        return Result.success("deleted");
    }

    @GetMapping("/get")
    public Result<?> get(@RequestParam("id") Long id) {
        return Result.success(vapiService.getVapi(id));
    }

    @GetMapping("/list")
    public Result<List<VapiDTO>> list(@RequestParam("appCode") String appCode) {
        return Result.success(vapiService.listByApp(appCode));
    }

    // ---------- 通用分发面 ----------

    @RequestMapping(value = "/invoke/{appCode}/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Result<Object>> invoke(@PathVariable("appCode") String appCode,
                                                 @RequestBody(required = false) Map<String, Object> body,
                                                 HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(java.util.Locale.ROOT);
        String relative = relativePath(request, "/api/lc/vapi/invoke/" + appCode);
        VapiDTO vapi = vapiService.resolve(appCode, method, relative);
        if (vapi == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail("接口未注册: " + method + " /" + relative).code(404));
        }
        VapiScriptExecutor executor = executorProvider.getIfAvailable();
        if (executor == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.fail("脚本执行链未装配 (z-lc-vapi/z-script 引擎缺席)").code(503));
        }
        if (!executor.supports(vapi.getDslType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.fail("执行器不支持 DSL: " + vapi.getDslType()).code(400));
        }
        Map<String, Object> params = collectParams(request, body);
        try {
            Object data = executor.execute(vapi.getDslType(), vapi.getSourceCode(), params);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("vapiCode", vapi.getVapiCode());
            payload.put("dslType", vapi.getDslType());
            payload.put("data", data);
            return ResponseEntity.ok(Result.success(payload));
        } catch (Exception e) {
            log.warn("vApi execution failed: app={} {} /{}: {}", appCode, method, relative, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("脚本执行失败: " + e.getMessage()).code(500));
        }
    }

    /** query 参数与 JSON body 合并 (body 优先); 供脚本以 params 取值。 */
    private Map<String, Object> collectParams(HttpServletRequest request, Map<String, Object> body) {
        Map<String, Object> params = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        if (body != null) {
            params.putAll(body);
        }
        return params;
    }

    /** 从 requestURI 剥前缀取相对路径并归一化 (与注册表口径一致: 无首尾斜杠)。 */
    private String relativePath(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String path = uri.substring(prefix.length());
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
