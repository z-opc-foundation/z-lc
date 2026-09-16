package com.zifang.z.lc.core.materialize;

import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import com.zifang.z.lc.core.materialize.template.CodeTemplateEngine;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * CodeTemplateEngine 单元测试 (FEATURE006 T1).
 * <p>
 * 验证渲染产物:
 * 1. Java Entity 包含 @TableName + 所有字段
 * 2. Mapper extends BaseMapper
 * 3. Service extends IService
 * 4. Controller 包含 6 个 REST 端点
 * 5. React 页面包含 antd Table + Form
 */
public class CodeTemplateEngineTest {

    private final CodeTemplateEngine engine = new CodeTemplateEngine();

    private EntityEntity mockEntity() {
        EntityEntity e = new EntityEntity();
        e.setEntityCode("user_info");
        e.setEntityName("User Info");
        e.setTableName("t_user_info");
        e.setDescription("Test user table");
        return e;
    }

    private List<FieldEntity> mockFields() {
        List<FieldEntity> list = new ArrayList<>();
        FieldEntity f1 = new FieldEntity();
        f1.setFieldCode("user_name");
        f1.setFieldName("User Name");
        f1.setFieldType("STRING");
        f1.setRequired(1);
        list.add(f1);
        FieldEntity f2 = new FieldEntity();
        f2.setFieldCode("age");
        f2.setFieldName("Age");
        f2.setFieldType("INT");
        f2.setRequired(0);
        list.add(f2);
        FieldEntity f3 = new FieldEntity();
        f3.setFieldCode("email");
        f3.setFieldName("Email");
        f3.setFieldType("STRING");
        f3.setRequired(0);
        list.add(f3);
        return list;
    }

    @Test
    public void testPascalCase() {
        assertEquals("UserInfo", CodeTemplateEngine.toPascalCase("user_info"));
        assertEquals("OrderItem", CodeTemplateEngine.toPascalCase("order_item"));
        assertEquals("Xyz", CodeTemplateEngine.toPascalCase("xyz"));
    }

    @Test
    public void testCamelCase() {
        assertEquals("userInfo", CodeTemplateEngine.camelCase("user_info"));
        assertEquals("orderItem", CodeTemplateEngine.camelCase("order_item"));
    }

    @Test
    public void testJavaTypeOf() {
        assertEquals("String", CodeTemplateEngine.javaTypeOf("STRING"));
        assertEquals("Integer", CodeTemplateEngine.javaTypeOf("INT"));
        assertEquals("Long", CodeTemplateEngine.javaTypeOf("BIGINT"));
        assertEquals("java.time.LocalDateTime", CodeTemplateEngine.javaTypeOf("DATETIME"));
        assertEquals("Boolean", CodeTemplateEngine.javaTypeOf("TINYINT"));
    }

    @Test
    public void testRenderEntity() {
        String code = engine.renderEntity(mockEntity(), mockFields());
        assertTrue("Should contain package", code.contains("package com.zifang.generated.entity"));
        assertTrue("Should contain @TableName", code.contains("@TableName(\"t_user_info\")"));
        assertTrue("Should contain class name", code.contains("public class UserInfo"));
        assertTrue("Should contain userName field", code.contains("private String userName"));
        assertTrue("Should contain age field", code.contains("private Integer age"));
        assertTrue("Should contain getters", code.contains("getUserName()"));
        assertTrue("Should contain setters", code.contains("setUserName("));
    }

    @Test
    public void testRenderMapper() {
        String code = engine.renderMapper(mockEntity());
        assertTrue(code.contains("public interface UserInfoMapper extends BaseMapper<UserInfo>"));
        assertTrue(code.contains("@Mapper"));
    }

    @Test
    public void testRenderService() {
        String code = engine.renderService(mockEntity());
        assertTrue(code.contains("public interface UserInfoService extends IService<UserInfo>"));
    }

    @Test
    public void testRenderServiceImpl() {
        String code = engine.renderServiceImpl(mockEntity());
        assertTrue(code.contains("public class UserInfoServiceImpl"));
        assertTrue(code.contains("extends ServiceImpl<UserInfoMapper, UserInfo>"));
        assertTrue(code.contains("@Service"));
    }

    @Test
    public void testRenderController() {
        String code = engine.renderController(mockEntity());
        assertTrue(code.contains("@RestController"));
        assertTrue(code.contains("@RequestMapping(\"/api/generated/userInfo\")"));
        assertTrue(code.contains("public class UserInfoController"));
        // 6 个端点
        assertTrue(code.contains("@GetMapping(\"/page\")"));
        assertTrue(code.contains("@GetMapping(\"/get\")"));
        assertTrue(code.contains("@PostMapping(\"/create\")"));
        assertTrue(code.contains("@PostMapping(\"/update\")"));
        assertTrue(code.contains("@DeleteMapping(\"/delete\")"));
        assertTrue(code.contains("@GetMapping(\"/list\")"));
    }

    @Test
    public void testRenderReactPage() {
        String code = engine.renderReactPage(mockEntity(), mockFields());
        assertTrue(code.contains("export default function UserInfoPage()"));
        assertTrue(code.contains("import { Button, Card, Form"));
        assertTrue(code.contains("dataIndex: 'userName'"));
        assertTrue(code.contains("dataIndex: 'age'"));
        assertTrue(code.contains("Form.Item name=\"userName\""));
        assertTrue(code.contains("Form.Item name=\"age\""));
        assertTrue(code.contains("/api/generated/userInfo"));
    }
}
