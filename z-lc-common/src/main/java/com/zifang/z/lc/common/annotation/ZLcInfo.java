package com.zifang.z.lc.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * z-lc SPI 元信息注解 — 蒸馏自 ace-platform-core
 * {@code Info} （{@code com.c2f.ace.core.common}}，行为完全对齐.
 *
 * <p>加在 SPI 接口或实现类上，提供业务侧的 name + desc 元信息，
 * 用于：
 * <ul>
 *   <li>低代码平台 UI 展示（SPI 选择器）</li>
 *   <li>运维监控 / 健康检查（SPI 注册清单）</li>
 *   <li>文档生成（Swagger / Knife4j 扩展）</li>
 * </ul>
 *
 * <p>与已有注解的关系：
 * <ul>
 *   <li>{@link com.zifang.z.lc.sdk.annotation.DataModelServiceInfo} — 抽象基类专用</li>
 *   <li>{@link com.zifang.z.lc.sdk.spi.task.TaskServiceInfo} — 任务 SPI 专用</li>
 *   <li>{@link com.zifang.z.lc.sdk.spi.sign.AssignServiceInfo} — 签 SPI 专用</li>
 *   <li>{@code ZLcInfo} — 通用 SPI 元信息（任意接口/类）</li>
 * </ul>
 *
 * <p>典型用法：
 * <pre>{@code
 *   @ZLcInfo(name = "我的 SPI 实现", desc = "处理客户数据校验")
 *   public class MySpiImpl implements MySpi { ... }
 * }</pre>
 *
 * @author zifang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ZLcInfo {

    /**
     * SPI 中文名（用于低代码平台 UI 展示）.
     */
    String name();

    /**
     * SPI 描述（详细说明，UI tooltip 用）.
     */
    String desc();
}
