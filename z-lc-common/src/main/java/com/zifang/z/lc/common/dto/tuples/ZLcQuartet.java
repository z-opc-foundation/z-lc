package com.zifang.z.lc.common.dto.tuples;

import java.io.Serializable;

/**
 * 四元素元组 — 蒸馏自 ace-platform-core
 * {@code Quartet} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为手写 getter/setter.
 *
 * @param <A> 第一个元素类型
 * @param <B> 第二个元素类型
 * @param <C> 第三个元素类型
 * @param <D> 第四个元素类型
 * @author zifang
 */
public class ZLcQuartet<A, B, C, D> extends ZLcTriplet<A, B, C> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected D d;

    public ZLcQuartet(A a, B b, C c, D d) {
        super(a, b, c);
        this.d = d;
    }

    public D getD() {
        return d;
    }

    public void setD(D d) {
        this.d = d;
    }
}
