package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPageableUtil 单元测试
 *
 * @author zifang
 */
class ZLcPageableUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcPageableUtil> constructor = ZLcPageableUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnCorrectSubList() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h");

        List<String> page1 = ZLcPageableUtil.sub(list, 1, 3);
        assertThat(page1).containsExactly("a", "b", "c");

        List<String> page2 = ZLcPageableUtil.sub(list, 2, 3);
        assertThat(page2).containsExactly("d", "e", "f");

        List<String> page3 = ZLcPageableUtil.sub(list, 3, 3);
        assertThat(page3).containsExactly("g", "h");
    }

    @Test
    void shouldHandleCurrentLessThanOne() {
        List<String> list = Arrays.asList("a", "b", "c");

        List<String> result = ZLcPageableUtil.sub(list, 0, 2);
        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void shouldHandleCurrentGreaterThanTotal() {
        List<String> list = Arrays.asList("a", "b", "c");

        // 当current > total时，from会大于to，会抛出IllegalArgumentException
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ZLcPageableUtil.sub(list, 10, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleSizeGreaterThanTotal() {
        List<String> list = Arrays.asList("a", "b", "c");

        List<String> result = ZLcPageableUtil.sub(list, 1, 10);
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void shouldNormalizeCurrentPage() {
        assertThat(ZLcPageableUtil.normalizeCurrent(null)).isEqualTo(1);
        assertThat(ZLcPageableUtil.normalizeCurrent(0L)).isEqualTo(1);
        assertThat(ZLcPageableUtil.normalizeCurrent(-1L)).isEqualTo(1);
        assertThat(ZLcPageableUtil.normalizeCurrent(5L)).isEqualTo(5);
    }

    @Test
    void shouldNormalizeSize() {
        assertThat(ZLcPageableUtil.normalizeSize(null)).isEqualTo(20);
        assertThat(ZLcPageableUtil.normalizeSize(0L)).isEqualTo(20);
        assertThat(ZLcPageableUtil.normalizeSize(-1L)).isEqualTo(20);
        assertThat(ZLcPageableUtil.normalizeSize(10L)).isEqualTo(10);
    }

    @Test
    void shouldHandleDefaultPageable() {
        long[] current = new long[]{0};
        long[] size = new long[]{0};

        ZLcPageableUtil.handleDefaultPageable(current, size);

        assertThat(current[0]).isEqualTo(1);
        assertThat(size[0]).isEqualTo(20);
    }

    @Test
    void shouldNotModifyValidPageable() {
        long[] current = new long[]{5};
        long[] size = new long[]{10};

        ZLcPageableUtil.handleDefaultPageable(current, size);

        assertThat(current[0]).isEqualTo(5);
        assertThat(size[0]).isEqualTo(10);
    }

    @Test
    void shouldHandleNullArrays() {
        // 不应抛出异常
        ZLcPageableUtil.handleDefaultPageable(null, null);
    }

    @Test
    void shouldHandleEmptyArrays() {
        long[] current = new long[]{};
        long[] size = new long[]{};

        // 不应抛出异常
        ZLcPageableUtil.handleDefaultPageable(current, size);
    }
}
