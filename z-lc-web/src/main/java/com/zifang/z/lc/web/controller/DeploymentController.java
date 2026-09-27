package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.core.deployment.DeploymentService;
import com.zifang.z.lc.core.deployment.DeploymentTypes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "创建部署任务（当场执行，返回的是执行后的真状态）")
    @PostMapping("/create")
    public Result<DeploymentDTO> create(@RequestBody DeploymentCreateReq request) {
        // 与 WorkflowBindingController/PermissionAdminController 同一口径：body 里带来的 tenantCode
        // 一律不用 —— /list 只按 DEFAULT_TENANT 过滤，写进别的租户就等于从清单里消失（#48 的 ③）。
        request.setTenantCode(DEFAULT_TENANT);
        return Result.success(deploymentService.createDeployment(request));
    }

    /**
     * 界面"部署方式"这一列的唯一来源：服务器真正会执行哪一种、哪一种不会以及为什么不会。
     * <p>
     * 存在的理由（缺陷 #70）：{@code DeploymentsPage} 手抄过三个选项（热加载 / Docker 镜像 / Git 推送），
     * 而当时三个都不执行 —— 选哪个都一样，都会得到一行永远 PENDING 的记录和一句"部署已创建"。
     */
    @Operation(summary = "服务器真正执行的部署方式（界面清单的唯一来源）")
    @GetMapping("/vocabulary")
    public Result<Map<String, Object>> vocabulary() {
        List<Map<String, String>> rejected = new ArrayList<Map<String, String>>();
        for (Map.Entry<String, String> entry : DeploymentTypes.unimplementedReasons().entrySet()) {
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("type", entry.getKey());
            item.put("reason", entry.getValue());
            rejected.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("executable", DeploymentTypes.implemented());
        out.put("rejected", rejected);
        return Result.success(out);
    }
}
