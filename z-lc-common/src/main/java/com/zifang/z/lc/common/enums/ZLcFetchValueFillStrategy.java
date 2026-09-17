package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 字段取值填充策略枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.FetchValueFillStrategy} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于模型字段的「值从哪里来」策略选择：
 * <ul>
 *   <li>{@link #STATIC_VALUE} — 固定值（业务方手动填）</li>
 *   <li>{@link #CONTEXT_VALUE} — 上下文取值（从当前 ThreadLocal 取）</li>
 *   <li>{@link #DICT_VALUE} — 字典取值（从数据字典项）</li>
 *   <li>{@link #SERVICE_VALUE} — 服务取值（调用外部服务/数据模型）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcFetchValueFillStrategy {

    /** 固定值 */
    STATIC_VALUE("固定值", "STATIC_VALUE"),

    /** 上下文取值 */
    CONTEXT_VALUE("上下文取值", "CONTEXT_VALUE"),

    /** 字典取值 */
    DICT_VALUE("字典取值", "DICT_VALUE"),

    /** 服务取值 */
    SERVICE_VALUE("服务取值", "SERVICE_VALUE");

    private final String name;
    private final String strategyType;

    ZLcFetchValueFillStrategy(String name, String strategyType) {
        this.name = name;
        this.strategyType = strategyType;
    }

    public String getName() {
        return name;
    }

    public String getStrategyType() {
        return strategyType;
    }

    /** 默认 4 种填充策略. */
    public static final List<ZLcFetchValueFillStrategy> DEFAULT_STRATEGIES = Arrays.asList(
            STATIC_VALUE, CONTEXT_VALUE, DICT_VALUE, SERVICE_VALUE);

    public static ZLcFetchValueFillStrategy fromStrategyType(String strategyType) {
        if (strategyType == null) {
            return null;
        }
        for (ZLcFetchValueFillStrategy s : values()) {
            if (s.strategyType.equals(strategyType)) {
                return s;
            }
        }
        return null;
    }
}
