package com.zifang.z.lc.common.dto;

/**
 * 空值哨兵对象 — 蒸馏自 ace-platform-core
 * {@code NullNode} ({@code com.c2f.ace.core.utils}).
 *
 * <p>用于 Spring SpEL (Expression Language) 上下文中, 区分"键不存在"和"键存在但值为null".
 * 当 MapAccessor 读取 Map 中不存在的 key 时, 返回 {@link ZLcNullNode} 而非 null,
 * 调用方可通过 {@link #isNullNode(Object)} 判断 key 是否存在.
 *
 * <p>典型场景：
 * <ul>
 *   <li>流程变量表达式中, 判断某个变量是否已设置</li>
 *   <li>表单字段值引用中, 区分"字段未定义"和"字段值为null"</li>
 *   <li>ExtensionMapAccessor 中的 null 安全读取</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcNullNode {

    /** 单例实例. */
    public static final ZLcNullNode INSTANCE = new ZLcNullNode();

    private ZLcNullNode() {
    }

    /**
     * 判断值是否为 NullNode 哨兵 (表示 key 不存在).
     *
     * @param value 待判断值
     * @return 是否为 NullNode
     */
    public static boolean isNullNode(Object value) {
        return value instanceof ZLcNullNode;
    }

    /**
     * 判断值是否非 NullNode 哨兵 (表示 key 存在).
     *
     * @param value 待判断值
     * @return 是否非 NullNode
     */
    public static boolean isKeyExist(Object value) {
        return !(value instanceof ZLcNullNode);
    }

    @Override
    public String toString() {
        return "NULL_NODE";
    }
}
