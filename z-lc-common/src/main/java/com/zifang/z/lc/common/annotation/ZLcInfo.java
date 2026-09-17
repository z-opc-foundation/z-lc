package com.zifang.z.lc.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通用业务信息注解 — 蒸馏自 ace-platform-core {@code Info} ({@code com.c2f.ace.core.common}).
 *
 * <p>标记在类、接口、枚举上声明业务元信息 (中文名 + 描述)，用于：
 * <ul>
 *   <li>SPI 注册中心扫描时显示业务方信息</li>
 *   <li>API 文档生成 (Swagger / Knife4j)</li>
 *   <li>管理后台"扩展点/SPI 列表"页面渲染</li>
 * </ul>
 *
 * <p>蒸馏说明：ace 原 {@code Info} 仅提供 {@code name} + {@code desc} 两个字段.
 * 蒸馏版新增 {@code group}/{@code order} 以便和 z-lc 自研的 SPI 注册中心（{@code InfoScanner}）
 * 配合使用，但保留原字段命名以保持 {@code @Info(name="...", desc="...")} 用法兼容。
 *
 * <p>用法示例：
 * <pre>{@code
 * @ZLcInfo(name = "表单发起前处理", desc = "在表单提交前执行业务方自定义逻辑",
 *         group = "表单", order = 100)
 * public class MySubmitPreHandler implements WfFormDataSubmitPreHandlerService {
 *     ...
 * }
 * }</pre>
 *
 * @author zifang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ZLcInfo {

    /**
     * 中文/英文简称 — 业务方展示用（如 "员工请假流程"）.
     */
    String name();

    /**
     * 详细描述 — 业务方展示用（如 "在表单提交前执行业务方自定义逻辑"）.
     */
    String desc();

    /**
     * 分组（可选）— 默认空字符串. 用于 SPI 注册中心按分组聚合.
     */
    String group() default "";

    /**
     * 顺序（可选）— 默认 {@code Integer.MAX_VALUE}，越小越先执行.
     */
    int order() default Integer.MAX_VALUE;
}