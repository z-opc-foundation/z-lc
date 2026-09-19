package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDeepCopyUtil 单元测试
 *
 * @author zifang
 */
class ZLcDeepCopyUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcDeepCopyUtil> constructor = ZLcDeepCopyUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldDeepCopySimpleObject() {
        TestObject original = new TestObject("test", 123);
        TestObject copy = ZLcDeepCopyUtil.jsonCopy(original, TestObject.class);

        assertThat(copy).isNotNull();
        assertThat(copy.getName()).isEqualTo("test");
        assertThat(copy.getValue()).isEqualTo(123);
        assertThat(copy).isNotSameAs(original);
    }

    @Test
    void shouldDeepCopyWithModifiedValues() {
        TestObject original = new TestObject("test", 123);
        TestObject copy = ZLcDeepCopyUtil.jsonCopy(original, TestObject.class);

        copy.setName("modified");
        assertThat(original.getName()).isEqualTo("test");
        assertThat(copy.getName()).isEqualTo("modified");
    }

    @Test
    void shouldDeepCopyList() {
        List<TestObject> originalList = Arrays.asList(
                new TestObject("a", 1),
                new TestObject("b", 2),
                new TestObject("c", 3)
        );

        List<TestObject> copyList = ZLcDeepCopyUtil.jsonCopyList(originalList, TestObject.class);

        assertThat(copyList).hasSize(3);
        assertThat(copyList.get(0).getName()).isEqualTo("a");
        assertThat(copyList.get(1).getName()).isEqualTo("b");
        assertThat(copyList.get(2).getName()).isEqualTo("c");
        assertThat(copyList).isNotSameAs(originalList);
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        List<TestObject> result = ZLcDeepCopyUtil.jsonCopyList(null, TestObject.class);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeepCopyNestedObject() {
        NestedObject original = new NestedObject();
        original.setName("parent");
        original.setChild(new TestObject("child", 456));

        NestedObject copy = ZLcDeepCopyUtil.jsonCopy(original, NestedObject.class);

        assertThat(copy.getName()).isEqualTo("parent");
        assertThat(copy.getChild().getName()).isEqualTo("child");
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getChild()).isNotSameAs(original.getChild());
    }

    // 测试用的内部类
    static class TestObject {
        private String name;
        private int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    static class NestedObject {
        private String name;
        private TestObject child;

        public NestedObject() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public TestObject getChild() { return child; }
        public void setChild(TestObject child) { this.child = child; }
    }
}
