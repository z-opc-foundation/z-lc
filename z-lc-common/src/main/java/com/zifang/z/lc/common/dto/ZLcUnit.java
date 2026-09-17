package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 单元素元组 — 蒸馏自 ace-platform-core
 * {@code Unit} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>轻量级不可变值容器, 无需定义专用 POJO 即可传递单个关联值.
 * 继承 {@link Serializable} 以支持跨进程序列化 (RPC / MQ).
 *
 * @param <A> 元素类型
 * @author zifang
 */
public class ZLcUnit<A> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final A a;

    public ZLcUnit(A a) {
        this.a = a;
    }

    public A getFirst() {
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZLcUnit)) return false;
        ZLcUnit<?> unit = (ZLcUnit<?>) o;
        return Objects.equals(a, unit.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a);
    }

    @Override
    public String toString() {
        return "(" + a + ")";
    }
}
