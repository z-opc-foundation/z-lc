package com.zifang.z.lc.common.dto.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelDataOrderBean 单元测试
 *
 * @author zifang
 */
class ModelDataOrderBeanTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        assertThat(bean).isNotNull();
    }

    @Test
    void shouldSetAndGetFieldCodes() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        List<String> fieldCodes = Arrays.asList("create_time", "id");
        bean.setFieldCodes(fieldCodes);
        assertThat(bean.getFieldCodes()).isEqualTo(fieldCodes);
    }

    @Test
    void shouldSetAndGetOrderType() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        bean.setOrderType("desc");
        assertThat(bean.getOrderType()).isEqualTo("desc");
    }

    @Test
    void shouldHandleNullValues() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        assertThat(bean.getFieldCodes()).isNull();
        assertThat(bean.getOrderType()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        assertThat(bean).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyFieldCodes() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        bean.setFieldCodes(Arrays.asList());
        assertThat(bean.getFieldCodes()).isEmpty();
    }

    @Test
    void shouldSetNullFieldCodes() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        bean.setFieldCodes(null);
        assertThat(bean.getFieldCodes()).isNull();
    }

    @Test
    void shouldSetEmptyOrderType() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        bean.setOrderType("");
        assertThat(bean.getOrderType()).isEmpty();
    }

    @Test
    void shouldSetNullOrderType() {
        ModelDataOrderBean bean = new ModelDataOrderBean();
        bean.setOrderType(null);
        assertThat(bean.getOrderType()).isNull();
    }
}