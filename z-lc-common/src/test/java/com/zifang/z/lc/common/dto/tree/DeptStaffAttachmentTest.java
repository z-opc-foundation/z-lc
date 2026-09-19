package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeptStaffAttachment 单元测试
 *
 * @author zifang
 */
class DeptStaffAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DeptStaffAttachment attachment = new DeptStaffAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldImplementSerializable() {
        DeptStaffAttachment attachment = new DeptStaffAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }
}