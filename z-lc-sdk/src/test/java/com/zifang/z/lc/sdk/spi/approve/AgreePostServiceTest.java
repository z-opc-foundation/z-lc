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
 * AgreePostService SPI 接口单元测试
 */
public class AgreePostServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AgreePostService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = AgreePostService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("同意后置处理", m.name());
        assertEquals("AgreePostService", m.code());
        assertEquals("审批", m.group());
    }

    @Test
    public void shouldDeclarePostAgreeMethod() throws NoSuchMethodException {
        Method m = AgreePostService.class.getMethod("postAgree",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldSucceed() {
        AgreePostService impl = new AgreePostService() {
            @Override
            public Result<Map<String, Object>> postAgree(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("approved", true);
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.postAgree(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData().get("approved"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.approve",
                AgreePostService.class.getPackage().getName());
    }
}