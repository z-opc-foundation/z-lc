package com.zifang.z.lc.core.relation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;
import com.zifang.z.lc.core.relation.entity.RelationEntity;
import com.zifang.z.lc.mapper.relation.RelationMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelationServiceImpl implements RelationService {

    private static final Logger log = LogManager.getLogger(RelationServiceImpl.class);

    @Resource
    private RelationMapper relationMapper;

    @Override
    public RelationDTO createRelation(RelationCreateReq req) {
        RelationEntity entity = new RelationEntity();
        BeanUtils.copyProperties(req, entity);
        if (entity.getTenantCode() == null) entity.setTenantCode("default");
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        relationMapper.insert(entity);
        log.info("Relation created: app={} code={}", entity.getAppCode(), entity.getRelationCode());
        return toDTO(entity);
    }

    @Override
    public RelationDTO updateRelation(RelationUpdateReq req) {
        RelationEntity entity = relationMapper.selectById(req.getId());
        if (entity == null) return null;
        if (req.getRelationName() != null) entity.setRelationName(req.getRelationName());
        if (req.getRelationType() != null) entity.setRelationType(req.getRelationType());
        if (req.getSourceFieldCode() != null) entity.setSourceFieldCode(req.getSourceFieldCode());
        if (req.getThroughTable() != null) entity.setThroughTable(req.getThroughTable());
        entity.setUpdateTime(new Date());
        relationMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public int deleteRelation(Long id) {
        RelationEntity entity = relationMapper.selectById(id);
        if (entity == null) return 0;
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return relationMapper.updateById(entity);
    }

    @Override
    public List<RelationDTO> listRelationsByEntity(String tenantCode, String appCode, String entityCode) {
        List<RelationEntity> list = relationMapper.selectList(
                new QueryWrapper<RelationEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .and(w -> w.eq("source_entity_code", entityCode).or().eq("target_entity_code", entityCode))
                        .eq("deleted", 0)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RelationDTO> listRelationsByApp(String tenantCode, String appCode) {
        List<RelationEntity> list = relationMapper.selectList(
                new QueryWrapper<RelationEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public RelationDTO getRelation(Long id) {
        RelationEntity entity = relationMapper.selectById(id);
        return entity == null ? null : toDTO(entity);
    }

    private RelationDTO toDTO(RelationEntity e) {
        RelationDTO dto = new RelationDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
