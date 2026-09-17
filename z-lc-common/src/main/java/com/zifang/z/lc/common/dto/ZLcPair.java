package com.zifang.z.lc.common.dto;

import java.util.Objects;

/**
 * 二元素元组 — 蒸馏自 ace-platform-core
 * {@code Pair} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>轻量级不可变值容器, 替代 {@code Map.Entry} 或专用 VO 传递两个关联值.
 * 常用于 SPI 返回值 (key-value 对)、批量查询结果等场景.
 *
 * @param <A> 第一个元素类型
 * @param <B> 第二个元素类型
 * @author zifang
 */
public class ZLcPair<A, B> extends ZLcUnit<A> {

    private static final long serialVersionUID = 1L;

    protected final B b;

    public ZLcPair(A a, B b) {
        super(a);
        this.b = b;
    }

    public B getSecond() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZLcPair)) return false;
        if (!super.equals(o)) return false;
        ZLcPair<?, ?> pair = (ZLcPair<?, ?>) o;
        return Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), b);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }
}
