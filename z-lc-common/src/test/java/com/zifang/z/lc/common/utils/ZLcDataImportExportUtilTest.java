package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcDataImportExportUtil 单元测试
 *
 * @author zifang
 */
class ZLcDataImportExportUtilTest {

    // ========== fieldDataFormat ==========

    @Test
    void fieldDataFormat_shouldReturnNull_WhenValueIsNull() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Text", null);
        assertThat(result).isNull();
    }

    @Test
    void fieldDataFormat_shouldReturnOriginalValue_WhenTextType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Text", "hello");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void fieldDataFormat_shouldParseBigDecimal_WhenNumberType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Number", "12345.678");
        assertThat(result).isInstanceOf(BigDecimal.class);
        assertThat(result.toString()).isEqualTo("12345.678");
    }

    @Test
    void fieldDataFormat_shouldHandleNegativeParentheses_WhenNumberType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Number", "(100)");
        assertThat(result).isEqualTo(new BigDecimal("-100"));
    }

    @Test
    void fieldDataFormat_shouldHandleComma_WhenNumberType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Number", "1,000.50");
        assertThat(result).isEqualTo(new BigDecimal("1000.50"));
    }

    @Test
    void fieldDataFormat_shouldHandlePercentage_WhenNumberType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Number", "50%");
        assertThat(result).isEqualTo(new BigDecimal("0.5000"));
    }

    @Test
    void fieldDataFormat_shouldThrowException_WhenInvalidNumber() {
        assertThatThrownBy(() -> ZLcDataImportExportUtil.fieldDataFormat("Number", "abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fieldDataFormat_shouldParseDate_WhenTimeType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Time", "2024-01-15 10:30:45");
        assertThat(result).isEqualTo("2024-01-15 10:30:45");
    }

    @Test
    void fieldDataFormat_shouldParseDateWithoutTime_WhenTimeType() {
        Object result = ZLcDataImportExportUtil.fieldDataFormat("Time", "2024-01-15");
        assertThat(result).isEqualTo("2024-01-15 00:00:00");
    }

    @Test
    void fieldDataFormat_shouldThrowException_WhenInvalidDate() {
        assertThatThrownBy(() -> ZLcDataImportExportUtil.fieldDataFormat("Time", "not-a-date"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== buildSimpleExcelHead ==========

    @Test
    void buildSimpleExcelHead_shouldReturnEmptyList_WhenNull() {
        List<List<String>> result = ZLcDataImportExportUtil.buildSimpleExcelHead(null);
        assertThat(result).isEmpty();
    }

    @Test
    void buildSimpleExcelHead_shouldReturnHeaders_WhenProvided() {
        List<List<String>> result = ZLcDataImportExportUtil.buildSimpleExcelHead(Arrays.asList("姓名", "年龄"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("姓名");
        assertThat(result.get(1)).containsExactly("年龄");
    }

    // ========== buildExcelHead ==========

    @Test
    void buildExcelHead_shouldReturnEmptyList_WhenFieldCodeListIsNull() {
        List<List<String>> result = ZLcDataImportExportUtil.buildExcelHead(null, null, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void buildExcelHead_shouldUseFieldDesc_WhenNoCustomTitle() {
        List<String> fieldCodes = Arrays.asList("name", "age");
        Map<String, String> fieldDescMap = new HashMap<>();
        fieldDescMap.put("name", "姓名");
        fieldDescMap.put("age", "年龄");

        List<List<String>> result = ZLcDataImportExportUtil.buildExcelHead(fieldCodes, fieldDescMap, null, null);
        assertThat(result.get(0)).containsExactly("姓名");
        assertThat(result.get(1)).containsExactly("年龄");
    }

    @Test
    void buildExcelHead_shouldUseCustomTitle_WhenProvided() {
        List<String> fieldCodes = Arrays.asList("name", "age");
        Map<String, String> fieldDescMap = new HashMap<>();
        fieldDescMap.put("name", "姓名");
        fieldDescMap.put("age", "年龄");
        Map<String, String> customTitles = new HashMap<>();
        customTitles.put("name", "用户姓名");

        List<List<String>> result = ZLcDataImportExportUtil.buildExcelHead(fieldCodes, fieldDescMap, null, customTitles);
        assertThat(result.get(0)).containsExactly("用户姓名");
        assertThat(result.get(1)).containsExactly("年龄");
    }

    @Test
    void buildExcelHead_shouldMarkRequiredFields() {
        List<String> fieldCodes = Arrays.asList("name", "email");
        Map<String, String> fieldDescMap = new HashMap<>();
        fieldDescMap.put("name", "姓名");
        fieldDescMap.put("email", "邮箱");
        Set<String> requiredSet = new HashSet<>();
        requiredSet.add("name");

        List<List<String>> result = ZLcDataImportExportUtil.buildExcelHead(fieldCodes, fieldDescMap, requiredSet, null);
        assertThat(result.get(0).get(0)).contains("必填");
        assertThat(result.get(1).get(0)).doesNotContain("必填");
    }
}
