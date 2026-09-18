package com.zifang.z.lc.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 金额序列化器 — 蒸馏自 ace-platform-core
 * {@code BigDecimalSerializer} ({@code com.c2f.ace.core.common.config}).
 *
 * <p>将 BigDecimal 值序列化为保留 2 位小数的字符串 (四舍五入).
 * 蒸馏时移除了原代码中的冗余空值处理.
 *
 * <p>典型场景：
 * <ul>
 *   <li>金额字段的 JSON 序列化</li>
 *   <li>需要统一精度的价格字段</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcBigDecimalSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        } else {
            gen.writeNull();
        }
    }
}
