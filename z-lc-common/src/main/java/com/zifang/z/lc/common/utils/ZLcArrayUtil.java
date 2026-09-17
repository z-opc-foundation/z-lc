package com.zifang.z.lc.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数组/列表工具 — 蒸馏自 ace-platform-core
 * {@code ArrayUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供列表交集、合并去重等常用操作.
 *
 * <p>典型场景：
 * <ul>
 *   <li>审批人候选人与实际用户列表取交集</li>
 *   <li>流程标签/分类列表合并去重</li>
 *   <li>多步审批中候选人动态合并</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcArrayUtil {

    private ZLcArrayUtil() {
    }

    /**
     * 从 candidator 中筛选出也在 userIds 中的元素, 并用结果替换 candidator.
     *
     * @param candidator 候选人列表 (会被原地修改)
     * @param userIds    用户 ID 列表
     * @return 交集是否非空
     */
    public static boolean retainAll(List<String> candidator, List<String> userIds) {
        List<String> intersection = new ArrayList<>();
        for (String item : candidator) {
            if (userIds.contains(item)) {
                intersection.add(item);
            }
        }
        candidator.clear();
        candidator.addAll(intersection);
        return !intersection.isEmpty();
    }

    /**
     * 合并列表和单个元素, 并去重.
     *
     * @param list 原始列表
     * @param t    要追加的元素
     * @param <T>  元素类型
     * @return 合并去重后的新列表
     */
    public static <T> List<T> merge(List<T> list, T t) {
        List<T> result = new ArrayList<>();
        if (list != null) {
            result.addAll(list);
        }
        if (t != null) {
            result.add(t);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }
}
