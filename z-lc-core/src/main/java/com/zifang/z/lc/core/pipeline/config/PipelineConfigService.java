package com.zifang.z.lc.core.pipeline.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import com.zifang.z.lc.mapper.pipeline.PipelineConfigMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Pipeline 配置服务 (F035 T3)
 * 管理实体的业务处理管线配置
 */
@Service
public class PipelineConfigService {

    private static final Logger log = LogManager.getLogger(PipelineConfigService.class);

    @Resource
    private PipelineConfigMapper pipelineConfigMapper;

    public List<PipelineConfigEntity> listByApp(String appCode) {
        return pipelineConfigMapper.selectList(
                new QueryWrapper<PipelineConfigEntity>()
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
    }

    public List<PipelineConfigEntity> listByEntity(String appCode, String entityCode) {
        return pipelineConfigMapper.selectList(
                new QueryWrapper<PipelineConfigEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
    }

    public List<PipelineConfigEntity> listByEvent(String appCode, String entityCode, String triggerEvent) {
        return pipelineConfigMapper.selectList(
                new QueryWrapper<PipelineConfigEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("trigger_event", triggerEvent)
                        .eq("enabled", 1)
                        .eq("deleted", 0));
    }

    public PipelineConfigEntity create(PipelineConfigEntity entity) {
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        pipelineConfigMapper.insert(entity);
        log.info("PipelineConfig created: app={} entity={} event={}", entity.getAppCode(), entity.getEntityCode(), entity.getTriggerEvent());
        return entity;
    }

    public PipelineConfigEntity update(PipelineConfigEntity entity) {
        entity.setUpdateTime(new Date());
        pipelineConfigMapper.updateById(entity);
        return entity;
    }

    public int delete(Long id) {
        PipelineConfigEntity entity = pipelineConfigMapper.selectById(id);
        if (entity == null) {
            return 0;
        }

        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return pipelineConfigMapper.updateById(entity);
    }

    public int toggleEnabled(Long id, boolean enabled) {
        PipelineConfigEntity entity = pipelineConfigMapper.selectById(id);
        if (entity == null) {
            return 0;
        }

        entity.setEnabled(enabled ? 1 : 0);
        entity.setUpdateTime(new Date());
        return pipelineConfigMapper.updateById(entity);
    }
}
