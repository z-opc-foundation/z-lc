package com.zifang.z.lc.core.deployment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.core.deployment.entity.DeploymentEntity;
import com.zifang.z.lc.mapper.deployment.DeploymentMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger log = LogManager.getLogger(DeploymentServiceImpl.class);

    @Resource
    private DeploymentMapper deploymentMapper;

    @Override
    public DeploymentDTO createDeployment(DeploymentCreateReq req) {
        DeploymentEntity entity = new DeploymentEntity();
        BeanUtils.copyProperties(req, entity);
        if (entity.getTenantCode() == null) entity.setTenantCode("default");
        entity.setStatus(DeploymentEntity.STATUS_PENDING);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        deploymentMapper.insert(entity);
        log.info("Deployment created: app={} type={}", entity.getAppCode(), entity.getDeployType());
        return toDTO(entity);
    }

    @Override
    public DeploymentDTO updateDeploymentStatus(Long id, String status, String deployLog) {
        DeploymentEntity entity = deploymentMapper.selectById(id);
        if (entity == null) return null;
        entity.setStatus(status);
        if (deployLog != null) entity.setDeployLog(deployLog);
        entity.setUpdateTime(new Date());
        deploymentMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public DeploymentDTO getDeployment(Long id) {
        DeploymentEntity entity = deploymentMapper.selectById(id);
        return entity == null ? null : toDTO(entity);
    }

    @Override
    public List<DeploymentDTO> listDeploymentsByApp(String tenantCode, String appCode) {
        List<DeploymentEntity> list = deploymentMapper.selectList(
                new QueryWrapper<DeploymentEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public DeploymentDTO getLatestDeployment(String tenantCode, String appCode) {
        DeploymentEntity entity = deploymentMapper.selectOne(
                new QueryWrapper<DeploymentEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .orderByDesc("id")
                        .last("LIMIT 1"));
        return entity == null ? null : toDTO(entity);
    }

    private DeploymentDTO toDTO(DeploymentEntity e) {
        DeploymentDTO dto = new DeploymentDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
