package com.zifang.z.lc.schema;

import com.zifang.z.lc.schema.logic.FlowNode;
import com.zifang.z.lc.schema.logic.LogicFlow;
import com.zifang.z.lc.schema.model.NodeStructure;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;
import com.zifang.z.lc.schema.util.SchemaCodec;
import com.zifang.z.lc.schema.util.SchemaValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0 统一 Schema 契约测试：扁平节点 + 树结构 + 具名插槽 + 逻辑流 + JSON 往返 + 校验。
 */
class SchemaContractTest {

    private PageSchema sampleSchema() {
        PageSchema s = new PageSchema();
        s.setId("demo-page");
        s.addElement("page-1", new SchemaNode("pageContainer"));
        s.addElement("btn-1", new SchemaNode("button").withProp("text", "查询"));
        s.addElement("table-1", new SchemaNode("table").withProp("title", "结果"));
        s.addElement("tabs-1", new SchemaNode("tabsContainer"));
        s.addElement("tab-title-1", new SchemaNode("text").withProp("text", "标题"));

        s.getLayout().setRoot("page-1");
        s.getLayout().structureOf("page-1").addChild("btn-1").addChild("table-1").addChild("tabs-1");
        s.getLayout().structureOf("tabs-1").addSlot("header", "tab-title-1");

        LogicFlow flow = new LogicFlow("flow-1", "start-1");
        flow.putNode("start-1", new FlowNode(FlowNode.Execute.logic("start", Collections.emptyMap())));
        flow.getFlowNodeMap().get("start-1").getNext().add(
                new FlowNode.NextEdge("req-1", "expression", "true"));
        flow.putNode("req-1", new FlowNode(FlowNode.Execute.logic("interfaceRequest",
                Collections.singletonMap("url", "/api/query"))));
        s.getLogic().addFlow(flow);
        s.getLogic().bind("btn-1::onClick", "flow-1");
        return s;
    }

    @Test
    void shouldBuildFlatNodesAndTreeSeparately() {
        PageSchema s = sampleSchema();
        assertEquals(5, s.getElements().size());
        assertEquals("page-1", s.getLayout().getRoot());
        assertEquals(Arrays.asList("btn-1", "table-1", "tabs-1"),
                s.getLayout().getStructure().get("page-1").getChildren());
        assertEquals(Collections.singletonList("tab-title-1"),
                s.getLayout().getStructure().get("tabs-1").getNamedSlots().get("header"));
    }

    @Test
    void validateShouldPassForWellFormedSchema() {
        SchemaValidator.Result r = SchemaValidator.validate(sampleSchema());
        assertTrue(r.ok(), r.toString());
    }

    @Test
    void validateShouldCatchDanglingChildAndBadBinding() {
        PageSchema s = sampleSchema();
        s.getLayout().structureOf("page-1").addChild("ghost-node");
        s.getLogic().bind("ghost-btn::onClick", "flow-1");
        SchemaValidator.Result r = SchemaValidator.validate(s);
        assertFalse(r.ok());
        assertTrue(r.toString().contains("ghost-node"));
        assertTrue(r.toString().contains("ghost-btn"));
    }

    @Test
    void validateShouldCatchCycle() {
        PageSchema s = sampleSchema();
        s.getLayout().structureOf("table-1").addChild("page-1");
        SchemaValidator.Result r = SchemaValidator.validate(s);
        assertFalse(r.ok());
        assertTrue(r.toString().contains("出现多次"));
    }

    @Test
    void shouldRoundTripViaJson() {
        PageSchema s = sampleSchema();
        String json = SchemaCodec.toJson(s);
        assertTrue(json.contains("pageContainer"));
        PageSchema back = SchemaCodec.fromJson(json);
        assertEquals(s.getId(), back.getId());
        assertEquals(s.getElements().size(), back.getElements().size());
        assertEquals(1, back.getLogic().getFlows().size());
        assertTrue(SchemaValidator.validate(back).ok());
    }

    @Test
    void shouldIgnoreUnknownFieldsOnRead() {
        PageSchema s = SchemaCodec.fromJson("{\"id\":\"x\",\"unknownField\":123}");
        assertEquals("x", s.getId());
        assertNotNull(s.getLayout());
        assertNotNull(s.getLogic());
    }
}
