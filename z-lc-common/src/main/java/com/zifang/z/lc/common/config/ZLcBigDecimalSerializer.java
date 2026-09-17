package com.zifang.z.lc.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal Jackson 序列化器 — 蒸馏自 ace-platform-core
 * {@code BigDecimalSerializer} ({@code com.c2f.ace.core.common.config}).
 *
 * <p>把 {@link BigDecimal} 序列化为 JSON 时统一保留 2 位小数 + 四舍五入,
 * 避免直接 writeNumber 出现 {@code 1.2300000000000001E-10} 等科学计数法浮点误差.
 *
 * <p>典型用法（Jackson 全局配置）:
 * <pre>{@code
 * @Bean
 * public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
 *     return builder -> builder.serializerByType(BigDecimal.class, new ZLcBigDecimalSerializer());
 * }
 * }</pre>
 *
 * @author gewenjie (zifang distillation)
 */
public class ZLcBigDecimalSerializer extends JsonSerializer<BigDecimal> {

    /** 默认保留小数位数 — 2 位 (对齐 ace 原版). */
    public static final int DEFAULT_SCALE = 2;

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        // 保留 2 位小数, 四舍五入
        BigDecimal scaled = value.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
        gen.writeString(scaled.toPlainString());
    }
}