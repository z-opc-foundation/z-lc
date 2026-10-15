package com.zifang.z.lc.core.event;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 事件回放: 把某 app 的事件链按 apply_seq 顺序 fold 出当前实体定义
 * 规则: 后者覆盖前者 (later overrides former) — 简单且无歧义
 */
@Service
public class EventReplayService {

    private static final Logger log = LogManager.getLogger(EventReplayService.class);

    /** 字段缺失或为 JSON null 时返回 def（对齐 Jackson 的 {@code path(k).asText(def)}）。 */
    private static String textOrDefault(JsonCursor n, String field, String def) {
        JsonCursor c = n.path(field);
        return (c.isMissing() || c.isNull()) ? def : c.asText();
    }
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
                JsonCursor data = JsonCursor.of(JsonUtil.parse(ev.getEventData()));
                String entityCode = textOrDefault(data, "entityCode", ev.getEntityCode());

                String type = ev.getEventType() == null ? "" : ev.getEventType();
                // 整实体的 DELETE（payload 里没有 fieldCode）在任何写入之前处理: 它要把这个实体从折叠里
                // 整个摘掉，于是也就**不该**先 computeIfAbsent 立一个空桩 —— 否则"删除"反而把实体造了出来，
                // 而运行时按码找得到它（实测：删掉的实体照样能 runtime/list 出旧记录）。
                if ("DELETE".equals(type) && textOrDefault(data, "fieldCode", null) == null) {
                    byEntity.remove(entityCode);
                    continue;
                }

                EntityDefDTO def = byEntity.computeIfAbsent(entityCode, k -> {
                    EntityDefDTO e = new EntityDefDTO();
                    e.setTenantCode(tenantCode);
                    e.setAppCode(appCode);
                    e.setEntityCode(k);
                    return e;
                });

                // 按 event_type 分发
                switch (type) {
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

    private void applyCreate(EntityDefDTO def, JsonCursor data) {
        if (data.has("entityName")) {
            def.setEntityName(data.path("entityName").asText());
        }

        if (data.has("tableName")) {
            def.setTableName(data.path("tableName").asText());
        }

        if (data.has("description")) {
            def.setDescription(data.path("description").asText());
        }


        if (data.has("fields") && data.path("fields").isArray()) {
            List<FieldDefDTO> fields = new ArrayList<>();
            for (JsonCursor n : data.path("fields").elements()) {
                fields.add(parseField(n));
            }
            def.setFields(fields);
        }
    }

    private void applyUpdate(EntityDefDTO def, JsonCursor data) {
        if (data.has("entityName")) {
            def.setEntityName(data.path("entityName").asText());
        }

        if (data.has("tableName")) {
            def.setTableName(data.path("tableName").asText());
        }

        if (data.has("description")) {
            def.setDescription(data.path("description").asText());
        }


        if (data.has("fields") && data.path("fields").isArray()) {
            List<FieldDefDTO> existing = def.getFields() != null ? def.getFields() : new ArrayList<>();
            for (JsonCursor n : data.path("fields").elements()) {
                FieldDefDTO upd = parseField(n);
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
                if (!replaced) {
                    existing.add(upd);
                }

            }
            def.setFields(existing);
        }
    }

    private void applyDelete(EntityDefDTO def, JsonCursor data) {
        if (data.has("fieldCode")) {
            String delCode = data.path("fieldCode").asText();
            if (def.getFields() != null) {
                def.getFields().removeIf(f -> delCode.equals(f.getFieldCode()));
            }
        }
    }

    private FieldDefDTO parseField(JsonCursor n) {
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(textOrDefault(n, "fieldCode", null));
        f.setFieldName(textOrDefault(n, "fieldName", null));
        f.setFieldType(textOrDefault(n, "fieldType", "STRING"));
        f.setRequired(n.path("required").asBoolean(false));
        f.setDefaultValue(textOrDefault(n, "defaultValue", null));
        f.setDictCode(textOrDefault(n, "dictCode", null));
        f.setRefEntity(textOrDefault(n, "refEntity", null));
        if (n.has("fieldLength")) {
            f.setFieldLength(n.path("fieldLength").asInt(0));
        }

        if (n.has("scale")) {
            f.setScale(n.path("scale").asInt(0));
        }

        if (n.has("sortOrder")) {
            f.setSortOrder(n.path("sortOrder").asInt(0));
        }

        f.setDescription(textOrDefault(n, "description", null));
        return f;
    }
}
