package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.dto.ZLcNullNode;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * SpEL Map 属性访问器扩展 — 蒸馏自 ace-platform-core
 * {@code ExtensionMapAccessor} ({@code com.c2f.ace.core.utils}).
 *
 * <p>继承 Spring {@link MapAccessor}, 重写 {@code canRead} / {@code read}
 * 方法, 当 Map 中不包含指定 key 时返回 {@link ZLcNullNode} 哨兵对象,
 * 而非抛出异常. 配合 {@link ZLcSpelUtil} 使用时可实现 "key 不存在" 的安全判断.
 *
 * <p>典型场景：
 * <ul>
 *   <li>SpEL 表达式中安全访问 Map 的不存在字段</li>
 *   <li>低代码数据模型的动态字段求值</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcExtensionMapAccessor extends MapAccessor {

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
        return true;
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {
        if (target == null) {
            return new TypedValue(ZLcNullNode.INSTANCE);
        }
        Assert.state(target instanceof Map, "Target must be of type Map");
        Map<?, ?> map = (Map<?, ?>) target;
        Object value;
        if (!map.containsKey(name)) {
            value = ZLcNullNode.INSTANCE;
        } else {
            value = map.get(name);
        }
        return new TypedValue(value);
    }

    /**
     * 判断 SpEL 求值结果是否为 "key 不存在" 哨兵.
     */
    public static boolean isKeyNotExist(Object value) {
        return value instanceof ZLcNullNode;
    }

    /**
     * 判断 SpEL 求值结果是否为 "key 存在".
     */
    public static boolean isKeyExist(Object value) {
        return !(value instanceof ZLcNullNode);
    }
}
