package com.zifang.z.lc.core.ai;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.llm.gateway.api.dto.UnifiedMessage;
import com.zifang.z.llm.gateway.api.dto.UnifiedRequest;
import com.zifang.z.llm.gateway.api.dto.UnifiedResponse;
import com.zifang.z.llm.gateway.core.service.ChatGatewayService;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.core.app.AppAdminService;
import com.zifang.z.lc.core.event.EventService;
import com.zifang.z.lc.core.relation.RelationService;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * 现成的元数据落库服务（SchemaAdminBizService）：写 z_lc_entity / z_lc_field，
     * 自带事务、app 存在校验、entityCode 重复校验、字段编码校验与 CREATE 事件。
     * <p>
     * 账本 013 §3 点名的 {@code LcAppEntityMapper} 声明后未使用，已换成真正会调用的落库服务。
     */
    @Resource
    private SchemaAdminService schemaAdminService;

    /** 现成的关系落库服务（RelationServiceImpl）：写 z_lc_relation。 */
    @Resource
    private RelationService relationService;

    /**
     * LLM Gateway - optional dependency, may be null if z-llm not on classpath
     */
    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("zLlmChatGatewayService")
    private Object llmGatewayServiceRaw;

    /**
     * 交给网关解析的模型标识。迁前这里是硬编码的 "default"，z-llm 的 ModelRouter 对空值直接
     * 抛 modelNotFound，所以留同一个默认值、开一个属性让部署方能指到具体 vendor/模型。
     */
    @org.springframework.beans.factory.annotation.Value("${z.lc.ai.model-code:default}")
    private String modelCode;

    /**
     * T10: 对话式建模 - 解析用户自然语言描述，生成实体定义建议
     */
    public Map<String, Object> generateModelSuggestion(String appCode, String userDescription) {
        log.info("AI modeling suggestion requested: app={} desc={}", appCode, userDescription);

        ChatGatewayService llmService = getChatGatewayService();
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
                                                              ChatGatewayService llmService) {
        try {
            UnifiedRequest request = new UnifiedRequest();
            request.setModel(modelCode);

            List<UnifiedMessage> messages = new ArrayList<>();
            messages.add(message("system", buildModelingPrompt()));
            messages.add(message("user", userDescription));
            request.setMessages(messages);
            request.setTemperature(0.3);
            request.setMaxTokens(2000);

            // z-llm 这个 chat(UnifiedRequest) 是 in-process 入口，正文在 choices[0].message
            String content = firstContent(llmService.chat(request));
            if (content != null) {
                content = content.trim();
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
                    Map<String, Object> suggestion = JsonUtil.parseToMap(content);
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
                log.warn("LLM call returned no content (model={})", modelCode);
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
     * T10: 应用 AI 建模建议 —— 把建议里的实体 / 字段 / 关系真正落库到元数据表。
     *
     * <p><b>落库路径</b>：复用 {@link SchemaAdminService#createEntity}（SchemaAdminBizService，
     * 自带事务、app 存在校验、entityCode 重复校验、字段编码校验与 CREATE 事件）与
     * {@link RelationService#createRelation}（RelationServiceImpl）。写入
     * {@code z_lc_entity} / {@code z_lc_field} / {@code z_lc_relation}。<b>不</b> provision
     * 物理表 —— 物理表由设计器单独触发，与前端「落库后仍需 provision」口径一致。
     *
     * <p><b>事务边界</b>：每个实体 / 关系由落库服务自身的事务独立提交，本方法不提供跨项原子性。
     * 任一落库调用抛异常时立即停止，返回 {@code success=false} 且返回值不含任何 createdXxx
     * 假数据，绝不把异常吞成「部分成功」。
     *
     * <p><b>幂等</b>：落库前按编码预检，已存在的实体 / 关系跳过并计入
     * {@code skippedEntities} / {@code skippedRelations}，不重复插入。
     *
     * <p><b>非法建议</b>（缺 appCode / entityCode / relationCode、结构不是数组或对象、
     * 字段编码非法）直接返回 {@code success=false} + {@code reason}，不落库。
     *
     * <p>{@code tenantCode} 取 {@code suggestion.tenantCode}，缺失时按仓内既有默认口径用
     * {@code "default"}（见 RelationServiceImpl），并在返回值里显式带出实际使用的租户编码。
     *
     * <p><b>返回契约</b>（{@code Map<String,Object>}）：
     * <ul>
     *   <li>{@code success} Boolean —— 仅当所有待处理项都真实落库（或幂等跳过）且无异常才 true；</li>
     *   <li>{@code appCode} / {@code tenantCode} String —— 本次落库的作用域；</li>
     *   <li>{@code createdEntities} / {@code createdFields} / {@code createdRelations}
     *       List&lt;String&gt; —— 只含落库服务真正返回成功的项（字段形如 {@code entity.field}）；</li>
     *   <li>{@code skippedEntities} / {@code skippedRelations} List&lt;String&gt; —— 幂等跳过的项；</li>
     *   <li>{@code reason} / {@code failedItem} String —— 仅失败时存在，点名失败原因与失败项。</li>
     * </ul>
     */
    public Map<String, Object> applyModelSuggestion(String appCode, Map<String, Object> suggestion) {
        log.info("Applying model suggestion (persist metadata): app={}", appCode);

        Map<String, Object> result = new HashMap<>();
        List<String> createdEntities = new ArrayList<>();
        List<String> createdFields = new ArrayList<>();
        List<String> createdRelations = new ArrayList<>();
        List<String> skippedEntities = new ArrayList<>();
        List<String> skippedRelations = new ArrayList<>();
        result.put("appCode", appCode);
        result.put("createdEntities", createdEntities);
        result.put("createdFields", createdFields);
        result.put("createdRelations", createdRelations);
        result.put("skippedEntities", skippedEntities);
        result.put("skippedRelations", skippedRelations);

        if (appCode == null || appCode.trim().isEmpty()) {
            return fail(result, "appCode is required", "appCode");
        }
        if (suggestion == null) {
            return fail(result, "suggestion is required", "suggestion");
        }
        // 没有落库依赖就绝不报成功 —— 这条同时是「不得改回只解析名字的假成功」的行为守卫。
        if (schemaAdminService == null || relationService == null) {
            return fail(result, "落库服务未注入 (SchemaAdminService/RelationService)，拒绝在未落库的情况下报成功",
                    "persistence");
        }

        String tenantCode = str(suggestion.get("tenantCode"));
        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            tenantCode = "default";
        }
        result.put("tenantCode", tenantCode);

        // 前端提交 entities (EntityDefDTO 形状)，LLM 建议为 suggestedEntities；两者形状一致。
        Object rawEntities = suggestion.containsKey("entities")
                ? suggestion.get("entities") : suggestion.get("suggestedEntities");
        Object rawRelations = suggestion.containsKey("relations")
                ? suggestion.get("relations") : suggestion.get("suggestedRelations");
        if (rawEntities != null && !(rawEntities instanceof List)) {
            return fail(result, "实体列表必须是数组 (entities / suggestedEntities)", "entities");
        }
        if (rawRelations != null && !(rawRelations instanceof List)) {
            return fail(result, "关系列表必须是数组 (suggestedRelations / relations)", "relations");
        }
        List<?> entityDefs = rawEntities == null ? Collections.emptyList() : (List<?>) rawEntities;
        List<?> relationDefs = rawRelations == null ? Collections.emptyList() : (List<?>) rawRelations;

        try {
            // 幂等预检: 先问一次现存编码，避免把「重复」吞成异常再当成功。
            Set<String> existingEntities = new HashSet<>();
            for (EntityDefDTO e : schemaAdminService.listEntities(tenantCode, appCode)) {
                if (e != null && e.getEntityCode() != null) {
                    existingEntities.add(e.getEntityCode());
                }
            }
            Set<String> existingRelations = new HashSet<>();
            for (RelationDTO r : relationService.listRelationsByApp(tenantCode, appCode)) {
                if (r != null && r.getRelationCode() != null) {
                    existingRelations.add(r.getRelationCode());
                }
            }

            for (Object raw : entityDefs) {
                if (!(raw instanceof Map)) {
                    return fail(result, "实体项必须是对象: " + raw, "entities");
                }
                Map<?, ?> def = (Map<?, ?>) raw;
                String entityCode = str(def.get("entityCode"));
                if (entityCode == null || entityCode.trim().isEmpty()) {
                    return fail(result, "实体缺 entityCode", "entities");
                }
                if (existingEntities.contains(entityCode)) {
                    skippedEntities.add(entityCode);
                    continue;
                }
                EntityDefDTO created = schemaAdminService.createEntity(tenantCode, appCode, toEntityDef(def));
                // 只报落库服务真正返回、且回读得到的项。
                if (created == null || created.getEntityCode() == null) {
                    return fail(result, "落库服务未返回创建结果: " + entityCode, entityCode);
                }
                createdEntities.add(created.getEntityCode());
                if (created.getFields() != null) {
                    for (FieldDefDTO f : created.getFields()) {
                        if (f != null && f.getFieldCode() != null) {
                            createdFields.add(created.getEntityCode() + "." + f.getFieldCode());
                        }
                    }
                }
                existingEntities.add(created.getEntityCode());
            }

            for (Object raw : relationDefs) {
                if (!(raw instanceof Map)) {
                    return fail(result, "关系项必须是对象: " + raw, "relations");
                }
                Map<?, ?> def = (Map<?, ?>) raw;
                String relationCode = str(def.get("relationCode"));
                if (relationCode == null || relationCode.trim().isEmpty()) {
                    return fail(result, "关系缺 relationCode", "relations");
                }
                if (existingRelations.contains(relationCode)) {
                    skippedRelations.add(relationCode);
                    continue;
                }
                RelationDTO created = relationService.createRelation(toRelationReq(tenantCode, appCode, def));
                if (created == null || created.getRelationCode() == null) {
                    return fail(result, "关系落库服务未返回创建结果: " + relationCode, relationCode);
                }
                createdRelations.add(created.getRelationCode());
                existingRelations.add(created.getRelationCode());
            }
        } catch (RuntimeException ex) {
            // 异常绝不能吞成 success=true 的「部分成功」: 如实失败，且不返回任何 createdXxx。
            log.warn("Failed to apply model suggestion: app={} reason={}", appCode, ex.getMessage());
            return fail(result, "落库失败: " + brief(ex), "persistence");
        }

        result.put("success", Boolean.TRUE);
        return result;
    }

    /** 失败统一出口: success=false + reason/failedItem，清空 createdXxx 不留「已创建」假象。 */
    private static Map<String, Object> fail(Map<String, Object> result, String reason, String failedItem) {
        ((List<?>) result.get("createdEntities")).clear();
        ((List<?>) result.get("createdFields")).clear();
        ((List<?>) result.get("createdRelations")).clear();
        result.put("success", Boolean.FALSE);
        result.put("reason", reason);
        result.put("failedItem", failedItem);
        return result;
    }

    private static EntityDefDTO toEntityDef(Map<?, ?> def) {
        EntityDefDTO dto = new EntityDefDTO();
        dto.setEntityCode(str(def.get("entityCode")));
        dto.setEntityName(str(def.get("entityName")));
        dto.setTableName(str(def.get("tableName")));
        dto.setDescription(str(def.get("description")));
        Object rawFields = def.get("fields");
        if (rawFields instanceof List) {
            List<FieldDefDTO> fields = new ArrayList<>();
            for (Object rf : (List<?>) rawFields) {
                if (rf instanceof Map) {
                    fields.add(toFieldDef((Map<?, ?>) rf));
                }
            }
            dto.setFields(fields);
        }
        return dto;
    }

    private static FieldDefDTO toFieldDef(Map<?, ?> def) {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setFieldCode(str(def.get("fieldCode")));
        dto.setFieldName(str(def.get("fieldName")));
        dto.setFieldType(str(def.get("fieldType")));
        dto.setRequired(bool(def.get("required")));
        dto.setDefaultValue(str(def.get("defaultValue")));
        dto.setDictCode(str(def.get("dictCode")));
        dto.setRefEntity(str(def.get("refEntity")));
        dto.setFieldLength(integer(def.get("fieldLength")));
        dto.setScale(integer(def.get("scale")));
        dto.setSortOrder(integer(def.get("sortOrder")));
        dto.setDescription(str(def.get("description")));
        return dto;
    }

    private static RelationCreateReq toRelationReq(String tenantCode, String appCode, Map<?, ?> def) {
        RelationCreateReq req = new RelationCreateReq();
        req.setTenantCode(tenantCode);
        req.setAppCode(appCode);
        req.setRelationCode(str(def.get("relationCode")));
        req.setRelationName(str(def.get("relationName")));
        req.setSourceEntityCode(str(def.get("sourceEntityCode")));
        req.setTargetEntityCode(str(def.get("targetEntityCode")));
        req.setRelationType(str(def.get("relationType")));
        req.setSourceFieldCode(str(def.get("sourceFieldCode")));
        req.setThroughTable(str(def.get("throughTable")));
        return req;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean bool(Object v) {
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.valueOf((String) v);
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        return null;
    }

    private static Integer integer(Object v) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.valueOf((String) v);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 只留最深一层原因的第一段并截断，避免把 JDBC 的 SQL / 索引名原文透给前端。 */
    private static String brief(RuntimeException ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String raw = root.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        String hint = raw.split("(?i)SQL|\\R", 2)[0].trim();
        if (hint.isEmpty()) {
            hint = ex.getClass().getSimpleName();
        }
        return hint.length() > 160 ? hint.substring(0, 160) : hint;
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
     * Safely get ChatGatewayService (may not be on classpath)
     */
    private ChatGatewayService getChatGatewayService() {
        if (llmGatewayServiceRaw instanceof ChatGatewayService) {
            return (ChatGatewayService) llmGatewayServiceRaw;
        }
        return null;
    }

    private static UnifiedMessage message(String role, String text) {
        UnifiedMessage m = new UnifiedMessage();
        m.setRole(role);
        m.setContent(text);
        return m;
    }

    /** z-llm 的回复没有 isSuccess()/getContent() 这一层，正文取 choices[0].message.content。 */
    private static String firstContent(UnifiedResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        UnifiedMessage message = response.getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            return null;
        }
        return message.getContent();
    }
}
