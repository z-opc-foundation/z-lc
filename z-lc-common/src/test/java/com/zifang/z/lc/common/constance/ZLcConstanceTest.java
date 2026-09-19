package com.zifang.z.lc.common.constance;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcConstance 单元测试
 *
 * @author zifang
 */
class ZLcConstanceTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcConstance> constructor = ZLcConstance.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveProductCode() {
        assertThat(ZLcConstance.PRODUCT_CODE).isEqualTo("z-lc");
    }

    @Test
    void shouldHaveAppCodeVariableName() {
        assertThat(ZLcConstance.APP_CODE).isEqualTo("appCode");
    }

    @Test
    void shouldHaveModelCodeVariableName() {
        assertThat(ZLcConstance.MODEL_CODE).isEqualTo("modelCode");
    }

    @Test
    void shouldHaveMonopolizePrefix() {
        assertThat(ZLcConstance.MONOPOLIZE_PREFIX).isEqualTo("Monopolize:");
    }

    @Test
    void shouldHaveMonopolizeFlag() {
        assertThat(ZLcConstance.MONOPOLIZE_FLAG).isEqualTo("MONOPOLIZEFLAG");
    }

    @Test
    void shouldHaveNrofPrefix() {
        assertThat(ZLcConstance.NROF_PREFIX).isEqualTo("nrOf");
    }

    @Test
    void shouldHaveElectronicSignFlag() {
        assertThat(ZLcConstance.ELECTRONIC_SIGN_FLAG).isEqualTo("ELECTRONIC_SIGN_FLAG");
    }

    @Test
    void shouldHaveTaskElectronicSignImageRelationId() {
        assertThat(ZLcConstance.TASK_ELECTRONIC_SIGN_IMAGE_RELATION_ID)
                .isEqualTo("T_ELECTRONIC_SIGN_IMAGE_RELATION_ID");
    }

    @Test
    void shouldHaveTopic() {
        assertThat(ZLcConstance.TOPIC).isEqualTo("ace:%s");
    }

    @Test
    void shouldHaveEventTopic() {
        assertThat(ZLcConstance.EVENT_TOPIC).isEqualTo("process-event:%s");
    }

    @Test
    void shouldHaveTaskEventTopic() {
        assertThat(ZLcConstance.TASK_EVENT_TOPIC).isEqualTo("task-event:%s");
    }

    @Test
    void shouldHaveProcessEventTypes() {
        assertThat(ZLcConstance.PROCESS_STARTED).isEqualTo("processStarted");
        assertThat(ZLcConstance.PROCESS_COMPLETED).isEqualTo("processCompleted");
        assertThat(ZLcConstance.PROCESS_DELETED).isEqualTo("processDeleted");
        assertThat(ZLcConstance.PROCESS_SUSPEND).isEqualTo("processSuspend");
    }

    @Test
    void shouldHaveTaskEventTypes() {
        assertThat(ZLcConstance.TASK_COMPLETED).isEqualTo("taskCompleted");
        assertThat(ZLcConstance.TASK_REFUSED).isEqualTo("taskRefused");
        assertThat(ZLcConstance.TASK_CREATED).isEqualTo("taskCreated");
        assertThat(ZLcConstance.TASK_AGREE).isEqualTo("taskAgree");
    }

    @Test
    void shouldHaveRejectModels() {
        assertThat(ZLcConstance.REJECT_MODEL_STEP).isEqualTo(1);
        assertThat(ZLcConstance.REJECT_MODEL_JUMP).isEqualTo(2);
    }

    @Test
    void shouldHaveAutoApprovalUserId() {
        assertThat(ZLcConstance.AUTO_APPROVAL_USER_ID).isEqualTo("-1");
    }

    @Test
    void shouldHaveAutoApprovalUserName() {
        assertThat(ZLcConstance.AUTO_APPROVAL_USER_NAME).isEqualTo("无审批人自动审批");
    }

    @Test
    void shouldHaveDataVersionCookieExpiry() {
        assertThat(ZLcConstance.DATA_VERSION_COOKIE_EXPIRY).isEqualTo(86400);
    }

    @Test
    void shouldHaveXLCOriginHeader() {
        assertThat(ZLcConstance.X_LC_ORIGIN).isEqualTo("X-LC-ORIGIN");
    }

    @Test
    void shouldHavePagePrefixes() {
        assertThat(ZLcConstance.EXTERNAL_LIST_PAGE_CODE_PREFIX).isEqualTo("external_list@@@");
        assertThat(ZLcConstance.EXTERNAL_DETAIL_PAGE_CODE_PREFIX).isEqualTo("external_detail@@@");
        assertThat(ZLcConstance.DATA_LIST_PAGE_CODE_PREFIX).isEqualTo("data_list@@@");
        assertThat(ZLcConstance.DATA_ADS_LIST_PAGE_CODE_PREFIX).isEqualTo("data_list@@@ads_");
        assertThat(ZLcConstance.DATA_DETAIL_PAGE_CODE_PREFIX).isEqualTo("data_detail@@@");
    }

    @Test
    void shouldHaveHdAndAdsPrefixes() {
        assertThat(ZLcConstance.HDOS_MODEL_PREFIX).isEqualTo("hdos@");
        assertThat(ZLcConstance.HDOS_ADS_MODEL_PREFIX).isEqualTo("hdos@ads_origin_");
        assertThat(ZLcConstance.ODS_PREFIX).isEqualTo("ods_");
        assertThat(ZLcConstance.ADS_PREFIX).isEqualTo("ads_");
        assertThat(ZLcConstance.ADS_ORIGIN_PREFIX).isEqualTo("ads_origin_");
        assertThat(ZLcConstance.LABEL_PREFIX).isEqualTo("label_");
    }

    @Test
    void shouldHaveDictCodes() {
        assertThat(ZLcConstance.BIG_ORDER_TYPE_DICT_CODE).isEqualTo("todo_order_type");
        assertThat(ZLcConstance.IS_URGENT_DICT_CODE).isEqualTo("isUrgent");
        assertThat(ZLcConstance.BIZ_DOMAIN_DICT_CODE).isEqualTo("biz_domain");
    }

    @Test
    void shouldHaveCacheKeyPrefixes() {
        assertThat(ZLcConstance.UPDATE_TABLE_FLAG_CACHE).isEqualTo("UPDATE_TABLE_FLAG_CACHE:");
        assertThat(ZLcConstance.IDENTITY_LINK_CACHE).isEqualTo("identity_Link_cache:");
    }

    @Test
    void shouldHaveSystemFieldTypes() {
        assertThat(ZLcConstance.MODEL_SYSTEM_FIELD).isEqualTo("model_system_field");
        assertThat(ZLcConstance.ODS_NORMAL_SYSTEM_FIELD).isEqualTo("ods_normal_system_field");
        assertThat(ZLcConstance.BIZ_NORMAL_SYSTEM_FIELD).isEqualTo("biz_normal_system_field");
        assertThat(ZLcConstance.ADS_NORMAL_SYSTEM_FIELD).isEqualTo("ads_normal_system_field");
    }

    @Test
    void shouldHaveAppExtendKeys() {
        assertThat(ZLcConstance.APP_EXTEND_URGE_FREQUENCY_KEY).isEqualTo("urge_frequency");
        assertThat(ZLcConstance.APP_EXTEND_URGE_NUM_KEY).isEqualTo("urge_num");
        assertThat(ZLcConstance.BUSINESS_CONTEXT_UUID).isEqualTo("business_context_uuid");
    }
}