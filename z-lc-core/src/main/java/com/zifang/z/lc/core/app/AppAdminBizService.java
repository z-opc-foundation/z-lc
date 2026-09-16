package com.zifang.z.lc.core.app;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.*;
import com.zifang.z.lc.core.event.EventService;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用管理服务实现 (F035 T1: App Admin Center)
 */
@Service
public class AppAdminBizService implements AppAdminService {

    private static final Logger log = LogManager.getLogger(AppAdminBizService.class);

    @Resource
    private LcAppEntityMapper appMapper;

    @Resource
    private EntityMapper entityMapper;

    @Resource
    private FieldMapper fieldMapper;

    @Resource
    private EventService eventService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppDTO createApp(AppCreateReq req) {
        if (req.getAppCode() == null || req.getAppCode().isEmpty()) {
            throw new IllegalArgumentException("appCode is required");
        }
        if (req.getTenantCode() == null || req.getTenantCode().isEmpty()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        // 唯一性校验
        AppEntity existing = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", req.getTenantCode())
                        .eq("app_code", req.getAppCode())
                        .eq("deleted", 0));
        if (existing != null) {
            throw new IllegalArgumentException("App already exists: " + req.getAppCode());
        }

        AppEntity entity = new AppEntity();
        entity.setTenantCode(req.getTenantCode());
        entity.setAppCode(req.getAppCode());
        entity.setAppName(req.getAppName());
        entity.setDescription(req.getDescription());
        entity.setIcon(req.getIcon());
        entity.setStatus("DRAFT");
        entity.setCurrentVersion(0L);
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        appMapper.insert(entity);

        // 追加 APP_CREATE 事件
        emitAppEvent(req.getTenantCode(), req.getAppCode(), "APP_CREATE", req.getAppCode());

        log.info("App created: tenant={} code={}", req.getTenantCode(), req.getAppCode());
        return toAppDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppDTO updateApp(AppUpdateReq req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        AppEntity entity = appMapper.selectById(req.getId());
        if (entity == null || entity.getDeleted() == 1) {
            throw new IllegalArgumentException("App not found: " + req.getId());
        }
        if (req.getAppName() != null) {
            entity.setAppName(req.getAppName());
        }
        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }
        if (req.getIcon() != null) {
            entity.setIcon(req.getIcon());
        }
        entity.setUpdateTime(new Date());
        appMapper.updateById(entity);

        // 追加 APP_UPDATE 事件
        emitAppEvent(entity.getTenantCode(), entity.getAppCode(), "APP_UPDATE", entity.getAppCode());

        log.info("App updated: id={} code={}", req.getId(), entity.getAppCode());
        return toAppDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteApp(Long id) {
        AppEntity entity = appMapper.selectById(id);
        if (entity == null) {
            return 0;
        }
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        int n = appMapper.updateById(entity);

        // 追加 APP_DELETE 事件
        emitAppEvent(entity.getTenantCode(), entity.getAppCode(), "APP_DELETE", entity.getAppCode());

        log.info("App deleted: id={} code={}", id, entity.getAppCode());
        return n;
    }

    @Override
    public PageResult<AppDTO> listApps(String tenantCode, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        size = Math.min(size, 200);

        QueryWrapper<AppEntity> qw = new QueryWrapper<AppEntity>()
                .eq("deleted", 0)
                .eq(tenantCode != null, "tenant_code", tenantCode)
                .orderByDesc("id");

        Long total = appMapper.selectCount(qw);
        if (total == null || total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L, page, size);
        }

        qw.last("LIMIT " + ((page - 1) * size) + "," + size);
        List<AppEntity> list = appMapper.selectList(qw);
        List<AppDTO> dtos = list.stream().map(this::toAppDTO).collect(Collectors.toList());
        return new PageResult<>(dtos, total, page, size);
    }

    @Override
    public AppDTO getAppByCode(String tenantCode, String appCode) {
        AppEntity entity = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0));
        return entity == null ? null : toAppDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppDTO publishApp(String tenantCode, String appCode) {
        AppEntity entity = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0));
        if (entity == null) {
            throw new IllegalArgumentException("App not found: " + appCode);
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw new IllegalArgumentException("App must be in DRAFT status to publish, current: " + entity.getStatus());
        }
        entity.setStatus("PUBLISHED");
        entity.setUpdateTime(new Date());
        appMapper.updateById(entity);

        // 追加 APP_PUBLISH 事件
        emitAppEvent(tenantCode, appCode, "APP_PUBLISH", appCode);

        log.info("App published: tenant={} code={}", tenantCode, appCode);
        return toAppDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppDTO archiveApp(String tenantCode, String appCode) {
        AppEntity entity = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0));
        if (entity == null) {
            throw new IllegalArgumentException("App not found: " + appCode);
        }
        if (!"PUBLISHED".equals(entity.getStatus())) {
            throw new IllegalArgumentException("App must be in PUBLISHED status to archive, current: " + entity.getStatus());
        }
        entity.setStatus("ARCHIVED");
        entity.setUpdateTime(new Date());
        appMapper.updateById(entity);

        // 追加 APP_ARCHIVE 事件
        emitAppEvent(tenantCode, appCode, "APP_ARCHIVE", appCode);

        log.info("App archived: tenant={} code={}", tenantCode, appCode);
        return toAppDTO(entity);
    }

    // ===== Internal Helpers =====

    private AppDTO toAppDTO(AppEntity e) {
        AppDTO dto = new AppDTO();
        BeanUtils.copyProperties(e, dto);
        // 统计 entityCount 和 fieldCount
        Long entityCount = entityMapper.selectCount(
                new QueryWrapper<EntityEntity>()
                        .eq("tenant_code", e.getTenantCode())
                        .eq("app_code", e.getAppCode())
                        .eq("deleted", 0));
        dto.setEntityCount(entityCount != null ? entityCount.intValue() : 0);

        // 查询该 app 下所有 entity 的 id 列表, 再统计 fieldCount
        List<EntityEntity> entities = entityMapper.selectList(
                new QueryWrapper<EntityEntity>()
                        .eq("tenant_code", e.getTenantCode())
                        .eq("app_code", e.getAppCode())
                        .eq("deleted", 0)
                        .select("id"));
        if (entities.isEmpty()) {
            dto.setFieldCount(0);
        } else {
            List<Long> entityIds = new ArrayList<>();
            for (EntityEntity ent : entities) {
                entityIds.add(ent.getId());
            }
            Long fieldCount = fieldMapper.selectCount(
                    new QueryWrapper<FieldEntity>()
                            .in("entity_id", entityIds)
                            .eq("deleted", 0));
            dto.setFieldCount(fieldCount != null ? fieldCount.intValue() : 0);
        }
        return dto;
    }

    private void emitAppEvent(String tenantCode, String appCode, String eventType, String entityCode) {
        try {
            EventAppendRequest req = new EventAppendRequest();
            req.setTenantCode(tenantCode);
            req.setEventType(eventType);
            req.setEntityCode(entityCode);
            req.setSource("ADMIN");
            EventDTO last = eventService.getLastEvent(tenantCode, appCode);
            req.setParentEventId(last == null ? null : last.getEventId());
            req.setEventData(appCode);
            eventService.append(appCode, req);
        } catch (Exception ex) {
            log.warn("Failed to emit {} event for app={}: {}", eventType, appCode, ex.getMessage());
        }
    }
}
