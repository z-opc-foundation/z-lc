package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSqlUtil 单元测试
 *
 * @author zifang
 */
class ZLcSqlUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcSqlUtil> constructor = ZLcSqlUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldConvertCamelToUnderline() {
        assertThat(ZLcSqlUtil.camelToUnderline("userName")).isEqualTo("user_name");
    }

    @Test
    void shouldConvertMultipleUpperCases() {
        assertThat(ZLcSqlUtil.camelToUnderline("userFirstName")).isEqualTo("user_first_name");
    }

    @Test
    void shouldHandleSingleWord() {
        assertThat(ZLcSqlUtil.camelToUnderline("name")).isEqualTo("name");
    }

    @Test
    void shouldHandleEmptyString() {
        assertThat(ZLcSqlUtil.camelToUnderline("")).isEmpty();
    }

    @Test
    void shouldGenerateInsertSql() {
        SqlTestObj obj = new SqlTestObj();
        obj.userName = "test";
        obj.age = 25;

        String sql = ZLcSqlUtil.generateInsertSql("user", obj);
        assertThat(sql).contains("INSERT INTO user(");
        assertThat(sql).contains("user_name");
        assertThat(sql).contains("age");
        assertThat(sql).contains("VALUES(");
        assertThat(sql).contains("'test'");
        assertThat(sql).contains("25");
        assertThat(sql).endsWith(");");
    }

    @Test
    void shouldHandleDeletedField() {
        SqlTestObj obj = new SqlTestObj();
        obj.userName = "test";
        obj.deleted = 0;

        String sql = ZLcSqlUtil.generateInsertSql("user", obj);
        assertThat(sql).contains("is_deleted");
    }

    // 测试用的内部类
    static class SqlTestObj {
        String userName;
        int age;
        Integer deleted;
    }
}