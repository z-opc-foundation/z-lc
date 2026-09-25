package com.zifang.z.lc.core.schema;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;

import java.util.List;

/**
 * Schema 管理服务: App / Entity / Field 的元数据 CRUD (写 z_lc_app / z_lc_entity / z_lc_field 表).
 * <p>
 * 与事件链 (EventReplayService) 互补: 事件链是运行时权威源, 本服务提供可查询的管理视图.
 * 创建/更新 entity 时同步追加事件, 保持双向一致.
 */
public interface SchemaAdminService {

    // ===== App =====

    /**
     * 创建应用
     */
    AppDTO createApp(AppDTO req);

    /**
     * 分页查询应用列表
     */
    PageResult<AppDTO> listApps(String tenantCode, int page, int size);

    /**
     * 按 id 取应用详情
     */
    AppDTO getApp(Long id);

    /**
     * 按 appCode 取应用
     */
    AppDTO getAppByCode(String tenantCode, String appCode);

    /**
     * 更新应用
     */
    int updateApp(Long id, AppDTO req);

    /**
     * 删除应用 (软删)
     */
    int deleteApp(Long id);

    // ===== Entity =====

    /**
     * 创建实体 (含字段), 同时追加 CREATE 事件到事件链
     */
    EntityDefDTO createEntity(String tenantCode, String appCode, EntityDefDTO req);

    /**
     * 列出某应用下的所有实体
     */
    List<EntityDefDTO> listEntities(String tenantCode, String appCode);

    /**
     * 按 id 取实体详情 (含字段)
     */
    EntityDefDTO getEntity(Long id);

    /**
     * 更新实体 (含字段), 同时追加 UPDATE 事件
     */
    int updateEntity(Long id, EntityDefDTO req);

    /**
     * 删除实体 (软删), 同时追加 DELETE 事件
     */
    int deleteEntity(Long id);

    // ===== DDL Provisioning =====

    /**
     * 为指定实体创建物理表，并**回读物理列**核对这份定义真的落地了。
     * <p>
     * 返回的不是 DDL 字符串: {@code CREATE TABLE IF NOT EXISTS} 对一张已在的表是空操作，
     * "这条语句没报错"可以完全等于"这份定义一列都没建出来"。
     */
    ProvisionReport.Item provisionTable(Long entityId);

    /**
     * 为某应用下所有实体逐个建表。一个实体坏**不连累**其他实体，
     * 但报告里必须看得见哪些没建成、为什么。
     */
    ProvisionReport provisionAllTables(String tenantCode, String appCode);
}
