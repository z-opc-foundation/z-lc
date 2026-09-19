package com.zifang.z.lc.common.dto.tree;

import com.zifang.z.lc.common.constance.PermissionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthorityAttachment 单元测试
 *
 * @author zifang
 */
class AuthorityAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetPermissionCode() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setPermissionCode("perm-001");
        assertThat(attachment.getPermissionCode()).isEqualTo("perm-001");
    }

    @Test
    void shouldSetAndGetResourceCode() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setResourceCode("resource-001");
        assertThat(attachment.getResourceCode()).isEqualTo("resource-001");
    }

    @Test
    void shouldSetAndGetResourceName() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setResourceName("资源名称");
        assertThat(attachment.getResourceName()).isEqualTo("资源名称");
    }

    @Test
    void shouldSetAndGetAttrCode() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setAttrCode("attr-001");
        assertThat(attachment.getAttrCode()).isEqualTo("attr-001");
    }

    @Test
    void shouldSetAndGetPermissionName() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setPermissionName("权限名称");
        assertThat(attachment.getPermissionName()).isEqualTo("权限名称");
    }

    @Test
    void shouldSetAndGetPermissionType() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setPermissionType(PermissionType.MENU);
        assertThat(attachment.getPermissionType()).isEqualTo(PermissionType.MENU);
    }

    @Test
    void shouldSetAndGetParentPermissionCode() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setParentPermissionCode("parent-perm-001");
        assertThat(attachment.getParentPermissionCode()).isEqualTo("parent-perm-001");
    }

    @Test
    void shouldSetAndGetMenuUrl() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setMenuUrl("/menu/url");
        assertThat(attachment.getMenuUrl()).isEqualTo("/menu/url");
    }

    @Test
    void shouldSetAndGetSelectedFlag() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setSelectedFlag(true);
        assertThat(attachment.isSelectedFlag()).isTrue();
    }

    @Test
    void shouldSetAndGetModifiedFlag() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setModifiedFlag(true);
        assertThat(attachment.isModifiedFlag()).isTrue();
    }

    @Test
    void shouldHandleNullValues() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getPermissionCode()).isNull();
        assertThat(attachment.getResourceCode()).isNull();
        assertThat(attachment.getResourceName()).isNull();
        assertThat(attachment.getAttrCode()).isNull();
        assertThat(attachment.getPermissionName()).isNull();
        assertThat(attachment.getPermissionType()).isNull();
        assertThat(attachment.getParentPermissionCode()).isNull();
        assertThat(attachment.getMenuUrl()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldCreateMenuAttachmentWithFactoryMethod() {
        AuthorityAttachment attachment = AuthorityAttachment.ofMenu("菜单名称", "/menu/url", "perm-001");
        
        assertThat(attachment.getPermissionName()).isEqualTo("菜单名称");
        assertThat(attachment.getMenuUrl()).isEqualTo("/menu/url");
        assertThat(attachment.getPermissionType()).isEqualTo(PermissionType.MENU);
        assertThat(attachment.getPermissionCode()).isEqualTo("perm-001");
    }

    @Test
    void shouldCreateAPIAttachmentWithFactoryMethod() {
        AuthorityAttachment attachment = AuthorityAttachment.ofAPI("API权限", "api-perm-001");
        
        assertThat(attachment.getPermissionType()).isEqualTo(PermissionType.API);
        assertThat(attachment.getPermissionName()).isEqualTo("API权限");
        assertThat(attachment.getPermissionCode()).isEqualTo("api-perm-001");
    }

    @Test
    void shouldCreateDataAttachmentWithFactoryMethod() {
        AuthorityAttachment attachment = AuthorityAttachment.ofData("资源名称", "resource-001", "attr-001");
        
        assertThat(attachment.getResourceCode()).isEqualTo("resource-001");
        assertThat(attachment.getResourceName()).isEqualTo("资源名称");
        assertThat(attachment.getAttrCode()).isEqualTo("attr-001");
    }

    @Test
    void shouldSetEmptyStrings() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setAppCode("");
        attachment.setPermissionCode("");
        attachment.setResourceCode("");
        attachment.setResourceName("");
        attachment.setAttrCode("");
        attachment.setPermissionName("");
        attachment.setParentPermissionCode("");
        attachment.setMenuUrl("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getPermissionCode()).isEmpty();
        assertThat(attachment.getResourceCode()).isEmpty();
        assertThat(attachment.getResourceName()).isEmpty();
        assertThat(attachment.getAttrCode()).isEmpty();
        assertThat(attachment.getPermissionName()).isEmpty();
        assertThat(attachment.getParentPermissionCode()).isEmpty();
        assertThat(attachment.getMenuUrl()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AuthorityAttachment attachment = new AuthorityAttachment();
        attachment.setAppCode(null);
        attachment.setPermissionCode(null);
        attachment.setResourceCode(null);
        attachment.setResourceName(null);
        attachment.setAttrCode(null);
        attachment.setPermissionName(null);
        attachment.setParentPermissionCode(null);
        attachment.setMenuUrl(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getPermissionCode()).isNull();
        assertThat(attachment.getResourceCode()).isNull();
        assertThat(attachment.getResourceName()).isNull();
        assertThat(attachment.getAttrCode()).isNull();
        assertThat(attachment.getPermissionName()).isNull();
        assertThat(attachment.getParentPermissionCode()).isNull();
        assertThat(attachment.getMenuUrl()).isNull();
    }
}