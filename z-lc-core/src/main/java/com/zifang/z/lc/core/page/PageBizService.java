package com.zifang.z.lc.core.page;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.PageCreateReq;
import com.zifang.z.lc.common.dto.PageDTO;
import com.zifang.z.lc.common.dto.PageUpdateReq;
import com.zifang.z.lc.core.page.entity.PageEntity;
import com.zifang.z.lc.mapper.page.PageMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 页面服务实现 (V2 页面域)。
 * <p>
 * 与 ViewConfig 同款直连 CRUD（页面对象不是 app schema 的一部分，不进事件回放链——
 * 挂事件链留待页面需要审计/撤销时以独立事件类型接入，避免与 schema 因果链互相挤占 parent_event_id）。
 * schema 保存前经 {@link PageSchemaValidator} 校验，坏结构进不了库。
 */
@Service
public class PageBizService implements PageService {

    private static final Logger log = LogManager.getLogger(PageBizService.class);

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private PageMapper pageMapper;

    @Override
    public PageDTO createPage(PageCreateReq req) {
        if (req.getAppCode() == null || req.getAppCode().trim().isEmpty()) {
            throw new IllegalArgumentException("appCode required");
        }
        if (req.getPageCode() == null || req.getPageCode().trim().isEmpty()) {
            throw new IllegalArgumentException("pageCode required");
        }
        PageSchemaValidator.validate(req.getSchema());

        PageEntity existing = pageMapper.selectOne(new QueryWrapper<PageEntity>()
                .eq("tenant_code", DEFAULT_TENANT)
                .eq("app_code", req.getAppCode())
                .eq("page_code", req.getPageCode())
                .eq("deleted", 0));
        if (existing != null) {
            throw new IllegalArgumentException("页面已存在: " + req.getAppCode() + "/" + req.getPageCode());
        }

        PageEntity entity = new PageEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setTenantCode(DEFAULT_TENANT);
        entity.setSchemaJson(req.getSchema());
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        pageMapper.insert(entity);
        log.info("Page created: app={} page={}", entity.getAppCode(), entity.getPageCode());
        return toDTO(entity);
    }

    @Override
    public PageDTO updatePage(PageUpdateReq req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        PageEntity entity = pageMapper.selectById(req.getId());
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new IllegalArgumentException("页面不存在: id=" + req.getId());
        }
        if (req.getSchema() != null) {
            PageSchemaValidator.validate(req.getSchema());
            entity.setSchemaJson(req.getSchema());
        }
        if (req.getPageName() != null) {
            entity.setPageName(req.getPageName());
        }
        entity.setUpdateTime(new Date());
        pageMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public int deletePage(Long id) {
        PageEntity entity = pageMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            return 0;
        }
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return pageMapper.updateById(entity);
    }

    @Override
    public PageDTO getPage(String tenantCode, String appCode, String pageCode) {
        PageEntity entity = pageMapper.selectOne(new QueryWrapper<PageEntity>()
                .eq("tenant_code", tenantCode == null ? DEFAULT_TENANT : tenantCode)
                .eq("app_code", appCode)
                .eq("page_code", pageCode)
                .eq("deleted", 0));
        return entity == null ? null : toDTO(entity);
    }

    @Override
    public List<PageDTO> listPages(String tenantCode, String appCode) {
        List<PageEntity> list = pageMapper.selectList(new QueryWrapper<PageEntity>()
                .eq("tenant_code", tenantCode == null ? DEFAULT_TENANT : tenantCode)
                .eq("app_code", appCode)
                .eq("deleted", 0)
                .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private PageDTO toDTO(PageEntity e) {
        PageDTO dto = new PageDTO();
        BeanUtils.copyProperties(e, dto);
        dto.setSchema(e.getSchemaJson());
        return dto;
    }
}
