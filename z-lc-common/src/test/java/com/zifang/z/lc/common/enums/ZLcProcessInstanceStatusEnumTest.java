package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcProcessInstanceStatusEnum 单元测试
 *
 * @author zifang
 */
class ZLcProcessInstanceStatusEnumTest {

    @Test
    void of_shouldReturnFINISHED_When0() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(0);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.FINISHED);
    }

    @Test
    void of_shouldReturnUNFINISHED_When1() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(1);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.UNFINISHED);
    }

    @Test
    void of_shouldReturnSUSPEND_When2() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(2);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.SUSPEND);
    }

    @Test
    void of_shouldReturnUNKNOWN_When3() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(3);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.UNKNOWN);
    }

    @Test
    void of_shouldReturnUNKNOWN_WhenInvalid() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(99);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.UNKNOWN);
    }

    @Test
    void of_shouldReturnUNKNOWN_WhenNull() {
        ZLcProcessInstanceStatusEnum result = ZLcProcessInstanceStatusEnum.of(null);
        assertThat(result).isEqualTo(ZLcProcessInstanceStatusEnum.UNKNOWN);
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcProcessInstanceStatusEnum.FINISHED.getCode()).isEqualTo(0);
        assertThat(ZLcProcessInstanceStatusEnum.UNFINISHED.getCode()).isEqualTo(1);
        assertThat(ZLcProcessInstanceStatusEnum.SUSPEND.getCode()).isEqualTo(2);
        assertThat(ZLcProcessInstanceStatusEnum.UNKNOWN.getCode()).isEqualTo(3);
    }

    @Test
    void getDescription_shouldReturnCorrectValue() {
        assertThat(ZLcProcessInstanceStatusEnum.FINISHED.getDescription()).isEqualTo("已结束");
        assertThat(ZLcProcessInstanceStatusEnum.UNFINISHED.getDescription()).isEqualTo("运行中");
        assertThat(ZLcProcessInstanceStatusEnum.SUSPEND.getDescription()).isEqualTo("暂停");
        assertThat(ZLcProcessInstanceStatusEnum.UNKNOWN.getDescription()).isEqualTo("未知");
    }
}
