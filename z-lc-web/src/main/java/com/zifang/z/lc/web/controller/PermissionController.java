package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.permission.PermissionService;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "低代码-权限管理")
@RestController
@RequestMapping("/api/lc/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @Operation(summary = "权限列表")
    @GetMapping("/list")
    public Result<List<PermissionEntity>> list(@RequestParam String appCode,
                                               @RequestParam(required = false) String entityCode,
                                               @RequestParam(required = false) String roleCode) {
        if (roleCode != null) {
            return Result.success(permissionService.listByRole(appCode, roleCode));
        }
        if (entityCode != null) {
            return Result.success(permissionService.listByEntity(appCode, entityCode));
        }
        return Result.success(permissionService.listByApp(appCode));
    }

    @Operation(summary = "授予权限")
    @PostMapping("/grant")
    public Result<PermissionEntity> grant(@RequestBody PermissionEntity entity) {
        return Result.success(permissionService.grant(entity));
    }

    @Operation(summary = "撤销权限")
    @PostMapping("/revoke")
    public Result<Boolean> revoke(@RequestBody PermissionEntity entity) {
        permissionService.revoke(entity.getId());
        return Result.success(true);
    }

    @Operation(summary = "检查权限")
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam String appCode,
                                 @RequestParam String entityCode,
                                 @RequestParam String roleCode,
                                 @RequestParam String permission) {
        return Result.success(permissionService.hasPermission(appCode, entityCode, roleCode, permission));
    }
}
