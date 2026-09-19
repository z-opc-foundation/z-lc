package com.zifang.z.lc.common.bpmn.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcGateWayInfo 单元测试
 */
class ZLcGateWayInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcGateWayInfo info = new ZLcGateWayInfo();
        assertThat(info).isNotNull();
        assertThat(info.getId()).isNull();
        assertThat(info.getName()).isNull();
        assertThat(info.getDocumentation()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        ZLcGateWayInfo info = new ZLcGateWayInfo("gw1", "Decision", "Choose path");
        assertThat(info.getId()).isEqualTo("gw1");
        assertThat(info.getName()).isEqualTo("Decision");
        assertThat(info.getDocumentation()).isEqualTo("Choose path");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcGateWayInfo info = new ZLcGateWayInfo();
        assertThat(info).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportSetters() {
        ZLcGateWayInfo info = new ZLcGateWayInfo();
        info.setId("id1");
        info.setName("name1");
        info.setDocumentation("doc1");
        assertThat(info.getId()).isEqualTo("id1");
        assertThat(info.getName()).isEqualTo("name1");
        assertThat(info.getDocumentation()).isEqualTo("doc1");
    }

    @Test
    void toStringShouldIncludeIdAndName() {
        ZLcGateWayInfo info = new ZLcGateWayInfo("gw1", "Decision", null);
        assertThat(info.toString()).contains("gw1").contains("Decision");
    }
}