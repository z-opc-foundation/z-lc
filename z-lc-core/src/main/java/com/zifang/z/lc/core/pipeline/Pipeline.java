package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.pipeline.config.PipelineConfigService;
import com.zifang.z.lc.core.pipeline.config.PipelineStages;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段处理管线: 写路径按配置执行, 读路径跑全部处理器.
 * <p>
 * 写路径的口径 (见 {@link PipelineStages}): 实体在该触发点没有启用的配置 → 跑默认全链
 * (处理器类名字典序, 与历史行为一致); 有配置 → **只**跑配置里点名的阶段, 且严格按 order。
 * 摘掉必填阶段/配了没有执行器的阶段, 在配置写入口就会被拒, 所以运行期再撞上 (配置行早于校验存在)
 * 一律抛错而不是"悄悄退回默认链" —— 后者等于把用户配的执行链又变成装饰。
 */
@Component
public class Pipeline {

    public static final String BEFORE_CREATE = PipelineStages.BEFORE_CREATE;
    public static final String BEFORE_UPDATE = PipelineStages.BEFORE_UPDATE;

    private static final Logger log = LogManager.getLogger(Pipeline.class);

    private final List<FieldProcessor> processors;
    private final Map<String, FieldProcessor> byName;

    /** 配置服务: 没有它 (单测直接 new Pipeline) 时写路径就只是默认全链. */
    private PipelineConfigService configService;

    public Pipeline(List<FieldProcessor> processors) {
        // 按 name 字典序排序, 保证没有配置时的默认执行顺序可预测
        this.processors = processors == null ? Collections.<FieldProcessor>emptyList() :
                Collections.unmodifiableList(processors.stream()
                        .sorted((a, b) -> a.name().compareTo(b.name()))
                        .collect(java.util.stream.Collectors.toList()));
        Map<String, FieldProcessor> index = new LinkedHashMap<String, FieldProcessor>();
        for (FieldProcessor p : this.processors) {
            index.put(p.name(), p);
        }
        this.byName = Collections.unmodifiableMap(index);
        log.info("Pipeline initialized with {} processors: {}",
                this.processors.size(),
                this.processors.stream().map(FieldProcessor::name).collect(java.util.stream.Collectors.toList()));
    }

    @Autowired(required = false)
    public void setConfigService(PipelineConfigService configService) {
        this.configService = configService;
    }

    /**
     * 解析某实体在某个写前挂接点上真正要执行的链。一次解析, 多次执行 —— 批量导入按行调 preWrite,
     * 每次调用都去查一遍配置表的话, 2000 行就是 2000 次查询。
     *
     * @param appCode      应用编码 (EntityDefDTO 上不承载 appCode, 必须由调用方给出)
     * @param entity       实体定义
     * @param triggerEvent {@link #BEFORE_CREATE} / {@link #BEFORE_UPDATE}
     */
    public Chain writeChain(String appCode, EntityDefDTO entity, String triggerEvent) {
        List<FieldProcessor> chain = resolve(appCode, entity, triggerEvent);
        return new Chain(chain);
    }

    private List<FieldProcessor> resolve(String appCode, EntityDefDTO entity, String triggerEvent) {
        PipelineConfigEntity config = enabledConfigFor(appCode, entity, triggerEvent);
        if (config == null) {
            return processors;
        }

        List<String> types;
        try {
            types = PipelineStages.validateAndResolve(config.getTriggerEvent(), config.getStages());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("流水线配置 [id=" + config.getId() + ", entity="
                    + config.getEntityCode() + "] 无法执行: " + ex.getMessage(), ex);
        }

        List<FieldProcessor> resolved = new ArrayList<FieldProcessor>(types.size());
        for (String type : types) {
            String processorName = PipelineStages.processorNameFor(type);
            FieldProcessor p = processorName == null ? null : byName.get(processorName);
            if (p == null) {
                // 阶段登记在 PipelineStages 里但 bean 没进容器: 不能跳过, 跳过就是"配了没跑"
                throw new IllegalArgumentException("流水线配置 [id=" + config.getId() + "] 的阶段 [" + type
                        + "] 找不到处理器 [" + processorName + "], 该实体本次写入被拒绝而不是退回默认链");
            }
            resolved.add(p);
        }
        return resolved;
    }

    /**
     * 取该实体该挂接点上启用的配置。同一个 (app, entity, trigger) 有多份启用配置时**不合并**:
     * 合并出来的链没有任何一处界面这样承诺过, 所以取最新一份 (id 最大) 并 warn,
     * 写入口已经会拒掉这种情况, 走到这里只可能是配置行早于校验存在。
     */
    private PipelineConfigEntity enabledConfigFor(String appCode, EntityDefDTO entity, String triggerEvent) {
        if (configService == null || entity == null || appCode == null || triggerEvent == null) {
            return null;
        }

        List<PipelineConfigEntity> configs =
                configService.listByEvent(appCode, entity.getEntityCode(), triggerEvent);
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        if (configs.size() > 1) {
            log.warn("Multiple enabled pipeline configs for app={} entity={} event={}: {}, using id={}",
                    appCode, entity.getEntityCode(), triggerEvent, configs.size(),
                    newest(configs).getId());
        }
        return newest(configs);
    }

    private static PipelineConfigEntity newest(List<PipelineConfigEntity> configs) {
        PipelineConfigEntity best = null;
        for (PipelineConfigEntity c : configs) {
            if (c == null) {
                continue;
            }
            if (best == null || (c.getId() != null && best.getId() != null && c.getId() > best.getId())) {
                best = c;
            }
        }
        return best;
    }

    /** 写前处理: 调用方必须点名挂接点, 否则等于绕过配置面自己挑链. */
    public void preWrite(String appCode, EntityDefDTO entity, RuntimeCrudDTO body, String triggerEvent) {
        writeChain(appCode, entity, triggerEvent).run(entity, body);
    }

    public void postRead(EntityDefDTO entity, Map<String, Object> row) {
        for (FieldProcessor p : processors) {
            try {
                p.postRead(entity, row);
            } catch (RuntimeException ex) {
                log.warn("postRead processor {} failed: {}", p.name(), ex.getMessage());
            }
        }
    }

    public void postReadList(EntityDefDTO entity, List<Map<String, Object>> rows) {
        for (FieldProcessor p : processors) {
            try {
                p.postReadList(entity, rows);
            } catch (RuntimeException ex) {
                log.warn("postReadList processor {} failed: {}", p.name(), ex.getMessage());
            }
        }
    }

    /**
     * 一条已经解析好的执行链。批量写入时解析一次、逐行执行, 保证整批用的是同一份配置
     * (中途改配置不该让一批数据一半按旧链校验、一半按新链校验)。
     */
    public static final class Chain {
        private final List<FieldProcessor> processors;

        private Chain(List<FieldProcessor> processors) {
            this.processors = processors;
        }

        /** 这条链实际会点名的处理器名 (给测试与诊断用, 不对外执行任何事). */
        public List<String> processorNames() {
            List<String> names = new ArrayList<String>(processors.size());
            for (FieldProcessor p : processors) {
                names.add(p.name());
            }
            return names;
        }

        public void run(EntityDefDTO entity, RuntimeCrudDTO body) {
            for (FieldProcessor p : processors) {
                try {
                    p.preWrite(entity, body);
                } catch (RuntimeException ex) {
                    throw new PipelineException(p.name(), ex);
                }
            }
        }
    }

    /**
     * 管线异常 (用于 Controller 转为 4xx)
     */
    public static class PipelineException extends RuntimeException {
        private final String processorName;

        public PipelineException(String processorName, Throwable cause) {
            super("[" + processorName + "] " + cause.getMessage(), cause);
            this.processorName = processorName;
        }

        public String getProcessorName() {
            return processorName;
        }
    }
}
