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
        validateForWrite(entity, null);
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        pipelineConfigMapper.insert(entity);
        log.info("PipelineConfig created: app={} entity={} event={}", entity.getAppCode(), entity.getEntityCode(), entity.getTriggerEvent());
        return entity;
    }

    public PipelineConfigEntity update(PipelineConfigEntity entity) {
        validateForWrite(entity, entity.getId());
        entity.setUpdateTime(new Date());
        pipelineConfigMapper.updateById(entity);
        return entity;
    }

    /**
     * 配置写入口的校验: 只有在这里拒掉"引擎兑现不了"的配置, 运行期才不需要在"退回默认链"和
     * "整批写入失败"之间做选择。校验项与 {@link PipelineStages} 同源, 不另写一份清单。
     *
     * @param selfId 更新时传自己的 id, 让"改这份配置"不与"同一挂接点上已有启用配置"相撞
     */
    private void validateForWrite(PipelineConfigEntity entity, Long selfId) {
        if (entity == null) {
            throw new IllegalArgumentException("配置体不能为空");
        }
        if (isBlank(entity.getAppCode())) {
            throw new IllegalArgumentException("appCode 不能为空: 流水线配置是按应用+实体查找的, 缺了永远不会被执行");
        }
        if (isBlank(entity.getEntityCode())) {
            throw new IllegalArgumentException("entityCode 不能为空: 流水线配置是按应用+实体查找的, 缺了永远不会被执行");
        }
        // 缺省按"启用"处理: 新建一条不启用的配置没有任何用处, 而 enabled 留空会让这条配置
        // 在 listByEvent 的 eq("enabled",1) 条件下永远查不到 —— 界面说"已保存", 运行期什么都不跑。
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        } else if (entity.getEnabled() != 0 && entity.getEnabled() != 1) {
            // 同一个理由: 存成 2/-1 的行永远查不到, 而界面上一切正常
            throw new IllegalArgumentException("enabled 只能是 1(启用) 或 0(停用), 实际: " + entity.getEnabled());
        }

        PipelineStages.validateAndResolve(entity.getTriggerEvent(), entity.getStages());

        if (Integer.valueOf(1).equals(entity.getEnabled())) {
            List<PipelineConfigEntity> existing = listByEvent(
                    entity.getAppCode(), entity.getEntityCode(), entity.getTriggerEvent().trim());
            for (PipelineConfigEntity row : existing) {
                if (row == null || (selfId != null && selfId.equals(row.getId()))) {
                    continue;
                }
                throw new IllegalArgumentException("实体 [" + entity.getEntityCode() + "] 的触发事件 ["
                        + entity.getTriggerEvent() + "] 已经有一份启用的流水线配置 (id=" + row.getId()
                        + ")。一个挂接点只跑一条链, 请先禁用或删除它, 或直接改现有那条");
            }
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public int delete(Long id) {
        PipelineConfigEntity entity = pipelineConfigMapper.selectById(requireId(id));
        if (entity == null) {
            return 0;
        }

        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return pipelineConfigMapper.updateById(entity);
    }

    public int toggleEnabled(Long id, boolean enabled) {
        PipelineConfigEntity entity = pipelineConfigMapper.selectById(requireId(id));
        if (entity == null) {
            throw new IllegalArgumentException("流水线配置不存在: id=" + id
                    + "。不能把「没人被改动」报成切换成功");
        }

        if (enabled) {
            // 打开一份"引擎兑现不了"的配置, 等于让该实体的每次写入都撞在执行链解析上,
            // 所以开关也走一遍写入口校验 (创建时校验过, 但配置行可能早于校验存在)。
            PipelineStages.validateAndResolve(entity.getTriggerEvent(), entity.getStages());
            for (PipelineConfigEntity other : listByEvent(
                    entity.getAppCode(), entity.getEntityCode(), entity.getTriggerEvent())) {
                if (other == null || entity.getId().equals(other.getId())) {
                    continue;
                }
                throw new IllegalArgumentException("实体 [" + entity.getEntityCode() + "] 的触发事件 ["
                        + entity.getTriggerEvent() + "] 已经有另一份启用的配置 (id=" + other.getId()
                        + ")，一个挂接点只跑一条链");
            }
        }

        entity.setEnabled(enabled ? 1 : 0);
        entity.setUpdateTime(new Date());
        return pipelineConfigMapper.updateById(entity);
    }

    private static Long requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("缺少 id: 不知道要操作哪一条流水线配置");
        }
        return id;
    }
}
