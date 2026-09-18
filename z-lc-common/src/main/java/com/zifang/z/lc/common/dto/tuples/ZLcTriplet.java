package com.zifang.z.lc.common.dto.tuples;

import java.io.Serializable;

/**
 * 三元素元组 — 蒸馏自 ace-platform-core
 * {@code Triplet} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为手写 getter/setter.
 *
 * @param <A> 第一个元素类型
 * @param <B> 第二个元素类型
 * @param <C> 第三个元素类型
 * @author zifang
 */
public class ZLcTriplet<A, B, C> extends ZLcPair<A, B> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected C c;

    public ZLcTriplet(A a, B b, C c) {
        super(a, b);
        this.c = c;
    }

    public C getC() {
        return c;
    }

    public void setC(C c) {
        this.c = c;
    }
}
