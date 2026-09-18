package com.zifang.z.lc.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 分页结果转换工具 — 蒸馏自 ace-platform-core
 * {@code PageUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 MyBatis Plus Page → 通用分页 Map 的转换,
 * 以及跨类型的分页记录拷贝.
 * 蒸馏时移除了 ace 对 MyBatis Plus Page / BeanConvertUtil / Pageable 的依赖,
 * 改为纯 JDK + Function 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>MyBatis Plus 分页结果转为前端通用分页格式</li>
 *   <li>分页记录列表的类型转换 (DO → DTO)</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcPageUtil {

    private ZLcPageUtil() {
    }

    /**
     * 将分页数据转为通用分页 Map (records/total/current/size).
     *
     * @param records  记录列表
     * @param total    总记录数
     * @param current  当前页码
     * @param size     每页大小
     * @param <T>      记录类型
     * @return 分页结果 Map
     */
    public static <T> Map<String, Object> toPageMap(List<T> records, long total,
                                                     long current, long size) {
        Map<String, Object> page = new java.util.LinkedHashMap<>();
        page.put("records", records != null ? records : new ArrayList<>());
        page.put("total", total);
        page.put("current", current);
        page.put("size", size);
        return page;
    }

    /**
     * 分页记录类型转换 (源类型 → 目标类型).
     *
     * @param sourceRecords 源记录列表
     * @param converter     转换函数
     * @param <S>           源类型
     * @param <T>           目标类型
     * @return 转换后的记录列表
     */
    public static <S, T> List<T> convertRecords(List<S> sourceRecords,
                                                 Function<S, T> converter) {
        List<T> result = new ArrayList<>();
        if (sourceRecords != null) {
            for (S source : sourceRecords) {
                result.add(converter.apply(source));
            }
        }
        return result;
    }
}
