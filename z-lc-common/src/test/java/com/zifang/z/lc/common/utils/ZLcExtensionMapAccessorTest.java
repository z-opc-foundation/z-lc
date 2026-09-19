package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.dto.ZLcNullNode;
import org.junit.jupiter.api.Test;
import org.springframework.expression.TypedValue;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExtensionMapAccessor 单元测试
 *
 * @author zifang
 */
class ZLcExtensionMapAccessorTest {

    @Test
    void shouldExtendMapAccessor() {
        ZLcExtensionMapAccessor accessor = new ZLcExtensionMapAccessor();
        assertThat(accessor).isInstanceOf(org.springframework.context.expression.MapAccessor.class);
    }

    @Test
    void shouldReturnNullNodeForMissingKey() {
        ZLcExtensionMapAccessor accessor = new ZLcExtensionMapAccessor();
        Map<String, Object> map = new HashMap<>();
        map.put("existing", "value");

        TypedValue result = accessor.read(null, map, "missing");
        assertThat(result.getValue()).isInstanceOf(ZLcNullNode.class);
    }

    @Test
    void shouldReturnValueForExistingKey() {
        ZLcExtensionMapAccessor accessor = new ZLcExtensionMapAccessor();
        Map<String, Object> map = new HashMap<>();
        map.put("existing", "value");

        TypedValue result = accessor.read(null, map, "existing");
        assertThat(result.getValue()).isEqualTo("value");
    }

    @Test
    void shouldReturnNullNodeForNullTarget() {
        ZLcExtensionMapAccessor accessor = new ZLcExtensionMapAccessor();
        TypedValue result = accessor.read(null, null, "any");
        assertThat(result.getValue()).isInstanceOf(ZLcNullNode.class);
    }

    @Test
    void shouldIdentifyNullNode() {
        ZLcNullNode node = ZLcNullNode.INSTANCE;
        assertThat(ZLcExtensionMapAccessor.isKeyNotExist(node)).isTrue();
        assertThat(ZLcExtensionMapAccessor.isKeyExist(node)).isFalse();
    }

    @Test
    void shouldIdentifyExistingValue() {
        Object value = "real-value";
        assertThat(ZLcExtensionMapAccessor.isKeyNotExist(value)).isFalse();
        assertThat(ZLcExtensionMapAccessor.isKeyExist(value)).isTrue();
    }
}