package com.zifang.z.lc.schema;

import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.util.SchemaCodec;
import com.zifang.z.lc.schema.util.SchemaValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨端闭环验证：设计器（z-lc-admin-ui/designer2 与 z-opc 主壳 lc-designer）产出的
 * PageSchema JSON，必须能被后端 z-lc-schema 正确反序列化 + 校验通过。
 *
 * 这段 JSON 是两个前端设计器 insertNode/appendToSlot 后 JSON.stringify(schema) 的真实形状：
 * elements 扁平表 + layout.structure 树 + 具名插槽 + context + logic。
 * 契约一旦漂移（字段改名/结构变化），这里立刻红 —— 是设计器↔后端的事实源守卫。
 */
class DesignerOutputContractTest {

    private static final String DESIGNER_OUTPUT = """
        {
          "version": "1.0",
          "id": "page",
          "elements": {
            "root": { "type": "container", "props": { "padding": 16 } },
            "form-1": { "type": "formContainer", "props": { "layout": "vertical", "labelWidth": 80 } },
            "name-1": { "type": "input", "props": { "name": "userName", "label": "用户名", "required": true } },
            "title-1": { "type": "text", "props": { "text": "请填写" } },
            "submit-1": { "type": "button", "props": { "text": "提交" } }
          },
          "layout": {
            "root": "root",
            "structure": {
              "root": { "children": ["form-1", "submit-1"] },
              "form-1": { "children": ["name-1"], "namedSlots": { "header": ["title-1"] } },
              "name-1": { "children": [] },
              "title-1": { "children": [] },
              "submit-1": { "children": [] }
            }
          },
          "context": {
            "variableList": [{ "name": "formData", "description": "表单数据" }],
            "eventList": [{ "name": "onClick", "description": "点击" }],
            "contextMethods": []
          },
          "logic": {
            "flowBinds": { "submit-1.onClick": ["flow-submit"] },
            "flows": {
              "flow-submit": {
                "id": "flow-submit",
                "headAddress": "start-1",
                "flowNodeMap": {
                  "start-1": { "execute": { "type": "logic", "logicType": "start" }, "next": [{ "address": "req-1" }] },
                  "req-1": { "execute": { "type": "logic", "logicType": "interfaceRequest" }, "next": [] }
                }
              }
            }
          }
        }
        """;

    @Test
    void designerOutputDeserializes() {
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_OUTPUT);
        assertNotNull(schema);
        assertEquals("page", schema.getId());
        assertEquals("1.0", schema.getVersion());
    }

    @Test
    void elementsAndLayoutRoundTrip() {
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_OUTPUT);
        assertEquals(5, schema.getElements().size());
        assertEquals("input", schema.getElements().get("name-1").getType());
        assertEquals(Boolean.TRUE, schema.getElements().get("name-1").getProps().get("required"));
        assertEquals("root", schema.getLayout().getRoot());
        assertTrue(schema.getLayout().getStructure().get("root").getChildren().contains("form-1"));
        assertTrue(schema.getLayout().getStructure().get("form-1").getNamedSlots().get("header").contains("title-1"));
    }

    @Test
    void contextAndLogicRoundTrip() {
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_OUTPUT);
        assertNotNull(schema.getContext());
        assertEquals(1, schema.getContext().getVariableList().size());
        assertNotNull(schema.getLogic());
        assertTrue(schema.getLogic().getFlowBinds().containsKey("submit-1.onClick"));
        assertNotNull(schema.getLogic().getFlows().get("flow-submit"));
        assertEquals("start-1", schema.getLogic().getFlows().get("flow-submit").getHeadAddress());
    }

    @Test
    void designerOutputPassesValidation() {
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_OUTPUT);
        SchemaValidator.Result result = SchemaValidator.validate(schema);
        assertTrue(result.ok(), "设计器产出应通过校验，实际问题：" + result.getProblems());
    }

    @Test
    void codecRoundTripStable() {
        PageSchema schema = SchemaCodec.fromJson(DESIGNER_OUTPUT);
        String reSerialized = SchemaCodec.toJson(schema);
        PageSchema again = SchemaCodec.fromJson(reSerialized);
        assertEquals(schema.getElements().size(), again.getElements().size());
        assertEquals(schema.getLayout().getRoot(), again.getLayout().getRoot());
        assertEquals(schema.getLogic().getFlowBinds(), again.getLogic().getFlowBinds());
    }
}
