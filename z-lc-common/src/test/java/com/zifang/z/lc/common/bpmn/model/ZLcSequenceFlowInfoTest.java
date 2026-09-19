package com.zifang.z.lc.common.bpmn.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSequenceFlowInfo 单元测试
 */
class ZLcSequenceFlowInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo();
        assertThat(flow).isNotNull();
        assertThat(flow.getId()).isNull();
        assertThat(flow.getSourceRef()).isNull();
        assertThat(flow.getTargetRef()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo("flow1", "start1", "task1");
        assertThat(flow.getId()).isEqualTo("flow1");
        assertThat(flow.getSourceRef()).isEqualTo("start1");
        assertThat(flow.getTargetRef()).isEqualTo("task1");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo();
        assertThat(flow).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportSetters() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo();
        flow.setId("id1");
        flow.setSourceRef("src");
        flow.setTargetRef("tgt");
        assertThat(flow.getId()).isEqualTo("id1");
        assertThat(flow.getSourceRef()).isEqualTo("src");
        assertThat(flow.getTargetRef()).isEqualTo("tgt");
    }

    @Test
    void toStringShouldIncludeAllFields() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo("flow1", "start1", "task1");
        assertThat(flow.toString())
                .contains("flow1")
                .contains("start1")
                .contains("task1");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSequenceFlowInfo flow = new ZLcSequenceFlowInfo(null, null, null);
        assertThat(flow.getId()).isNull();
        assertThat(flow.getSourceRef()).isNull();
        assertThat(flow.getTargetRef()).isNull();
    }
}