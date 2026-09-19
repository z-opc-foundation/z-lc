package com.zifang.z.lc.common.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * ZLcEnums 常量单元测试
 *
 * @author zifang
 */
class ZLcEnumsTest {

    @Test
    void outerClassShouldHavePrivateConstructor() throws Exception {
        Constructor<ZLcEnums> ctor = ZLcEnums.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        // ZLcEnums has an empty private constructor; instantiation is allowed but discouraged
        Object instance = ctor.newInstance();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(ZLcEnums.class);
    }

    // --- MenuEnableFlag ---

    @Test
    void menuEnableFlagShouldHaveConstants() {
        assertThat(ZLcEnums.MenuEnableFlag.ENABLE).isEqualTo(1);
        assertThat(ZLcEnums.MenuEnableFlag.DISABLE).isEqualTo(0);
    }

    @Test
    void menuEnableFlagShouldBeUninstantiable() throws Exception {
        verifyUninstantiable(ZLcEnums.MenuEnableFlag.class);
    }

    // --- ModelTypeFlag ---

    @Test
    void modelTypeFlagShouldHaveConstants() {
        assertThat(ZLcEnums.ModelTypeFlag.PHYSICAL_TYPE).isEqualTo(1);
        assertThat(ZLcEnums.ModelTypeFlag.VIRTUAL_TYPE).isEqualTo(0);
    }

    @Test
    void modelTypeFlagShouldBeUninstantiable() throws Exception {
        verifyUninstantiable(ZLcEnums.ModelTypeFlag.class);
    }

    // --- ModelFieldType ---

    @Test
    void modelFieldTypeShouldHaveConstants() {
        assertThat(ZLcEnums.ModelFieldType.TIME).isEqualTo("Time");
        assertThat(ZLcEnums.ModelFieldType.NUMBER).isEqualTo("Number");
        assertThat(ZLcEnums.ModelFieldType.TEXT).isEqualTo("Text");
        assertThat(ZLcEnums.ModelFieldType.OBJECT).isEqualTo("Object");
        assertThat(ZLcEnums.ModelFieldType.ARRAY).isEqualTo("Array");
    }

    @Test
    void sysFieldTypesShouldContainExpected() {
        assertThat(ZLcEnums.ModelFieldType.SYS_FIELD_TYPES).contains("Time", "Number", "Text");
        assertThat(ZLcEnums.ModelFieldType.SYS_FIELD_TYPES).hasSize(3);
    }

    @Test
    void sysFieldTypesShouldBeUnmodifiable() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ZLcEnums.ModelFieldType.SYS_FIELD_TYPES.add("New"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modelFieldTypeShouldBeUninstantiable() throws Exception {
        verifyUninstantiable(ZLcEnums.ModelFieldType.class);
    }

    // --- FieldRelateType ---

    @Test
    void fieldRelateTypeShouldHaveConstants() {
        assertThat(ZLcEnums.FieldRelateType.ONE_TO_ONE).isEqualTo("oneToOne");
        assertThat(ZLcEnums.FieldRelateType.ONE_TO_MANY).isEqualTo("oneToMany");
    }

    // --- HttpRequestType ---

    @Test
    void httpRequestTypeShouldHaveConstants() {
        assertThat(ZLcEnums.HttpRequestType.GET).isEqualTo("get");
        assertThat(ZLcEnums.HttpRequestType.POST).isEqualTo("post");
    }

    // --- PageTemplateType ---

    @Test
    void pageTemplateTypeShouldHaveConstants() {
        assertThat(ZLcEnums.PageTemplateType.FORM).isEqualTo("form");
        assertThat(ZLcEnums.PageTemplateType.LIST).isEqualTo("list");
        assertThat(ZLcEnums.PageTemplateType.APP_FORM).isEqualTo("app_form");
        assertThat(ZLcEnums.PageTemplateType.FORM_V2).isEqualTo("formV2");
        assertThat(ZLcEnums.PageTemplateType.LIST_V2).isEqualTo("listV2");
        assertThat(ZLcEnums.PageTemplateType.COMMON).isEqualTo("common");
        assertThat(ZLcEnums.PageTemplateType.PRINT).isEqualTo("print");
    }

    @Test
    void defaultPageTypesShouldContainExpected() {
        assertThat(ZLcEnums.PageTemplateType.DEFAULT_PAGE_TYPES).isNotNull();
        assertThat(ZLcEnums.PageTemplateType.DEFAULT_PAGE_TYPES).contains("form", "list");
    }

    @Test
    void defaultPageTypesShouldBeUnmodifiable() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ZLcEnums.PageTemplateType.DEFAULT_PAGE_TYPES.add("New"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- AppType ---

    @Test
    void appTypeShouldHaveConstants() {
        assertThat(ZLcEnums.AppType.LC).isEqualTo("LC");
        assertThat(ZLcEnums.AppType.NC).isEqualTo("NC");
    }

    @Test
    void appTypeContainsShouldReturnTrueForKnown() {
        assertThat(ZLcEnums.AppType.contains("LC")).isTrue();
        assertThat(ZLcEnums.AppType.contains("NC")).isTrue();
    }

    @Test
    void appTypeContainsShouldReturnFalseForUnknown() {
        assertThat(ZLcEnums.AppType.contains("UNKNOWN")).isFalse();
        assertThat(ZLcEnums.AppType.contains("")).isFalse();
        assertThat(ZLcEnums.AppType.contains(null)).isFalse();
    }

    // --- AppComponentType ---

    @Test
    void appComponentTypeShouldHaveConstants() {
        assertThat(ZLcEnums.AppComponentType.MODEL).isEqualTo("model");
        assertThat(ZLcEnums.AppComponentType.PAGE).isEqualTo("page");
        assertThat(ZLcEnums.AppComponentType.DICT).isEqualTo("dict");
        assertThat(ZLcEnums.AppComponentType.WORKFLOW).isEqualTo("workflow");
        assertThat(ZLcEnums.AppComponentType.SERVICE).isEqualTo("service");
    }

    // --- IdentityLinkType ---

    @Test
    void identityLinkTypeShouldHaveConstants() {
        assertThat(ZLcEnums.IdentityLinkType.ASSIGNEE).isEqualTo("assignee");
        assertThat(ZLcEnums.IdentityLinkType.CANDIDATE).isEqualTo("candidate");
        assertThat(ZLcEnums.IdentityLinkType.OWNER).isEqualTo("owner");
        assertThat(ZLcEnums.IdentityLinkType.STARTER).isEqualTo("starter");
        assertThat(ZLcEnums.IdentityLinkType.PARTICIPANT).isEqualTo("participant");
    }

    // --- Helpers ---

    private static void verifyUninstantiable(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        assertThat(constructors.length >= 1).isTrue();
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                assertThat(Modifier.isPrivate(c.getModifiers()))
                        .as(clazz.getSimpleName() + " default constructor should be private")
                        .isTrue();
                return;
            }
        }
        fail(clazz.getSimpleName() + " should have a no-arg constructor");
    }
}