package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 扩展字段解析工具 — 蒸馏自 ace-platform-core
 * {@code ExtendUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>用于低代码平台的扩展字段 (extend JSON) 解析:
 * 当 extend 为空/null 时自动创建默认实例, 非空时反序列化为指定类型.
 *
 * <p>蒸馏时移除了 ace 对 fastjson 的依赖, 改为 Jackson 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>应用扩展配置 (AppExtend) 自动填充</li>
 *   <li>模型字段扩展属性解析</li>
 *   <li>流程模板扩展参数解析</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcExtendUtil {

    private ZLcExtendUtil() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 自动填充扩展字段 — JSON 字符串版本.
     *
     * @param extend JSON 字符串 (可为 null / 空串 / "null")
     * @param clazz  目标类型
     * @param <T>    泛型
     * @return 解析后的对象; extend 为空时返回 clazz 的默认实例
     */
    public static <T> T autoFillExtend(String extend, Class<T> clazz) {
        if (extend == null || extend.isEmpty() || "null".equals(extend)) {
            return createDefault(clazz);
        }
        try {
            return MAPPER.readValue(extend, clazz);
        } catch (Exception e) {
            throw new RuntimeException("扩展字段解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 自动填充扩展字段 — 对象版本.
     *
     * @param extend 已有对象 (可为 null)
     * @param clazz  目标类型
     * @param <T>    泛型
     * @return extend 非空时直接返回, 为空时返回 clazz 的默认实例
     */
    public static <T> T autoFillExtend(T extend, Class<T> clazz) {
        if (extend != null) {
            return extend;
        }
        return createDefault(clazz);
    }

    /**
     * 创建默认实例 (通过无参构造器).
     */
    private static <T> T createDefault(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建默认实例失败: " + clazz.getName(), e);
        }
    }
}
