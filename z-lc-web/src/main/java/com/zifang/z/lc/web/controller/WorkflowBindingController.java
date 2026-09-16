package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.workflow.WorkflowBindingService;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "低代码-流程绑定")
@RestController
@RequestMapping("/api/lc/workflow-binding")
public class WorkflowBindingController {

    @Resource
    private WorkflowBindingService workflowBindingService;

    @Operation(summary = "流程绑定列表")
    @GetMapping("/list")
    public Result<List<WorkflowBindingEntity>> list(@RequestParam String appCode,
                                                    @RequestParam(required = false) String entityCode) {
        if (entityCode != null) {
            return Result.success(workflowBindingService.listByEntity(appCode, entityCode));
        }
        return Result.success(workflowBindingService.listByApp(appCode));
    }

    @Operation(summary = "创建流程绑定")
    @PostMapping("/create")
    public Result<WorkflowBindingEntity> create(@RequestBody WorkflowBindingEntity entity) {
        return Result.success(workflowBindingService.create(entity));
    }

    @Operation(summary = "更新流程绑定")
    @PostMapping("/update")
    public Result<WorkflowBindingEntity> update(@RequestBody WorkflowBindingEntity entity) {
        return Result.success(workflowBindingService.update(entity));
    }

    @Operation(summary = "删除流程绑定")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody WorkflowBindingEntity entity) {
        workflowBindingService.delete(entity.getId());
        return Result.success(true);
    }
}
