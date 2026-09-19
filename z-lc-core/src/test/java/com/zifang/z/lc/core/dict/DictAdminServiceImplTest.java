package com.zifang.z.lc.core.dict;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.core.dict.entity.DictEntity;
import com.zifang.z.lc.core.dict.entity.DictItemEntity;
import com.zifang.z.lc.mapper.dict.DictItemMapper;
import com.zifang.z.lc.mapper.dict.DictMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DictAdminServiceImpl 单元测试
 * <p>
 * 通过 JDK 动态代理 + 内存 Map 模拟 DictMapper / DictItemMapper, 无需 Mockito / 数据库.
 */
public class DictAdminServiceImplTest {

    private DictAdminServiceImpl service;
    private Map<Long, DictEntity> dictStore;
    private Map<Long, DictItemEntity> itemStore;
    private AtomicLong dictIdGen;
    private AtomicLong itemIdGen;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        service = new DictAdminServiceImpl();
        dictStore = new ConcurrentHashMap<>();
        itemStore = new ConcurrentHashMap<>();
        dictIdGen = new AtomicLong(0);
        itemIdGen = new AtomicLong(0);

        InvocationHandler dictHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return handleEntityOp(dictStore, dictIdGen, method, args);
            }
        };
        InvocationHandler itemHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return handleEntityOp(itemStore, itemIdGen, method, args);
            }
        };

        DictMapper dictMapper = (DictMapper) Proxy.newProxyInstance(
                DictMapper.class.getClassLoader(),
                new Class<?>[]{DictMapper.class, BaseMapper.class},
                dictHandler);
        DictItemMapper itemMapper = (DictItemMapper) Proxy.newProxyInstance(
                DictItemMapper.class.getClassLoader(),
                new Class<?>[]{DictItemMapper.class, BaseMapper.class},
                itemHandler);

        setField(service, "dictMapper", dictMapper);
        setField(service, "dictItemMapper", itemMapper);
    }

    @SuppressWarnings("unchecked")
    private static Object handleEntityOp(Map<Long, ?> store, AtomicLong idGen,
                                         Method method, Object[] args) {
        String name = method.getName();
        if ("insert".equals(name)) {
            Object e = args[0];
            Long id = idGen.incrementAndGet();
            try {
                Method setId = e.getClass().getMethod("setId", Long.class);
                setId.invoke(e, id);
            } catch (Exception ignore) {
                // id setter 不存在时忽略
            }
            ((Map<Long, Object>) store).put(id, e);
            return 1;
        }
        if ("updateById".equals(name)) {
            Object e = args[0];
            try {
                Long id = (Long) e.getClass().getMethod("getId").invoke(e);
                if (id != null && store.containsKey(id)) {
                    ((Map<Long, Object>) store).put(id, e);
                    return 1;
                }
            } catch (Exception ignore) {
                // fall through
            }
            return 0;
        }
        if ("selectById".equals(name)) {
            return store.get((Long) args[0]);
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

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = DictAdminServiceImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("DictAdminServiceImpl 应当标注 @Service",
                DictAdminServiceImpl.class.getAnnotation(Service.class));
    }

    @Test
    public void shouldImplementDictAdminService() {
        assertTrue("DictAdminServiceImpl 应当实现 DictAdminService",
                DictAdminService.class.isAssignableFrom(DictAdminServiceImpl.class));
    }

    @Test
    public void createDictShouldSetDefaults() {
        DictDTO req = new DictDTO();
        req.setDictCode("gender");
        req.setDictName("性别");

        DictDTO dto = service.createDict(req);

        assertNotNull(dto);
        assertEquals("gender", dto.getDictCode());
        assertEquals("default", dto.getTenantCode());
        assertNotNull(dto.getCreateTime());
        assertEquals(1, dictStore.size());
        assertEquals(Integer.valueOf(0), dictStore.get(dto.getId()).getDeleted());
    }

    @Test
    public void createDictShouldKeepExplicitTenant() {
        DictDTO req = new DictDTO();
        req.setTenantCode("t1");

        DictDTO dto = service.createDict(req);

        assertEquals("t1", dto.getTenantCode());
    }

    @Test
    public void updateDictShouldReturnNullForMissing() {
        DictDTO req = new DictDTO();
        req.setId(999L);
        assertNull(service.updateDict(req));
    }

    @Test
    public void updateDictShouldMergeFields() {
        DictDTO req = new DictDTO();
        req.setDictCode("gender");
        DictDTO created = service.createDict(req);

        DictDTO update = new DictDTO();
        update.setId(created.getId());
        update.setDictName("性别 (新)");
        update.setDescription("性别字典");

        DictDTO updated = service.updateDict(update);

        assertNotNull(updated);
        assertEquals("性别 (新)", updated.getDictName());
        assertEquals("性别字典", updated.getDescription());
    }

    @Test
    public void deleteDictShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteDict(999L));
    }

    @Test
    public void deleteDictShouldSoftDelete() {
        DictDTO req = new DictDTO();
        DictDTO created = service.createDict(req);

        int n = service.deleteDict(created.getId());

        assertEquals(1, n);
        assertEquals(Integer.valueOf(1), dictStore.get(created.getId()).getDeleted());
    }

    @Test
    public void listDictsShouldReturnEmptyList() {
        List<DictDTO> list = service.listDicts("t1");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void getDictByCodeShouldReturnNullForMissing() {
        assertNull(service.getDictByCode("t1", "gender"));
    }

    @Test
    public void saveItemsShouldInsertAll() {
        DictItemDTO item1 = new DictItemDTO();
        item1.setItemLabel("男");
        item1.setItemValue("M");
        DictItemDTO item2 = new DictItemDTO();
        item2.setItemLabel("女");
        item2.setItemValue("F");

        List<DictItemDTO> result = service.saveItems(
                "t1", "gender", Arrays.asList(item1, item2));

        assertNotNull(result);
        assertEquals(2, itemStore.size());
    }

    @Test
    public void saveItemsShouldHandleEmptyList() {
        List<DictItemDTO> result = service.saveItems("t1", "gender", Collections.<DictItemDTO>emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void listItemsShouldReturnEmptyList() {
        List<DictItemDTO> list = service.listItems("t1", "gender");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void deleteItemShouldReturnZeroForMissing() {
        assertEquals(0, service.deleteItem(999L));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.dict",
                DictAdminServiceImpl.class.getPackage().getName());
    }
}