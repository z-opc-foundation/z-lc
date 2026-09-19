package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSpecialModelFieldDefinition 单元测试
 *
 * @author zifang
 */
class ZLcSpecialModelFieldDefinitionTest {

    @Test
    void shouldCreateWithConstructor() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        assertThat(def).isNotNull();
    }

    @Test
    void shouldGetFieldCode() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        assertThat(def.getFieldCode()).isEqualTo("code");
    }

    @Test
    void shouldGetColumnName() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        assertThat(def.getColumnName()).isEqualTo("column_name");
    }

    @Test
    void shouldGetPosition() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        assertThat(def.getPosition()).isEqualTo("$.code");
    }

    @Test
    void shouldImplementSerializable() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        assertThat(def).isInstanceOf(Serializable.class);
    }

    @Test
    void shouldHaveStaticIdDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.ID).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.ID.getFieldCode()).isEqualTo("id");
        assertThat(ZLcSpecialModelFieldDefinition.ID.getColumnName()).isEqualTo("id");
        assertThat(ZLcSpecialModelFieldDefinition.ID.getPosition()).isEqualTo("$.id");
    }

    @Test
    void shouldHaveStaticCreateTimeDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_TIME).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_TIME.getFieldCode()).isEqualTo("createTime");
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_TIME.getColumnName()).isEqualTo("createTime");
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_TIME.getPosition()).isEqualTo("$.createTime");
    }

    @Test
    void shouldHaveStaticUpdateTimeDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_TIME).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_TIME.getFieldCode()).isEqualTo("updateTime");
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_TIME.getColumnName()).isEqualTo("update_time");
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_TIME.getPosition()).isEqualTo("$.updateTime");
    }

    @Test
    void shouldHaveStaticCreateByDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_BY).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_BY.getFieldCode()).isEqualTo("createBy");
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_BY.getColumnName()).isEqualTo("CREATE_BY");
        assertThat(ZLcSpecialModelFieldDefinition.CREATE_BY.getPosition()).isEqualTo("$.createBy");
    }

    @Test
    void shouldHaveStaticUpdateByDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_BY).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_BY.getFieldCode()).isEqualTo("updateBy");
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_BY.getColumnName()).isEqualTo("update_by");
        assertThat(ZLcSpecialModelFieldDefinition.UPDATE_BY.getPosition()).isEqualTo("$.updateBy");
    }

    @Test
    void shouldHaveStaticIsDeletedDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.IS_DELETED).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.IS_DELETED.getFieldCode()).isEqualTo("isDeleted");
        assertThat(ZLcSpecialModelFieldDefinition.IS_DELETED.getColumnName()).isEqualTo("is_deleted");
        assertThat(ZLcSpecialModelFieldDefinition.IS_DELETED.getPosition()).isEqualTo("$.isDeleted");
    }

    @Test
    void shouldHaveStaticTenantIdDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.TENANT_ID).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.TENANT_ID.getFieldCode()).isEqualTo("tenantId");
        assertThat(ZLcSpecialModelFieldDefinition.TENANT_ID.getColumnName()).isEqualTo("tenant_id");
        assertThat(ZLcSpecialModelFieldDefinition.TENANT_ID.getPosition()).isEqualTo("$.tenantId");
    }

    @Test
    void shouldHaveStaticAppCodeDefinition() {
        assertThat(ZLcSpecialModelFieldDefinition.APP_CODE).isNotNull();
        assertThat(ZLcSpecialModelFieldDefinition.APP_CODE.getFieldCode()).isEqualTo("appCode");
        assertThat(ZLcSpecialModelFieldDefinition.APP_CODE.getColumnName()).isEqualTo("app_code");
        assertThat(ZLcSpecialModelFieldDefinition.APP_CODE.getPosition()).isEqualTo("$.appCode");
    }

    @Test
    void shouldHaveAllSpecialModelFieldDefinitions() {
        assertThat(ZLcSpecialModelFieldDefinition.SPECIAL_MODEL_FIELD_DEFINITIONS).hasSize(8);
    }

    @Test
    void shouldGetByFieldCode() {
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("id")).isEqualTo(ZLcSpecialModelFieldDefinition.ID);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("createTime")).isEqualTo(ZLcSpecialModelFieldDefinition.CREATE_TIME);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("updateTime")).isEqualTo(ZLcSpecialModelFieldDefinition.UPDATE_TIME);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("createBy")).isEqualTo(ZLcSpecialModelFieldDefinition.CREATE_BY);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("updateBy")).isEqualTo(ZLcSpecialModelFieldDefinition.UPDATE_BY);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("isDeleted")).isEqualTo(ZLcSpecialModelFieldDefinition.IS_DELETED);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("tenantId")).isEqualTo(ZLcSpecialModelFieldDefinition.TENANT_ID);
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("appCode")).isEqualTo(ZLcSpecialModelFieldDefinition.APP_CODE);
    }

    @Test
    void shouldReturnNullForUnknownFieldCode() {
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFieldCode() {
        assertThat(ZLcSpecialModelFieldDefinition.getByFieldCode(null)).isNull();
    }

    @Test
    void shouldToString() {
        ZLcSpecialModelFieldDefinition def = new ZLcSpecialModelFieldDefinition("code", "column_name", "$.code");
        String str = def.toString();
        assertThat(str).contains("code");
        assertThat(str).contains("column_name");
        assertThat(str).contains("$.code");
    }
}
