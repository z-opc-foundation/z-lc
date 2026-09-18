package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPageUtil 单元测试
 *
 * @author zifang
 */
class ZLcPageUtilTest {

    @Test
    void toPageMap_shouldReturnPageMap_WhenValidInput() {
        List<String> records = Arrays.asList("a", "b", "c");
        Map<String, Object> result = ZLcPageUtil.toPageMap(records, 100L, 1L, 10L);
        assertThat(result).containsKey("records");
        assertThat(result).containsEntry("total", 100L);
        assertThat(result).containsEntry("current", 1L);
        assertThat(result).containsEntry("size", 10L);
    }

    @Test
    void toPageMap_shouldReturnEmptyRecords_WhenRecordsIsNull() {
        Map<String, Object> result = ZLcPageUtil.toPageMap(null, 0L, 1L, 10L);
        @SuppressWarnings("unchecked")
        List<String> records = (List<String>) result.get("records");
        assertThat(records).isEmpty();
    }

    @Test
    void toPageMap_shouldReturnRecords_WhenRecordsHasData() {
        List<String> records = Arrays.asList("a", "b");
        Map<String, Object> result = ZLcPageUtil.toPageMap(records, 2L, 1L, 10L);
        @SuppressWarnings("unchecked")
        List<String> resultRecords = (List<String>) result.get("records");
        assertThat(resultRecords).hasSize(2);
    }

    @Test
    void convertRecords_shouldReturnEmptyList_WhenInputIsNull() {
        List<?> result = ZLcPageUtil.convertRecords(null, m -> m);
        assertThat(result).isEmpty();
    }

    @Test
    void convertRecords_shouldConvertAllElements() {
        List<String> input = Arrays.asList("1", "2", "3");
        List<Integer> result = ZLcPageUtil.convertRecords(input, Integer::parseInt);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void convertRecords_shouldReturnEmptyList_WhenInputIsEmpty() {
        List<String> input = Collections.emptyList();
        List<Integer> result = ZLcPageUtil.convertRecords(input, Integer::parseInt);
        assertThat(result).isEmpty();
    }
}
