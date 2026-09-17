package com.zifang.z.lc.common.dto;

import java.util.Objects;

/**
 * 三元素元组 — 蒸馏自 ace-platform-core
 * {@code Triplet} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>轻量级不可变值容器, 适用于需要传递三个关联值的场景
 * (如: 模型编码 + 字段名 + 字段类型).
 *
 * @param <A> 第一个元素类型
 * @param <B> 第二个元素类型
 * @param <C> 第三个元素类型
 * @author zifang
 */
public class ZLcTriplet<A, B, C> extends ZLcPair<A, B> {

    private static final long serialVersionUID = 1L;

    protected final C c;

    public ZLcTriplet(A a, B b, C c) {
        super(a, b);
        this.c = c;
    }

    public C getThird() {
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZLcTriplet)) return false;
        if (!super.equals(o)) return false;
        ZLcTriplet<?, ?, ?> triplet = (ZLcTriplet<?, ?, ?>) o;
        return Objects.equals(c, triplet.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), c);
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ")";
    }
}
