package com.zifang.z.lc.common.dto.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelDataQueryBean 单元测试
 *
 * @author zifang
 */
class ModelDataQueryBeanTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        assertThat(bean).isNotNull();
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        ModelDataQueryBean bean = new ModelDataQueryBean("field1", "=", "value1");
        assertThat(bean.getFieldCode()).isEqualTo("field1");
        assertThat(bean.getMark()).isEqualTo("=");
        assertThat(bean.getValue()).isEqualTo("value1");
    }

    @Test
    void shouldSetAndGetFieldCode() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        bean.setFieldCode("field1");
        assertThat(bean.getFieldCode()).isEqualTo("field1");
    }

    @Test
    void shouldSetAndGetMark() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        bean.setMark("like");
        assertThat(bean.getMark()).isEqualTo("like");
    }

    @Test
    void shouldSetAndGetValue() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        bean.setValue("value1");
        assertThat(bean.getValue()).isEqualTo("value1");
    }

    @Test
    void shouldHandleNullValues() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        assertThat(bean.getFieldCode()).isNull();
        assertThat(bean.getMark()).isNull();
        assertThat(bean.getValue()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        assertThat(bean).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldCreateEqConditionWithFactoryMethod() {
        ModelDataQueryBean bean = ModelDataQueryBean.eq("field1", "value1");
        assertThat(bean.getFieldCode()).isEqualTo("field1");
        assertThat(bean.getMark()).isEqualTo("=");
        assertThat(bean.getValue()).isEqualTo("value1");
    }

    @Test
    void shouldCreateLikeConditionWithFactoryMethod() {
        ModelDataQueryBean bean = ModelDataQueryBean.like("field1", "value1");
        assertThat(bean.getFieldCode()).isEqualTo("field1");
        assertThat(bean.getMark()).isEqualTo("like");
        assertThat(bean.getValue()).isEqualTo("value1");
    }

    @Test
    void shouldCreateEqConditionWithNullValue() {
        ModelDataQueryBean bean = ModelDataQueryBean.eq("field1", null);
        assertThat(bean.getFieldCode()).isEqualTo("field1");
        assertThat(bean.getMark()).isEqualTo("=");
        assertThat(bean.getValue()).isNull();
    }

    @Test
    void shouldCreateLikeConditionWithNullValue() {
        ModelDataQueryBean bean = ModelDataQueryBean.like("field1", null);
        assertThat(bean.getFieldCode()).isEqualTo("field1");
        assertThat(bean.getMark()).isEqualTo("like");
        assertThat(bean.getValue()).isNull();
    }

    @Test
    void shouldSetEmptyStrings() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        bean.setFieldCode("");
        bean.setMark("");
        
        assertThat(bean.getFieldCode()).isEmpty();
        assertThat(bean.getMark()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ModelDataQueryBean bean = new ModelDataQueryBean();
        bean.setFieldCode(null);
        bean.setMark(null);
        
        assertThat(bean.getFieldCode()).isNull();
        assertThat(bean.getMark()).isNull();
    }
}