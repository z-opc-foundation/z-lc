package com.zifang.z.lc.core.app;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppCreateReq;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.AppUpdateReq;

/**
 * 应用管理服务接口 (F035 T1: App Admin Center)
 * <p>
 * 提供应用的 CRUD + 发布/归档, 列表查询含 entityCount/fieldCount 统计.
 */
public interface AppAdminService {

    /**
     * 创建应用
     */
    AppDTO createApp(AppCreateReq req);

    /**
     * 更新应用
     */
    AppDTO updateApp(AppUpdateReq req);

    /**
     * 删除应用 (软删)
     */
    int deleteApp(Long id);

    /**
     * 分页查询应用列表 (含 entityCount/fieldCount)
     */
    PageResult<AppDTO> listApps(String tenantCode, int page, int size);

    /**
     * 按 appCode 获取应用详情 (含 entityCount/fieldCount)
     */
    AppDTO getAppByCode(String tenantCode, String appCode);

    /**
     * 发布应用 (DRAFT → PUBLISHED)
     */
    AppDTO publishApp(String tenantCode, String appCode);

    /**
     * 归档应用 (PUBLISHED → ARCHIVED)
     */
    AppDTO archiveApp(String tenantCode, String appCode);
}
