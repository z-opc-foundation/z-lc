package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcArrayUtil 单元测试
 *
 * @author zifang
 */
class ZLcArrayUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcArrayUtil> constructor = ZLcArrayUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldRetainAllMatchingElements() {
        List<String> candidator = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> userIds = Arrays.asList("b", "d", "e");

        boolean result = ZLcArrayUtil.retainAll(candidator, userIds);

        assertThat(result).isTrue();
        assertThat(candidator).containsExactly("b", "d");
    }

    @Test
    void shouldReturnFalseWhenNoIntersection() {
        List<String> candidator = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> userIds = Arrays.asList("c", "d");

        boolean result = ZLcArrayUtil.retainAll(candidator, userIds);

        assertThat(result).isFalse();
        assertThat(candidator).isEmpty();
    }

    @Test
    void shouldHandleEmptyCandidator() {
        List<String> candidator = new ArrayList<>();
        List<String> userIds = Arrays.asList("a", "b");

        boolean result = ZLcArrayUtil.retainAll(candidator, userIds);

        assertThat(result).isFalse();
        assertThat(candidator).isEmpty();
    }

    @Test
    void shouldHandleEmptyUserIds() {
        List<String> candidator = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> userIds = new ArrayList<>();

        boolean result = ZLcArrayUtil.retainAll(candidator, userIds);

        assertThat(result).isFalse();
        assertThat(candidator).isEmpty();
    }

    @Test
    void shouldMergeListAndElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        String t = "c";

        List<String> result = ZLcArrayUtil.merge(list, t);

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void shouldMergeAndDeduplicate() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        String t = "b";

        List<String> result = ZLcArrayUtil.merge(list, t);

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void shouldMergeWithNullList() {
        String t = "a";

        List<String> result = ZLcArrayUtil.merge(null, t);

        assertThat(result).containsExactly("a");
    }

    @Test
    void shouldMergeWithNullElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));

        List<String> result = ZLcArrayUtil.merge(list, null);

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void shouldMergeWithBothNull() {
        List<String> result = ZLcArrayUtil.merge(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMergeIntegerLists() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        Integer t = 4;

        List<Integer> result = ZLcArrayUtil.merge(list, t);

        assertThat(result).containsExactly(1, 2, 3, 4);
    }
}
