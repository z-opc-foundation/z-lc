package com.zifang.z.lc.core.viewconfig;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;
import com.zifang.z.lc.core.viewconfig.entity.ViewConfigEntity;
import com.zifang.z.lc.mapper.viewconfig.ViewConfigMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ViewConfigServiceImpl implements ViewConfigService {

    private static final Logger log = LogManager.getLogger(ViewConfigServiceImpl.class);

    @Resource
    private ViewConfigMapper viewConfigMapper;

    @Override
    public ViewConfigDTO createViewConfig(ViewConfigCreateReq req) {
        ViewConfigEntity entity = new ViewConfigEntity();
        BeanUtils.copyProperties(req, entity);
        if (entity.getTenantCode() == null) entity.setTenantCode("default");
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        viewConfigMapper.insert(entity);
        log.info("ViewConfig created: app={} entity={} type={}", entity.getAppCode(), entity.getEntityCode(), entity.getViewType());
        return toDTO(entity);
    }

    @Override
    public ViewConfigDTO updateViewConfig(ViewConfigUpdateReq req) {
        ViewConfigEntity entity = viewConfigMapper.selectById(req.getId());
        if (entity == null) return null;
        if (req.getViewType() != null) entity.setViewType(req.getViewType());
        if (req.getConfig() != null) entity.setConfig(req.getConfig());
        entity.setUpdateTime(new Date());
        viewConfigMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public int deleteViewConfig(Long id) {
        ViewConfigEntity entity = viewConfigMapper.selectById(id);
        if (entity == null) return 0;
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return viewConfigMapper.updateById(entity);
    }

    @Override
    public ViewConfigDTO getViewConfig(String tenantCode, String appCode, String entityCode, String viewType) {
        ViewConfigEntity entity = viewConfigMapper.selectOne(
                new QueryWrapper<ViewConfigEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("view_type", viewType)
                        .eq("deleted", 0));
        return entity == null ? null : toDTO(entity);
    }

    @Override
    public List<ViewConfigDTO> listViewConfigs(String tenantCode, String appCode, String entityCode) {
        List<ViewConfigEntity> list = viewConfigMapper.selectList(
                new QueryWrapper<ViewConfigEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ViewConfigDTO> listViewConfigsByApp(String tenantCode, String appCode) {
        List<ViewConfigEntity> list = viewConfigMapper.selectList(
                new QueryWrapper<ViewConfigEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ViewConfigDTO toDTO(ViewConfigEntity e) {
        ViewConfigDTO dto = new ViewConfigDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
