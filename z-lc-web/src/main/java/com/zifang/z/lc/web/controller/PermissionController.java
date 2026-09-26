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

    /**
     * 与其余 admin 控制器同一口径 ({@code AppAdminController} / {@code DictAdminController})：
     * HTTP 入口这一层的租户是钉死的，不是调用方说什么就是什么。
     * <p>
     * 这一族里 {@code /permission/grant} 原本是唯一的例外 —— 它把 body 里的 {@code tenantCode}
     * 直接落库，而三个列表查询与 {@code /check} 又完全不分租户。两半合起来的意思是:
     * 任何人都能把"别的租户"的授权写进这张表，然后在本租户的判定里读到它。
     */
    private static final String DEFAULT_TENANT = "default";

    @Resource
    private PermissionService permissionService;

    @Operation(summary = "权限列表")
    @GetMapping("/list")
    public Result<List<PermissionEntity>> list(@RequestParam String appCode,
                                               @RequestParam(required = false) String entityCode,
                                               @RequestParam(required = false) String roleCode) {
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            return Result.success(permissionService.listByRole(DEFAULT_TENANT, appCode, roleCode));
        }
        if (entityCode != null && !entityCode.trim().isEmpty()) {
            return Result.success(permissionService.listByEntity(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(permissionService.listByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "授予权限")
    @PostMapping("/grant")
    public Result<PermissionEntity> grant(@RequestBody PermissionEntity entity) {
        if (entity != null) {
            entity.setTenantCode(DEFAULT_TENANT);
        }
        return Result.success(permissionService.grant(entity));
    }

    @Operation(summary = "撤销权限")
    @PostMapping("/revoke")
    public Result<Boolean> revoke(@RequestBody PermissionEntity entity) {
        Long id = entity == null ? null : entity.getId();
        int removed = permissionService.revoke(DEFAULT_TENANT, id);
        if (removed == 0) {
            // 0 行只可能是"这个 id 不存在"或"它不是本租户的"，两种都不该回一句"已回收"。
            throw new IllegalArgumentException("没有可回收的授权: id=" + id
                    + " (不存在，或不属于当前租户)");
            // → LcExceptionHandler 统一转 400 + 这句原文
        }
        return Result.success(true);
    }

    @Operation(summary = "检查权限")
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam String appCode,
                                @RequestParam(required = false) String entityCode,
                                @RequestParam String roleCode,
                                @RequestParam String permission) {
        return Result.success(permissionService.hasPermission(
                DEFAULT_TENANT, appCode, entityCode, roleCode, permission));
    }
}
