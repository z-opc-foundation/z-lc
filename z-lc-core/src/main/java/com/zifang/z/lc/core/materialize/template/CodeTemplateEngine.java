package com.zifang.z.lc.core.materialize.template;

import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 代码模板引擎 (FEATURE006 T1).
 * <p>
 * 将 z_lc_entity + z_lc_field 渲染为可编译的代码:
 * <ul>
 *   <li>Java Entity POJO (MyBatis-Plus 注解)</li>
 *   <li>Mapper 接口 (extends BaseMapper)</li>
 *   <li>Service 类 (CRUD wrapper)</li>
 *   <li>REST Controller (Knife4j 注解)</li>
 *   <li>React 页面 (antd Table + Form)</li>
 * </ul>
 * <p>
 * 字符串模板策略 — 不引入 FreeMarker/Velocity 等额外依赖,
 * 使用 StringBuilder + 占位符替换, 编译期无 runtime 模板解析开销.
 */
@Component
public class CodeTemplateEngine {

    /**
     * 类名转换: snake_case → PascalCase
     */
    public static String toPascalCase(String snake) {
        if (snake == null || snake.isEmpty()) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : snake.toCharArray()) {
            if (c == '_' || c == '-') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    /**
     * snake_case → camelCase
     */
    public static String camelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /**
     * Java 类型推断: 字段类型 → Java 类型
     */
    public static String javaTypeOf(String fieldType) {
        if (fieldType == null) {
            return "String";
        }

        switch (fieldType.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return "Integer";
            case "LONG":
            case "BIGINT":
                return "Long";
            case "DOUBLE":
                return "Double";
            case "BIGDECIMAL":
            case "DECIMAL":
                return "java.math.BigDecimal";
            case "BOOLEAN":
            case "BOOL":
            case "TINYINT":
                return "Boolean";
            case "DATE":
                return "java.time.LocalDate";
            case "DATETIME":
            case "TIMESTAMP":
                return "java.time.LocalDateTime";
            case "TEXT":
            case "LONGTEXT":
                return "String";
            default:
                return "String";
        }
    }

    /**
     * Java 类型 → 表单输入组件类型
     */
    public static String formInputOf(String javaType) {
        if (javaType.contains("Integer") || javaType.contains("Long")
                || javaType.contains("Double") || javaType.contains("BigDecimal")) {
            return "InputNumber";
        }
        if (javaType.contains("Boolean")) {
            return "Switch";
        }
        if (javaType.contains("LocalDate")) {
            return "DatePicker";
        }
        if (javaType.contains("LocalDateTime")) {
            return "DatePicker";
        }
        return "Input";
    }

    /**
     * 生成 Java Entity
     */
    public String renderEntity(EntityEntity entity, List<FieldEntity> fields) {
        String className = toPascalCase(entity.getEntityCode());
        StringBuilder sb = new StringBuilder();
        sb.append("package com.zifang.generated.entity;\n\n");
        sb.append("import com.baomidou.mybatisplus.annotation.*;\n");
        sb.append("import java.io.Serializable;\n");
        sb.append("import java.time.LocalDate;\n");
        sb.append("import java.time.LocalDateTime;\n");
        sb.append("import java.math.BigDecimal;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(className).append(" — 自动生成 (FEATURE006 T1 物化引擎)\n");
        sb.append(" * 物理表: ").append(entity.getTableName()).append("\n");
        if (entity.getDescription() != null) {
            sb.append(" * ").append(entity.getDescription()).append("\n");
        }
        sb.append(" */\n");
        sb.append("@TableName(\"").append(entity.getTableName()).append("\")\n");
        sb.append("public class ").append(className).append(" implements Serializable {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");

        for (FieldEntity f : fields) {
            String camel = camelCase(f.getFieldCode());
            sb.append("    @TableField(\"").append(f.getFieldCode()).append("\")\n");
            sb.append("    private ").append(javaTypeOf(f.getFieldType())).append(" ").append(camel).append(";\n\n");
        }
        for (FieldEntity f : fields) {
            String camel = camelCase(f.getFieldCode());
            String cap = toPascalCase(f.getFieldCode());
            String type = javaTypeOf(f.getFieldType());
            sb.append("    public ").append(type).append(" get").append(cap).append("() { return ").append(camel).append("; }\n");
            sb.append("    public void set").append(cap).append("(").append(type).append(" ").append(camel).append(") { this.").append(camel).append(" = ").append(camel).append("; }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 生成 Mapper
     */
    public String renderMapper(EntityEntity entity) {
        String className = toPascalCase(entity.getEntityCode());
        return "package com.zifang.generated.mapper;\n\n" +
                "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
                "import com.zifang.generated.entity." + className + ";\n" +
                "import org.apache.ibatis.annotations.Mapper;\n\n" +
                "/**\n * " + className + " Mapper (FEATURE006 T1 自动生成)\n */\n" +
                "@Mapper\n" +
                "public interface " + className + "Mapper extends BaseMapper<" + className + "> {\n}\n";
    }

    /**
     * 生成 Service 接口
     */
    public String renderService(EntityEntity entity) {
        String className = toPascalCase(entity.getEntityCode());
        return "package com.zifang.generated.service;\n\n" +
                "import com.baomidou.mybatisplus.extension.service.IService;\n" +
                "import com.zifang.generated.entity." + className + ";\n\n" +
                "/**\n * " + className + " Service (FEATURE006 T1 自动生成)\n */\n" +
                "public interface " + className + "Service extends IService<" + className + "> {\n}\n";
    }

    /**
     * 生成 ServiceImpl
     */
    public String renderServiceImpl(EntityEntity entity) {
        String className = toPascalCase(entity.getEntityCode());
        return "package com.zifang.generated.service.impl;\n\n" +
                "import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n" +
                "import com.zifang.generated.entity." + className + ";\n" +
                "import com.zifang.generated.mapper." + className + "Mapper;\n" +
                "import com.zifang.generated.service." + className + "Service;\n" +
                "import org.springframework.stereotype.Service;\n\n" +
                "/**\n * " + className + " ServiceImpl (FEATURE006 T1 自动生成)\n */\n" +
                "@Service\n" +
                "public class " + className + "ServiceImpl\n" +
                "        extends ServiceImpl<" + className + "Mapper, " + className + ">\n" +
                "        implements " + className + "Service {\n}\n";
    }

    /**
     * 生成 Controller
     */
    public String renderController(EntityEntity entity) {
        String className = toPascalCase(entity.getEntityCode());
        String urlPath = camelCase(entity.getEntityCode());
        return "package com.zifang.generated.controller;\n\n" +
                "import com.zifang.generated.entity." + className + ";\n" +
                "import com.zifang.generated.service." + className + "Service;\n" +
                "import io.swagger.v3.oas.annotations.Operation;\n" +
                "import io.swagger.v3.oas.annotations.tags.Tag;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n\n" +
                "/**\n * " + className + " Controller (FEATURE006 T1 自动生成)\n */\n" +
                "@Tag(name = \"" + className + " 管理\")\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api/generated/" + urlPath + "\")\n" +
                "public class " + className + "Controller {\n\n" +
                "    @Autowired\n" +
                "    private " + className + "Service service;\n\n" +
                "    @Operation(summary = \"分页查询\")\n" +
                "    @GetMapping(\"/page\")\n" +
                "    public Map<String, Object> page(@RequestParam(defaultValue = \"1\") int pageNum,\n" +
                "                                    @RequestParam(defaultValue = \"20\") int pageSize) {\n" +
                "        Map<String, Object> r = new HashMap<>();\n" +
                "        r.put(\"data\", service.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize)));\n" +
                "        return r;\n" +
                "    }\n\n" +
                "    @Operation(summary = \"根据 ID 查询\")\n" +
                "    @GetMapping(\"/get\")\n" +
                "    public " + className + " get(@RequestParam Long id) {\n" +
                "        return service.getById(id);\n" +
                "    }\n\n" +
                "    @Operation(summary = \"创建\")\n" +
                "    @PostMapping(\"/create\")\n" +
                "    public boolean create(@RequestBody " + className + " entity) {\n" +
                "        return service.save(entity);\n" +
                "    }\n\n" +
                "    @Operation(summary = \"更新\")\n" +
                "    @PostMapping(\"/update\")\n" +
                "    public boolean update(@RequestBody " + className + " entity) {\n" +
                "        return service.updateById(entity);\n" +
                "    }\n\n" +
                "    @Operation(summary = \"删除\")\n" +
                "    @DeleteMapping(\"/delete\")\n" +
                "    public boolean delete(@RequestParam Long id) {\n" +
                "        return service.removeById(id);\n" +
                "    }\n\n" +
                "    @Operation(summary = \"列表 (不分页)\")\n" +
                "    @GetMapping(\"/list\")\n" +
                "    public List<" + className + "> list() {\n" +
                "        return service.list();\n" +
                "    }\n}\n";
    }

    /**
     * 生成 React 页面 (antd Table + Form)
     */
    public String renderReactPage(EntityEntity entity, List<FieldEntity> fields) {
        String className = toPascalCase(entity.getEntityCode());
        String camel = camelCase(entity.getEntityCode());
        StringBuilder formItems = new StringBuilder();
        StringBuilder tableCols = new StringBuilder();

        for (FieldEntity f : fields) {
            String fieldCamel = camelCase(f.getFieldCode());
            String javaType = javaTypeOf(f.getFieldType());
            String inputType = formInputOf(javaType);
            String required = f.getRequired() != null && f.getRequired() == 1 ? "true" : "false";
            formItems.append("            <Form.Item name=\"").append(fieldCamel).append("\" label=\"")
                    .append(f.getFieldName() != null ? f.getFieldName() : fieldCamel)
                    .append("\" rules={[{ required: ").append(required).append(" }]}>\n");
            formItems.append("                <").append(inputType).append(" placeholder=\"请输入")
                    .append(f.getFieldName() != null ? f.getFieldName() : fieldCamel).append("\"/>\n");
            formItems.append("            </Form.Item>\n");
            tableCols.append("        { title: '").append(f.getFieldName() != null ? f.getFieldName() : fieldCamel)
                    .append("', dataIndex: '").append(fieldCamel).append("' },\n");
        }

        StringBuilder page = new StringBuilder();
        page.append("/**\n");
        page.append(" * ").append(className).append(" 管理页面 — FEATURE006 T1 自动生成\n");
        page.append(" * 后端路径: /api/generated/").append(camel).append("\n");
        page.append(" */\n");
        page.append("import { useEffect, useState } from 'react';\n");
        page.append("import { Button, Card, Form, Input, message, Modal, Space, Table } from 'antd';\n");
        page.append("import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';\n");
        page.append("import { request } from '@/utils/request';\n\n");
        page.append("export default function ").append(className).append("Page() {\n");
        page.append("  const [data, setData] = useState([]);\n");
        page.append("  const [loading, setLoading] = useState(false);\n");
        page.append("  const [modalOpen, setModalOpen] = useState(false);\n");
        page.append("  const [editing, setEditing] = useState(null);\n");
        page.append("  const [form] = Form.useForm();\n\n");
        page.append("  const load = async () => {\n");
        page.append("    setLoading(true);\n");
        page.append("    try { const r = await request.get('/generated/").append(camel).append("/list'); setData(r?.data || []); }\n");
        page.append("    catch { message.error('加载失败'); } finally { setLoading(false); }\n");
        page.append("  };\n");
        page.append("  useEffect(() => { load(); }, []);\n\n");
        page.append("  const onSave = async () => {\n");
        page.append("    try {\n");
        page.append("      const values = await form.validateFields();\n");
        page.append("      if (editing) { await request.post('/generated/").append(camel).append("/update', { id: editing.id, ...values }); }\n");
        page.append("      else { await request.post('/generated/").append(camel).append("/create', values); }\n");
        page.append("      message.success('保存成功'); setModalOpen(false); load();\n");
        page.append("    } catch (e) { if (!e?.errorFields) message.error('保存失败'); }\n");
        page.append("  };\n\n");
        page.append("  return (\n");
        page.append("    <Card title='").append(className).append(" 管理' extra={<Space>\n");
        page.append("      <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>\n");
        page.append("      <Button type='primary' icon={<PlusOutlined/>} onClick={()=>{ setEditing(null); form.resetFields(); setModalOpen(true); }}>新建</Button>\n");
        page.append("    </Space>}>\n");
        page.append("      <Table dataSource={data} loading={loading} rowKey='id' size='small' pagination={pageSize: 20}}\n");
        page.append("        columns={[\n").append(tableCols);
        page.append("          { title: '操作', render: (_, r) => (\n");
        page.append("            <Space>\n");
        page.append("              <Button size='small' icon={<EditOutlined/>} onClick={()=>{ setEditing(r); form.setFieldsValue(r); setModalOpen(true); }}/>\n");
        page.append("              <Button size='small' danger icon={<DeleteOutlined/>} onClick={async()=>{ await request.post('/generated/").append(camel).append("/delete', null, {params:{id:r.id}}); load(); }}/>\n");
        page.append("            </Space>)}\n");
        page.append("        ]}/>\n");
        page.append("      <Modal title={editing?'编辑':'新建'} open={modalOpen} onCancel={()=>setModalOpen(false)} onOk={onSave}>\n");
        page.append("        <Form form={form} layout='vertical'>\n").append(formItems);
        page.append("        </Form>\n");
        page.append("      </Modal>\n");
        page.append("    </Card>\n");
        page.append("  );\n");
        page.append("}\n");
        return page.toString();
    }
}
