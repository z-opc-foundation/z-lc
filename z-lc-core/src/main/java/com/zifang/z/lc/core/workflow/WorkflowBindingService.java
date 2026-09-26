package com.zifang.z.lc.core.workflow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowBindingMapper;
import com.zifang.z.lc.mapper.workflow.WorkflowFireMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 流程绑定服务 (F035 T4)
 * 管理低代码实体与 z-wf 流程的绑定关系
 * <p>
 * 缺陷 #61 之后这里的口径：绑定不再是"存起来的一张纸"。写入口先过
 * {@link WorkflowTriggers#validateForWrite} 那一份词表（兑现不了的形态直接 400，不生出永远不发
 * 的绑定行），读侧三个查询全部按 {@code tenant_code} 过滤（与 #48 那一族同一口径：不比租户的
 * 查询等于没有租户这一列），{@code listByEvent} 有了生产调用者
 * {@link WorkflowTriggerDispatcher} —— 它零调用者的那段时间就是"保存成功而运行期什么都不变"的
 * 硬证据。
 */
@Service
public class WorkflowBindingService {

    private static final Logger log = LogManager.getLogger(WorkflowBindingService.class);

    @Resource
    private WorkflowBindingMapper workflowBindingMapper;

    @Resource
    private WorkflowFireMapper workflowFireMapper;

    /**
     * 某条记录（或某个实体近期）的流程发起结局，按 id 倒序、最多 200 条。
     * 这张表是"绑定到底兑现过没有"的唯一证据面，读侧不给跨租户的行（同上，租户是硬条件）。
     */
    public List<WorkflowFireEntity> listFires(
            String tenantCode, String appCode, String entityCode, Long recordId) {
        QueryWrapper<WorkflowFireEntity> q =
                new QueryWrapper<WorkflowFireEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0);
        if (entityCode != null && !entityCode.trim().isEmpty()) {
            q.eq("entity_code", entityCode);
        }
        if (recordId != null) {
            q.eq("record_id", recordId);
        }
        q.orderByDesc("id").last("LIMIT 200");
        return workflowFireMapper.selectList(q);
    }

    public List<WorkflowBindingEntity> listByApp(String tenantCode, String appCode) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
    }

    public List<WorkflowBindingEntity> listByEntity(String tenantCode, String appCode, String entityCode) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
    }

    /**
     * 某个挂接点上真正会执行的绑定 —— {@link WorkflowTriggerDispatcher} 在每条记录写成功后读它。
     * <p>
     * {@code auto_submit = 1} 是这个查询的硬条件而不是装饰：写入口已经拒掉过 0，走到这里还留着 0
     * 的行只可能是校验存在之前落的旧数据，把它们当"已启用"会让一条不发单的旧行混进结局账。
     */
    public List<WorkflowBindingEntity> listByEvent(String tenantCode, String appCode, String entityCode,
                                                  String triggerEvent) {
        return workflowBindingMapper.selectList(
                new QueryWrapper<WorkflowBindingEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("entity_code", entityCode)
                        .eq("trigger_event", triggerEvent)
                        .eq("auto_submit", 1)
                        .eq("deleted", 0)
                        .orderByAsc("id"));
    }

    public WorkflowBindingEntity create(WorkflowBindingEntity entity) {
        normalizeBeforeJudgement(entity);
        WorkflowTriggers.validateForWrite(entity);
        requireNotDuplicate(entity, null);

        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        if (entity.getAutoSubmit() == null) {
            entity.setAutoSubmit(1);
        }

        workflowBindingMapper.insert(entity);
        log.info("WorkflowBinding created: id={} tenant={} app={} entity={} event={} process={}",
                entity.getId(), entity.getTenantCode(), entity.getAppCode(), entity.getEntityCode(),
                entity.getTriggerEvent(), entity.getProcessDefinitionKey());
        return entity;
    }

    /**
     * 按 id 原地更新。早先这条链路只有 {@code updateById} 而没有任何校验，改完的绑定能不能兑现
     * 无人过问 —— 与 create 不同口径的话，"先建一条合法的再改成合法的以外"就是绕过闸的那条路。
     */
    public WorkflowBindingEntity update(WorkflowBindingEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("流程绑定更新必须带 id");
        }
        WorkflowBindingEntity existing = workflowBindingMapper.selectById(entity.getId());
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            throw new IllegalArgumentException("流程绑定不存在或已删除: id=" + entity.getId());
        }
        entity.setTenantCode(existing.getTenantCode());
        normalizeBeforeJudgement(entity);
        WorkflowTriggers.validateForWrite(entity);
        requireNotDuplicate(entity, entity.getId());

        entity.setUpdateTime(new Date());
        entity.setCreateTime(existing.getCreateTime());
        if (entity.getAutoSubmit() == null) {
            entity.setAutoSubmit(1);
        }
        entity.setDeleted(existing.getDeleted());
        workflowBindingMapper.updateById(entity);
        return entity;
    }

    /**
     * 把"用来判定的那几格"剪到它们在库里的形状，且必须排在任何按值判定之前（缺陷 #66）。
     * <p>
     * 顺序不是风格问题：{@code /update} 的控制器在进门前把 {@code tenantCode} 抹成 null
     * （{@code WorkflowBindingController} 里那句"归一化而不是覆盖"），而
     * {@link #requireNotDuplicate} 是按 (租户, 应用, 实体, 事件) 去查库的 ——
     * 先查重后补租户，那一句 SQL 里带的就是 {@code tenant_code = NULL}，对任何行都是 UNKNOWN
     * ⇒ 查重恒查 0 行恒不拒 ⇒ 界面上"把第二条改成和第一条同 KEY"走得通，
     * 库里留下两条都满足 {@link #listByEvent} 的绑定，一条记录写成功发 N 个并行流程实例。
     * 空白同理：{@code trigger_event} 落库前会被剪，判定时拿未剪的值去查也是对不上。
     */
    private void normalizeBeforeJudgement(WorkflowBindingEntity entity) {
        if (entity.getTriggerEvent() != null) {
            entity.setTriggerEvent(entity.getTriggerEvent().trim());
        }
        if (entity.getProcessDefinitionKey() != null) {
            entity.setProcessDefinitionKey(entity.getProcessDefinitionKey().trim());
        }
    }

    /**
     * 软删一条绑定。删 0 行要说话：{@code deleteById} 风格的"无条件返回 true"会让界面在
     * 什么都没删掉的情况下报"已删除"（#48 的 ④ 就是这一支）。
     */
    public int delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("流程绑定删除必须带 id");
        }
        WorkflowBindingEntity entity = workflowBindingMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted())) {
            return 0;
        }

        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return workflowBindingMapper.updateById(entity);
    }

    /**
     * 同一 (租户, 应用, 实体, 事件, 流程 KEY) 只留一份。不查重的话每点一次"保存"就多一行，
     * 而它们全都满足 {@link #listByEvent} ⇒ 一条记录写成功会发起 N 个并行流程实例。
     */
    private void requireNotDuplicate(WorkflowBindingEntity candidate, Long selfId) {
        if (candidate.getTenantCode() == null) {
            // 没有租户的查重在 SQL 上是 `tenant_code = NULL` ⇒ 恒 0 行 ⇒ 恒不拒，而它自己不知道。
            // 与其信调用方记得补，不如把这种"看着做了其实什么都没做"的形状当场说响。
            throw new IllegalArgumentException("查重前必须先确定这条绑定属于哪个租户（tenantCode 为 null）");
        }
        for (WorkflowBindingEntity other : listByEvent(candidate.getTenantCode(), candidate.getAppCode(),
                candidate.getEntityCode(), candidate.getTriggerEvent())) {
            if (selfId != null && selfId.equals(other.getId())) {
                continue;
            }
            if (other.getProcessDefinitionKey() != null
                    && other.getProcessDefinitionKey().trim()
                        .equals(candidate.getProcessDefinitionKey().trim())) {
                throw new IllegalArgumentException("同一个实体在同一个触发事件上已经绑定过这个流程: id="
                        + other.getId() + "（重复登记会让一条记录同时提出多个流程实例）");
            }
        }
    }
}
