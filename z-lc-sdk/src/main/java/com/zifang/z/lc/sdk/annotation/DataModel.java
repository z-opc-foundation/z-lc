package com.zifang.z.lc.sdk.annotation;

import java.lang.annotation.*;

/**
 * 业务 POJO 注册为低代码模型的标记.
 * <p>
 * 设计哲学:
 * 业务侧写一个普通 POJO (如 Customer), 加 @DataModel 注解, z-lc 引擎即可在运行时接管
 * 该 POJO 的 CRUD — 通过 AbstractDataModelService 子类暴露给上层.
 * <p>
 * 关键字段:
 * <ul>
 *   <li>appCode: 应用标识 (多应用隔离)</li>
 *   <li>modelCode: 模型标识 (引擎按此注册 RuntimeCrudExecutor 可识别的实体)</li>
 *   <li>modelName: 模型中文名 (UI 显示)</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataModel {
    String appCode() default "";

    String modelCode();

    String modelName() default "";
}
