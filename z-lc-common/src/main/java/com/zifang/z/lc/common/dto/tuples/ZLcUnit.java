package com.zifang.z.lc.common.dto.tuples;

import java.io.Serializable;

/**
 * 单元素元组 — 蒸馏自 ace-platform-core
 * {@code Unit} ({@code com.c2f.ace.core.common.tuples}).
 *
 * <p>蒸馏时移除了 ace 对 Lombok @Data / @NoArgsConstructor 的依赖,
 * 改为手写 getter/setter/toString.
 *
 * @param <A> 元素类型
 * @author zifang
 */
public class ZLcUnit<A> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected A a;

    public ZLcUnit() {
    }

    public ZLcUnit(A a) {
        this.a = a;
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return String.valueOf(a);
    }
}
