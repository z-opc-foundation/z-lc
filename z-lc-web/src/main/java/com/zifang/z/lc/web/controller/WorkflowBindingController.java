package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.core.workflow.WorkflowBindingService;
import com.zifang.z.lc.core.workflow.WorkflowTriggers;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程绑定的管理面（配置）+ 结局面（发起了什么）。
 * <p>
 * 租户口径与 {@code AppAdminController}/{@code PermissionAdminController} 一致：body 里带来的
 * {@code tenantCode} 一律不用，写入归一到 {@link #DEFAULT_TENANT}、读取按它过滤 —— 否则任何人都
 * 能把绑定写到别的租户名下，再在本租户的清单里读到它（#48 的 ③ 就是这个形状）。
 * <p>
 * {@code /vocabulary} 存在的理由：界面上的"触发时机"此前是前端手抄的三份字符串，而引擎只兑现
 * {@code AFTER_CREATE}。清单改成从引擎这一侧问出来，抄错这件事就没有落脚处了。
 */
@Tag(name = "低代码-流程绑定")
@RestController
@RequestMapping("/api/lc/workflow-binding")
public class WorkflowBindingController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private WorkflowBindingService workflowBindingService;

    @Operation(summary = "流程绑定列表")
    @GetMapping("/list")
    public Result<List<WorkflowBindingEntity>> list(@RequestParam String appCode,
                                                    @RequestParam(required = false) String entityCode) {
        if (entityCode != null) {
            return Result.success(workflowBindingService.listByEntity(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(workflowBindingService.listByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "引擎真正兑现的触发事件（界面清单的唯一来源）")
    @GetMapping("/vocabulary")
    public Result<Map<String, Object>> vocabulary() {
        List<Map<String, String>> rejected = new ArrayList<Map<String, String>>();
        for (Map.Entry<String, String> entry : WorkflowTriggers.unimplementedReasons().entrySet()) {
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("event", entry.getKey());
            item.put("reason", entry.getValue());
            rejected.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("implemented", WorkflowTriggers.implemented());
        out.put("rejected", rejected);
        return Result.success(out);
    }

    @Operation(summary = "创建流程绑定")
    @PostMapping("/create")
    public Result<WorkflowBindingEntity> create(@RequestBody WorkflowBindingEntity entity) {
        entity.setTenantCode(DEFAULT_TENANT);
        return Result.success(workflowBindingService.create(entity));
    }

    @Operation(summary = "更新流程绑定")
    @PostMapping("/update")
    public Result<WorkflowBindingEntity> update(@RequestBody WorkflowBindingEntity entity) {
        // 归一化而不是覆盖：update 里租户仍取库里那一份（service 负责），这里只是明确
        // "body 带的 tenantCode 不会把绑定搬去别的租户"。
        entity.setTenantCode(null);
        return Result.success(workflowBindingService.update(entity));
    }

    @Operation(summary = "删除流程绑定")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody WorkflowBindingEntity entity) {
        int affected = workflowBindingService.delete(entity.getId());
        if (affected == 0) {
            throw new IllegalArgumentException("没有删掉任何绑定：id=" + entity.getId()
                    + " 不存在或已经是删除状态");
        }
        return Result.success(true);
    }

    @Operation(summary = "某条记录的流程发起结局（STARTED/FAILED 逐条，含失败原因；分页读，total 是真总数）")
    @GetMapping("/fires")
    public Result<PageResult<WorkflowFireEntity>> fires(@RequestParam String appCode,
                                                        @RequestParam(required = false) String entityCode,
                                                        @RequestParam(required = false) Long recordId,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        return Result.success(workflowBindingService.pageFires(
                DEFAULT_TENANT, appCode, entityCode, recordId, page, size));
    }
}
