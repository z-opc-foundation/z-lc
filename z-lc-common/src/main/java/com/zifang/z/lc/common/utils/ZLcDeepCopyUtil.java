package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 深拷贝工具 — 蒸馏自 ace-platform-core
 * {@code DeepCopyUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>基于 Jackson 序列化/反序列化实现深拷贝, 移除了 ace 对 fastjson 的依赖.
 * 适用于不可变对象拷贝、SPI 数据隔离等场景.
 *
 * <p>典型场景：
 * <ul>
 *   <li>SPI preHandler 中拷贝表单数据再修改, 不影响原始数据</li>
 *   <li>流程变量快照保存</li>
 *   <li>审批意见深度复制</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcDeepCopyUtil {

    private ZLcDeepCopyUtil() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 深拷贝单个对象.
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <SOURCE>    源类型
     * @param <T>         目标类型
     * @return 深拷贝后的新对象
     */
    public static <SOURCE, T> T jsonCopy(SOURCE source, Class<T> targetClass) {
        try {
            return MAPPER.readValue(MAPPER.writeValueAsString(source), targetClass);
        } catch (Exception e) {
            throw new RuntimeException("深拷贝失败: " + e.getMessage(), e);
        }
    }

    /**
     * 深拷贝列表.
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @param <SOURCE>    源类型
     * @param <T>         目标类型
     * @return 深拷贝后的新列表
     */
    public static <SOURCE, T> List<T> jsonCopyList(List<SOURCE> sourceList, Class<T> targetClass) {
        List<T> result = new ArrayList<>();
        if (sourceList != null) {
            for (SOURCE source : sourceList) {
                result.add(jsonCopy(source, targetClass));
            }
        }
        return result;
    }
}
