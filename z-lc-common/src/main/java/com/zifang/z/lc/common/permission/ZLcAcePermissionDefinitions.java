package com.zifang.z.lc.common.permission;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台级权限定义清单 — 蒸馏自 ace-platform-core
 * {@code AcePermissionDefinitions} ({@code com.c2f.ace.core.common.permission}).
 *
 * <p>集中定义 z-lc 平台级 / 应用级权限编码 (BUTTON / API / MENU 类型), 业务方可:
 * <ul>
 *   <li>通过 {@link #getList()} 获取所有权限定义</li>
 *   <li>通过 {@link #getMap()} 获取权限编码 → 定义的 Map</li>
 *   <li>通过 {@link #filter(List)} 过滤出指定编码的权限定义</li>
 * </ul>
 *
 * <p>每个权限定义 {@link ZLcPermissionDefinition} 含:
 * <ul>
 *   <li>{@code permissionCode} — 权限编码 (用于权限分配/校验)</li>
 *   <li>{@code name} — 权限名称 (展示用)</li>
 *   <li>{@code permissionDesc} — 权限描述 (可选)</li>
 *   <li>{@code permissionType} — 权限类型 (BUTTON / API / MENU)</li>
 *   <li>{@code errorMsg} — 校验失败时的提示信息 (可选)</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcAcePermissionDefinitions {

    private static final Logger log = LogManager.getLogger(ZLcAcePermissionDefinitions.class);

    /** 按钮权限类型 — 用于功能按钮 (新建/删除/修改 等). */
    public static final String BUTTON_PERMISSION_TYPE = "BUTTON";

    /** API 权限类型 — 用于后端接口路径权限校验. */
    public static final String API_PERMISSION_TYPE = "API";

    /** 菜单权限类型 — 用于左侧菜单/导航栏访问. */
    public static final String MENU_PERMISSION_TYPE = "MENU";

    private static final List<ZLcPermissionDefinition> PERMISSION_DEFINITIONS_CACHE = new ArrayList<>();
    private static final Map<String, ZLcPermissionDefinition> PERMISSION_DEFINITIONS_MAP_CACHE = new LinkedHashMap<>();

    static {
        // ============ 平台级权限 (无 appCode 前缀) ============
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:app:create", "平台应用创建", null, API_PERMISSION_TYPE,
                "您没有创建应用的权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:app:remove", "平台应用删除", null, API_PERMISSION_TYPE,
                "您没有删除应用的权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:app:modify", "平台应用修改", null, API_PERMISSION_TYPE,
                "您没有修改应用的权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:authorizeUnit:create", "平台授权主体创建", null, API_PERMISSION_TYPE,
                "您没有平台授权主体创建权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:authorizeUnit:remove", "平台授权主体删除", null, API_PERMISSION_TYPE,
                "您没有平台授权主体删除权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:platform:authorizeUnit:modify", "平台授权主体修改", null, API_PERMISSION_TYPE,
                "您没有平台授权主体修改权限，不允许操作"));

        // ============ 应用级数据源权限 ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:datasource:create", "应用新建数据源", null, API_PERMISSION_TYPE,
                "您没有应用级新建数据源能力，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:datasource:remove", "应用删除数据源", null, API_PERMISSION_TYPE,
                "您没有应用级删除数据源能力，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:datasource:modify", "应用修改数据源", null, API_PERMISSION_TYPE,
                "您没有应用级修改数据源能力，不允许操作"));

        // ============ 应用级授权主体权限 ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:authorizeUnit:create", "应用新建授权主体", null, API_PERMISSION_TYPE,
                "您没有应用级新建授权主体权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:authorizeUnit:remove", "应用删除授权主体", null, API_PERMISSION_TYPE,
                "您没有应用级删除授权主体权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:authorizeUnit:authorize", "应用授权授权主体", null, API_PERMISSION_TYPE,
                "您没有应用级授权授权主体权限，不允许操作"));

        // ============ 应用级功能权限权限 (递归) ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:function:create", "应用新建功能权限", null, API_PERMISSION_TYPE,
                "您没有应用级新建功能权限权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:function:remove", "应用删除功能权限", null, API_PERMISSION_TYPE,
                "您没有应用级删除功能权限权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:function:authorize", "应用授权功能权限", null, API_PERMISSION_TYPE,
                "您没有应用级授权功能权限权限，不允许操作"));

        // ============ 应用级数据权限 ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:data:create", "应用新建数据权限", null, API_PERMISSION_TYPE,
                "您没有应用级新建数据权限权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:data:remove", "应用删除数据权限", null, API_PERMISSION_TYPE,
                "您没有应用级删除数据权限权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:authorize:data:authorize", "应用授权数据权限", null, API_PERMISSION_TYPE,
                "您没有应用级授权数据权限权限，不允许操作"));

        // ============ 应用级构件权限 (模型/页面/字典/流程/服务) ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:model:create", "应用新建模型", null, API_PERMISSION_TYPE,
                "您没有应用级新建模型权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:model:remove", "应用删除模型", null, API_PERMISSION_TYPE,
                "您没有应用级删除模型权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:model:modify", "应用修改模型", null, API_PERMISSION_TYPE,
                "您没有应用级修改模型权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:page:create", "应用新建页面", null, API_PERMISSION_TYPE,
                "您没有应用级新建页面权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:page:remove", "应用删除页面", null, API_PERMISSION_TYPE,
                "您没有应用级删除页面权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:page:modify", "应用修改页面", null, API_PERMISSION_TYPE,
                "您没有应用级修改页面权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:dict:create", "应用新建字典", null, API_PERMISSION_TYPE,
                "您没有应用级新建字典权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:dict:remove", "应用删除字典", null, API_PERMISSION_TYPE,
                "您没有应用级删除字典权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:dict:modify", "应用修改字典", null, API_PERMISSION_TYPE,
                "您没有应用级修改字典权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:workflow:create", "应用新建流程", null, API_PERMISSION_TYPE,
                "您没有应用级新建流程权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:workflow:remove", "应用删除流程", null, API_PERMISSION_TYPE,
                "您没有应用级删除流程权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:workflow:modify", "应用修改流程", null, API_PERMISSION_TYPE,
                "您没有应用级修改流程权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:service:create", "应用新建服务", null, API_PERMISSION_TYPE,
                "您没有应用级新建服务权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:service:remove", "应用删除服务", null, API_PERMISSION_TYPE,
                "您没有应用级删除服务权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:build:service:modify", "应用修改服务", null, API_PERMISSION_TYPE,
                "您没有应用级修改服务权限，不允许操作"));

        // ============ 应用包管理权限 ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:create", "应用包新建", null, API_PERMISSION_TYPE,
                "您没有应用包新建权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:remove", "应用包删除", null, API_PERMISSION_TYPE,
                "您没有应用包删除权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:modify", "应用包修改", null, API_PERMISSION_TYPE,
                "您没有应用包修改权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:import", "应用包导入", null, API_PERMISSION_TYPE,
                "您没有应用包导入权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:output", "应用包导出", null, API_PERMISSION_TYPE,
                "您没有应用包导出权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:package:execute", "应用包执行", null, API_PERMISSION_TYPE,
                "您没有应用包执行权限，不允许操作"));

        // ============ 应用版本管理权限 ============
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:version:submit", "应用版本提交", null, API_PERMISSION_TYPE,
                "您没有应用版本提交权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:version:reset", "应用版本重置", null, API_PERMISSION_TYPE,
                "您没有应用版本重置权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:component:manage:version:modify", "应用版本修改", null, API_PERMISSION_TYPE,
                "您没有应用版本修改权限，不允许操作"));

        // 数据可读 / 可写权限 (按应用 code 维度)
        add(ZLcPermissionDefinition.of(
                "z-lc:${appCode}:app:data:readable", "${appCode}应用数据可读权限", null, API_PERMISSION_TYPE,
                "您没有数据可读权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:${appCode}:app:data:writeable", "${appCode}应用数据可写权限", null, API_PERMISSION_TYPE,
                "您没有数据可写权限，不允许操作"));
        add(ZLcPermissionDefinition.of(
                "z-lc:${appCode}:app:data:readableOwn", "${appCode}应用数据自写可读权限", null, API_PERMISSION_TYPE,
                null));

        // 填充缓存
        PERMISSION_DEFINITIONS_CACHE.forEach(d -> PERMISSION_DEFINITIONS_MAP_CACHE.put(d.getPermissionCode(), d));
    }

    private ZLcAcePermissionDefinitions() {
        // constant holder
    }

    private static void add(ZLcPermissionDefinition def) {
        PERMISSION_DEFINITIONS_CACHE.add(def);
    }

    /**
     * 获取全部权限定义列表 — 使用反射扫描当前类的所有 public static AcePermissionDefinition 字段 (兼容 ace 反射方式).
     */
    public static List<ZLcPermissionDefinition> getList() {
        return new ArrayList<>(PERMISSION_DEFINITIONS_CACHE);
    }

    /**
     * 获取权限编码 → 定义 的 Map.
     */
    public static Map<String, ZLcPermissionDefinition> getMap() {
        return new LinkedHashMap<>(PERMISSION_DEFINITIONS_MAP_CACHE);
    }

    /**
     * 按给定权限编码列表过滤, 返回命中的定义 (Map 形式).
     */
    public static Map<String, ZLcPermissionDefinition> filter(List<String> permissions) {
        Map<String, ZLcPermissionDefinition> map = new LinkedHashMap<>();
        if (permissions == null) {
            return map;
        }
        for (String rawPermission : permissions) {
            map.put(rawPermission, PERMISSION_DEFINITIONS_MAP_CACHE.get(rawPermission));
        }
        return map;
    }

    /**
     * 反射扫描兜底 — 如果子类用 {@code public static final} 字段扩展权限, 可被本方法发现.
     */
    public static List<ZLcPermissionDefinition> scanViaReflection() {
        List<ZLcPermissionDefinition> result = new ArrayList<>(PERMISSION_DEFINITIONS_CACHE);
        try {
            for (Field field : ZLcAcePermissionDefinitions.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && field.getType() == ZLcPermissionDefinition.class) {
                    field.setAccessible(true);
                    try {
                        ZLcPermissionDefinition def = (ZLcPermissionDefinition) field.get(null);
                        if (def != null && !result.contains(def)) {
                            result.add(def);
                        }
                    } catch (IllegalAccessException e) {
                        log.warn("权限定义反射失败: {}", field.getName(), e);
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("权限定义反射扫描异常", e);
        }
        return result;
    }

    /**
     * 权限定义 DTO.
     */
    public static class ZLcPermissionDefinition {
        private String permissionCode;
        private String name;
        private String permissionDesc;
        private String errorMsg;
        private String permissionType;

        public static ZLcPermissionDefinition of(String permissionCode, String name,
                                                  String permissionDesc, String permissionType,
                                                  String errorMsg) {
            ZLcPermissionDefinition def = new ZLcPermissionDefinition();
            def.permissionCode = permissionCode;
            def.name = name;
            def.permissionDesc = permissionDesc;
            def.permissionType = permissionType;
            def.errorMsg = errorMsg;
            return def;
        }

        /** 把 {@code ${appCode}} 占位符替换为真实 appCode, 返回最终权限编码. */
        public String valueOfPermissionCode(String appCode) {
            return permissionCode == null ? null : permissionCode.replace("${appCode}", appCode);
        }

        public String valueOfName(String appCode) {
            return name == null ? null : name.replace("${appCode}", appCode);
        }

        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPermissionDesc() { return permissionDesc; }
        public void setPermissionDesc(String permissionDesc) { this.permissionDesc = permissionDesc; }

        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

        public String getPermissionType() { return permissionType; }
        public void setPermissionType(String permissionType) { this.permissionType = permissionType; }

        @Override
        public String toString() {
            return "ZLcPermissionDefinition{code='" + permissionCode + "', name='" + name + "', type='" + permissionType + "'}";
        }
    }
}