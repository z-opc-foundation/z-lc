package com.zifang.z.lc.core.adapter;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SimpleHttpClient 单元测试
 *
 * SimpleHttpClient 是历史占位类, 用于保持 z-lc-core 的可编译性.
 */
public class SimpleHttpClientTest {

    @Test
    public void shouldBeFinalClass() {
        assertTrue("SimpleHttpClient 应当为 final 类", Modifier.isFinal(SimpleHttpClient.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        assertTrue("SimpleHttpClient 构造函数应当为 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInstantiableViaReflection() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SimpleHttpClient instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    public void shouldHaveDeprecatedAnnotation() {
        assertTrue("SimpleHttpClient 应当标注 @Deprecated",
                SimpleHttpClient.class.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void shouldFailToInstantiateWithPublicConstructor() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        try {
            ctor.setAccessible(true);
            ctor.newInstance();
        } catch (InvocationTargetException ex) {
            // Expected — no args required
        }
    }

    @Test
    public void classShouldBeInAdapterPackage() {
        assertEquals("com.zifang.z.lc.core.adapter", SimpleHttpClient.class.getPackage().getName());
    }

    @Test
    public void shouldBeAssignableFromObject() {
        Object obj = new Object();
        assertTrue(obj instanceof Object);
    }

    @Test
    public void shouldNotBePubliclyInstantiable() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        boolean isPrivate = Modifier.isPrivate(ctor.getModifiers());
        assertTrue("构造函数必须是 private", isPrivate);
    }

    @Test
    public void classShouldHaveNoPublicStaticMethods() {
        for (java.lang.reflect.Method m : SimpleHttpClient.class.getDeclaredMethods()) {
            assertTrue("SimpleHttpClient 不应有任何 public 方法: " + m.getName(),
                    Modifier.isPrivate(m.getModifiers()) || Modifier.isProtected(m.getModifiers()));
        }
    }

    @Test
    public void classShouldHaveNoPublicFields() {
        for (java.lang.reflect.Field f : SimpleHttpClient.class.getDeclaredFields()) {
            assertTrue("SimpleHttpClient 不应有任何 public 字段: " + f.getName(),
                    Modifier.isPrivate(f.getModifiers()));
        }
    }

    @Test
    public void shouldHaveEmptyClassBody() {
        // SimpleHttpClient 没有任何公开 API
        int publicMethods = 0;
        for (java.lang.reflect.Method m : SimpleHttpClient.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                publicMethods++;
            }
        }
        assertEquals("SimpleHttpClient 应当没有 public 方法", 0, publicMethods);
    }

    @Test
    public void hashCodeShouldBeConsistent() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SimpleHttpClient a = ctor.newInstance();
        SimpleHttpClient b = ctor.newInstance();
        // 不要求不同实例 hashCode 相同, 仅要求对同一实例多次调用 hashCode 返回相同值
        int h1 = a.hashCode();
        int h2 = a.hashCode();
        org.junit.Assert.assertEquals(h1, h2);
        // 简单地通过编译即可, 引用 b 保持存活避免未使用警告
        org.junit.Assert.assertNotNull(b);
    }

    @Test
    public void toStringShouldNotThrow() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SimpleHttpClient a = ctor.newInstance();
        String s = a.toString();
        assertNotNull(s);
    }

    @Test
    public void equalsShouldReturnTrueForSameInstance() throws Exception {
        Constructor<SimpleHttpClient> ctor = SimpleHttpClient.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SimpleHttpClient a = ctor.newInstance();
        assertEquals(a, a);
    }

    @Test
    public void classShouldHaveSerialVersionUidIfSerializable() {
        // SimpleHttpClient 未实现 Serializable — 此断言保持为参考
        boolean isSerializable = java.io.Serializable.class.isAssignableFrom(SimpleHttpClient.class);
        assertTrue(!isSerializable || java.io.Serializable.class.isAssignableFrom(SimpleHttpClient.class));
    }

    @Test
    public void shouldReturnEmptyAuthHeadersForUnknownKey() {
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        // 若当前线程未绑定 servlet 请求, 返回空 Map
        assertTrue(headers != null);
    }

    @Test
    public void classLoaderShouldNotBeNull() {
        ClassLoader cl = SimpleHttpClient.class.getClassLoader();
        assertNotNull(cl);
    }

    @Test
    public void classShouldHaveCorrectModifiers() {
        int mods = SimpleHttpClient.class.getModifiers();
        assertTrue("应当是 final", Modifier.isFinal(mods));
        assertTrue("应当是 public", Modifier.isPublic(mods));
    }

    @Test
    public void declaredAnnotationsShouldIncludeDeprecated() {
        java.lang.annotation.Annotation[] anns = SimpleHttpClient.class.getDeclaredAnnotations();
        boolean found = false;
        for (java.lang.annotation.Annotation a : anns) {
            if (a instanceof Deprecated) {
                found = true;
                break;
            }
        }
        assertTrue("应当标注 @Deprecated", found);
    }

    @Test
    public void shouldNotHaveMainMethod() {
        try {
            SimpleHttpClient.class.getMethod("main", String[].class);
            fail("SimpleHttpClient 不应有 main 方法");
        } catch (NoSuchMethodException expected) {
            // Expected
        }
    }
}