package com.zifang.z.lc.common.bpmn.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFormData 单元测试
 *
 * @author zifang
 */
class ZLcFormDataTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcFormData formData = new ZLcFormData();
        assertThat(formData).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        ZLcFormData formData = new ZLcFormData();
        formData.setAppCode("app-001");
        assertThat(formData.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetFormCode() {
        ZLcFormData formData = new ZLcFormData();
        formData.setFormCode("form-001");
        assertThat(formData.getFormCode()).isEqualTo("form-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        ZLcFormData formData = new ZLcFormData();
        formData.setModelCode("model-001");
        assertThat(formData.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetFormInstanceId() {
        ZLcFormData formData = new ZLcFormData();
        formData.setFormInstanceId(123L);
        assertThat(formData.getFormInstanceId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetData() {
        ZLcFormData formData = new ZLcFormData();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "张三");
        data.put("age", 25);
        formData.setData(data);
        assertThat(formData.getData()).containsEntry("name", "张三");
        assertThat(formData.getData()).containsEntry("age", 25);
    }

    @Test
    void shouldSetAndGetBusinessKey() {
        ZLcFormData formData = new ZLcFormData();
        formData.setBusinessKey("biz-001");
        assertThat(formData.getBusinessKey()).isEqualTo("biz-001");
    }

    @Test
    void shouldSetAndGetFormName() {
        ZLcFormData formData = new ZLcFormData();
        formData.setFormName("请假申请");
        assertThat(formData.getFormName()).isEqualTo("请假申请");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcFormData formData = new ZLcFormData();
        assertThat(formData.getAppCode()).isNull();
        assertThat(formData.getFormCode()).isNull();
        assertThat(formData.getModelCode()).isNull();
        assertThat(formData.getFormInstanceId()).isNull();
        assertThat(formData.getData()).isNull();
        assertThat(formData.getBusinessKey()).isNull();
        assertThat(formData.getFormName()).isNull();
    }
}
