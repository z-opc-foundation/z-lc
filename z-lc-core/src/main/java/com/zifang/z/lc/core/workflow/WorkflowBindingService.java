package com.zifang.z.lc.core.workflow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowBindingMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 流程绑定服务 (F035 T4)
 * 管理低代码实体与 z-wf 流程的绑定关系
 */
@Service
public class WorkflowBindingService {

    private static final Logger log = LogManager.getLogger(WorkflowBindingService.class);

    @Resource
    private WorkflowBindingMapper workflowBindingMapper;

    public List<WorkflowBindingEntity> listByApp(String appCode) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
    }

    public List<WorkflowBindingEntity> listByEntity(String appCode, String entityCode) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("deleted", 0));
    }

    public List<WorkflowBindingEntity> listByEvent(String appCode, String entityCode, String triggerEvent) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("trigger_event", triggerEvent)
                        .eq("deleted", 0));
    }

    public WorkflowBindingEntity create(WorkflowBindingEntity entity) {
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        if (entity.getAutoSubmit() == null) entity.setAutoSubmit(1);
        workflowBindingMapper.insert(entity);
        log.info("WorkflowBinding created: app={} entity={} process={}", entity.getAppCode(), entity.getEntityCode(), entity.getProcessDefinitionKey());
        return entity;
    }

    public WorkflowBindingEntity update(WorkflowBindingEntity entity) {
        entity.setUpdateTime(new Date());
        workflowBindingMapper.updateById(entity);
        return entity;
    }

    public int delete(Long id) {
        WorkflowBindingEntity entity = workflowBindingMapper.selectById(id);
        if (entity == null) return 0;
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return workflowBindingMapper.updateById(entity);
    }
}
