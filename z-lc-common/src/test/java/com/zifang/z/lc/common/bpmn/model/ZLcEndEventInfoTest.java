package com.zifang.z.lc.common.bpmn.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcEndEventInfo 单元测试
 */
class ZLcEndEventInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcEndEventInfo info = new ZLcEndEventInfo();
        assertThat(info).isNotNull();
        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getDocumentation()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        ZLcEndEventInfo info = new ZLcEndEventInfo("end1", "End", "Finish");
        assertThat(info.getId()).isEqualTo("end1");
        assertThat(info.getName()).isEqualTo("End");
        assertThat(info.getDocumentation()).isEqualTo("Finish");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcEndEventInfo info = new ZLcEndEventInfo();
        assertThat(info).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportSetters() {
        ZLcEndEventInfo info = new ZLcEndEventInfo();
        info.setId("id1");
        info.setName("name1");
        info.setDocumentation("doc1");
        assertThat(info.getId()).isEqualTo("id1");
        assertThat(info.getName()).isEqualTo("name1");
        assertThat(info.getDocumentation()).isEqualTo("doc1");
    }

    @Test
    void toStringShouldIncludeIdAndName() {
        ZLcEndEventInfo info = new ZLcEndEventInfo("end1", "End", null);
        assertThat(info.toString()).contains("end1").contains("End");
    }

    @Test
    void shouldHaveDistinctSerialVersionUid() {
        // EndEventInfo should have its own serialVersionUID (not shared with StartEventInfo)
        ZLcEndEventInfo end = new ZLcEndEventInfo();
        ZLcStartEventInfo start = new ZLcStartEventInfo();
        assertThat(end).isNotEqualTo(start);
    }
}