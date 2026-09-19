package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceEnum 单元测试
 *
 * @author zifang
 */
class DataSourceEnumTest {

    @Test
    void shouldHaveCorrectTypeForMySQL() {
        assertThat(DataSourceEnum.DATA_SOURCE_MYSQL.getType()).isEqualTo("mysql");
    }

    @Test
    void shouldHaveCorrectNameForMySQL() {
        assertThat(DataSourceEnum.DATA_SOURCE_MYSQL.getName()).isEqualTo("mysql");
    }

    @Test
    void shouldHaveCorrectTypeForDoris() {
        assertThat(DataSourceEnum.DATA_SOURCE_DORIS.getType()).isEqualTo("doris");
    }

    @Test
    void shouldHaveCorrectNameForDoris() {
        assertThat(DataSourceEnum.DATA_SOURCE_DORIS.getName()).isEqualTo("doris");
    }

    @Test
    void shouldHaveCorrectTypeForDM() {
        assertThat(DataSourceEnum.DATA_SOURCE_DM.getType()).isEqualTo("dm");
    }

    @Test
    void shouldHaveCorrectNameForDM() {
        assertThat(DataSourceEnum.DATA_SOURCE_DM.getName()).isEqualTo("达梦数据库");
    }

    @Test
    void shouldHaveCorrectTypeForStarRocks() {
        assertThat(DataSourceEnum.DATA_SOURCE_STARROCKS.getType()).isEqualTo("sr");
    }

    @Test
    void shouldHaveCorrectNameForStarRocks() {
        assertThat(DataSourceEnum.DATA_SOURCE_STARROCKS.getName()).isEqualTo("starrocks");
    }

    @Test
    void shouldHaveCorrectTypeForKingBase() {
        assertThat(DataSourceEnum.DATA_SOURCE_KINGBASE.getType()).isEqualTo("kingbase");
    }

    @Test
    void shouldHaveCorrectNameForKingBase() {
        assertThat(DataSourceEnum.DATA_SOURCE_KINGBASE.getName()).isEqualTo("人大金仓");
    }

    @Test
    void shouldReturnEnumByTypeForMySQL() {
        assertThat(DataSourceEnum.getEnumByType("mysql")).isEqualTo(DataSourceEnum.DATA_SOURCE_MYSQL);
    }

    @Test
    void shouldReturnEnumByTypeForDoris() {
        assertThat(DataSourceEnum.getEnumByType("doris")).isEqualTo(DataSourceEnum.DATA_SOURCE_DORIS);
    }

    @Test
    void shouldReturnEnumByTypeForDM() {
        assertThat(DataSourceEnum.getEnumByType("dm")).isEqualTo(DataSourceEnum.DATA_SOURCE_DM);
    }

    @Test
    void shouldReturnEnumByTypeForStarRocks() {
        assertThat(DataSourceEnum.getEnumByType("sr")).isEqualTo(DataSourceEnum.DATA_SOURCE_STARROCKS);
    }

    @Test
    void shouldReturnEnumByTypeForKingBase() {
        assertThat(DataSourceEnum.getEnumByType("kingbase")).isEqualTo(DataSourceEnum.DATA_SOURCE_KINGBASE);
    }

    @Test
    void shouldReturnNullForUnknownType() {
        assertThat(DataSourceEnum.getEnumByType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullType() {
        assertThat(DataSourceEnum.getEnumByType(null)).isNull();
    }

    @Test
    void shouldHaveFiveValues() {
        assertThat(DataSourceEnum.values()).hasSize(5);
    }
}