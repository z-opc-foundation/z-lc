package com.zifang.z.lc.core.pipeline;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 字段处理管线: 按注册顺序串联所有 FieldProcessor
 */
@Component
public class Pipeline {

    private static final Logger log = LogManager.getLogger(Pipeline.class);

    private final List<FieldProcessor> processors;

    public Pipeline(List<FieldProcessor> processors) {
        // 按 name 字典序排序, 保证可预测的执行顺序
        this.processors = processors == null ? java.util.Collections.emptyList() :
                java.util.Collections.unmodifiableList(processors.stream()
                        .sorted((a, b) -> a.name().compareTo(b.name()))
                        .collect(java.util.stream.Collectors.toList()));
        log.info("Pipeline initialized with {} processors: {}",
                this.processors.size(),
                this.processors.stream().map(FieldProcessor::name).collect(java.util.stream.Collectors.toList()));
    }

    public void preWrite(EntityDefDTO entity, RuntimeCrudDTO body) {
        for (FieldProcessor p : processors) {
            try {
                p.preWrite(entity, body);
            } catch (RuntimeException ex) {
                throw new PipelineException(p.name(), ex);
            }
        }
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
