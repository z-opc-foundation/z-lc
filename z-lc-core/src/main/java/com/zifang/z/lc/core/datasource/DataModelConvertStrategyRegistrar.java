package com.zifang.z.lc.core.datasource;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * 方言策略注册器 — 启动时扫描所有 {@link ZLcDialect} 注解的类，自动注册到 {@link DataModelConvertSqlDispatch}.
 *
 * <p>蒸馏自 ace-platform-core {@code DataModelConvertSqlDispatch} 的 switch 实现 —
 * 改为「注解 + 反射扫描」，新增方言无需修改 dispatch.
 *
 * <p>扫描规则：
 * <ul>
 *   <li>从 {@code basePackages} 指定的包路径开始扫描（默认 {@code com.zifang.z.lc.core.datasource}）</li>
 *   <li>扫描所有标 {@code @ZLcDialect("xxx")} 的类</li>
 *   <li>调用 {@link DataModelConvertSqlDispatch#register(String, DataModelConvertSqlStrategy)} 注册</li>
 * </ul>
 *
 * <p>典型用法（业务方接入 PostgreSQL 方言）：
 * <pre>{@code
 *   @ZLcDialect("postgresql")
 *   public class PostgresqlConvertStrategy implements DataModelConvertSqlStrategy { ... }
 *
 *   // 启动时:
 *   DataModelConvertStrategyRegistrar.registerAll(dispatch, "com.example.zlc.dialect");
 * }</pre>
 *
 * @author zifang
 */
public final class DataModelConvertStrategyRegistrar {

    private DataModelConvertStrategyRegistrar() {
    }

    /**
     * 执行扫描 + 注册.
     *
     * @param dispatch     调度器
     * @param basePackages 扫描根包列表
     */
    public static void registerAll(DataModelConvertSqlDispatch dispatch, String... basePackages) {
        if (dispatch == null || basePackages == null || basePackages.length == 0) {
            return;
        }
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ZLcDialect.class));

        for (String pkg : basePackages) {
            Set<BeanDefinition> defs = scanner.findCandidateComponents(pkg);
            for (BeanDefinition def : defs) {
                try {
                    Class<?> clazz = Class.forName(def.getBeanClassName());
                    ZLcDialect anno = clazz.getAnnotation(ZLcDialect.class);
                    if (anno == null || !DataModelConvertSqlStrategy.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    DataModelConvertSqlStrategy strategy =
                            (DataModelConvertSqlStrategy) clazz.getDeclaredConstructor().newInstance();
                    dispatch.register(anno.value(), strategy);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to register dialect strategy: " + def.getBeanClassName(), e);
                }
            }
        }
    }

    /**
     * 标记一个 {@link DataModelConvertSqlStrategy} 实现为某个方言.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ZLcDialect {
        /**
         * 方言类型字符串（对应 {@code DataSourceEnum.type}）.
         */
        String value();
    }
}
