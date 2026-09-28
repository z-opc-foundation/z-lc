package com.zifang.z.lc.common.permission;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAcePermissionDefinitions 单元测试
 *
 * @author zifang
 */
class ZLcAcePermissionDefinitionsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcAcePermissionDefinitions> constructor = ZLcAcePermissionDefinitions.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveButtonPermissionType() {
        assertThat(ZLcAcePermissionDefinitions.BUTTON_PERMISSION_TYPE).isEqualTo("BUTTON");
    }

    @Test
    void shouldHaveApiPermissionType() {
        assertThat(ZLcAcePermissionDefinitions.API_PERMISSION_TYPE).isEqualTo("API");
    }

    @Test
    void shouldHaveMenuPermissionType() {
        assertThat(ZLcAcePermissionDefinitions.MENU_PERMISSION_TYPE).isEqualTo("MENU");
    }

    @Test
    void shouldGetListReturnNonEmpty() {
        List<ZLcAcePermissionDefinitions.ZLcPermissionDefinition> list = ZLcAcePermissionDefinitions.getList();
        assertThat(list).isNotEmpty();
    }

    @Test
    void shouldGetMapReturnNonEmpty() {
        Map<String, ZLcAcePermissionDefinitions.ZLcPermissionDefinition> map = ZLcAcePermissionDefinitions.getMap();
        assertThat(map).isNotEmpty();
    }

    @Test
    void shouldFilterReturnMatchingDefinitions() {
        List<String> permissions = Arrays.asList("z-lc:platform:app:create", "z-lc:platform:app:remove");
        Map<String, ZLcAcePermissionDefinitions.ZLcPermissionDefinition> filtered = ZLcAcePermissionDefinitions.filter(permissions);
        assertThat(filtered).hasSize(2);
        assertThat(filtered).containsKey("z-lc:platform:app:create");
        assertThat(filtered).containsKey("z-lc:platform:app:remove");
    }

    @Test
    void shouldFilterReturnEmptyForNullPermissions() {
        Map<String, ZLcAcePermissionDefinitions.ZLcPermissionDefinition> filtered = ZLcAcePermissionDefinitions.filter(null);
        assertThat(filtered).isEmpty();
    }

    @Test
    void shouldScanViaReflectionReturnNonEmpty() {
        List<ZLcAcePermissionDefinitions.ZLcPermissionDefinition> scanned = ZLcAcePermissionDefinitions.scanViaReflection();
        assertThat(scanned).isNotEmpty();
    }

    @Test
    void shouldPermissionDefinitionOfWork() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                "test:permission", "测试权限", "测试描述", "API", "测试错误信息");
        assertThat(def.getPermissionCode()).isEqualTo("test:permission");
        assertThat(def.getName()).isEqualTo("测试权限");
        assertThat(def.getPermissionDesc()).isEqualTo("测试描述");
        assertThat(def.getPermissionType()).isEqualTo("API");
        assertThat(def.getErrorMsg()).isEqualTo("测试错误信息");
    }

    @Test
    void shouldPermissionDefinitionValueOfPermissionCode() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:datasource:create", "应用新建数据源", null, "API", "错误信息");
        assertThat(def.valueOfPermissionCode("myApp")).isEqualTo("z-lc:app:myApp:datasource:create");
    }

    @Test
    void shouldPermissionDefinitionValueOfName() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                "z-lc:app:${appCode}:datasource:create", "${appCode}应用新建数据源", null, "API", "错误信息");
        assertThat(def.valueOfName("myApp")).isEqualTo("myApp应用新建数据源");
    }

    @Test
    void shouldPermissionDefinitionValueOfPermissionCodeReturnNullForNullCode() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                null, "测试权限", null, "API", null);
        assertThat(def.valueOfPermissionCode("myApp")).isNull();
    }

    @Test
    void shouldPermissionDefinitionValueOfNameReturnNullForNullName() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                "test:permission", null, null, "API", null);
        assertThat(def.valueOfName("myApp")).isNull();
    }

    @Test
    void shouldPermissionDefinitionToString() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = ZLcAcePermissionDefinitions.ZLcPermissionDefinition.of(
                "test:permission", "测试权限", null, "API", null);
        assertThat(def.toString()).contains("test:permission");
        assertThat(def.toString()).contains("测试权限");
        assertThat(def.toString()).contains("API");
    }

    @Test
    void shouldPermissionDefinitionSetAndGet() {
        ZLcAcePermissionDefinitions.ZLcPermissionDefinition def = new ZLcAcePermissionDefinitions.ZLcPermissionDefinition();
        def.setPermissionCode("new:code");
        def.setName("新权限");
        def.setPermissionDesc("新描述");
        def.setErrorMsg("新错误信息");
        def.setPermissionType("MENU");
        
        assertThat(def.getPermissionCode()).isEqualTo("new:code");
        assertThat(def.getName()).isEqualTo("新权限");
        assertThat(def.getPermissionDesc()).isEqualTo("新描述");
        assertThat(def.getErrorMsg()).isEqualTo("新错误信息");
        assertThat(def.getPermissionType()).isEqualTo("MENU");
    }
}
