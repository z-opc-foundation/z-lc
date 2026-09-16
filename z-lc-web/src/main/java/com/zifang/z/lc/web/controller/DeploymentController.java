package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.core.deployment.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "低代码-部署管理")
@RestController
@RequestMapping("/api/lc/deployment")
public class DeploymentController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private DeploymentService deploymentService;

    @Operation(summary = "部署记录列表")
    @GetMapping("/list")
    public Result<List<DeploymentDTO>> list(@RequestParam String appCode) {
        return Result.success(deploymentService.listDeploymentsByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "部署详情")
    @GetMapping("/detail")
    public Result<DeploymentDTO> detail(@RequestParam Long id) {
        return Result.success(deploymentService.getDeployment(id));
    }

    @Operation(summary = "创建部署任务")
    @PostMapping("/create")
    public Result<DeploymentDTO> create(@RequestBody DeploymentCreateReq request) {
        DeploymentDTO created = deploymentService.createDeployment(request);
        // TODO: 异步执行物化/部署逻辑
        return Result.success(created);
    }
}
