package com.zifang.z.lc.common.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据订正记录注解 — 蒸馏自 ace-platform-core
 * {@code DataCorrectRecord} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标记在 Controller/Service 方法或参数上, 由
 * {@code ZLcDataCorrectRecordAspect} 切面拦截,
 * 在方法 {@code @AfterReturning} 时把"操作人/参数 JSON/动作类型"持久化到数据订正记录表.
 *
 * <p>典型用法:
 * <pre>{@code
 * @ZLcDataCorrectRecord(action = "FORM_CORRECT")
 * public Result<Boolean> correctFormData(@RequestBody FormCorrectDTO dto) {
 *     ...
 * }
 * }</pre>
 *
 * <p>蒸馏差异：ace 原版要求 {@code action} 为 {@code DataCorrectActionEnum} 枚举值,
 * 蒸馏版简化为 {@code String} 以减少 z-lc-common 的常量依赖, 业务方可自由扩展.
 *
 * @author zifang
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZLcDataCorrectRecord {

    /**
     * 操作动作类型 — 与 ace DataCorrectActionEnum.code 对齐 (例: "FORM_CORRECT").
     */
    String action();
}