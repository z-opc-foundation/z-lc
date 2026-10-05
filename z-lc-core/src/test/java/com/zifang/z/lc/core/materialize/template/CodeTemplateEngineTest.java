package com.zifang.z.lc.core.materialize.template;

import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    // ======================================================================
    // 缺陷 #64：camelCase 在合法输入上抛 StringIndexOutOfBoundsException
    // ======================================================================

    /**
     * <b>反向：连续下划线是合法字段码，旧写法在这里抛。</b>
     * {@code SchemaAdminBizService.FIELD_CODE_RE = ^[A-Za-z][A-Za-z0-9_]*$} 明确放行这些，
     * 所以这不是"非法输入的容错"，而是<b>正常输入的崩溃</b>。
     * 下面每一个都是实测抛 {@code StringIndexOutOfBoundsException: String index out of range: 0} 的那一个。
     */
    @Test
    public void camelCaseSurvivesConsecutiveUnderscores() {
        assertEquals("aB", CodeTemplateEngine.camelCase("a__b"));
        assertEquals("userId", CodeTemplateEngine.camelCase("user__id"));
        assertEquals("aB", CodeTemplateEngine.camelCase("a___b"));
        assertEquals("aBC", CodeTemplateEngine.camelCase("a__b__c"));
    }

    /**
     * 首尾下划线同样不抛。
     * <p>
     * <b>这一支含一处有意的行为变更，不是"保持原样"：</b>
     * 旧写法的 {@code parts[0]} 拿到的是空串、抬首字母从 index 1 开始，所以 {@code _a} 过去返回
     * {@code "A"}；新写法把空段整个跳过，{@code a} 成为"第一个非空段"因而保持小写 ⇒ {@code "a"}。
     * 取 {@code "a"} 是对的：本方法的三个用处（Java 字段名、Controller 的 URL 段、React prop 名）
     * 都要求首字母小写，写成 {@code "A"} 虽能编译但违背命名约定，
     * 且与 {@code toPascalCase("_a") == "A"} 一起用会让 getter 变成 {@code getA()} 配字段 {@code A}。
     */
    @Test
    public void camelCaseSurvivesLeadingAndTrailingUnderscores() {
        assertEquals("a", CodeTemplateEngine.camelCase("_a"));
        assertEquals("a", CodeTemplateEngine.camelCase("a_"));
        assertEquals("a", CodeTemplateEngine.camelCase("_a_"));
        assertEquals("aB", CodeTemplateEngine.camelCase("_a_b"));
    }

    /**
     * 大小写折叠不能受 JVM 默认区域影响。
     * <p>
     * 土耳其语区域（tr-TR）下 {@code "I".toLowerCase()} 是无点 {@code ı}，
     * 同一份字段码在两种 JVM 上会生成出两个不同的标识符，产物与源码对不上。
     * 所以这里固定住 {@code Locale.ROOT}，并在土耳其语区域下重跑一遍。
     */
    @Test
    public void camelCaseIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ROOT);
            String underRoot = CodeTemplateEngine.camelCase("INDEX_CODE");

            Locale.setDefault(new Locale("tr", "TR"));
            String underTurkish = CodeTemplateEngine.camelCase("INDEX_CODE");

            assertEquals("区域不应改变标识符（tr-TR 下 I 会变成无点 ı）", underRoot, underTurkish);
            assertEquals("indexCode", underRoot);
        } finally {
            Locale.setDefault(original);
        }
    }

    /** {@code toPascalCase} 逐字符走，遇 {@code _} 只置标记不取下标 —— 连续下划线对它无害（钉住"本来就没坏"）。 */
    @Test
    public void toPascalCaseToleratesConsecutiveUnderscores() {
        assertEquals("AB", CodeTemplateEngine.toPascalCase("a__b"));
        assertEquals("UserId", CodeTemplateEngine.toPascalCase("user__id"));
        assertEquals("A", CodeTemplateEngine.toPascalCase("_a"));
    }

    /**
     * <b>本次缺陷的正面证据</b>：一个含 {@code user__id} 字段的实体，
     * {@code renderEntity} 必须真的吐出源码，而不是在中途炸掉。
     * <p>
     * 旧写法在这里抛 {@code StringIndexOutOfBoundsException}，
     * 而物化是<b>逐 entity 循环生成、循环结束后才统一写文件</b>
     * （{@code MaterializationService.run} 第 2/3 步），所以这一个字段会让整批物化零产出，
     * 记进 {@code error_message} 的还只是 {@code String index out of range: 0}。
     */
    @Test
    public void renderEntityProducesSourceForAFieldWithConsecutiveUnderscores() {
        EntityEntity entity = makeEntity("order_item", "z_lc_order_item");
        List<FieldEntity> fields = Arrays.asList(
                makeField("user__id", "用户", "LONG", null),
                makeField("amount", "金额", "DECIMAL", null));

        String src = engine.renderEntity(entity, fields);

        assertNotNull(src);
        assertTrue("坏字段那一行必须真的生成出来: " + src, src.contains("private Long userId;"));
        assertTrue("@TableField 要保留数据库里的原字段名: " + src, src.contains("@TableField(\"user__id\")"));
        assertTrue("getter/setter 也要跟着走: " + src,
                src.contains("getUserId()") && src.contains("setUserId("));
        assertTrue("相邻的正常字段不能被带坏: " + src,
                src.contains("private java.math.BigDecimal amount;"));
    }

    /**
     * Controller 的 URL 段也吃 {@code camelCase(entityCode)}，
     * 而实体码连 {@code FIELD_CODE_RE} 都没有（{@code SchemaAdminBizService} 里搜不到任何 entityCode 校验）。
     */
    @Test
    public void renderControllerSurvivesAnEntityCodeWithConsecutiveUnderscores() {
        EntityEntity entity = makeEntity("order__item", "z_lc_order_item");

        String src = engine.renderController(entity);

        assertTrue("URL 段要生成出来: " + src, src.contains("/api/generated/orderItem"));
        assertTrue("类名也要生成出来: " + src, src.contains("public class OrderItemController"));
    }

    @Test
    public void renderMapperAndPageAlsoSurviveOddFieldCodes() {
        EntityEntity entity = makeEntity("order__item", "z_lc_order_item");
        List<FieldEntity> fields = Collections.singletonList(
                makeField("user__id", "用户", "LONG", null));

        assertTrue(engine.renderMapper(entity).contains("interface OrderItemMapper"));
        assertTrue("React 页面那份也吃 camelCase: ",
                engine.renderReactPage(entity, fields).contains("userId"));
    }
}