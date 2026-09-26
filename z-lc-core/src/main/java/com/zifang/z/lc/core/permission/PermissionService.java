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
 * <p>
 * ⚠ 这里的读方法一律带 {@code tenantCode}: 这一张表是租户内的策略数据，而曾经有三个方法
 * (三个列表查询加一个 {@code hasPermission}) 一个字都不比这一列 —— 别的租户授权的行会出现在
 * 本租户的清单里，并且直接把本租户的"允许/拒绝"答成 true。写入口能收下任意
 * {@code tenantCode}，读路径却不分租户，这两半合起来等于没有租户这一列。
 */
@Service
public class PermissionService {

    private static final Logger log = LogManager.getLogger(PermissionService.class);

    @Resource
    private PermissionMapper permissionMapper;

    public List<PermissionEntity> listByApp(String tenantCode, String appCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("tenant_code", require(tenantCode, "tenantCode"))
                        .eq("app_code", require(appCode, "appCode"))
                        .orderByAsc("entity_code", "role_code"));
    }

    public List<PermissionEntity> listByEntity(String tenantCode, String appCode, String entityCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("tenant_code", require(tenantCode, "tenantCode"))
                        .eq("app_code", require(appCode, "appCode"))
                        .eq("entity_code", require(entityCode, "entityCode")));
    }

    public List<PermissionEntity> listByRole(String tenantCode, String appCode, String roleCode) {
        return permissionMapper.selectList(
                new QueryWrapper<PermissionEntity>()
                        .eq("tenant_code", require(tenantCode, "tenantCode"))
                        .eq("app_code", require(appCode, "appCode"))
                        .eq("role_code", require(roleCode, "roleCode")));
    }

    /**
     * 授予一条权限。幂等：同一 (租户, 应用, 实体范围, 角色, 权限项) 反复点只有一行。
     * <p>
     * 幂等这一条以前是假的：查重写的是 {@code eq("entity_code", entity.getEntityCode())}，
     * 而"整个应用"的授权 {@code entityCode} 是 null —— SQL 里的 {@code entity_code = NULL}
     * 恒为 unknown，一行也匹配不上，于是每点一次多一行。这一支恰恰是矩阵页默认的那一支
     * (不选实体时点格子就是应用级授权)，所以重复得最快。空范围必须用 {@code IS NULL} 比。
     */
    public PermissionEntity grant(PermissionEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("授权内容不能为空");
        }
        String appCode = require(entity.getAppCode(), "appCode");
        String roleCode = require(entity.getRoleCode(), "roleCode");
        // 归一 + 当场拒掉不在词表里的项 (词表只有 PermissionKeys 一份，见那里的注释)
        String permission = PermissionKeys.canonical(entity.getPermission());
        // 空串与 null 在库里是两回事，而它们的语义相同 (整个应用)。留成 "" 会让查重与
        // hasPermission 的 IS NULL 各认一种，又是一处"存下了但没人读得懂"。
        String entityCode = trimmedToNull(entity.getEntityCode());
        String tenantCode = trimmedToNull(entity.getTenantCode());
        if (tenantCode == null) {
            tenantCode = "default";
        }

        QueryWrapper<PermissionEntity> same = new QueryWrapper<PermissionEntity>()
                .eq("tenant_code", tenantCode)
                .eq("app_code", appCode)
                .eq("role_code", roleCode)
                .eq("permission", permission);
        if (entityCode == null) {
            same.isNull("entity_code");
        } else {
            same.eq("entity_code", entityCode);
        }

        PermissionEntity existing = permissionMapper.selectOne(same);
        if (existing != null) {
            return existing;
        }

        entity.setAppCode(appCode);
        entity.setRoleCode(roleCode);
        entity.setEntityCode(entityCode);
        entity.setPermission(permission);
        entity.setTenantCode(tenantCode);
        entity.setCreateTime(new Date());
        permissionMapper.insert(entity);
        log.info("Permission granted: tenant={} app={} entity={} role={} perm={}",
                tenantCode, appCode, entityCode, roleCode, permission);
        return entity;
    }

    /**
     * 回收一条授权，只允许回收**本租户**的那一条。
     * <p>
     * 返回受影响行数 (0 = 那个 id 不存在或不属于这个租户)，而不是 void —— 以前的
     * {@code deleteById} 谁都删得掉，而控制器把返回值无条件说成"已回收"。
     */
    public int revoke(String tenantCode, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 是必填的");
        }
        return permissionMapper.delete(new QueryWrapper<PermissionEntity>()
                .eq("tenant_code", require(tenantCode, "tenantCode"))
                .eq("id", id));
    }

    /**
     * 检查某角色是否有某权限。
     * <p>
     * 作用范围有两种，语义不同，所以两个维度都得进条件：
     * <ul>
     *   <li>{@code entityCode} 给了具体实体 —— 该实体的授权 <b>或</b> 应用级授权
     *       ( {@code entity_code IS NULL} ) 都算成立。以前只比前者，于是矩阵里明明白白
     *       渲染成「整个应用」的那些行，在 {@code /check} 里对任何一个实体都答"拒绝"。</li>
     *   <li>{@code entityCode} 为空 —— 只问应用级那一条，不拿某个实体的授权冒充"整个应用都能干"。</li>
     * </ul>
     */
    public boolean hasPermission(String tenantCode, String appCode, String entityCode,
                                 String roleCode, String permission) {
        String wanted = PermissionKeys.canonical(permission);
        String scope = trimmedToNull(entityCode);
        QueryWrapper<PermissionEntity> q = new QueryWrapper<PermissionEntity>()
                .eq("tenant_code", require(tenantCode, "tenantCode"))
                .eq("app_code", require(appCode, "appCode"))
                .eq("role_code", require(roleCode, "roleCode"))
                .eq("permission", wanted);
        if (scope == null) {
            q.isNull("entity_code");
        } else {
            q.and(w -> w.eq("entity_code", scope).or().isNull("entity_code"));
        }
        Long count = permissionMapper.selectCount(q);
        return count != null && count > 0;
    }

    private static String require(String value, String what) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(what + " 是必填的");
        }
        return value.trim();
    }

    private static String trimmedToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
