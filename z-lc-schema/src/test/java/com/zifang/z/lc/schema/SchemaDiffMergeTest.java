package com.zifang.z.lc.schema;

import com.zifang.z.lc.schema.diff.SchemaDiff;
import com.zifang.z.lc.schema.diff.SchemaDiffer;
import com.zifang.z.lc.schema.diff.SchemaMerger;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.model.SchemaNode;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P4 结构化 diff + 三方合并测试。
 */
class SchemaDiffMergeTest {

    private PageSchema base() {
        PageSchema s = new PageSchema();
        s.setId("p");
        s.addElement("root", new SchemaNode("pageContainer"));
        s.addElement("btn", new SchemaNode("button").withProp("text", "默认").withProp("width", 100));
        s.getLayout().setRoot("root");
        s.getLayout().structureOf("root").addChild("btn");
        return s;
    }

    @Test
    void shouldDetectPropNodeAndStructureChanges() {
        PageSchema old = base();
        PageSchema nw = base();
        nw.getElements().get("btn").getProps().put("text", "查询");      // 属性改
        nw.addElement("table", new SchemaNode("table"));                 // 节点增
        nw.getLayout().structureOf("root").addChild("table");            // 结构改

        SchemaDiff d = SchemaDiffer.diff(old, nw);
        assertEquals(1, d.getNodeAdded().size());
        assertEquals("table", d.getNodeAdded().get(0).getNodeId());
        assertEquals(1, d.getPropChanged().size());
        assertTrue(d.getPropChanged().containsKey("btn::text"));
        assertEquals(1, d.getStructureChanged().size());
    }

    @Test
    void shouldIgnoreRenderStateProps() {
        PageSchema old = base();
        PageSchema nw = base();
        nw.getElements().get("btn").getProps().put("actual-width", 999);
        assertTrue(SchemaDiffer.diff(old, nw).getPropChanged().isEmpty());
    }

    @Test
    void businessWinsOnPropConflict() {
        PageSchema base = base();

        PageSchema platform = base();
        platform.getElements().get("btn").getProps().put("text", "平台文案");
        platform.getElements().get("btn").getProps().put("width", 200);   // 平台改 width

        PageSchema business = base();
        business.getElements().get("btn").getProps().put("text", "业务文案"); // 业务也改 text

        PageSchema merged = SchemaMerger.merge(base, platform, business);
        // text 冲突 → 业务优先
        assertEquals("业务文案", merged.getElements().get("btn").getProps().get("text"));
        // width 只有平台改 → 合并进来
        assertEquals(200, merged.getElements().get("btn").getProps().get("width"));
    }

    @Test
    void shouldAddPlatformNewNodeWithoutBusinessEdit() {
        PageSchema base = base();
        PageSchema platform = base();
        platform.addElement("new", new SchemaNode("alert").withProp("text", "新能力"));
        PageSchema business = base();

        PageSchema merged = SchemaMerger.merge(base, platform, business);
        assertNotNull(merged.getElements().get("new"));
        assertEquals("新能力", merged.getElements().get("new").getProps().get("text"));
        // 原始 business 不被污染
        assertNull(business.getElements().get("new"));
    }

    @Test
    void shouldNotOverwriteBusinessStructureChange() {
        PageSchema base = base();
        base.addElement("x", new SchemaNode("text"));
        base.addElement("y", new SchemaNode("text"));

        PageSchema platform = base();
        // 平台把 root children 改成 [btn, x]
        platform.getLayout().structureOf("root").setChildren(new java.util.ArrayList<>(java.util.List.of("btn", "x")));

        PageSchema business = base();
        // 业务把 root children 改成 [btn, y]
        business.getLayout().structureOf("root").setChildren(new java.util.ArrayList<>(java.util.List.of("btn", "y")));

        PageSchema merged = SchemaMerger.merge(base, platform, business);
        assertEquals(java.util.List.of("btn", "y"), merged.getLayout().getStructure().get("root").getChildren());
    }
}
