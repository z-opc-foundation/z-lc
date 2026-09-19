package com.zifang.z.lc.core.materialize.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.materialize.dto.MaterializationReq;
import com.zifang.z.lc.core.materialize.dto.MaterializationResp;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.core.materialize.mapper.MaterializationMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterializationService 单元测试
 * <p>
 * 仅覆盖不触发文件写入的安全路径: 参数校验 / 状态查询 / 列表查询.
 * run() 内部会写文件系统 (默认 user.home 根目录), 单元测试不执行该路径.
 */
public class MaterializationServiceTest {

    private MaterializationService service;

    @Before
    public void setUp() throws Exception {
        service = new MaterializationService();

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectById".equals(name)) {
                    return null;
                }
                if ("listByApp".equals(name)) {
                    return new java.util.ArrayList<>();
                }
                Class<?> rt = method.getReturnType();
                if (rt == int.class) {
                    return 0;
                }
                if (rt == boolean.class) {
                    return false;
                }
                if (List.class.isAssignableFrom(rt)) {
                    return new java.util.ArrayList<>();
                }
                if (Map.class.isAssignableFrom(rt)) {
                    return new ConcurrentHashMap<>();
                }
                return null;
            }
        };

        MaterializationMapper mapper = (MaterializationMapper) Proxy.newProxyInstance(
                MaterializationMapper.class.getClassLoader(),
                new Class<?>[]{MaterializationMapper.class, BaseMapper.class},
                handler);

        Field f = MaterializationService.class.getDeclaredField("materializationMapper");
        f.setAccessible(true);
        f.set(service, mapper);
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("MaterializationService 应当标注 @Service",
                MaterializationService.class.getAnnotation(Service.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerShouldRejectNullReq() {
        service.trigger(null, "t1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerShouldRejectNullAppCode() {
        MaterializationReq req = new MaterializationReq();
        req.setMaterializationPath("out/demo");
        service.trigger(req, "t1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerShouldRejectEmptyAppCode() {
        MaterializationReq req = new MaterializationReq();
        req.setAppCode("");
        req.setMaterializationPath("out/demo");
        service.trigger(req, "t1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerShouldRejectNullPath() {
        MaterializationReq req = new MaterializationReq();
        req.setAppCode("crm");
        service.trigger(req, "t1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void triggerShouldRejectEmptyPath() {
        MaterializationReq req = new MaterializationReq();
        req.setAppCode("crm");
        req.setMaterializationPath("");
        service.trigger(req, "t1");
    }

    @Test
    public void getStatusShouldReturnNullForMissing() {
        assertNull(service.getStatus(999L));
    }

    @Test
    public void listByAppShouldReturnEmptyList() {
        List<MaterializationResp> list = service.listByApp("t1", "crm", 10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void listFilesShouldReturnEmptyForMissing() {
        List<MaterializationResp.GeneratedFile> files = service.listFiles(999L);
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.materialize.service",
                MaterializationService.class.getPackage().getName());
    }
}