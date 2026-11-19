package com.zifang.z.lc.web.controller.materialize;

import com.zifang.z.lc.core.materialize.dto.MaterializationReq;
import com.zifang.z.lc.core.materialize.dto.MaterializationResp;
import com.zifang.z.lc.core.materialize.service.MaterializationService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-lc 代码物化 Controller (FEATURE006 T1).
 * <p>
 * API 基础路径: /api/lc/app/{appCode}/materialize
 * 所属模块: z-lc-web
 * 鉴权: 无
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/lc/app/{appCode}/materialize                 — 触发导出 (异步)</li>
 *   <li>GET  /api/lc/app/{appCode}/materialize/status?id=...   — 查询状态</li>
 *   <li>GET  /api/lc/app/{appCode}/materialize/list?limit=20  — 列出某 app 的所有批次</li>
 *   <li>GET  /api/lc/app/{appCode}/materialize/diff?id=...    — 列出本次生成的文件清单</li>
 *   <li>GET  /api/lc/app/{appCode}/materialize/preview?id=... — 预览单个文件</li>
 * </ul>
 */
@Tag(name = "低代码-物化")
@RestController
@RequestMapping("/api/lc/app/materialize")
public class MaterializationController {

    @Autowired
    private MaterializationService service;

    /**
     * 触发一次物化导出 (异步执行).
     * <p>
     * 内部会启动异步任务, 立即返回本次物化的响应 (含 id, 可用于后续 status 查询).
     *
     * @param appCode 应用编码
     * @param req     物化请求参数, 可空 (空时使用默认配置)
     * @return 包含 success 标志与 {@link MaterializationResp} 数据的 Map
     */
    @Operation(summary = "触发物化导出 (异步)")
    @PostMapping
    @BizApi(summary = "触发一次物化导出 (异步执行)", description = "触发一次物化导出 (异步执行). 内部会启动异步任务, 立即返回本次物化的响应 (含 id, 可用于后续 status 查询).", tags = { "z-lc" })
    public Map<String, Object> trigger(@RequestParam @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码", position = BizParam.Position.QUERY) String appCode,
                                       @RequestBody(required = false)
                                       @BizParam(name = "req", type = BizField.FieldType.STRING, description = "物化请求参数, 可空 (空时使用默认配置)", position = BizParam.Position.BODY) MaterializationReq req) {
        if (req == null) {
            req = new MaterializationReq();
        }

        req.setAppCode(appCode);
        MaterializationResp r = service.trigger(req, "default");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", r);
        return result;
    }

    /**
     * 查询某次物化的执行状态.
     *
     * @param appCode 应用编码 (路由占位, 当前未参与逻辑)
     * @param id      物化批次 id
     * @return 包含 success 标志与 {@link MaterializationResp} 数据的 Map; 不存在时 success=false
     */
    @Operation(summary = "查询物化执行状态")
    @GetMapping("/status")
    @BizApi(summary = "查询某次物化的执行状态", description = "查询某次物化的执行状态.", tags = { "z-lc" })
    public Map<String, Object> status(@RequestParam(required = false) @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码 (路由占位, 当前未参与逻辑)", position = BizParam.Position.QUERY) String appCode, @RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "物化批次 id", position = BizParam.Position.QUERY) Long id) {
        MaterializationResp r = service.getStatus(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", r != null);
        result.put("data", r);
        return result;
    }

    /**
     * 列出指定 app 的最近物化批次.
     *
     * @param appCode 应用编码
     * @param limit   返回条数上限, 默认 20
     * @return 包含 success 标志与 {@link MaterializationResp} 列表的 Map
     */
    @Operation(summary = "列出物化批次")
    @GetMapping("/list")
    @BizApi(summary = "列出指定 app 的最近物化批次", description = "列出指定 app 的最近物化批次.", tags = { "z-lc" })
    public Map<String, Object> list(@RequestParam @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码", position = BizParam.Position.QUERY) String appCode,
                                    @RequestParam(defaultValue = "20")
                                    @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "返回条数上限, 默认 20", position = BizParam.Position.QUERY) int limit) {
        List<MaterializationResp> list = service.listByApp("default", appCode, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", list);
        return result;
    }

    /**
     * 查看本次物化产出的文件清单 (即 diff: 新增/修改/删除的文件路径).
     *
     * @param appCode 应用编码 (路由占位, 当前未参与逻辑)
     * @param id      物化批次 id
     * @return 包含 success 标志与 {@link MaterializationResp.GeneratedFile} 列表的 Map
     */
    @Operation(summary = "查看物化产出文件清单")
    @GetMapping("/diff")
    @BizApi(summary = "查看本次物化产出的文件清单 (即 diff: 新增/修改/删除的文件路径)", description = "查看本次物化产出的文件清单 (即 diff: 新增/修改/删除的文件路径).", tags = { "z-lc" })
    public Map<String, Object> diff(@RequestParam(required = false) @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码 (路由占位, 当前未参与逻辑)", position = BizParam.Position.QUERY) String appCode, @RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "物化批次 id", position = BizParam.Position.QUERY) Long id) {
        List<MaterializationResp.GeneratedFile> files = service.listFiles(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", files);
        return result;
    }
}



