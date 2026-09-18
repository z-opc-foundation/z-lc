package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWorkflowTemplateReleaseTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcWorkflowTemplateReleaseTypeEnumTest {

    @Test
    void fromCode_shouldReturnTEST_When1() {
        ZLcWorkflowTemplateReleaseTypeEnum result = ZLcWorkflowTemplateReleaseTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcWorkflowTemplateReleaseTypeEnum.TEST);
    }

    @Test
    void fromCode_shouldReturnPROD_When2() {
        ZLcWorkflowTemplateReleaseTypeEnum result = ZLcWorkflowTemplateReleaseTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcWorkflowTemplateReleaseTypeEnum.PROD);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcWorkflowTemplateReleaseTypeEnum result = ZLcWorkflowTemplateReleaseTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcWorkflowTemplateReleaseTypeEnum result = ZLcWorkflowTemplateReleaseTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getReleaseType_shouldReturnCorrectValue() {
        assertThat(ZLcWorkflowTemplateReleaseTypeEnum.TEST.getReleaseType()).isEqualTo(1);
        assertThat(ZLcWorkflowTemplateReleaseTypeEnum.PROD.getReleaseType()).isEqualTo(2);
    }

    @Test
    void getReleaseTypeName_shouldReturnCorrectValue() {
        assertThat(ZLcWorkflowTemplateReleaseTypeEnum.TEST.getReleaseTypeName()).isEqualTo("测试");
        assertThat(ZLcWorkflowTemplateReleaseTypeEnum.PROD.getReleaseTypeName()).isEqualTo("正式");
    }
}
