package com.zifang.z.lc.common.annotation;

import java.lang.annotation.*;

/**
 * 组件信息标注注解 — 蒸馏自 ace-platform-core
 * {@code Info} ({@code com.c2f.ace.core.common}).
 *
 * <p>标注在类上, 声明组件的名称与描述.
 * 典型用于扩展服务、适配器等可插拔组件的元数据声明.
 *
 * @author zifang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ZLcInfo {

    /**
     * 组件名称.
     */
    String name();

    /**
     * 组件描述.
     */
    String desc();
}
