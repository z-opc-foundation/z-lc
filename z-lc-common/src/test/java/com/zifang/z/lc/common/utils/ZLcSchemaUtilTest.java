package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSchemaUtil 单元测试
 *
 * @author zifang
 */
class ZLcSchemaUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcSchemaUtil> constructor = ZLcSchemaUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveTablePrefixes() {
        assertThat(ZLcSchemaUtil.ODS_PREFIX).isEqualTo("ods_");
        assertThat(ZLcSchemaUtil.ADS_PREFIX).isEqualTo("ads_");
        assertThat(ZLcSchemaUtil.ADS_ORIGIN_PREFIX).isEqualTo("ads_origin_");
        assertThat(ZLcSchemaUtil.DATA_LIST_PREFIX).isEqualTo("data_list@@@");
        assertThat(ZLcSchemaUtil.DATA_DETAIL_PREFIX).isEqualTo("data_detail@@@");
    }

    @Test
    void shouldHaveSystemFieldNames() {
        assertThat(ZLcSchemaUtil.FIELD_ID).isEqualTo("id");
        assertThat(ZLcSchemaUtil.FIELD_GMT_CREATE).isEqualTo("gmt_create");
        assertThat(ZLcSchemaUtil.FIELD_GMT_MODIFIED).isEqualTo("gmt_modified");
        assertThat(ZLcSchemaUtil.FIELD_DELETED).isEqualTo("deleted");
        assertThat(ZLcSchemaUtil.FIELD_CREATED_BY).isEqualTo("created_by");
        assertThat(ZLcSchemaUtil.FIELD_MODIFIED_BY).isEqualTo("modified_by");
    }

    @Test
    void shouldReturnFalseForNullSystemField() {
        assertThat(ZLcSchemaUtil.isSystemField(null)).isFalse();
    }

    @Test
    void shouldReturnFalseForEmptySystemField() {
        assertThat(ZLcSchemaUtil.isSystemField("")).isFalse();
    }

    @Test
    void shouldIdentifyIdField() {
        assertThat(ZLcSchemaUtil.isSystemField("id")).isTrue();
        assertThat(ZLcSchemaUtil.isSystemField("ID")).isTrue();
    }

    @Test
    void shouldIdentifyGmtCreateField() {
        assertThat(ZLcSchemaUtil.isSystemField("gmt_create")).isTrue();
    }

    @Test
    void shouldIdentifyDeletedField() {
        assertThat(ZLcSchemaUtil.isSystemField("deleted")).isTrue();
    }

    @Test
    void shouldReturnFalseForNonSystemField() {
        assertThat(ZLcSchemaUtil.isSystemField("name")).isFalse();
        assertThat(ZLcSchemaUtil.isSystemField("email")).isFalse();
    }

    @Test
    void shouldIdentifyOdsTable() {
        assertThat(ZLcSchemaUtil.isOdsTable("ods_user_data")).isTrue();
    }

    @Test
    void shouldNotIdentifyNonOdsTable() {
        assertThat(ZLcSchemaUtil.isOdsTable("user_data")).isFalse();
        assertThat(ZLcSchemaUtil.isOdsTable(null)).isFalse();
    }

    @Test
    void shouldIdentifyAdsTable() {
        assertThat(ZLcSchemaUtil.isAdsTable("ads_user_stats")).isTrue();
        assertThat(ZLcSchemaUtil.isAdsTable("ads_origin_data")).isTrue();
    }

    @Test
    void shouldNotIdentifyNonAdsTable() {
        assertThat(ZLcSchemaUtil.isAdsTable("user_data")).isFalse();
        assertThat(ZLcSchemaUtil.isAdsTable(null)).isFalse();
    }
}