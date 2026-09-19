package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcApprovalOpinionUtil 单元测试
 *
 * @author zifang
 */
class ZLcApprovalOpinionUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcApprovalOpinionUtil> constructor = ZLcApprovalOpinionUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(ZLcApprovalOpinionUtil.extractOpinion(null)).isNull();
    }

    @Test
    void shouldReturnNullForEmptyInput() {
        assertThat(ZLcApprovalOpinionUtil.extractOpinion("")).isNull();
    }

    @Test
    void shouldExtractApprovalOpinion() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("审批意见：同意通过");
        assertThat(result).isEqualTo("同意通过");
    }

    @Test
    void shouldExtractTurnOpinion() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("改派流程,转给张三处理");
        assertThat(result).isEqualTo("转给张三处理");
    }

    @Test
    void shouldExtractDelegateOpinion() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("委派流程,请李四处理");
        assertThat(result).isEqualTo("请李四处理");
    }

    @Test
    void shouldExtractAddMultiOpinion() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("增加会签人：张三,李四");
        assertThat(result).isEqualTo("增加会签人：张三,李四");
    }

    @Test
    void shouldExtractAddMultiV2Opinion() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("增加加签人：王五");
        assertThat(result).isEqualTo("增加加签人：王五");
    }

    @Test
    void shouldReturnNullForUnknownPrefix() {
        String result = ZLcApprovalOpinionUtil.extractOpinion("其他操作");
        assertThat(result).isNull();
    }
}