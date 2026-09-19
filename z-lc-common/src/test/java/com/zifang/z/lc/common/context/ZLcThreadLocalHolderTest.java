package com.zifang.z.lc.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcThreadLocalHolder 单元测试
 */
class ZLcThreadLocalHolderTest {

    private final ZLcThreadLocalHolder<String> holder = new ZLcThreadLocalHolder<>();

    @AfterEach
    void cleanup() {
        holder.clean();
    }

    @Test
    void shouldCreateWithoutValue() {
        assertThat(holder.get()).isNull();
        assertThat(holder.hasValue()).isFalse();
    }

    @Test
    void shouldSetAndGetValue() {
        holder.set("hello");
        assertThat(holder.get()).isEqualTo("hello");
        assertThat(holder.hasValue()).isTrue();
    }

    @Test
    void shouldOverwriteValue() {
        holder.set("v1");
        holder.set("v2");
        assertThat(holder.get()).isEqualTo("v2");
    }

    @Test
    void shouldAllowNullValue() {
        holder.set(null);
        assertThat(holder.get()).isNull();
        assertThat(holder.hasValue()).isFalse();
    }

    @Test
    void shouldCleanValue() {
        holder.set("v");
        holder.clean();
        assertThat(holder.get()).isNull();
        assertThat(holder.hasValue()).isFalse();
    }

    @Test
    void shouldBeThreadIsolated() throws Exception {
        holder.set("main-thread");

        final String[] otherThreadValue = new String[1];
        Thread t = new Thread(() -> otherThreadValue[0] = holder.get());
        t.start();
        t.join();

        assertThat(holder.get()).isEqualTo("main-thread");
        assertThat(otherThreadValue[0]).isNull();
    }

    @Test
    void hasValueShouldReturnFalseInitially() {
        ZLcThreadLocalHolder<Integer> h = new ZLcThreadLocalHolder<>();
        assertThat(h.hasValue()).isFalse();
    }

    @Test
    void hasValueShouldReturnTrueAfterSet() {
        ZLcThreadLocalHolder<Integer> h = new ZLcThreadLocalHolder<>();
        h.set(42);
        assertThat(h.hasValue()).isTrue();
        h.clean();
        assertThat(h.hasValue()).isFalse();
    }

    @Test
    void cleanShouldBeIdempotent() {
        holder.set("v");
        holder.clean();
        holder.clean();  // No exception
        assertThat(holder.get()).isNull();
    }
}