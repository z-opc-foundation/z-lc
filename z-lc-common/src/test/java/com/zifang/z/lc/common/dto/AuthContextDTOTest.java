package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthContextDTO 单元测试
 *
 * @author zifang
 */
class AuthContextDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AuthContextDTO dto = new AuthContextDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetUserId() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setUserId("user-123");
        assertThat(dto.getUserId()).isEqualTo("user-123");
    }

    @Test
    void shouldSetAndGetUserName() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setUserName("张三");
        assertThat(dto.getUserName()).isEqualTo("张三");
    }

    @Test
    void shouldSetAndGetTenantCode() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetRoles() {
        AuthContextDTO dto = new AuthContextDTO();
        List<String> roles = Arrays.asList("ADMIN", "USER");
        dto.setRoles(roles);
        assertThat(dto.getRoles()).isEqualTo(roles);
    }

    @Test
    void shouldSetAndGetPermissions() {
        AuthContextDTO dto = new AuthContextDTO();
        List<String> permissions = Arrays.asList("read", "write", "delete");
        dto.setPermissions(permissions);
        assertThat(dto.getPermissions()).isEqualTo(permissions);
    }

    @Test
    void shouldHandleNullValues() {
        AuthContextDTO dto = new AuthContextDTO();
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getUserName()).isNull();
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getRoles()).isNull();
        assertThat(dto.getPermissions()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AuthContextDTO dto = new AuthContextDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setUserId("");
        dto.setUserName("");
        dto.setTenantCode("");
        
        assertThat(dto.getUserId()).isEmpty();
        assertThat(dto.getUserName()).isEmpty();
        assertThat(dto.getTenantCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setUserId(null);
        dto.setUserName(null);
        dto.setTenantCode(null);
        
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getUserName()).isNull();
        assertThat(dto.getTenantCode()).isNull();
    }

    @Test
    void shouldSetEmptyRoles() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setRoles(Arrays.asList());
        assertThat(dto.getRoles()).isEmpty();
    }

    @Test
    void shouldSetNullRoles() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setRoles(null);
        assertThat(dto.getRoles()).isNull();
    }

    @Test
    void shouldSetEmptyPermissions() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setPermissions(Arrays.asList());
        assertThat(dto.getPermissions()).isEmpty();
    }

    @Test
    void shouldSetNullPermissions() {
        AuthContextDTO dto = new AuthContextDTO();
        dto.setPermissions(null);
        assertThat(dto.getPermissions()).isNull();
    }
}