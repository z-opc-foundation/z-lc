package com.zifang.z.lc.common.bpmn.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStartEventInfo 单元测试
 */
class ZLcStartEventInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcStartEventInfo info = new ZLcStartEventInfo();
        assertThat(info).isNotNull();
        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getDocumentation()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        ZLcStartEventInfo info = new ZLcStartEventInfo("start1", "Start", "Begin process");
        assertThat(info.getId()).isEqualTo("start1");
        assertThat(info.getName()).isEqualTo("Start");
        assertThat(info.getDocumentation()).isEqualTo("Begin process");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcStartEventInfo info = new ZLcStartEventInfo();
        assertThat(info).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportSetters() {
        ZLcStartEventInfo info = new ZLcStartEventInfo();
        info.setId("id1");
        info.setName("name1");
        info.setDocumentation("doc1");
        assertThat(info.getId()).isEqualTo("id1");
        assertThat(info.getName()).isEqualTo("name1");
        assertThat(info.getDocumentation()).isEqualTo("doc1");
    }

    @Test
    void toStringShouldIncludeIdAndName() {
        ZLcStartEventInfo info = new ZLcStartEventInfo("start1", "Start", null);
        assertThat(info.toString()).contains("start1").contains("Start");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcStartEventInfo info = new ZLcStartEventInfo(null, null, null);
        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getDocumentation()).isNull();
    }
}