package com.zifang.z.lc.core.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;
import com.zifang.z.lc.core.relation.entity.RelationEntity;
import com.zifang.z.lc.mapper.relation.RelationMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * RelationServiceImpl 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 RelationMapper, 无需 Mockito / 数据库.
 */
public class RelationServiceImplTest {

    private RelationServiceImpl service;
    private Map<Long, RelationEntity> store;
    private AtomicLong idGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new RelationServiceImpl();
        store = new ConcurrentHashMap<>();
        idGen = new AtomicLong(0);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("insert".equals(name)) {
                    RelationEntity e = (RelationEntity) args[0];
                    if (e.getId() == null) {
                        e.setId(idGen.incrementAndGet());
                    }
                    store.put(e.getId(), e);
                    return 1;
                }
                if ("updateById".equals(name)) {
                    RelationEntity e = (RelationEntity) args[0];
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

        RelationMapper mapper = (RelationMapper) Proxy.newProxyInstance(
                RelationMapper.class.getClassLoader(),
                new Class<?>[]{RelationMapper.class, BaseMapper.class},
                handler);

        Field f = RelationServiceImpl.class.getDeclaredField("relationMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("RelationServiceImpl 应当标注 @Service",
                RelationServiceImpl.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementRelationService() {
        assertTrue("RelationServiceImpl 应当实现 RelationService",
                RelationService.class.isAssignableFrom(RelationServiceImpl.class));
    }

    @Test
    public void createRelationShouldSetDefaults() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode("rel-order-customer");
        req.setSourceEntityCode("order");
        req.setTargetEntityCode("customer");
        req.setRelationType("MANY_TO_ONE");

        RelationDTO dto = service.createRelation(req);

        assertNotNull(dto);
        assertEquals("rel-order-customer", dto.getRelationCode());
        assertEquals("default", dto.getTenantCode());
        assertNotNull(dto.getCreateTime());
        assertEquals(1, store.size());
        assertEquals(Integer.valueOf(0), store.get(dto.getId()).getDeleted());
    }

    @Test
    public void createRelationShouldKeepExplicitTenant() {
        RelationCreateReq req = new RelationCreateReq();
        req.setTenantCode("t1");

        RelationDTO dto = service.createRelation(req);

        assertEquals("t1", dto.getTenantCode());
    }

    @Test
    public void updateRelationShouldReturnNullForMissing() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setId(999L);
        assertNull(service.updateRelation(req));
    }

    @Test
    public void updateRelationShouldMergeFields() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode("rel-1");
        RelationDTO created = service.createRelation(req);

        RelationUpdateReq update = new RelationUpdateReq();
        update.setId(created.getId());
        update.setRelationName("新名称");
        update.setRelationType("ONE_TO_MANY");
        update.setSourceFieldCode("customer_id");
        update.setThroughTable("lc_rel");

        RelationDTO updated = service.updateRelation(update);

        assertNotNull(updated);
        assertEquals("新名称", updated.getRelationName());
        assertEquals("ONE_TO_MANY", updated.getRelationType());
        assertEquals("customer_id", updated.getSourceFieldCode());
        assertEquals("lc_rel", updated.getThroughTable());
    }

    @Test
    public void deleteRelationShouldSoftDelete() {
        RelationCreateReq req = new RelationCreateReq();
        RelationDTO created = service.createRelation(req);

        int n = service.deleteRelation(created.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), store.get(created.getId()).getDeleted());
    }

    @Test
    public void deleteRelationShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteRelation(999L));
    }

    @Test
    public void getRelationShouldReturnNullForMissing() {
        assertNull(service.getRelation(999L));
    }

    @Test
    public void getRelationShouldReturnDto() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode("rel-x");
        RelationDTO created = service.createRelation(req);

        RelationDTO found = service.getRelation(created.getId());

        assertNotNull(found);
        assertEquals("rel-x", found.getRelationCode());
    }

    @Test
    public void listRelationsByEntityShouldReturnEmptyList() {
        List<RelationDTO> list = service.listRelationsByEntity("t1", "crm", "order");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void listRelationsByAppShouldReturnEmptyList() {
        List<RelationDTO> list = service.listRelationsByApp("t1", "crm");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.relation",
                RelationServiceImpl.class.getPackage().getName());
    }
}