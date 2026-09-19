package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcIdentityLinkType 单元测试
 *
 * @author zifang
 */
class ZLcIdentityLinkTypeTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcIdentityLinkType> constructor = ZLcIdentityLinkType.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveAssigneeConstant() {
        assertThat(ZLcIdentityLinkType.ASSIGNEE).isEqualTo("assignee");
    }

    @Test
    void shouldHaveCandidateConstant() {
        assertThat(ZLcIdentityLinkType.CANDIDATE).isEqualTo("candidate");
    }

    @Test
    void shouldHaveOwnerConstant() {
        assertThat(ZLcIdentityLinkType.OWNER).isEqualTo("owner");
    }

    @Test
    void shouldHaveStarterConstant() {
        assertThat(ZLcIdentityLinkType.STARTER).isEqualTo("starter");
    }

    @Test
    void shouldHaveParticipantConstant() {
        assertThat(ZLcIdentityLinkType.PARTICIPANT).isEqualTo("participant");
    }
}
