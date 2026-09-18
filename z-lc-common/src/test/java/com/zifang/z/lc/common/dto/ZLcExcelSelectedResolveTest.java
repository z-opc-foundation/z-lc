package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExcelSelectedResolve 单元测试
 *
 * @author zifang
 */
class ZLcExcelSelectedResolveTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        assertThat(resolve).isNotNull();
    }

    @Test
    void shouldSetAndGetFirstRow() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        resolve.setFirstRow(1);
        assertThat(resolve.getFirstRow()).isEqualTo(1);
    }

    @Test
    void shouldSetAndGetLastRow() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        resolve.setLastRow(100);
        assertThat(resolve.getLastRow()).isEqualTo(100);
    }

    @Test
    void shouldSetAndGetSource() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        String[] source = {"Option1", "Option2", "Option3"};
        resolve.setSource(source);
        assertThat(resolve.getSource()).containsExactly("Option1", "Option2", "Option3");
    }

    @Test
    void shouldHaveDefaultValues() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        // int primitive fields default to 0
        assertThat(resolve.getFirstRow()).isEqualTo(0);
        assertThat(resolve.getLastRow()).isEqualTo(0);
        assertThat(resolve.getSource()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        assertThat(resolve).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHandleEmptySource() {
        ZLcExcelSelectedResolve resolve = new ZLcExcelSelectedResolve();
        resolve.setSource(new String[]{});
        assertThat(resolve.getSource()).isEmpty();
    }
}
