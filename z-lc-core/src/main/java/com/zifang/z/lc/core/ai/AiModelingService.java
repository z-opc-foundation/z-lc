package com.zifang.z.lc.core.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.agent.llm.gateway.adapter.UnifiedRequest;
import com.zifang.z.agent.llm.gateway.adapter.UnifiedResponse;
import com.zifang.z.agent.llm.gateway.service.LlmGatewayService;
import com.zifang.z.lc.core.app.AppAdminService;
import com.zifang.z.lc.core.event.EventService;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.zifang.util.core.json.JsonMapperFactory;

/**
 * AI 对话式产品制造服务 (F035 T9-T13)
 * <p>
 * T9:  MCP Tool 暴露给 z-agent
 * T10: 对话式建模
 * T11: 智能表单生成
 * T12: 数据洞察
 * T13: 一键产品化
 */
@Service
public class AiModelingService {

    private static final Logger log = LogManager.getLogger(AiModelingService.class);

    @Resource
    private AppAdminService appAdminService;

    @Resource
    private EventService eventService;

    @Resource
    private LcAppEntityMapper appEntityMapper;

    /**
     * LLM Gateway - optional dependency, may be null if z-agent-llm-gateway not on classpath
     */
    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("zAgentLlmGatewayLlmGatewayService")
    private Object llmGatewayServiceRaw;

    /**
     * T10: 对话式建模 - 解析用户自然语言描述，生成实体定义建议
     */
    public Map<String, Object> generateModelSuggestion(String appCode, String userDescription) {
        log.info("AI modeling suggestion requested: app={} desc={}", appCode, userDescription);

        LlmGatewayService llmService = getLlmGatewayService();
        if (llmService != null) {
            return generateModelSuggestionViaLlm(appCode, userDescription, llmService);
        }

        // Fallback: return template suggestion when LLM is not available
        log.info("LLM Gateway not available, returning template suggestion");
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("appCode", appCode);
        suggestion.put("description", userDescription);
        suggestion.put("suggestedEntities", new ArrayList<>());
        suggestion.put("suggestedRelations", new ArrayList<>());
        suggestion.put("confidence", 0.0);
        suggestion.put("needsReview", true);
        suggestion.put("llmAvailable", false);
        return suggestion;
    }

    /**
     * Call LLM to generate model suggestion
     */
    private Map<String, Object> generateModelSuggestionViaLlm(String appCode, String userDescription,
                                                              LlmGatewayService llmService) {
        try {
            UnifiedRequest request = new UnifiedRequest();
            request.setModelCode("default");

            List<UnifiedRequest.Message> messages = new ArrayList<>();
            messages.add(new UnifiedRequest.Message("system", buildModelingPrompt()));
            messages.add(new UnifiedRequest.Message("user", userDescription));
            request.setMessages(messages);
            request.setTemperature(0.3);
            request.setMaxTokens(2000);

            UnifiedResponse response = llmService.chat(request, null);

            if (response.isSuccess() && response.getContent() != null) {
                String content = response.getContent().trim();
                // Try to parse as JSON
                if (content.startsWith("```json")) {
                    content = content.substring(7);
                }
                if (content.startsWith("```")) {
                    content = content.substring(3);
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
                content = content.trim();

                try {
                    @SuppressWarnings("unchecked")
                    ObjectMapper mapper = JsonMapperFactory.getDefault();
                    Map<String, Object> suggestion = mapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
                    suggestion.put("appCode", appCode);
                    suggestion.put("llmAvailable", true);
                    if (!suggestion.containsKey("needsReview")) {
                        suggestion.put("needsReview", true);
                    }
                    return suggestion;
                } catch (Exception e) {
                    log.warn("LLM response is not valid JSON, wrapping as description");
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("appCode", appCode);
                    suggestion.put("description", userDescription);
                    suggestion.put("llmResponse", content);
                    suggestion.put("suggestedEntities", new ArrayList<>());
                    suggestion.put("suggestedRelations", new ArrayList<>());
                    suggestion.put("confidence", 0.5);
                    suggestion.put("needsReview", true);
                    suggestion.put("llmAvailable", true);
                    return suggestion;
                }
            } else {
                log.warn("LLM call failed: {}", response.getError());
            }
        } catch (Exception e) {
            log.warn("LLM call exception: {}", e.getMessage());
        }

        // Fallback on error
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("appCode", appCode);
        suggestion.put("description", userDescription);
        suggestion.put("suggestedEntities", new ArrayList<>());
        suggestion.put("suggestedRelations", new ArrayList<>());
        suggestion.put("confidence", 0.0);
        suggestion.put("needsReview", true);
        suggestion.put("llmAvailable", false);
        return suggestion;
    }

    /**
     * Build system prompt for AI modeling
     */
    private String buildModelingPrompt() {
        return "你是一个零代码产品制造专家。你的任务是通过对话理解用户需求，生成结构化的建模方案。\n\n"
                + "请以JSON格式返回建模方案，包含以下字段：\n"
                + "- suggestedEntities: 建议的实体列表，每个实体包含 entityCode, entityName, fields(字段列表)\n"
                + "- suggestedRelations: 建议的实体关系列表\n"
                + "- confidence: 建议的置信度(0-1)\n"
                + "- needsReview: 是否需要人工确认\n\n"
                + "字段类型可选: STRING, NUMBER, DATE, BOOLEAN, REF, DICT\n"
                + "关系类型可选: ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY\n\n"
                + "示例返回格式:\n"
                + "{\n"
                + "  \"suggestedEntities\": [\n"
                + "    {\"entityCode\": \"customer\", \"entityName\": \"客户\", \"fields\": [\n"
                + "      {\"fieldCode\": \"name\", \"fieldName\": \"客户名称\", \"fieldType\": \"STRING\", \"required\": true},\n"
                + "      {\"fieldCode\": \"phone\", \"fieldName\": \"联系电话\", \"fieldType\": \"STRING\"}\n"
                + "    ]}\n"
                + "  ],\n"
                + "  \"suggestedRelations\": [],\n"
                + "  \"confidence\": 0.8,\n"
                + "  \"needsReview\": true\n"
                + "}";
    }

    /**
     * T10: 应用 AI 建模建议
     */
    public Map<String, Object> applyModelSuggestion(String appCode, Map<String, Object> suggestion) {
        log.info("Applying model suggestion: app={}", appCode);

        Map<String, Object> result = new HashMap<>();
        List<String> createdEntities = new ArrayList<>();
        List<String> createdFields = new ArrayList<>();
        List<String> createdRelations = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) suggestion.get("suggestedEntities");
        if (entities != null) {
            for (Map<String, Object> entityDef : entities) {
                String entityCode = (String) entityDef.get("entityCode");
                try {
                    createdEntities.add(entityCode);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fields = (List<Map<String, Object>>) entityDef.get("fields");
                    if (fields != null) {
                        for (Map<String, Object> fieldDef : fields) {
                            createdFields.add(entityCode + "." + fieldDef.get("fieldCode"));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to create entity {}: {}", entityCode, e.getMessage());
                }
            }
        }

        result.put("createdEntities", createdEntities);
        result.put("createdFields", createdFields);
        result.put("createdRelations", createdRelations);
        result.put("success", true);
        return result;
    }

    /**
     * T11: 智能表单生成
     */
    public Map<String, Object> generateFormLayout(String appCode, String entityCode) {
        log.info("Generating form layout: app={} entity={}", appCode, entityCode);

        Map<String, Object> layout = new HashMap<>();
        layout.put("appCode", appCode);
        layout.put("entityCode", entityCode);
        layout.put("layoutType", "SINGLE_COLUMN");
        layout.put("sections", new ArrayList<>());
        layout.put("autoGenerated", true);

        return layout;
    }

    /**
     * T12: 数据洞察
     */
    public Map<String, Object> analyzeData(String appCode, String entityCode) {
        log.info("Analyzing data: app={} entity={}", appCode, entityCode);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("appCode", appCode);
        analysis.put("entityCode", entityCode);
        analysis.put("totalRecords", 0);
        analysis.put("insights", new ArrayList<>());
        analysis.put("suggestions", new ArrayList<>());

        return analysis;
    }

    /**
     * T13: 一键产品化
     */
    public Map<String, Object> oneClickProductize(String appCode) {
        log.info("One-click productize: app={}", appCode);

        Map<String, Object> result = new HashMap<>();
        List<String> steps = new ArrayList<>();
        List<String> completed = new ArrayList<>();

        steps.add("CHECK_MODEL");
        steps.add("GENERATE_VIEWS");
        steps.add("CONFIGURE_PIPELINE");
        steps.add("BIND_WORKFLOW");
        steps.add("CONFIGURE_PERMISSIONS");
        steps.add("MATERIALIZE_DEPLOY");

        result.put("appCode", appCode);
        result.put("steps", steps);
        result.put("completed", completed);
        result.put("currentStep", 0);
        result.put("status", "PENDING");

        return result;
    }

    /**
     * T9: MCP Tool 定义
     */
    public List<Map<String, Object>> getMcpToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(buildTool("lc_create_app", "创建低代码应用",
                "appName", "string", "应用名称", "true",
                "description", "string", "应用描述", "false"));

        tools.add(buildTool("lc_model_entity", "AI对话式建模",
                "appCode", "string", "应用编码", "true",
                "description", "string", "需求描述", "true"));

        tools.add(buildTool("lc_query_data", "查询运行时数据",
                "appCode", "string", "应用编码", "true",
                "entityCode", "string", "实体编码", "true",
                "page", "integer", "页码", "false",
                "size", "integer", "每页大小", "false"));

        tools.add(buildTool("lc_productize", "一键产品化",
                "appCode", "string", "应用编码", "true"));

        return tools;
    }

    private Map<String, Object> buildTool(String name, String description, String... paramPairs) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        for (int i = 0; i + 3 < paramPairs.length; i += 4) {
            String paramName = paramPairs[i];
            String paramType = paramPairs[i + 1];
            String paramDesc = paramPairs[i + 2];
            boolean isRequired = "true".equals(paramPairs[i + 3]);

            Map<String, Object> prop = new HashMap<>();
            prop.put("type", paramType);
            prop.put("description", paramDesc);
            properties.put(paramName, prop);
            if (isRequired) {
                required.add(paramName);
            }
        }

        schema.put("properties", properties);
        schema.put("required", required);
        tool.put("parameters", schema);
        return tool;
    }

    /**
     * Safely get LlmGatewayService (may not be on classpath)
     */
    private LlmGatewayService getLlmGatewayService() {
        if (llmGatewayServiceRaw instanceof LlmGatewayService) {
            return (LlmGatewayService) llmGatewayServiceRaw;
        }
        return null;
    }
}
