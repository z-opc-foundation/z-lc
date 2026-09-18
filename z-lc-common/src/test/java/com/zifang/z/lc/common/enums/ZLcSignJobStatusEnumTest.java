package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSignJobStatusEnum 单元测试
 *
 * @author zifang
 */
class ZLcSignJobStatusEnumTest {

    @Test
    void fromStatus_shouldReturnUNSIGN_WhenUnsign() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("UNSIGN");
        assertThat(result).isEqualTo(ZLcSignJobStatusEnum.UNSIGN);
    }

    @Test
    void fromStatus_shouldReturnFINISH_WhenFinish() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("FINISH");
        assertThat(result).isEqualTo(ZLcSignJobStatusEnum.FINISH);
    }

    @Test
    void fromStatus_shouldReturnEXPIRE_WhenExpire() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("EXPIRE");
        assertThat(result).isEqualTo(ZLcSignJobStatusEnum.EXPIRE);
    }

    @Test
    void fromStatus_shouldReturnREVOKE_WhenRevoke() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("REVOKE");
        assertThat(result).isEqualTo(ZLcSignJobStatusEnum.REVOKE);
    }

    @Test
    void fromStatus_shouldReturnREFUSE_WhenRefuse() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("REFUSE");
        assertThat(result).isEqualTo(ZLcSignJobStatusEnum.REFUSE);
    }

    @Test
    void fromStatus_shouldReturnNull_WhenUnknown() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus("UNKNOWN");
        assertThat(result).isNull();
    }

    @Test
    void fromStatus_shouldReturnNull_WhenNull() {
        ZLcSignJobStatusEnum result = ZLcSignJobStatusEnum.fromStatus(null);
        assertThat(result).isNull();
    }

    @Test
    void getStatus_shouldReturnCorrectValue() {
        assertThat(ZLcSignJobStatusEnum.UNSIGN.getStatus()).isEqualTo("UNSIGN");
        assertThat(ZLcSignJobStatusEnum.FINISH.getStatus()).isEqualTo("FINISH");
    }

    @Test
    void getStatusName_shouldReturnCorrectChineseName() {
        assertThat(ZLcSignJobStatusEnum.UNSIGN.getStatusName()).isEqualTo("待签");
        assertThat(ZLcSignJobStatusEnum.FINISH.getStatusName()).isEqualTo("已签");
        assertThat(ZLcSignJobStatusEnum.EXPIRE.getStatusName()).isEqualTo("过期");
    }
}
