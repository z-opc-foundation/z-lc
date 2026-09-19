package com.zifang.z.lc.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Json 单元测试
 *
 * @author zifang
 */
public class JsonTest {

    @Test
    public void objectMapperShouldBeInitialized() {
        assertNotNull(Json.objectMapper);
    }

    @Test
    public void objectMapperShouldNotFailOnUnknownProperties() {
        assertTrue("FAIL_ON_UNKNOWN_PROPERTIES should be disabled",
                !Json.objectMapper.getDeserializationConfig()
                        .isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    public void toMapShouldConvertObject() {
        TestPojo obj = new TestPojo("Alice", 30);
        Map<String, Object> map = Json.toMap(obj);
        assertNotNull(map);
        assertEquals("Alice", map.get("name"));
        assertEquals(30, ((Number) map.get("age")).intValue());
    }

    @Test
    public void mtoShouldConvertMapToObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Bob");
        map.put("age", 25);
        TestPojo obj = Json.mto(map, TestPojo.class);
        assertNotNull(obj);
        assertEquals("Bob", obj.getName());
        assertEquals(25, obj.getAge());
    }

    @Test
    public void mtoShouldHandleUnknownFields() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Carol");
        map.put("unknown_field", "ignored");
        TestPojo obj = Json.mto(map, TestPojo.class);
        assertEquals("Carol", obj.getName());
    }

    @Test
    public void stoShouldDeserializeJsonString() {
        String json = "{\"name\":\"Dave\",\"age\":40}";
        TestPojo obj = Json.sto(json, TestPojo.class);
        assertNotNull(obj);
        assertEquals("Dave", obj.getName());
        assertEquals(40, obj.getAge());
    }

    @Test
    public void stoShouldReturnEmptyObjectOnInvalidJson() {
        String invalidJson = "not valid json";
        TestPojo obj = Json.sto(invalidJson, TestPojo.class);
        assertNotNull("Should return non-null even on parse failure", obj);
    }

    @Test
    public void stoShouldThrowOnNullJson() {
        // Jackson readValue(null, ...) throws IllegalArgumentException
        try {
            Json.sto(null, TestPojo.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void stoShouldHandleEmptyJson() {
        TestPojo obj = Json.sto("", TestPojo.class);
        assertNotNull(obj);
    }

    @Test
    public void stoShouldThrowIfClassHasNoDefaultConstructor() {
        try {
            Json.sto("{}", NoDefaultCtor.class);
            fail("Expected RuntimeException");
        } catch (RuntimeException expected) {
            // ok - cannot instantiate without default constructor
        }
    }

    // --- Test helpers ---

    public static class TestPojo {
        private String name;
        private int age;

        public TestPojo() {
        }

        public TestPojo(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class NoDefaultCtor {
        private final String required;
        public NoDefaultCtor(String required) {
            this.required = required;
        }
    }
}