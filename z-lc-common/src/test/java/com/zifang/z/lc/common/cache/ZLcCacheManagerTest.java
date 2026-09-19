package com.zifang.z.lc.common.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcCacheManager 单元测试
 */
class ZLcCacheManagerTest {

    @AfterEach
    void cleanup() {
        ZLcCacheManager.cleanAll();
    }

    @Test
    void buildShouldRegisterCache() {
        ZLcCache<String> cache = ZLcCacheManager.build(String.class);
        assertThat(cache).isNotNull();
        assertThat(ZLcCacheManager.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void buildShouldReturnSameInstanceForSameClass() {
        ZLcCache<String> c1 = ZLcCacheManager.build(String.class);
        ZLcCache<String> c2 = ZLcCacheManager.build(String.class);
        assertThat(c1).isSameAs(c2);
    }

    @Test
    void buildShouldThrowForNullClass() {
        assertThatThrownBy(() -> ZLcCacheManager.build(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetchCacheShouldReturnRegistered() {
        ZLcCache<String> registered = ZLcCacheManager.build(String.class);
        ZLcCache<String> fetched = ZLcCacheManager.fetchCache(String.class);
        assertThat(fetched).isSameAs(registered);
    }

    @Test
    void fetchCacheShouldReturnNullForUnregistered() {
        assertThat(ZLcCacheManager.fetchCache(String.class)).isNull();
    }

    @Test
    void fetchCacheShouldReturnNullForNullClass() {
        assertThat(ZLcCacheManager.fetchCache(null)).isNull();
    }

    @Test
    void evictShouldRemoveCache() {
        ZLcCacheManager.build(String.class);
        ZLcCacheManager.evict(String.class);
        assertThat(ZLcCacheManager.fetchCache(String.class)).isNull();
    }

    @Test
    void evictShouldIgnoreNull() {
        ZLcCacheManager.build(String.class);
        ZLcCacheManager.evict(null);
        assertThat(ZLcCacheManager.fetchCache(String.class)).isNotNull();
    }

    @Test
    void cleanAllShouldRemoveAllCaches() {
        ZLcCacheManager.build(String.class);
        ZLcCacheManager.build(Integer.class);
        ZLcCacheManager.cleanAll();
        assertThat(ZLcCacheManager.size()).isZero();
    }

    @Test
    void sizeShouldReturnZeroInitially() {
        ZLcCacheManager.cleanAll();
        assertThat(ZLcCacheManager.size()).isZero();
    }

    @Test
    void sizeShouldGrowWithNewCaches() {
        ZLcCacheManager.cleanAll();
        ZLcCacheManager.build(String.class);
        assertThat(ZLcCacheManager.size()).isEqualTo(1);
        ZLcCacheManager.build(Integer.class);
        assertThat(ZLcCacheManager.size()).isEqualTo(2);
    }

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<ZLcCacheManager> ctor =
                ZLcCacheManager.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers())).isTrue();
    }
}