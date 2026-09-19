package com.zifang.z.lc.core.ai;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AiModelingService 单元测试
 * <p>
 * 覆盖 LLM Gateway 缺失时的模板 fallback 路径 (llmGatewayServiceRaw 保持 null).
 * LLM 真实调用路径由集成测试覆盖.
 */
public class AiModelingServiceTest {

    private AiModelingService service;

    @Before
    public void setUp() {
        // llmGatewayServiceRaw 为 null (未注入), 走 fallback 路径
        service = new AiModelingService();
    }

    @Test
    public void shouldBeAnnotatedWithService() {
        assertNotNull("AiModelingService 应当标注 @Service",
                AiModelingService.class.getAnnotation(Service.class));
    }

    @Test
    public void generateModelSuggestionShouldFallbackWithoutLlm() {
        Map<String, Object> suggestion = service.generateModelSuggestion("crm", "创建一个订单管理");

        assertNotNull(suggestion);
        assertEquals("crm", suggestion.get("appCode"));
        assertEquals(Boolean.FALSE, suggestion.get("llmAvailable"));
        assertEquals(Boolean.TRUE, suggestion.get("needsReview"));
        assertEquals(0.0, ((Number) suggestion.get("confidence")).doubleValue(), 0.0001);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void generateModelSuggestionShouldReturnEmptySuggestions() {
        Map<String, Object> suggestion = service.generateModelSuggestion("crm", "库存系统");

        List<String> entities = (List<String>) suggestion.get("suggestedEntities");
        List<String> relations = (List<String>) suggestion.get("suggestedRelations");
        assertNotNull(entities);
        assertNotNull(relations);
        assertTrue(entities.isEmpty());
        assertTrue(relations.isEmpty());
    }

    @Test
    public void generateModelSuggestionShouldKeepDescription() {
        Map<String, Object> suggestion = service.generateModelSuggestion("crm", "员工考勤");

        assertEquals("员工考勤", suggestion.get("description"));
    }

    @Test
    public void generateModelSuggestionShouldHandleNullDescription() {
        Map<String, Object> suggestion = service.generateModelSuggestion("crm", null);

        assertNotNull(suggestion);
        assertFalse((Boolean) suggestion.get("llmAvailable"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.ai",
                AiModelingService.class.getPackage().getName());
    }
}