package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 模型字段默认值策略枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.DefaultValueStrategy} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>每个枚举项提供：
 * <ul>
 *   <li>{@code name} — 中文名（低代码 UI 展示）</li>
 *   <li>{@code script} — 解析脚本（引擎执行 ${...} 占位符注入真实值）</li>
 * </ul>
 *
 * <p>{@link #DEFAULT_STRATEGIES} 是低代码平台初始化时注册的 8 种内置策略.
 *
 * @author zifang
 */
public enum ZLcDefaultValueStrategy {

    /** 当前时间戳（毫秒） */
    CURRENT_TIME_MILLIS("当前时间戳", "${CURRENT_TIME_MILLIS}"),

    /** 当前时间 */
    CURRENT_TIME("当前时间", "${CURRENT_TIME}"),

    /** 当前操作者 */
    OPERATOR("当前操作者", "${OPERATOR}"),

    /** 当前操作者机构 ID */
    ORG_ID("当前操作者机构", "${ORG_ID}"),

    /** 当前操作者院区 ID */
    CURRENT_CAMPUS_ID("当前操作者院区", "${CURRENT_CAMPUS_ID}"),

    /** 当前操作的应用标识 */
    CURRENT_APP_CODE("当前操作的应用标识", "${CURRENT_APP_CODE}"),

    /** 当前操作的数据模型标识 */
    CURRENT_MODEL_CODE("当前操作的数据模型标识", "${CURRENT_MODEL_CODE}"),

    /** 当前租户 */
    CURRENT_TENANT_CODE("当前租户", "${CURRENT_TENANT_CODE}");

    private final String name;
    private final String script;

    ZLcDefaultValueStrategy(String name, String script) {
        this.name = name;
        this.script = script;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    /** 默认注册的 8 种内置策略. */
    public static final List<ZLcDefaultValueStrategy> DEFAULT_STRATEGIES = Arrays.asList(
            CURRENT_TIME, OPERATOR, ORG_ID, CURRENT_TIME_MILLIS,
            CURRENT_APP_CODE, CURRENT_MODEL_CODE, CURRENT_TENANT_CODE, CURRENT_CAMPUS_ID);

    public static ZLcDefaultValueStrategy fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ZLcDefaultValueStrategy s : values()) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
