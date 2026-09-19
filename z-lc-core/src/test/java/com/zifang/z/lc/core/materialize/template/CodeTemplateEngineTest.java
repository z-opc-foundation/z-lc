package com.zifang.z.lc.core.materialize.template;

import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * CodeTemplateEngine 单元测试
 *
 * @author zifang
 */
public class CodeTemplateEngineTest {

    private final CodeTemplateEngine engine = new CodeTemplateEngine();

    @Test
    public void shouldCreateEngine() {
        assertNotNull(engine);
    }

    // --- toPascalCase ---

    @Test
    public void toPascalCaseShouldHandleNull() {
        assertEquals("Unknown", CodeTemplateEngine.toPascalCase(null));
    }

    @Test
    public void toPascalCaseShouldHandleEmpty() {
        assertEquals("Unknown", CodeTemplateEngine.toPascalCase(""));
    }

    @Test
    public void toPascalCaseShouldConvertSnakeCase() {
        assertEquals("UserProfile", CodeTemplateEngine.toPascalCase("user_profile"));
    }

    @Test
    public void toPascalCaseShouldHandleSingleWord() {
        assertEquals("User", CodeTemplateEngine.toPascalCase("user"));
    }

    @Test
    public void toPascalCaseShouldHandleAlreadyCapitalized() {
        assertEquals("UserProfile", CodeTemplateEngine.toPascalCase("UserProfile"));
    }

    @Test
    public void toPascalCaseShouldHandleHyphens() {
        assertEquals("UserProfile", CodeTemplateEngine.toPascalCase("user-profile"));
    }

    // --- camelCase ---

    @Test
    public void camelCaseShouldHandleNull() {
        assertEquals(null, CodeTemplateEngine.camelCase(null));
    }

    @Test
    public void camelCaseShouldHandleEmpty() {
        assertEquals("", CodeTemplateEngine.camelCase(""));
    }

    @Test
    public void camelCaseShouldConvertSnakeCase() {
        assertEquals("userProfile", CodeTemplateEngine.camelCase("user_profile"));
    }

    @Test
    public void camelCaseShouldHandleSingleWord() {
        assertEquals("user", CodeTemplateEngine.camelCase("user"));
    }

    @Test
    public void camelCaseShouldHandleMultipleUnderscores() {
        assertEquals("userProfileInfo", CodeTemplateEngine.camelCase("user_profile_info"));
    }

    // --- javaTypeOf ---

    @Test
    public void javaTypeOfShouldHandleNull() {
        assertEquals("String", CodeTemplateEngine.javaTypeOf(null));
    }

    @Test
    public void javaTypeOfShouldMapInt() {
        assertEquals("Integer", CodeTemplateEngine.javaTypeOf("INT"));
        assertEquals("Integer", CodeTemplateEngine.javaTypeOf("INTEGER"));
    }

    @Test
    public void javaTypeOfShouldMapLong() {
        assertEquals("Long", CodeTemplateEngine.javaTypeOf("LONG"));
        assertEquals("Long", CodeTemplateEngine.javaTypeOf("BIGINT"));
    }

    @Test
    public void javaTypeOfShouldMapDouble() {
        assertEquals("Double", CodeTemplateEngine.javaTypeOf("DOUBLE"));
    }

    @Test
    public void javaTypeOfShouldMapBigDecimal() {
        assertEquals("java.math.BigDecimal", CodeTemplateEngine.javaTypeOf("BIGDECIMAL"));
        assertEquals("java.math.BigDecimal", CodeTemplateEngine.javaTypeOf("DECIMAL"));
    }

    @Test
    public void javaTypeOfShouldMapBoolean() {
        assertEquals("Boolean", CodeTemplateEngine.javaTypeOf("BOOLEAN"));
        assertEquals("Boolean", CodeTemplateEngine.javaTypeOf("BOOL"));
        assertEquals("Boolean", CodeTemplateEngine.javaTypeOf("TINYINT"));
    }

    @Test
    public void javaTypeOfShouldMapDate() {
        assertEquals("java.time.LocalDate", CodeTemplateEngine.javaTypeOf("DATE"));
    }

    @Test
    public void javaTypeOfShouldMapDateTime() {
        assertEquals("java.time.LocalDateTime", CodeTemplateEngine.javaTypeOf("DATETIME"));
        assertEquals("java.time.LocalDateTime", CodeTemplateEngine.javaTypeOf("TIMESTAMP"));
    }

    @Test
    public void javaTypeOfShouldMapText() {
        assertEquals("String", CodeTemplateEngine.javaTypeOf("TEXT"));
        assertEquals("String", CodeTemplateEngine.javaTypeOf("LONGTEXT"));
    }

    @Test
    public void javaTypeOfShouldDefaultToString() {
        assertEquals("String", CodeTemplateEngine.javaTypeOf("UNKNOWN"));
    }

    @Test
    public void javaTypeOfShouldBeCaseInsensitive() {
        assertEquals("Integer", CodeTemplateEngine.javaTypeOf("int"));
        assertEquals("Long", CodeTemplateEngine.javaTypeOf("long"));
    }

    // --- formInputOf ---

    @Test
    public void formInputOfShouldMapNumeric() {
        assertEquals("InputNumber", CodeTemplateEngine.formInputOf("Integer"));
        assertEquals("InputNumber", CodeTemplateEngine.formInputOf("Long"));
        assertEquals("InputNumber", CodeTemplateEngine.formInputOf("Double"));
        assertEquals("InputNumber", CodeTemplateEngine.formInputOf("java.math.BigDecimal"));
    }

    @Test
    public void formInputOfShouldMapBoolean() {
        assertEquals("Switch", CodeTemplateEngine.formInputOf("Boolean"));
    }

    @Test
    public void formInputOfShouldMapLocalDate() {
        assertEquals("DatePicker", CodeTemplateEngine.formInputOf("java.time.LocalDate"));
    }

    @Test
    public void formInputOfShouldMapLocalDateTime() {
        assertEquals("DatePicker", CodeTemplateEngine.formInputOf("java.time.LocalDateTime"));
    }

    @Test
    public void formInputOfShouldDefaultToInput() {
        assertEquals("Input", CodeTemplateEngine.formInputOf("String"));
    }

    // --- renderEntity ---

    @Test
    public void renderEntityShouldGenerateJavaCode() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = makeField("name", "Name", "STRING", 0);
        String code = engine.renderEntity(entity, Collections.singletonList(field));
        assertNotNull(code);
        assertTrue(code.contains("public class User"));
        assertTrue(code.contains("@TableName(\"t_user\")"));
        assertTrue(code.contains("private String name"));
        assertTrue(code.contains("public String getName()"));
        assertTrue(code.contains("public void setName(String name)"));
    }

    @Test
    public void renderEntityShouldIncludeDescription() {
        EntityEntity entity = makeEntity("user", "t_user");
        entity.setDescription("User entity");
        String code = engine.renderEntity(entity, Collections.emptyList());
        assertTrue(code.contains("User entity"));
    }

    @Test
    public void renderEntityShouldHandleEmptyFields() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderEntity(entity, Collections.emptyList());
        assertNotNull(code);
        assertTrue(code.contains("public class User"));
    }

    // --- renderMapper ---

    @Test
    public void renderMapperShouldGenerateInterface() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderMapper(entity);
        assertNotNull(code);
        assertTrue(code.contains("public interface UserMapper"));
        assertTrue(code.contains("BaseMapper<User>"));
    }

    // --- renderService ---

    @Test
    public void renderServiceShouldGenerateInterface() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderService(entity);
        assertNotNull(code);
        assertTrue(code.contains("public interface UserService"));
        assertTrue(code.contains("IService<User>"));
    }

    // --- renderServiceImpl ---

    @Test
    public void renderServiceImplShouldGenerateClass() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderServiceImpl(entity);
        assertNotNull(code);
        assertTrue(code.contains("public class UserServiceImpl"));
        assertTrue(code.contains("ServiceImpl<UserMapper, User>"));
        assertTrue(code.contains("implements UserService"));
        assertTrue(code.contains("@Service"));
    }

    // --- renderController ---

    @Test
    public void renderControllerShouldGenerateClass() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderController(entity);
        assertNotNull(code);
        assertTrue(code.contains("public class UserController"));
        assertTrue(code.contains("@RestController"));
        assertTrue(code.contains("@RequestMapping"));
        assertTrue(code.contains("/api/generated/user"));
    }

    @Test
    public void renderControllerShouldIncludeStandardEndpoints() {
        EntityEntity entity = makeEntity("user", "t_user");
        String code = engine.renderController(entity);
        assertTrue(code.contains("/page"));
        assertTrue(code.contains("/get"));
        assertTrue(code.contains("/create"));
        assertTrue(code.contains("/update"));
        assertTrue(code.contains("/delete"));
        assertTrue(code.contains("/list"));
    }

    // --- renderReactPage ---

    @Test
    public void renderReactPageShouldGenerateJSX() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = makeField("name", "Name", "STRING", 1);
        String code = engine.renderReactPage(entity, Collections.singletonList(field));
        assertNotNull(code);
        assertTrue(code.contains("UserPage"));
        assertTrue(code.contains("/api/generated/user"));
        assertTrue(code.contains("Form.Item"));
        assertTrue(code.contains("Input"));
    }

    @Test
    public void renderReactPageShouldHandleRequiredFields() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = makeField("name", "Name", "STRING", 1);
        String code = engine.renderReactPage(entity, Collections.singletonList(field));
        assertTrue(code.contains("required: true"));
    }

    @Test
    public void renderReactPageShouldHandleNonRequiredFields() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = makeField("nickname", "Nickname", "STRING", 0);
        String code = engine.renderReactPage(entity, Collections.singletonList(field));
        assertTrue(code.contains("required: false"));
    }

    @Test
    public void renderReactPageShouldHandleMultipleFields() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity f1 = makeField("name", "Name", "STRING", 1);
        FieldEntity f2 = makeField("age", "Age", "INT", 0);
        String code = engine.renderReactPage(entity, Arrays.asList(f1, f2));
        assertTrue(code.contains("name"));
        assertTrue(code.contains("age"));
        assertTrue(code.contains("InputNumber"));
    }

    @Test
    public void renderReactPageShouldHandleNullRequired() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = makeField("name", "Name", "STRING", null);
        String code = engine.renderReactPage(entity, Collections.singletonList(field));
        assertTrue(code.contains("required: false"));
    }

    @Test
    public void renderReactPageShouldUseFieldCodeWhenNameIsNull() {
        EntityEntity entity = makeEntity("user", "t_user");
        FieldEntity field = new FieldEntity();
        field.setFieldCode("name");
        field.setFieldName(null);
        field.setFieldType("STRING");
        String code = engine.renderReactPage(entity, Collections.singletonList(field));
        assertTrue(code.contains("name"));
    }

    // --- Helpers ---

    private static EntityEntity makeEntity(String code, String table) {
        EntityEntity entity = new EntityEntity();
        entity.setEntityCode(code);
        entity.setTableName(table);
        return entity;
    }

    private static FieldEntity makeField(String code, String name, String type, Integer required) {
        FieldEntity field = new FieldEntity();
        field.setFieldCode(code);
        field.setFieldName(name);
        field.setFieldType(type);
        field.setRequired(required);
        return field;
    }
}