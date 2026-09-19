package com.zifang.z.lc.sdk.spi.approve;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AgreePreService SPI 接口单元测试
 */
public class AgreePreServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AgreePreService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = AgreePreService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("同意前置处理", m.name());
        assertEquals("AgreePreService", m.code());
        assertEquals("审批", m.group());
    }

    @Test
    public void shouldDeclarePreAgreeMethod() throws NoSuchMethodException {
        Method m = AgreePreService.class.getMethod("preAgree",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldAddComment() {
        AgreePreService impl = new AgreePreService() {
            @Override
            public Result<Map<String, Object>> preAgree(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("comment", "agreed");
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.preAgree(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("agreed", r.getData().get("comment"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.approve",
                AgreePreService.class.getPackage().getName());
    }
}