package com.zifang.z.lc.sdk.reflect;

import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.define.DataModelService;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 反射工具 — 蒸馏自 ace-platform-engine {@code Helper}
 * （{@code com.c2f.ace.engine}），但移除 {@code sun.reflect.*} 私有 API 依赖，
 * 改用标准 JDK {@link ParameterizedType}（避免 Java 9+ module 系统报错）.
 *
 * <p>核心能力：
 * <ul>
 *   <li>{@link #resolveGenericType} — 从 {@link AbstractDataModelService} 子类的泛型 T 拿到 Class&lt;T&gt;
 *       （递归处理多层继承，例如 {@code MyService extends BaseService&lt;User&gt;}）</li>
 *   <li>{@link #checkAnnotatedWithDataModel} — 校验 Class 是否带 {@code @DataModel} 注解</li>
 *   <li>{@link #extractDataModelAnnotation} — 取出 {@code @DataModel} 注解对象（null safe）</li>
 * </ul>
 *
 * <p>典型用法（在 {@code ModelServiceCollector} 中）：
 * <pre>{@code
 *   Class<?> genericType = LcReflectHelper.resolveGenericType(myService);
 *   if (genericType != null) {
 *       LcReflectHelper.checkAnnotatedWithDataModel(genericType);
 *       DataModel dm = genericType.getAnnotation(DataModel.class);
 *       String key = dm.appCode() + ":" + dm.modelCode();
 *   }
 * }</pre>
 *
 * @author zifang
 */
public final class LcReflectHelper {

    private LcReflectHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 从 {@link AbstractDataModelService} 子类的泛型 T 解析出 {@code Class<T>}.
     *
     * <p>递归处理多层继承，例如：
     * <pre>{@code
     *   class BaseService&lt;T&gt; extends AbstractDataModelService&lt;T&gt; { ... }
     *   class UserService extends BaseService&lt;User&gt; { ... }
     *
     *   // 调用方式：
     *   Class&lt;?&gt; t = LcReflectHelper.resolveGenericType(new UserService(...));
     *   // 返回 User.class
     * }</pre>
     *
     * @param service 任意 AbstractDataModelService 子类实例
     * @return 泛型 T 的 Class；解析失败返回 null
     */
    public static Class<?> resolveGenericType(DataModelService<?> service) {
        if (service == null) {
            return null;
        }
        return resolveGenericTypeFromClass(service.getClass());
    }

    /**
     * 从 Class 反向解析 AbstractDataModelService 的泛型 T.
     */
    public static Class<?> resolveGenericTypeFromClass(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        // 1. 直接命中：clazz extends AbstractDataModelService<T>
        if (AbstractDataModelService.class.isAssignableFrom(clazz)) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperclass;
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    Type arg = args[0];
                    if (arg instanceof Class) {
                        return (Class<?>) arg;
                    }
                    if (arg instanceof ParameterizedType) {
                        return (Class<?>) ((ParameterizedType) arg).getRawType();
                    }
                }
            }
            // 2. 多层继承：clazz 继承的是中间类，中间类才是 AbstractDataModelService<T>
            //    例如 MyService extends BaseService<User>，BaseService extends AbstractDataModelService<User>
            //    此处 genericSuperclass 不是 AbstractDataModelService，需要递归向父类查找
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                Class<?> result = findGenericInHierarchy(superclass, AbstractDataModelService.class);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 在继承链中向上查找指定基类的泛型实参.
     */
    private static Class<?> findGenericInHierarchy(Class<?> clazz, Class<?> targetBase) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        if (targetBase.isAssignableFrom(clazz)) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperclass;
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    Type arg = args[0];
                    if (arg instanceof Class) {
                        return (Class<?>) arg;
                    }
                    if (arg instanceof ParameterizedType) {
                        return (Class<?>) ((ParameterizedType) arg).getRawType();
                    }
                }
            }
        }
        // 继续向上找父类
        Class<?> sup = clazz.getSuperclass();
        if (sup != null && sup != Object.class) {
            return findGenericInHierarchy(sup, targetBase);
        }
        return null;
    }

    /**
     * 校验 Class 是否带 {@code @DataModel} 注解 — 缺失时抛 {@link IllegalStateException}.
     *
     * @param genericType 待校验的 Class
     * @throws IllegalStateException 当 Class 上无 {@code @DataModel} 注解时
     */
    public static void checkAnnotatedWithDataModel(Class<?> genericType) {
        if (genericType == null) {
            throw new IllegalStateException("genericType is null — 调用方未传入有效 Class");
        }
        if (!genericType.isAnnotationPresent(DataModel.class)) {
            throw new IllegalStateException(
                    "[" + genericType.getName() + "] 未标注 @DataModel 注解 — " +
                            "AbstractDataModelService 子类的泛型 T 必须带 @DataModel");
        }
    }

    /**
     * 取出 Class 上的 {@code @DataModel} 注解 — null safe.
     *
     * @param genericType 业务 Class
     * @return {@code @DataModel} 注解；Class 为 null 或无注解时返回 null
     */
    public static DataModel extractDataModelAnnotation(Class<?> genericType) {
        if (genericType == null) {
            return null;
        }
        return genericType.getAnnotation(DataModel.class);
    }

    /**
     * 从 Class 一次性解析 (appCode, modelCode) 二元组.
     *
     * <p>优先返回 {@code @DataModel} 注解上的 (appCode, modelCode)；
     * 缺失时抛出 {@link IllegalStateException}.
     *
     * @param genericType 业务 Class
     * @return {@code [appCode, modelCode]} 二元数组
     */
    public static String[] resolveAppAndModel(Class<?> genericType) {
        checkAnnotatedWithDataModel(genericType);
        DataModel dm = genericType.getAnnotation(DataModel.class);
        return new String[]{dm.appCode(), dm.modelCode()};
    }
}
