package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * FormDataQueryPostHandlerService SPI 接口单元测试
 */
public class FormDataQueryPostHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataQueryPostHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataQueryPostHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单查询后置处理", m.name());
        assertEquals("FormDataQueryPostHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePostHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataQueryPostHandlerService.class.getMethod("postHandler",
                ExtensionServiceContext.class, List.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldEnrichRows() {
        FormDataQueryPostHandlerService impl = new FormDataQueryPostHandlerService() {
            @Override
            public Result<List<Map<String, Object>>> postHandler(ExtensionServiceContext context, List<Map<String, Object>> rows) {
                if (rows == null) rows = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    row.put("processed", true);
                }
                return Result.success(rows);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new HashMap<>());
        Result<List<Map<String, Object>>> r = impl.postHandler(ctx, rows);
        assertTrue(r.isSuccess());
        assertEquals(1, r.getData().size());
        assertEquals(Boolean.TRUE, r.getData().get(0).get("processed"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataQueryPostHandlerService.class.getPackage().getName());
    }
}