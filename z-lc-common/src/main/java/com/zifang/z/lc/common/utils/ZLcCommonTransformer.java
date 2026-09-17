package com.zifang.z.lc.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用数据转换工具 — 蒸馏自 ace-platform-core
 * {@code CommonTransformer} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供列表转换、分页转换等数据映射能力.
 * 蒸馏时移除了 ace 对 MyBatis Plus / BeanConvertUtil 的依赖,
 * 改为纯 JDK + Spring BeanUtils 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>DO → DTO 列表转换</li>
 *   <li>DO → VO 分页转换</li>
 *   <li>跨模块数据映射 (带额外转换逻辑)</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcCommonTransformer {

    private ZLcCommonTransformer() {
    }

    /**
     * 列表转换 (源类型 → 目标类型).
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @param converter   转换函数
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass,
                                             Function<S, T> converter) {
        List<T> result = new ArrayList<>();
        if (sourceList != null) {
            for (S source : sourceList) {
                result.add(converter.apply(source));
            }
        }
        return result;
    }

    /**
     * 列表转换 (使用 BiConsumer 做额外映射).
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @param consumer    额外映射逻辑 (target, source)
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass,
                                             BiConsumer<T, S> consumer) {
        List<T> result = new ArrayList<>();
        if (sourceList != null) {
            for (S source : sourceList) {
                try {
                    T target = targetClass.getDeclaredConstructor().newInstance();
                    consumer.accept(target, source);
                    result.add(target);
                } catch (Exception e) {
                    throw new RuntimeException("列表转换失败: " + targetClass.getName(), e);
                }
            }
        }
        return result;
    }

    /**
     * 分页结果转换 (源分页 → 目标分页).
     *
     * @param records     源记录列表
     * @param total       总记录数
     * @param current     当前页码
     * @param size        每页大小
     * @param converter   转换函数
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的分页结果 Map (records/total/current/size)
     */
    public static <S, T> java.util.Map<String, Object> convertPage(
            List<S> records, long total, long current, long size,
            Function<S, T> converter) {
        java.util.Map<String, Object> pageResult = new java.util.LinkedHashMap<>();
        pageResult.put("records", convertList(records, null, converter));
        pageResult.put("total", total);
        pageResult.put("current", current);
        pageResult.put("size", size);
        return pageResult;
    }
}
