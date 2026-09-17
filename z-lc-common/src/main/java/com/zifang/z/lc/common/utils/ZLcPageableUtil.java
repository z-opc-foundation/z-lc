package com.zifang.z.lc.common.utils;

import java.util.List;

/**
 * 内存分页与分页参数校正工具 — 蒸馏自 ace-platform-core
 * {@code PageableUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供内存列表的分页切片能力, 以及分页请求参数的默认值修正.
 * 蒸馏时移除了 ace 对 PageRequest (ace-boot meta) 的依赖,
 * 改为纯 JDK 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>小数据量列表内存分页</li>
 *   <li>分页参数 null / 非法值修正</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcPageableUtil {

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 20L;

    private ZLcPageableUtil() {
    }

    /**
     * 内存列表分页切片.
     *
     * @param dto     待分页的完整列表
     * @param current 当前页码 (从 1 开始, &lt;= 0 时修正为 1)
     * @param size    每页大小
     * @param <T>     元素类型
     * @return 当前页对应的子列表
     */
    public static <T> List<T> sub(List<T> dto, long current, long size) {
        int total = dto.size();
        if (current <= 0) {
            current = 1L;
        }
        long to;
        if (current * size <= total) {
            to = current * size;
        } else {
            to = total;
        }
        long from = (current - 1) * size;
        return dto.subList((int) from, (int) to);
    }

    /**
     * 修正分页参数 — null 或非法值时填充默认值.
     *
     * @param current 当前页码引用
     * @param size    每页大小引用
     */
    public static void handleDefaultPageable(long[] current, long[] size) {
        if (current == null || current.length == 0 || current[0] < 1) {
            if (current != null && current.length > 0) {
                current[0] = DEFAULT_PAGE;
            }
        }
        if (size == null || size.length == 0 || size[0] < 1) {
            if (size != null && size.length > 0) {
                size[0] = DEFAULT_SIZE;
            }
        }
    }

    /**
     * 修正分页参数 — 返回修正后的 current.
     *
     * @param current 当前页码 (可能为 null 或 &lt; 1)
     * @return 修正后的页码
     */
    public static long normalizeCurrent(Long current) {
        return (current == null || current < 1) ? DEFAULT_PAGE : current;
    }

    /**
     * 修正分页参数 — 返回修正后的 size.
     *
     * @param size 每页大小 (可能为 null 或 &lt; 1)
     * @return 修正后的每页大小
     */
    public static long normalizeSize(Long size) {
        return (size == null || size < 1) ? DEFAULT_SIZE : size;
    }
}
