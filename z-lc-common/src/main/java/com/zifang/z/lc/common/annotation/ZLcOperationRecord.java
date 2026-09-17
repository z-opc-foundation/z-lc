package com.zifang.z.lc.common.annotation;

import java.lang.annotation.*;

/**
 * 操作记录注解 — 蒸馏自 ace-platform-core
 * {@code OperationRecord} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标注在方法或参数上, 配合 AOP 切面自动记录用户操作日志.
 * 对齐 ace 平台的审计追溯能力, 支持低代码平台的数据变更追踪.
 *
 * <p>典型场景：
 * <ul>
 *   <li>模型创建/修改/删除操作审计</li>
 *   <li>页面模板变更记录</li>
 *   <li>流程发起/审批操作追溯</li>
 * </ul>
 *
 * @author zifang
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZLcOperationRecord {

    /** 操作类型 (create/update/delete/query 等). */
    String action() default "";
}
