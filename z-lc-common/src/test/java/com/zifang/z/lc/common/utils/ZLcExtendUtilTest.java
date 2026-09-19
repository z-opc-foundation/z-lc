package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExtendUtil 单元测试
 *
 * @author zifang
 */
class ZLcExtendUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcExtendUtil> constructor = ZLcExtendUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnDefaultInstanceForNullExtend() {
        ExtendInfo result = ZLcExtendUtil.autoFillExtend((String) null, ExtendInfo.class);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isNull();
    }

    @Test
    void shouldReturnDefaultInstanceForEmptyExtend() {
        ExtendInfo result = ZLcExtendUtil.autoFillExtend("", ExtendInfo.class);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldReturnDefaultInstanceForNullStringExtend() {
        ExtendInfo result = ZLcExtendUtil.autoFillExtend("null", ExtendInfo.class);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldParseJsonExtend() {
        String json = "{\"name\":\"test\",\"value\":123}";
        ExtendInfo result = ZLcExtendUtil.autoFillExtend(json, ExtendInfo.class);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getValue()).isEqualTo(123);
    }

    @Test
    void shouldReturnExtendObjectWhenNotNull() {
        ExtendInfo existing = new ExtendInfo();
        existing.setName("existing");

        ExtendInfo result = ZLcExtendUtil.autoFillExtend(existing, ExtendInfo.class);
        assertThat(result).isSameAs(existing);
        assertThat(result.getName()).isEqualTo("existing");
    }

    @Test
    void shouldReturnDefaultWhenExtendObjectIsNull() {
        ExtendInfo result = ZLcExtendUtil.autoFillExtend((ExtendInfo) null, ExtendInfo.class);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isNull();
    }

    // 测试用的内部类
    static class ExtendInfo {
        private String name;
        private int value;

        public ExtendInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
