package com.zifang.z.lc.common.dto.tuples;

import java.io.Serializable;

/**
 * 双元素元组 — 蒸馏自 ace-platform-core
 * {@code Pair} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为手写 getter/setter/toString.
 *
 * @param <A> 第一个元素类型
 * @param <B> 第二个元素类型
 * @author zifang
 */
public class ZLcPair<A, B> extends ZLcUnit<A> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected B b;

    public ZLcPair(A a, B b) {
        super(a);
        this.b = b;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return a + ":" + b;
    }
}
