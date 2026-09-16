package com.zifang.z.lc.sdk.annotation;

import java.lang.annotation.*;

/**
 * SPI 元信息注解: 标记一个 SPI 接口 / 实现的语义身份.
 * <p>
 * 设计哲学:
 * <ul>
 *   <li><b>name</b>: 用户可读的中文名 (UI/日志显示)</li>
 *   <li><b>code</b>: 机器可读的唯一码 (引擎按 code 索引)</li>
 *   <li><b>group</b>: SPI 分组 (表单 / 审批 / 工作流 / 模型)</li>
 * </ul>
 * 使用方式: 在用户实现的 SPI bean 上标注, z-lc 引擎扫描时按 (group, code) 索引.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InterfaceMapping {
    String name() default "";

    String code() default "";

    String group() default "";
}
