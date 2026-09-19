package com.zifang.z.lc.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCache 单元测试
 */
class ZLcCacheTest {

    private ZLcCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new ZLcCache<>();
    }

    @Test
    void shouldCreateEmpty() {
        assertThat(cache.size()).isZero();
        assertThat(cache.isEmpty()).isTrue();
    }

    @Test
    void shouldInjectAndPick() {
        cache.inject("key1", "value1");
        assertThat(cache.pick("key1")).isEqualTo("value1");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void shouldHitAfterInject() {
        cache.inject("k", "v");
        assertThat(cache.hit("k")).isTrue();
    }

    @Test
    void shouldNotHitMissingKey() {
        assertThat(cache.hit("missing")).isFalse();
    }

    @Test
    void shouldNotHitNullKey() {
        assertThat(cache.hit(null)).isFalse();
    }

    @Test
    void shouldReturnNullForNullKeyOnPick() {
        assertThat(cache.pick(null)).isNull();
    }

    @Test
    void shouldReturnNullForMissingKeyOnPick() {
        assertThat(cache.pick("missing")).isNull();
    }

    @Test
    void shouldRemoveOnInjectNull() {
        cache.inject("k", "v");
        cache.inject("k", null);
        assertThat(cache.size()).isZero();
    }

    @Test
    void shouldIgnoreInjectNullKey() {
        cache.inject(null, "v");
        assertThat(cache.size()).isZero();
    }

    @Test
    void shouldEvitSingleKey() {
        cache.inject("a", "1");
        cache.inject("b", "2");
        cache.evit("a");
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.pick("a")).isNull();
        assertThat(cache.pick("b")).isEqualTo("2");
    }

    @Test
    void shouldIgnoreEvitNullKey() {
        cache.inject("a", "1");
        cache.evit(null);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void shouldCleanAll() {
        cache.inject("a", "1");
        cache.inject("b", "2");
        cache.clean();
        assertThat(cache.size()).isZero();
        assertThat(cache.isEmpty()).isTrue();
    }

    @Test
    void shouldSupportNullValueCleanly() {
        cache.inject("k", null);
        assertThat(cache.size()).isZero();
        assertThat(cache.pick("k")).isNull();
    }

    @Test
    void shouldSetAndGetClazz() {
        cache.setClazz(String.class);
        assertThat(cache.getClazz()).isEqualTo(String.class);
    }

    @Test
    void shouldSupportOverwrite() {
        cache.inject("k", "v1");
        cache.inject("k", "v2");
        assertThat(cache.pick("k")).isEqualTo("v2");
        assertThat(cache.size()).isEqualTo(1);
    }
}