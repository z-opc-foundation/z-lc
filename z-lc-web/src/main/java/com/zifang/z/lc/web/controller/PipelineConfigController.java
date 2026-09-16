package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.pipeline.config.PipelineConfigService;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "低代码-Pipeline配置")
@RestController
@RequestMapping("/api/lc/pipeline-config")
public class PipelineConfigController {

    @Resource
    private PipelineConfigService pipelineConfigService;

    @Operation(summary = "Pipeline配置列表")
    @GetMapping("/list")
    public Result<List<PipelineConfigEntity>> list(@RequestParam String appCode,
                                                   @RequestParam(required = false) String entityCode) {
        if (entityCode != null) {
            return Result.success(pipelineConfigService.listByEntity(appCode, entityCode));
        }
        return Result.success(pipelineConfigService.listByApp(appCode));
    }

    @Operation(summary = "创建Pipeline配置")
    @PostMapping("/create")
    public Result<PipelineConfigEntity> create(@RequestBody PipelineConfigEntity entity) {
        return Result.success(pipelineConfigService.create(entity));
    }

    @Operation(summary = "更新Pipeline配置")
    @PostMapping("/update")
    public Result<PipelineConfigEntity> update(@RequestBody PipelineConfigEntity entity) {
        return Result.success(pipelineConfigService.update(entity));
    }

    @Operation(summary = "删除Pipeline配置")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody PipelineConfigEntity entity) {
        pipelineConfigService.delete(entity.getId());
        return Result.success(true);
    }

    @Operation(summary = "启用/禁用Pipeline")
    @PostMapping("/toggle")
    public Result<Boolean> toggle(@RequestBody PipelineConfigEntity entity) {
        pipelineConfigService.toggleEnabled(entity.getId(), entity.getEnabled() == 1);
        return Result.success(true);
    }
}
