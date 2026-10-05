package com.zifang.z.lc.core.adapter;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 只答 {@code getHeader} 的 servlet 请求桩（缺陷 #62 用）.
 * <p>
 * 为什么用 {@link Proxy} 而不是实现接口：{@code HttpServletRequest} 有五十几个方法，
 * 手写实现类里真正用到的只有 {@code getHeader}，其余全得是 throw。
 * {@link Proxy} 把"没答上来的方法"统一退化成类型合宜的默认值，
 * 将来被测代码多调一个 {@code getAttribute}/{@code getRemoteAddr} 也不会编译期炸掉。
 * <p>
 * <b>取值大小写不敏感</b>（{@code getHeader} 按 HTTP 语义本就不该区分大小写），
 * 内部统一小写存，{@code CtcAdapter.fetchContext} 读的是 {@code "Authorization"}。
 */
public final class StubServletRequest implements InvocationHandler {

    private final Map<String, String> headers = new LinkedHashMap<>();

    private StubServletRequest() {
    }

    /** 带一个 {@code Authorization} 头的请求。token 为 null/空时不放这个头。 */
    public static HttpServletRequest withAuthorization(String token) {
        StubServletRequest handler = new StubServletRequest();
        if (token != null && !token.isEmpty()) {
            handler.headers.put("authorization", token);
        }
        return handler.toRequest();
    }

    /** 带任意一组头的请求（键大小写不敏感）。 */
    public static HttpServletRequest withHeaders(Map<String, String> rawHeaders) {
        StubServletRequest handler = new StubServletRequest();
        if (rawHeaders != null) {
            for (Map.Entry<String, String> e : rawHeaders.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    handler.headers.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
                }
            }
        }
        return handler.toRequest();
    }

    private HttpServletRequest toRequest() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                StubServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("getHeader".equals(name)) {
            String key = (String) args[0];
            return key == null ? null : headers.get(key.toLowerCase(Locale.ROOT));
        }
        if ("getHeaderNames".equals(name)) {
            return Collections.enumeration(headers.keySet());
        }
        if ("getIntHeader".equals(name)) {
            String v = headers.get(((String) args[0]).toLowerCase(Locale.ROOT));
            return v == null ? -1 : Integer.parseInt(v);
        }
        // Object 的三个方法必须自己答，否则 Proxy 生成的是恒等式之外的垃圾值，
        // 断言消息里打印 request 时会直接 NPE。
        if ("equals".equals(name) && args != null && args.length == 1) {
            return proxy == args[0];
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("toString".equals(name)) {
            return "StubServletRequest" + headers.keySet();
        }
        return defaultFor(method.getReturnType());
    }

    /** 没答上来的方法按返回类型退化成默认值（基本类型不能返回 null）。 */
    private static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        return 0d;
    }
}
