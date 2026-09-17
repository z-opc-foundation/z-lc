package com.zifang.z.lc.design.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为"低代码模型" — z-lc 引擎识别此注解后,
 * 自动把类的字段配置纳入模型元数据 (Entity / Field / Relation).
 *
 * <p>设计意图:
 * <ul>
 *   <li>区别于 {@link LowCodeModelService}: 后者标记的是"运行时 SPI 实现 bean" (用于 Collector 扫描),
 *       本注解标记的是"模型定义类" (用于模型元数据扫描)</li>
 *   <li>无字段, 仅作标记 — 真正的字段配置在 EntityEntity / FieldEntity / RelationEntity 里</li>
 * </ul>
 *
 * <p>用法 (运行时扫描示例):
 * <pre>{@code
 *   @LowCodeModel
 *   public class UserModel {
 *       // 字段配置由前端拖拽生成, 入库 z_lc_entity / z_lc_field 表
 *   }
 * }</pre>
 *
 * @author zifang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LowCodeModel {
}
