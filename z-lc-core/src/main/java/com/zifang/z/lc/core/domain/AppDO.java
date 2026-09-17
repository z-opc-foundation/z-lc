package com.zifang.z.lc.core.domain;

/**
 * 应用 DO — 蒸馏自 ace-platform-core {@code AppDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code app} 表 — 顶级隔离单元，每个 app 拥有独立的模型 / 页面 / 流程 / 服务 / 字典.
 *
 * <p>与 z-lc 已有的 {@code z-lc-common/dto/AppDTO} 互补：
 * <ul>
 *   <li>{@code AppDTO} — 业务 DTO（含 appCreateReq / appUpdateReq 等子 DTO）</li>
 *   <li>{@code AppDO} — 纯持久化对象</li>
 * </ul>
 *
 * @author zifang
 */
public class AppDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code（业务唯一）.
     */
    private String appCode;

    /**
     * 应用名称.
     */
    private String appName;

    /**
     * 应用描述.
     */
    private String appDesc;

    /**
     * 应用 logo / icon 地址.
     */
    private String appIcon;

    /**
     * 应用首页 URL（用户登录后默认跳转）.
     */
    private String homeUrl;

    /**
     * 应用主题（默认主题 / 暗色主题）.
     */
    private String theme;

    /**
     * 应用状态（0 草稿 / 1 已发布 / 2 已下线）.
     */
    private Integer status;

    /**
     * 默认数据源 code（应用绑定的主数据源）.
     */
    private String defaultDatasourceCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(String appDesc) {
        this.appDesc = appDesc;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDefaultDatasourceCode() {
        return defaultDatasourceCode;
    }

    public void setDefaultDatasourceCode(String defaultDatasourceCode) {
        this.defaultDatasourceCode = defaultDatasourceCode;
    }

    /**
     * 是否已发布.
     */
    public boolean isPublished() {
        return status != null && status == 1;
    }

    /**
     * 是否已下线.
     */
    public boolean isOffline() {
        return status != null && status == 2;
    }
}
