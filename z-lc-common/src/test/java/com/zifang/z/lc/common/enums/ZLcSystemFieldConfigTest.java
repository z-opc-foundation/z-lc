package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSystemFieldConfig 单元测试
 *
 * @author zifang
 */
class ZLcSystemFieldConfigTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcSystemFieldConfig.values()).hasSize(12);
    }

    @Test
    void shouldHaveGmtCreateConfig() {
        assertThat(ZLcSystemFieldConfig.GMT_CREATE.getFieldCode()).isEqualTo("gmt_create");
        assertThat(ZLcSystemFieldConfig.GMT_CREATE.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.GMT_CREATE.getDefaultValueConfig()).contains("CURRENT_TIMM");
    }

    @Test
    void shouldHaveGmtModifyConfig() {
        assertThat(ZLcSystemFieldConfig.GMT_MODIFY.getFieldCode()).isEqualTo("gmt_modify");
        assertThat(ZLcSystemFieldConfig.GMT_MODIFY.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.GMT_MODIFY.getDefaultValueConfig()).contains("CURRENT_TIMM");
    }

    @Test
    void shouldHaveCreateByConfig() {
        assertThat(ZLcSystemFieldConfig.CREATE_BY.getFieldCode()).isEqualTo("create_by");
        assertThat(ZLcSystemFieldConfig.CREATE_BY.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.CREATE_BY.getDefaultValueConfig()).contains("OPERATOR");
    }

    @Test
    void shouldHaveModifyByConfig() {
        assertThat(ZLcSystemFieldConfig.MODIFY_BY.getFieldCode()).isEqualTo("modify_by");
        assertThat(ZLcSystemFieldConfig.MODIFY_BY.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.MODIFY_BY.getDefaultValueConfig()).contains("OPERATOR");
    }

    @Test
    void shouldHaveCreateOrgIdConfig() {
        assertThat(ZLcSystemFieldConfig.CREATE_ORG_ID.getFieldCode()).isEqualTo("create_org_id");
        assertThat(ZLcSystemFieldConfig.CREATE_ORG_ID.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.CREATE_ORG_ID.getDefaultValueConfig()).contains("ORG_ID");
    }

    @Test
    void shouldHaveOrgIdConfig() {
        assertThat(ZLcSystemFieldConfig.ORG_ID.getFieldCode()).isEqualTo("org_id");
        assertThat(ZLcSystemFieldConfig.ORG_ID.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.ORG_ID.getDefaultValueConfig()).contains("ORG_ID");
    }

    @Test
    void shouldHaveModifyOrgIdConfig() {
        assertThat(ZLcSystemFieldConfig.MODIFY_ORG_ID.getFieldCode()).isEqualTo("modify_org_id");
        assertThat(ZLcSystemFieldConfig.MODIFY_ORG_ID.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.MODIFY_ORG_ID.getDefaultValueConfig()).contains("ORG_ID");
    }

    @Test
    void shouldHaveIsDeletedConfig() {
        assertThat(ZLcSystemFieldConfig.IS_DELETED.getFieldCode()).isEqualTo("is_deleted");
        assertThat(ZLcSystemFieldConfig.IS_DELETED.getStaticValueConfig()).contains("0");
        assertThat(ZLcSystemFieldConfig.IS_DELETED.getDefaultValueConfig()).isNull();
    }

    @Test
    void shouldHaveModelCodeConfig() {
        assertThat(ZLcSystemFieldConfig.MODEL_CODE.getFieldCode()).isEqualTo("model_code");
        assertThat(ZLcSystemFieldConfig.MODEL_CODE.getStaticValueConfig()).isNotNull();
        assertThat(ZLcSystemFieldConfig.MODEL_CODE.getDefaultValueConfig()).isNull();
    }

    @Test
    void shouldHaveAppCodeConfig() {
        assertThat(ZLcSystemFieldConfig.APP_CODE.getFieldCode()).isEqualTo("app_code");
        assertThat(ZLcSystemFieldConfig.APP_CODE.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.APP_CODE.getDefaultValueConfig()).contains("CURRENT_APP_CODE");
    }

    @Test
    void shouldHaveIdConfig() {
        assertThat(ZLcSystemFieldConfig.ID.getFieldCode()).isEqualTo("id");
        assertThat(ZLcSystemFieldConfig.ID.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.ID.getDefaultValueConfig()).isNull();
    }

    @Test
    void shouldHavePeriodIdConfig() {
        assertThat(ZLcSystemFieldConfig.PERIOD_ID.getFieldCode()).isEqualTo("period_id");
        assertThat(ZLcSystemFieldConfig.PERIOD_ID.getStaticValueConfig()).isNull();
        assertThat(ZLcSystemFieldConfig.PERIOD_ID.getDefaultValueConfig()).isNull();
    }

    @Test
    void shouldGetByCode() {
        assertThat(ZLcSystemFieldConfig.getByCode("gmt_create")).isEqualTo(ZLcSystemFieldConfig.GMT_CREATE);
        assertThat(ZLcSystemFieldConfig.getByCode("gmt_modify")).isEqualTo(ZLcSystemFieldConfig.GMT_MODIFY);
        assertThat(ZLcSystemFieldConfig.getByCode("create_by")).isEqualTo(ZLcSystemFieldConfig.CREATE_BY);
        assertThat(ZLcSystemFieldConfig.getByCode("modify_by")).isEqualTo(ZLcSystemFieldConfig.MODIFY_BY);
        assertThat(ZLcSystemFieldConfig.getByCode("create_org_id")).isEqualTo(ZLcSystemFieldConfig.CREATE_ORG_ID);
        assertThat(ZLcSystemFieldConfig.getByCode("org_id")).isEqualTo(ZLcSystemFieldConfig.ORG_ID);
        assertThat(ZLcSystemFieldConfig.getByCode("modify_org_id")).isEqualTo(ZLcSystemFieldConfig.MODIFY_ORG_ID);
        assertThat(ZLcSystemFieldConfig.getByCode("is_deleted")).isEqualTo(ZLcSystemFieldConfig.IS_DELETED);
        assertThat(ZLcSystemFieldConfig.getByCode("model_code")).isEqualTo(ZLcSystemFieldConfig.MODEL_CODE);
        assertThat(ZLcSystemFieldConfig.getByCode("app_code")).isEqualTo(ZLcSystemFieldConfig.APP_CODE);
        assertThat(ZLcSystemFieldConfig.getByCode("id")).isEqualTo(ZLcSystemFieldConfig.ID);
        assertThat(ZLcSystemFieldConfig.getByCode("period_id")).isEqualTo(ZLcSystemFieldConfig.PERIOD_ID);
    }

    @Test
    void shouldReturnNullForUnknownGetByCode() {
        assertThat(ZLcSystemFieldConfig.getByCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullGetByCode() {
        assertThat(ZLcSystemFieldConfig.getByCode(null)).isNull();
    }

    @Test
    void shouldIsSystemFieldReturnTrueForSystemFields() {
        assertThat(ZLcSystemFieldConfig.isSystemField("gmt_create")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("gmt_modify")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("create_by")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("modify_by")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("create_org_id")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("org_id")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("modify_org_id")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("is_deleted")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("model_code")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("app_code")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("id")).isTrue();
        assertThat(ZLcSystemFieldConfig.isSystemField("period_id")).isTrue();
    }

    @Test
    void shouldIsSystemFieldReturnFalseForNonSystemFields() {
        assertThat(ZLcSystemFieldConfig.isSystemField("unknown")).isFalse();
    }

    @Test
    void shouldIsSystemFieldReturnFalseForNull() {
        assertThat(ZLcSystemFieldConfig.isSystemField(null)).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcSystemFieldConfig.valueOf("GMT_CREATE")).isEqualTo(ZLcSystemFieldConfig.GMT_CREATE);
        assertThat(ZLcSystemFieldConfig.valueOf("GMT_MODIFY")).isEqualTo(ZLcSystemFieldConfig.GMT_MODIFY);
        assertThat(ZLcSystemFieldConfig.valueOf("CREATE_BY")).isEqualTo(ZLcSystemFieldConfig.CREATE_BY);
    }
}
