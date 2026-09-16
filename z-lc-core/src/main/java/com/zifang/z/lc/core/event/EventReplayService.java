package com.zifang.z.lc.core.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.zifang.util.core.json.JsonMapperFactory;

/**
 * 事件回放: 把某 app 的事件链按 apply_seq 顺序 fold 出当前实体定义
 * 规则: 后者覆盖前者 (later overrides former) — 简单且无歧义
 */
@Service
public class EventReplayService {

    private static final Logger log = LogManager.getLogger(EventReplayService.class);
    private final ObjectMapper mapper = JsonMapperFactory.getDefault();
    @Autowired
    private EventService eventService;

    /**
     * 回放所有事件, 返回该 app 下所有 entity 的当前态
     */
    public List<EntityDefDTO> replay(String tenantCode, String appCode) {
        List<EventDTO> events = eventService.listSince(tenantCode, appCode, null);
        log.debug("Replaying {} events for app={}", events.size(), appCode);

        // 按 entityCode 聚合
        Map<String, EntityDefDTO> byEntity = new java.util.LinkedHashMap<>();

        for (EventDTO ev : events) {
            try {
                JsonNode data = mapper.readTree(ev.getEventData());
                String entityCode = data.path("entityCode").asText(ev.getEntityCode());

                EntityDefDTO def = byEntity.computeIfAbsent(entityCode, k -> {
                    EntityDefDTO e = new EntityDefDTO();
                    e.setTenantCode(tenantCode);
                    e.setAppCode(appCode);
                    e.setEntityCode(k);
                    return e;
                });

                // 按 event_type 分发
                switch (ev.getEventType() == null ? "" : ev.getEventType()) {
                    case "CREATE":
                        applyCreate(def, data);
                        break;
                    case "UPDATE":
                        applyUpdate(def, data);
                        break;
                    case "DELETE":
                        applyDelete(def, data);
                        break;
                    default:
                        log.debug("Skip event type={}", ev.getEventType());
                }
            } catch (Exception ex) {
                log.warn("Failed to parse event id={}: {}", ev.getEventId(), ex.getMessage());
            }
        }

        return new ArrayList<>(byEntity.values());
    }

    private void applyCreate(EntityDefDTO def, JsonNode data) {
        if (data.has("entityName")) def.setEntityName(data.get("entityName").asText());
        if (data.has("tableName")) def.setTableName(data.get("tableName").asText());
        if (data.has("description")) def.setDescription(data.get("description").asText());

        if (data.has("fields") && data.get("fields").isArray()) {
            List<FieldDefDTO> fields = new ArrayList<>();
            Iterator<JsonNode> it = data.get("fields").elements();
            while (it.hasNext()) {
                fields.add(parseField(it.next()));
            }
            def.setFields(fields);
        }
    }

    private void applyUpdate(EntityDefDTO def, JsonNode data) {
        if (data.has("entityName")) def.setEntityName(data.get("entityName").asText());
        if (data.has("tableName")) def.setTableName(data.get("tableName").asText());
        if (data.has("description")) def.setDescription(data.get("description").asText());

        if (data.has("fields") && data.get("fields").isArray()) {
            List<FieldDefDTO> existing = def.getFields() != null ? def.getFields() : new ArrayList<>();
            Iterator<JsonNode> it = data.get("fields").elements();
            while (it.hasNext()) {
                FieldDefDTO upd = parseField(it.next());
                // 替换或新增 (按 fieldCode)
                boolean replaced = false;
                for (int i = 0; i < existing.size(); i++) {
                    if (upd.getFieldCode() != null
                            && upd.getFieldCode().equals(existing.get(i).getFieldCode())) {
                        existing.set(i, upd);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) existing.add(upd);
            }
            def.setFields(existing);
        }
    }

    private void applyDelete(EntityDefDTO def, JsonNode data) {
        if (data.has("fieldCode")) {
            String delCode = data.get("fieldCode").asText();
            if (def.getFields() != null) {
                def.getFields().removeIf(f -> delCode.equals(f.getFieldCode()));
            }
        }
    }

    private FieldDefDTO parseField(JsonNode n) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(n.path("fieldCode").asText(null));
        f.setFieldName(n.path("fieldName").asText(null));
        f.setFieldType(n.path("fieldType").asText("STRING"));
        f.setRequired(n.path("required").asBoolean(false));
        f.setDefaultValue(n.path("defaultValue").asText(null));
        f.setDictCode(n.path("dictCode").asText(null));
        f.setRefEntity(n.path("refEntity").asText(null));
        if (n.has("fieldLength")) f.setFieldLength(n.get("fieldLength").asInt());
        if (n.has("scale")) f.setScale(n.get("scale").asInt());
        if (n.has("sortOrder")) f.setSortOrder(n.get("sortOrder").asInt());
        f.setDescription(n.path("description").asText(null));
        return f;
    }
}
