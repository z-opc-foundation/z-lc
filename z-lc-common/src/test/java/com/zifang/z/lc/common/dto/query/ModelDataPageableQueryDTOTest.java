package com.zifang.z.lc.common.dto.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelDataPageableQueryDTO 单元测试
 *
 * @author zifang
 */
class ModelDataPageableQueryDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetCurrent() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setCurrent(5L);
        assertThat(dto.getCurrent()).isEqualTo(5L);
    }

    @Test
    void shouldSetAndGetSize() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setSize(50L);
        assertThat(dto.getSize()).isEqualTo(50L);
    }

    @Test
    void shouldSetAndGetAppCode() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setModelCode("model-001");
        assertThat(dto.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetPageCode() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setPageCode("page-001");
        assertThat(dto.getPageCode()).isEqualTo("page-001");
    }

    @Test
    void shouldSetAndGetQueryBeans() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        ModelDataQueryBean bean = new ModelDataQueryBean();
        List<ModelDataQueryBean> queryBeans = Arrays.asList(bean);
        dto.setQueryBeans(queryBeans);
        assertThat(dto.getQueryBeans()).isEqualTo(queryBeans);
    }

    @Test
    void shouldSetAndGetOrderBean() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        ModelDataOrderBean orderBean = new ModelDataOrderBean();
        dto.setOrderBean(orderBean);
        assertThat(dto.getOrderBean()).isEqualTo(orderBean);
    }

    @Test
    void shouldSetAndGetQueryCondition() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        ModelDataQueryCondition queryCondition = new ModelDataQueryCondition();
        dto.setQueryCondition(queryCondition);
        assertThat(dto.getQueryCondition()).isEqualTo(queryCondition);
    }

    @Test
    void shouldSetAndGetSelectFieldCodes() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        List<String> selectFieldCodes = Arrays.asList("field1", "field2");
        dto.setSelectFieldCodes(selectFieldCodes);
        assertThat(dto.getSelectFieldCodes()).isEqualTo(selectFieldCodes);
    }

    @Test
    void shouldSetAndGetKeysetPagination() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setKeysetPagination(true);
        assertThat(dto.getKeysetPagination()).isTrue();
    }

    @Test
    void shouldSetAndGetKeysetAfterPk() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setKeysetAfterPk(123L);
        assertThat(dto.getKeysetAfterPk()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetQueryOffset() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setQueryOffset(100L);
        assertThat(dto.getQueryOffset()).isEqualTo(100L);
    }

    @Test
    void shouldHandleNullValues() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        assertThat(dto.getCurrent()).isEqualTo(1L);
        assertThat(dto.getSize()).isEqualTo(20L);
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
        assertThat(dto.getPageCode()).isNull();
        assertThat(dto.getQueryBeans()).isNull();
        assertThat(dto.getOrderBean()).isNull();
        assertThat(dto.getQueryCondition()).isNull();
        assertThat(dto.getSelectFieldCodes()).isNull();
        assertThat(dto.getKeysetPagination()).isNull();
        assertThat(dto.getKeysetAfterPk()).isNull();
        assertThat(dto.getQueryOffset()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetDefaultValues() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        assertThat(dto.getCurrent()).isEqualTo(1L);
        assertThat(dto.getSize()).isEqualTo(20L);
    }

    @Test
    void shouldSetEmptyStrings() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setAppCode("");
        dto.setModelCode("");
        dto.setPageCode("");
        
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getModelCode()).isEmpty();
        assertThat(dto.getPageCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setAppCode(null);
        dto.setModelCode(null);
        dto.setPageCode(null);
        
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getModelCode()).isNull();
        assertThat(dto.getPageCode()).isNull();
    }

    @Test
    void shouldSetEmptyQueryBeans() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setQueryBeans(Arrays.asList());
        assertThat(dto.getQueryBeans()).isEmpty();
    }

    @Test
    void shouldSetNullQueryBeans() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setQueryBeans(null);
        assertThat(dto.getQueryBeans()).isNull();
    }

    @Test
    void shouldSetEmptySelectFieldCodes() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setSelectFieldCodes(Arrays.asList());
        assertThat(dto.getSelectFieldCodes()).isEmpty();
    }

    @Test
    void shouldSetNullSelectFieldCodes() {
        ModelDataPageableQueryDTO dto = new ModelDataPageableQueryDTO();
        dto.setSelectFieldCodes(null);
        assertThat(dto.getSelectFieldCodes()).isNull();
    }
}