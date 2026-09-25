package com.zifang.z.lc.core.pipeline.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import com.zifang.z.lc.mapper.pipeline.PipelineConfigMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * PipelineConfigService 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 PipelineConfigMapper, 无需 Mockito / 数据库.
 * <p>
 * ⚠ 这一族的写入口校验是缺陷 #41 的一半 (另一半是执行面, 见 {@code PipelineWriteChainTest}):
 * 以前 create/update 什么都收, 收下来又没人执行, 于是"保存成功"和"真的在跑"是两件事。
 * 原来那条 {@code createShouldSetDefaultsAndStore} 用 triggerEvent="CREATE" 也能存,
 * 钉住的正是这个脱节, 已改成一份引擎兑现得了的配置。
 */
public class PipelineConfigServiceTest {

    private static final String VALID_STAGES = "[{\"type\":\"REQUIRED_CHECK\",\"order\":1},"
            + "{\"type\":\"TYPE_CONVERT\",\"order\":2},"
            + "{\"type\":\"VALUE_VALIDATE\",\"order\":3}]";

    private PipelineConfigService service;
    private Map<Long, PipelineConfigEntity> store;
    private AtomicLong idGen;

    @Before
    public void setUp() throws Exception {
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);
        service = wired(new PipelineConfigService(), store, idGen);
    }

    /** 把代理 mapper 注进 service (或它的子类) 的私有字段. */
    private static PipelineConfigService wired(PipelineConfigService target,
                                               final Map<Long, PipelineConfigEntity> store,
                                               final AtomicLong idGen) throws Exception {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    PipelineConfigEntity e = (PipelineConfigEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    PipelineConfigEntity e = (PipelineConfigEntity) args[0];
                    if (e.getId() != null && store.containsKey(e.getId())) {
                        store.put(e.getId(), e);
                        return 1;
                    }
                    return 0;
                }
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
                }
                if ("deleteById".equals(name)) {
                    return store.remove((Long) args[0]) != null ? 1 : 0;
                }
                if ("selectList".equals(name) || "selectCount".equals(name)
                        || "selectOne".equals(name) || "selectMap".equals(name)) {
                    return new ArrayList<>();
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                if (List.class.isAssignableFrom(rt)) {
                    return new ArrayList<>();
                }
                if (Map.class.isAssignableFrom(rt)) {
                    return new ConcurrentHashMap<>();
                }
                return null;
            }
        };

        PipelineConfigMapper mapper = (PipelineConfigMapper) Proxy.newProxyInstance(
                PipelineConfigMapper.class.getClassLoader(),
                new Class<?>[]{PipelineConfigMapper.class, BaseMapper.class},
                handler);

        Field f = PipelineConfigService.class.getDeclaredField("pipelineConfigMapper");
        f.setAccessible(true);
        f.set(target, mapper);
        return target;
    }

    private static PipelineConfigEntity valid() {
        PipelineConfigEntity e = new PipelineConfigEntity();
        e.setAppCode("crm");
        e.setEntityCode("order");
        e.setTriggerEvent(PipelineStages.BEFORE_CREATE);
        e.setStages(VALID_STAGES);
        return e;
    }

    private static String expectReject(Runnable call, String... mustName) {
        try {
            call.run();
            fail("写入口应当拒掉这份配置");
            return null;
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            for (String needle : mustName) {
                assertTrue("拒绝消息要点名 [" + needle + "], 实际: " + msg, msg.contains(needle));
            }
            return msg;
        }
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("PipelineConfigService 应当标注 @Service",
                PipelineConfigService.class.getAnnotation(Service.class));
    }

    @Test
    public void createShouldSetDefaultsAndStore() {
        PipelineConfigEntity e = valid();

        PipelineConfigEntity result = service.create(e);

        assertSame(e, result);
        assertNotNull(result.getId());
        assertEquals(Integer.valueOf(0), result.getDeleted());
        assertNotNull(result.getCreateTime());
        assertEquals(1, store.size());
    }

    @Test
    public void createShouldDefaultEnabledInsteadOfStoringADeadRow() {
        // enabled 留 null 的话 listByEvent 的 eq("enabled",1) 永远查不到它 —— 界面说已保存, 运行期什么都不跑
        PipelineConfigEntity e = valid();
        e.setEnabled(null);
        service.create(e);
        assertEquals(Integer.valueOf(1), e.getEnabled());
    }

    @Test
    public void createRejectsAnEnabledFlagThatIsNotZeroOrOne() {
        // listByEvent 只认 enabled=1: 存成 2 的这一行永远查不到, 而界面上一切正常
        final PipelineConfigEntity e = valid();
        e.setEnabled(2);
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        }, "enabled", "1(启用)");
        assertEquals("被拒的配置不该留下一行死数据", 0, store.size());
    }

    @Test
    public void updateShouldRefreshTime() {
        PipelineConfigEntity e = valid();
        service.create(e);
        Long id = e.getId();

        PipelineConfigEntity result = service.update(e);

        assertSame(e, result);
        assertNotNull(result.getUpdateTime());
        assertEquals(id, result.getId());
    }

    @Test
    public void deleteShouldSoftDeleteExisting() {
        PipelineConfigEntity e = valid();
        service.create(e);

        int n = service.delete(e.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), e.getDeleted());
    }

    @Test
    public void deleteShouldReturnZeroForMissingId() {
        int n = service.delete(999L);
        assertEquals(0, n);
    }

    @Test
    public void toggleEnabledShouldSetOneWhenTrue() {
        PipelineConfigEntity e = valid();
        e.setEnabled(0);
        service.create(e);

        int n = service.toggleEnabled(e.getId(), true);

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), e.getEnabled());
    }

    @Test
    public void toggleEnabledShouldSetZeroWhenFalse() {
        PipelineConfigEntity e = valid();
        e.setEnabled(1);
        service.create(e);

        int n = service.toggleEnabled(e.getId(), false);

        assertEquals(1, n);
        assertEquals(Integer.valueOf(0), e.getEnabled());
    }

    @Test
    public void toggleEnabledShouldRejectMissingIdInsteadOfReportingSuccess() {
        // 控制器过去无条件 return success(true): 一次漏带 id 的请求会"切换成功"而什么都没改
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.toggleEnabled(null, true);
            }
        }, "id");
    }

    @Test
    public void toggleEnabledShouldRejectUnknownIdInsteadOfReportingSuccess() {
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.toggleEnabled(424242L, true);
            }
        }, "424242", "没人被改动");
    }

    @Test
    public void togglingOnALegacyBrokenConfigIsRejected() {
        // 校验是后加的, 库里可能躺着一条打不开的配置; 开关必须把它拦在"启用"之前
        final PipelineConfigEntity e = valid();
        service.create(e);
        e.setStages("[{\"type\":\"DICT_RESOLVE\",\"order\":1}]");
        e.setEnabled(0);

        expectReject(new Runnable() {
            @Override
            public void run() {
                service.toggleEnabled(e.getId(), true);
            }
        }, PipelineStages.REQUIRED_CHECK, "必填阶段");
        assertEquals("被拒的开关不许把 enabled 写下去", Integer.valueOf(0), e.getEnabled());
    }

    @Test
    public void listByAppShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByApp("crm");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEntityShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByEntity("crm", "order");
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void listByEventShouldReturnEmptyListInitially() {
        List<PipelineConfigEntity> list = service.listByEvent("crm", "order", PipelineStages.BEFORE_CREATE);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.pipeline.config",
                PipelineConfigService.class.getPackage().getName());
    }

    // ===== 写入口对"引擎兑现不了的东西"说不 =====

    @Test
    public void createRejectsTriggerWithoutAHookPoint() {
        final PipelineConfigEntity e = valid();
        e.setTriggerEvent("AFTER_CREATE");
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        }, "AFTER_CREATE", "没有挂接点");
        assertEquals("被拒的配置一行都不该落库", 0, store.size());
    }

    @Test
    public void createRejectsStageWithoutAnExecutor() {
        final PipelineConfigEntity e = valid();
        e.setStages(VALID_STAGES.substring(0, VALID_STAGES.length() - 1)
                + ",{\"type\":\"WEBHOOK\",\"order\":4}]");
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        }, "WEBHOOK", "没有执行器");
        assertEquals(0, store.size());
    }

    @Test
    public void createRejectsAChainThatWouldBypassTheWriteGates() {
        final PipelineConfigEntity e = valid();
        e.setStages("[{\"type\":\"REF_CHECK\",\"order\":1}]");
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        }, PipelineStages.TYPE_CONVERT, PipelineStages.VALUE_VALIDATE, PipelineStages.REQUIRED_CHECK);
        assertEquals(0, store.size());
    }

    @Test
    public void createRejectsAConfigThatCanNeverBeFound() {
        // appCode / entityCode 是 listByEvent 的键; 缺了它们这条配置永远不会被执行,
        // 而界面会说"已保存"
        final PipelineConfigEntity noApp = valid();
        noApp.setAppCode("  ");
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(noApp);
            }
        }, "appCode");

        final PipelineConfigEntity noEntity = valid();
        noEntity.setEntityCode(null);
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(noEntity);
            }
        }, "entityCode");
        assertEquals(0, store.size());
    }

    @Test
    public void createRejectsASecondEnabledConfigOnTheSameHookPoint() throws Exception {
        Stubbed service = new Stubbed();
        wired(service, store, idGen);
        service.enabledElsewhere = Arrays.asList(existingRow(7L));

        final PipelineConfigEntity e = valid();
        expectReject(new Runnable() {
            @Override
            public void run() {
                service.create(e);
            }
        }, "id=7", "一个挂接点只跑一条链");

        // 关掉的那份不算占位
        service.enabledElsewhere = new ArrayList<PipelineConfigEntity>();
        assertNotNull(service.create(e).getId());
    }

    @Test
    public void updateDoesNotCollideWithItself() throws Exception {
        Stubbed service = new Stubbed();
        wired(service, store, idGen);
        PipelineConfigEntity mine = existingRow(3L);
        service.store(mine);
        service.enabledElsewhere = Arrays.asList(mine);

        mine.setStages("[{\"type\":\"TYPE_CONVERT\",\"order\":1},"
                + "{\"type\":\"REQUIRED_CHECK\",\"order\":2},"
                + "{\"type\":\"VALUE_VALIDATE\",\"order\":3}]");
        assertNotNull(service.update(mine));
    }

    @Test
    public void disabledConfigsMayCoexistOnTheSameHookPoint() throws Exception {
        Stubbed service = new Stubbed();
        wired(service, store, idGen);
        service.enabledElsewhere = Arrays.asList(existingRow(7L));
        PipelineConfigEntity e = valid();
        e.setEnabled(0);
        assertNotNull(service.create(e).getId());
    }

    private static PipelineConfigEntity existingRow(long id) {
        PipelineConfigEntity e = valid();
        e.setId(id);
        e.setEnabled(1);
        return e;
    }

    /** 只改"同一挂接点上已有多少份启用配置"这一个查询, 其余走真实实现. */
    private static final class Stubbed extends PipelineConfigService {
        List<PipelineConfigEntity> enabledElsewhere = new ArrayList<PipelineConfigEntity>();
        private final Map<Long, PipelineConfigEntity> rows = new ConcurrentHashMap<>();

        void store(PipelineConfigEntity e) {
            rows.put(e.getId(), e);
        }

        @Override
        public PipelineConfigEntity create(PipelineConfigEntity entity) {
            PipelineConfigEntity created = super.create(entity);
            store(created);
            return created;
        }

        @Override
        public List<PipelineConfigEntity> listByEvent(String appCode, String entityCode, String triggerEvent) {
            return new ArrayList<>(enabledElsewhere);
        }
    }
}
