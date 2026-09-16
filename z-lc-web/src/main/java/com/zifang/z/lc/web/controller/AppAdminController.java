package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppCreateReq;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.AppUpdateReq;
import com.zifang.z.lc.core.app.AppAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * App 管理中心 Controller - F035 T1
 * <p>
 * API: /api/lc/app
 */
@Tag(name = "低代码-应用管理")
@RestController
@RequestMapping("/api/lc/app")
public class AppAdminController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private AppAdminService appAdminService;

    @Operation(summary = "创建应用")
    @PostMapping("/create")
    public Result<AppDTO> create(@RequestBody AppCreateReq request) {
        if (request.getTenantCode() == null) {
            request.setTenantCode(DEFAULT_TENANT);
        }
        AppDTO result = appAdminService.createApp(request);
        return Result.success(result);
    }

    @Operation(summary = "更新应用")
    @PostMapping("/update")
    public Result<AppDTO> update(@RequestBody AppUpdateReq request) {
        AppDTO result = appAdminService.updateApp(request);
        return Result.success(result);
    }

    @Operation(summary = "删除应用")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody AppDTO request) {
        // 按 appCode 查找再删除
        AppDTO app = appAdminService.getAppByCode(DEFAULT_TENANT, request.getAppCode());
        if (app != null && app.getId() != null) {
            appAdminService.deleteApp(app.getId());
        }
        return Result.success(true);
    }

    @Operation(summary = "应用列表")
    @GetMapping("/list")
    public Result<List<AppDTO>> list() {
        PageResult<AppDTO> page = appAdminService.listApps(DEFAULT_TENANT, 1, 200);
        return Result.success(page.getRecords());
    }

    @Operation(summary = "应用详情")
    @GetMapping("/detail")
    public Result<AppDTO> getByCode(@RequestParam String appCode) {
        AppDTO result = appAdminService.getAppByCode(DEFAULT_TENANT, appCode);
        return Result.success(result);
    }

    @Operation(summary = "发布应用")
    @PostMapping("/publish")
    public Result<Boolean> publish(@RequestBody AppDTO request) {
        appAdminService.publishApp(DEFAULT_TENANT, request.getAppCode());
        return Result.success(true);
    }

    @Operation(summary = "归档应用")
    @PostMapping("/archive")
    public Result<Boolean> archive(@RequestBody AppDTO request) {
        appAdminService.archiveApp(DEFAULT_TENANT, request.getAppCode());
        return Result.success(true);
    }
}
