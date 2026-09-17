package com.zifang.z.lc.design.collector;

import com.zifang.z.lc.design.annotation.LowCodeModelService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 低代码服务收集器.
 * <p>
 * 设计哲学:
 * z-lc 启动时, 扫描 ApplicationContext 内所有标 @LowCodeModelService 注解的 Bean,
 * 按 (group, code) 索引到 Map — 业务方调用时按 pageCode 即可定位到具体服务.
 * <p>
 * 与 z-lc 既有 AdapterRegistry 互不冲突:
 * <ul>
 *   <li>AdapterRegistry 收 z-opc 下游模块 (z-ctc / z-meta / z-script) 的适配器</li>
 *   <li>LowCodeModelServiceCollector 收用户实现的低代码服务</li>
 * </ul>
 * <p>
 * 设计选择:
 * <ul>
 *   <li>用 ApplicationContextAware 一次性扫描 (启动时完成, 运行时不再扫描)</li>
 *   <li>按 SPI 接口的 @InterfaceMapping(group, code) 索引 (而非按类名)</li>
 *   <li>支持同一 (group, code) 多个实现 (按 pageCode 路由时取第一个)</li>
 * </ul>
 */
@Component
public class LowCodeModelServiceCollector implements ApplicationContextAware {

    private static final Logger log = LogManager.getLogger(LowCodeModelServiceCollector.class);

    /**
     * (group, code) -> 实现 bean 列表
     */
    private final Map<String, Map<String, Object>> byGroupAndCode = new LinkedHashMap<>();

    /**
     * class -> 实现 bean (用于按类型反查)
     */
    private final Map<Class<?>, Object> byType = new LinkedHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Collection<Object> beans = getBeansWithAnnotation(applicationContext, LowCodeModelService.class);
        for (Object bean : beans) {
            registerOne(bean);
        }
        log.info("LowCodeModelServiceCollector 启动完成: 共注册 {} 个低代码服务 (group/code 总数={})",
                byType.size(), byGroupAndCode.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<Object> getBeansWithAnnotation(ApplicationContext ctx, Class<? extends java.lang.annotation.Annotation> annoType) {
        try {
            // 优先用 Spring 6.x 风格 API
            return (Collection) ctx.getBeansWithAnnotation(annoType).values();
        } catch (Throwable t) {
            // 兜底: 遍历所有 bean name
            java.util.List<Object> result = new java.util.ArrayList<>();
            for (String name : ctx.getBeanDefinitionNames()) {
                try {
                    Object bean = ctx.getBean(name);
                    if (bean.getClass().isAnnotationPresent(annoType)
                            || AnnotationUtils.findAnnotation(bean.getClass(), annoType) != null) {
                        result.add(bean);
                    }
                } catch (Throwable ignore) {
                }
            }
            return result;
        }
    }

    /**
     * 注册一个低代码服务 Bean.
     * 索引规则:
     * <ol>
     *   <li>若 Bean 实现了 z-lc-sdk 任意 SPI 接口 (带 @InterfaceMapping), 按 (group, code) 索引</li>
     *   <li>同时按 class 索引, 供按类型反查</li>
     * </ol>
     */
    private void registerOne(Object bean) {
        byType.put(bean.getClass(), bean);
        // 遍历 bean 实现的接口, 找带 @InterfaceMapping 的
        Class<?>[] interfaces = bean.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            com.zifang.z.lc.sdk.annotation.InterfaceMapping mapping =
                    iface.getAnnotation(com.zifang.z.lc.sdk.annotation.InterfaceMapping.class);
            if (mapping != null) {
                String key = mapping.group() + "::" + mapping.code();
                byGroupAndCode.computeIfAbsent(key, k -> new LinkedHashMap<>())
                        .put(mapping.code(), bean);
                log.debug("注册低代码服务: group={}, code={}, impl={}",
                        mapping.group(), mapping.code(), bean.getClass().getSimpleName());
            }
        }
    }

    /**
     * 按 (group, code) 查找服务.
     */
    public Object pickByGroupAndCode(String group, String code) {
        Map<String, Object> map = byGroupAndCode.get(group + "::" + code);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map.values().iterator().next();
    }

    /**
     * 按类型查找服务.
     */
    @SuppressWarnings("unchecked")
    public <T> T pickByType(Class<T> type) {
        Object bean = byType.get(type);
        return bean == null ? null : (T) bean;
    }

    /**
     * 列出所有已注册的低代码服务 (只读).
     */
    public List<Object> all() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(byType.values()));
    }

    /**
     * 统计已注册数量 (诊断用).
     */
    public int size() {
        return byType.size();
    }
}
