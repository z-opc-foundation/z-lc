package com.zifang.z.lc.core.page;

import com.zifang.z.lc.common.dto.PageCreateReq;
import com.zifang.z.lc.common.dto.PageDTO;
import com.zifang.z.lc.common.dto.PageUpdateReq;
import com.zifang.z.lc.core.page.entity.PageEntity;
import com.zifang.z.lc.mapper.page.PageMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「设计器保存 → 校验 → 落库」这一条链的回归测试。
 *
 * <p><b>为什么用 Proxy 伪造 Mapper 而不是 Mockito</b>：z-lc-core 的测试作用域里没有
 * Mockito（全仓只有 JUnit 5），为这一个用例引一套 mock 框架不值。{@code PageMapper}
 * 是接口，JDK 动态代理足够记录「有没有调 insert / 传进去的实体长什么样」。</p>
 *
 * <p><b>补的是什么洞</b>：端到端用例只能证明「设计器点保存确实把请求打到了
 * {@code /api/lc/page}」——它被 SSO 拦在 302，走不到 Controller。运行态读页面能通，
 * 是因为数据由 seed 脚本直接写库。也就是说「Controller → 校验 → 写 z_lc_page」
 * 这一段此前没有任何自动化覆盖。这里从 Service 层把这段钉死。</p>
 */
class PageSaveChainTest {

    /** 设计器真实产出的 canonical schema（与运行态 DEMO 页面同形）。 */
    private static final String DESIGNER_SCHEMA = """
            {
              "version": "1.0",
              "elements": {
                "root": {"type": "container", "props": {"direction": "column", "gap": 16}},
                "t1":   {"type": "title", "props": {"text": "工单管理", "level": 2}},
                "tb1":  {"type": "table", "props": {"columns": [{"field": "no", "title": "编号"}]}}
              },
              "layout": {
                "root": "root",
                "mode": "flow",
                "structure": {"root": {"children": ["t1", "tb1"], "namedSlots": {}}}
              }
            }
            """;

    /** 旧报表写法：同一模型的扁平淡法，保存时也必须被接受。 */
    private static final String LEGACY_REPORT_SCHEMA = """
            {
              "widgets": [
                {"id": "w1", "type": "KPI", "title": "待处理", "datasetId": "ds1"},
                {"id": "w2", "type": "TABLE", "title": "工单", "datasetId": "ds1"}
              ],
              "datasets": {
                "ds1": {"sourceId": "tickets", "baseTable": "z_lc_ticket"}
              },
              "layout": [
                {"widgetId": "w1", "x": 0, "y": 0, "w": 12, "h": 4},
                {"widgetId": "w2", "x": 0, "y": 4, "w": 12, "h": 8}
              ]
            }
            """;

    // ------------------------------------------------------------------ 新增

    @Test
    void createPage_persistsDesignerSchemaAsIs() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageBizService service = serviceWith(fake.proxy());

        PageCreateReq req = new PageCreateReq();
        req.setAppCode("tickets");
        req.setPageCode("list");
        req.setPageName("工单列表");
        req.setSchema(DESIGNER_SCHEMA);

        PageDTO dto = service.createPage(req);

        assertEquals(1, fake.inserted.size(), "create 必须真的写一次库");
        PageEntity saved = fake.inserted.get(0);

        // schema 原文入库，不在保存时做格式改写 —— 否则设计器读回来的东西和存的不一样
        assertEquals(DESIGNER_SCHEMA, saved.getSchemaJson());
        assertEquals("tickets", saved.getAppCode());
        assertEquals("list", saved.getPageCode());
        assertEquals("工单列表", saved.getPageName());
        // 租户与软删标记由服务端兜底，前端传不传都得对
        assertEquals("default", saved.getTenantCode());
        assertEquals(0, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());

        // 返回给前端的 DTO 里 schema 走的是 schemaJson（同一个字段的两个名字）
        assertNotNull(dto);
        assertEquals(DESIGNER_SCHEMA, dto.getSchema());
    }

    @Test
    void createPage_acceptsLegacyReportSchema() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageBizService service = serviceWith(fake.proxy());

        PageCreateReq req = new PageCreateReq();
        req.setAppCode("report");
        req.setPageCode("kpi");
        req.setSchema(LEGACY_REPORT_SCHEMA);

        service.createPage(req);

        assertEquals(1, fake.inserted.size());
        // 旧写法原样入库：归一化发生在读取侧（WidgetSchemaAdapter），保存侧不偷换成另一种形态
        assertEquals(LEGACY_REPORT_SCHEMA, fake.inserted.get(0).getSchemaJson());
    }

    @Test
    void createPage_rejectsBrokenSchemaWithoutTouchingDb() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageBizService service = serviceWith(fake.proxy());

        PageCreateReq req = new PageCreateReq();
        req.setAppCode("tickets");
        req.setPageCode("bad");
        req.setSchema("{\"foo\": 1}");

        assertThrows(IllegalArgumentException.class, () -> service.createPage(req));
        assertTrue(fake.inserted.isEmpty(), "坏结构必须在校验就止住，不能落库");
    }

    /**
     * 统一 schema 不等于放宽旧写法的要求：报表页的取数规则（widget 必须绑数据集、
     * 引用的数据集必须有定义）在被归一化之前仍然要拦住。
     */
    @Test
    void createPage_stillEnforcesLegacyDatasetBinding() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageBizService service = serviceWith(fake.proxy());

        PageCreateReq req = new PageCreateReq();
        req.setAppCode("report");
        req.setPageCode("loose");
        // 去掉 datasets 定义，widget 的 datasetId 就悬空了
        req.setSchema(LEGACY_REPORT_SCHEMA.replaceAll(
                "\\s*\"datasets\"\\s*:\\s*\\{[\\s\\S]*?\\},\\s*\"layout\"", "\n              \"layout\""));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createPage(req));
        assertTrue(e.getMessage().contains("dataset"), "报错应指向数据集绑定: " + e.getMessage());
        assertTrue(fake.inserted.isEmpty());
    }

    @Test
    void createPage_rejectsDuplicate() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageEntity existing = new PageEntity();
        existing.setId(7L);
        existing.setDeleted(0);
        fake.selectOneResult = existing;

        PageBizService service = serviceWith(fake.proxy());

        PageCreateReq req = new PageCreateReq();
        req.setAppCode("tickets");
        req.setPageCode("list");
        req.setSchema(DESIGNER_SCHEMA);

        assertThrows(IllegalArgumentException.class, () -> service.createPage(req));
        assertTrue(fake.inserted.isEmpty(), "重复页面不该插第二条");
    }

    // ------------------------------------------------------------------ 更新

    @Test
    void updatePage_overwritesSchemaAndName() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageEntity existing = new PageEntity();
        existing.setId(1L);
        existing.setAppCode("tickets");
        existing.setPageCode("list");
        existing.setPageName("旧名字");
        existing.setSchemaJson("{\"elements\":{}}");
        existing.setTenantCode("default");
        existing.setDeleted(0);
        fake.selectByIdResult = existing;

        PageBizService service = serviceWith(fake.proxy());

        PageUpdateReq req = new PageUpdateReq();
        req.setId(1L);
        req.setPageName("工单列表");
        req.setSchema(DESIGNER_SCHEMA);

        PageDTO dto = service.updatePage(req);

        assertEquals(1, fake.updated.size(), "update 必须真的写一次库");
        PageEntity saved = fake.updated.get(0);
        assertEquals(DESIGNER_SCHEMA, saved.getSchemaJson());
        assertEquals("工单列表", saved.getPageName());
        assertNotNull(saved.getUpdateTime());
        assertNotNull(dto);
        assertEquals(DESIGNER_SCHEMA, dto.getSchema());
    }

    @Test
    void updatePage_rejectsBrokenSchemaWithoutTouchingDb() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageEntity existing = new PageEntity();
        existing.setId(1L);
        existing.setDeleted(0);
        fake.selectByIdResult = existing;

        PageBizService service = serviceWith(fake.proxy());

        PageUpdateReq req = new PageUpdateReq();
        req.setId(1L);
        req.setSchema("{\"elements\": \"not-an-object\"}");

        assertThrows(IllegalArgumentException.class, () -> service.updatePage(req));
        assertTrue(fake.updated.isEmpty(), "坏结构不能把线上已有 schema 覆盖掉");
    }

    @Test
    void updatePage_missingIdRejected() throws Exception {
        FakeMapper fake = new FakeMapper();
        PageBizService service = serviceWith(fake.proxy());

        PageUpdateReq req = new PageUpdateReq();
        req.setSchema(DESIGNER_SCHEMA);

        assertThrows(IllegalArgumentException.class, () -> service.updatePage(req));
        assertTrue(fake.updated.isEmpty());
    }

    // ------------------------------------------------------------------ 帮扶

    private static PageBizService serviceWith(PageMapper mapper) throws Exception {
        PageBizService service = new PageBizService();
        Field f = PageBizService.class.getDeclaredField("pageMapper");
        f.setAccessible(true);
        f.set(service, mapper);
        return service;
    }

    /**
     * 记录调用的假 Mapper。只实现本用例真正会走到的方法，其余一律返回 null ——
     * 真走到未实现的方法会直接 NPE 炸出来，比静默返回更安全。
     */
    static final class FakeMapper {
        final List<PageEntity> inserted = new ArrayList<>();
        final List<PageEntity> updated = new ArrayList<>();
        PageEntity selectOneResult;
        PageEntity selectByIdResult;
        List<PageEntity> selectListResult = new ArrayList<>();
        int deleteByIdCalls;

        PageMapper proxy() {
            Map<String, java.util.function.Function<Object[], Object>> handlers = new HashMap<>();
            handlers.put("insert", args -> {
                inserted.add((PageEntity) args[0]);
                return 1;
            });
            handlers.put("updateById", args -> {
                updated.add((PageEntity) args[0]);
                return 1;
            });
            handlers.put("selectOne", args -> selectOneResult);
            handlers.put("selectById", args -> selectByIdResult);
            handlers.put("selectList", args -> selectListResult);
            handlers.put("deleteById", args -> {
                deleteByIdCalls++;
                return 1;
            });

            return (PageMapper) Proxy.newProxyInstance(
                    PageMapper.class.getClassLoader(),
                    new Class<?>[]{PageMapper.class},
                    (p, method, args) -> {
                        if ("toString".equals(method.getName())) return "FakePageMapper";
                        if ("hashCode".equals(method.getName())) return System.identityHashCode(p);
                        if ("equals".equals(method.getName())) return p == args[0];
                        java.util.function.Function<Object[], Object> h = handlers.get(method.getName());
                        if (h == null) {
                            throw new UnsupportedOperationException(
                                    "FakeMapper 未实现方法: " + method.getName() + "（用例走偏了，请补上）");
                        }
                        return h.apply(args);
                    });
        }
    }

}
