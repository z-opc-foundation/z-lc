package com.zifang.z.lc.common.dto.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelDataQueryDTO 单元测试
 *
 * @author zifang
 */
class ModelDataQueryDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setModelCode("model-001");
        assertThat(dto.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetQueryBeans() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        ModelDataQueryBean bean = new ModelDataQueryBean();
        List<ModelDataQueryBean> queryBeans = Arrays.asList(bean);
        dto.setQueryBeans(queryBeans);
        assertThat(dto.getQueryBeans()).isEqualTo(queryBeans);
    }

    @Test
    void shouldSetAndGetOrderBean() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        ModelDataOrderBean orderBean = new ModelDataOrderBean();
        dto.setOrderBean(orderBean);
        assertThat(dto.getOrderBean()).isEqualTo(orderBean);
    }

    @Test
    void shouldHandleNullValues() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
        assertThat(dto.getQueryBeans()).isNull();
        assertThat(dto.getOrderBean()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setAppCode("");
        dto.setModelCode("");
        
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getModelCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setAppCode(null);
        dto.setModelCode(null);
        
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
    }

    @Test
    void shouldSetEmptyQueryBeans() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setQueryBeans(Arrays.asList());
        assertThat(dto.getQueryBeans()).isEmpty();
    }

    @Test
    void shouldSetNullQueryBeans() {
        ModelDataQueryDTO dto = new ModelDataQueryDTO();
        dto.setQueryBeans(null);
        assertThat(dto.getQueryBeans()).isNull();
    }
}