package com.zifang.z.lc.sdk.define;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * ModelDataSaveMode 单元测试
 *
 * @author zifang
 */
public class ModelDataSaveModeTest {

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ModelDataSaveMode> constructor = ModelDataSaveMode.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    public void shouldHaveSaveCommonConstant() {
        assertEquals(Integer.valueOf(0), ModelDataSaveMode.SAVE_COMMON);
    }

    @Test
    public void shouldHaveSaveTempConstant() {
        assertEquals(Integer.valueOf(1), ModelDataSaveMode.SAVE_TEMP);
    }

    @Test
    public void shouldHaveSaveInProcessConstant() {
        assertEquals(Integer.valueOf(2), ModelDataSaveMode.SAVE_IN_PROCESS);
    }

    @Test
    public void shouldHaveDistinctValues() {
        assertNotEquals(ModelDataSaveMode.SAVE_COMMON, ModelDataSaveMode.SAVE_TEMP);
        assertNotEquals(ModelDataSaveMode.SAVE_TEMP, ModelDataSaveMode.SAVE_IN_PROCESS);
        assertNotEquals(ModelDataSaveMode.SAVE_COMMON, ModelDataSaveMode.SAVE_IN_PROCESS);
    }
}