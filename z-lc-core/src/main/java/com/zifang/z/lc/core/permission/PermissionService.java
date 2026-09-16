package com.zifang.z.lc.core.permission;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import com.zifang.z.lc.mapper.permission.PermissionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 权限管理服务 (F035 T8)
 * 管理低代码应用的 ACL 权限
 */
@Service
public class PermissionService {

    private static final Logger log = LogManager.getLogger(PermissionService.class);

    @Resource
    private PermissionMapper permissionMapper;

    public List<PermissionEntity> listByApp(String appCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("app_code", appCode)
                        .orderByAsc("entity_code", "role_code"));
    }

    public List<PermissionEntity> listByEntity(String appCode, String entityCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode));
    }

    public List<PermissionEntity> listByRole(String appCode, String roleCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("app_code", appCode)
                        .eq("role_code", roleCode));
    }

    public PermissionEntity grant(PermissionEntity entity) {
        // 检查是否已存在
        PermissionEntity existing = permissionMapper.selectOne(
                new QueryWrapper<PermissionEntity>()
                        .eq("app_code", entity.getAppCode())
                        .eq("entity_code", entity.getEntityCode())
                        .eq("role_code", entity.getRoleCode())
                        .eq("permission", entity.getPermission())
                        .eq("tenant_code", entity.getTenantCode() != null ? entity.getTenantCode() : "default"));
        if (existing != null) return existing;
        if (entity.getTenantCode() == null) entity.setTenantCode("default");
        entity.setCreateTime(new Date());
        permissionMapper.insert(entity);
        log.info("Permission granted: app={} entity={} role={} perm={}", entity.getAppCode(), entity.getEntityCode(), entity.getRoleCode(), entity.getPermission());
        return entity;
    }

    public int revoke(Long id) {
        return permissionMapper.deleteById(id);
    }

    /**
     * 检查某角色是否有某权限
     */
    public boolean hasPermission(String appCode, String entityCode, String roleCode, String permission) {
        Long count = permissionMapper.selectCount(
                new QueryWrapper<PermissionEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("role_code", roleCode)
                        .eq("permission", permission));
        return count != null && count > 0;
    }
}
