package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcInitPackageTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcInitPackageTypeEnumTest {

    @Test
    void getByCode_shouldReturnMODEL_WhenModel() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode("model");
        assertThat(result).isEqualTo(ZLcInitPackageTypeEnum.MODEL);
    }

    @Test
    void getByCode_shouldReturnDICT_WhenDict() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode("dict");
        assertThat(result).isEqualTo(ZLcInitPackageTypeEnum.DICT);
    }

    @Test
    void getByCode_shouldReturnSERVICE_WhenService() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode("service");
        assertThat(result).isEqualTo(ZLcInitPackageTypeEnum.SERVICE);
    }

    @Test
    void getByCode_shouldReturnWORKFLOW_WhenWorkflow() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode("workflow");
        assertThat(result).isEqualTo(ZLcInitPackageTypeEnum.WORKFLOW);
    }

    @Test
    void getByCode_shouldReturnNull_WhenUnknown() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode("unknown");
        assertThat(result).isNull();
    }

    @Test
    void getByCode_shouldReturnNull_WhenNull() {
        ZLcInitPackageTypeEnum result = ZLcInitPackageTypeEnum.getByCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageTypeEnum.MODEL.getCode()).isEqualTo("model");
        assertThat(ZLcInitPackageTypeEnum.DICT.getCode()).isEqualTo("dict");
        assertThat(ZLcInitPackageTypeEnum.SERVICE.getCode()).isEqualTo("service");
    }

    @Test
    void getName_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageTypeEnum.MODEL.getName()).isEqualTo("模型");
        assertThat(ZLcInitPackageTypeEnum.DICT.getName()).isEqualTo("字典");
        assertThat(ZLcInitPackageTypeEnum.SERVICE.getName()).isEqualTo("服务");
    }

    @Test
    void getGroupCode_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageTypeEnum.MODEL.getGroupCode()).isEqualTo("model");
        assertThat(ZLcInitPackageTypeEnum.DICT.getGroupCode()).isEqualTo("dict");
        assertThat(ZLcInitPackageTypeEnum.SERVICE.getGroupCode()).isEqualTo("service");
    }

    @Test
    void getIndex_shouldReturnPositiveInt() {
        // getIndex returns int, just verify it's positive
        assertThat(ZLcInitPackageTypeEnum.MODEL.getIndex()).isGreaterThan(0);
        assertThat(ZLcInitPackageTypeEnum.DICT.getIndex()).isGreaterThan(0);
        assertThat(ZLcInitPackageTypeEnum.SERVICE.getIndex()).isGreaterThan(0);
    }

    @Test
    void getByGroupCode_shouldReturnAllModelTypes() {
        List<ZLcInitPackageTypeEnum> result = ZLcInitPackageTypeEnum.getByGroupCode("model");
        assertThat(result).isNotEmpty();
        for (ZLcInitPackageTypeEnum e : result) {
            assertThat(e.getGroupCode()).isEqualTo("model");
        }
    }

    @Test
    void getSortedGroupCodeList_shouldReturnDistinctSortedGroups() {
        List<String> result = ZLcInitPackageTypeEnum.getSortedGroupCodeList();
        assertThat(result).isNotEmpty();
        // 验证没有重复
        assertThat((long) result.size()).isEqualTo(result.stream().distinct().count());
    }
}
