package com.zifang.z.lc.core.materialize.service;

import com.zifang.z.lc.core.event.entity.EventEntity;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.mapper.event.EventMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.zifang.util.core.lang.RandomUtil;

/**
 * 物化事件服务 — 把代码物化动作写入 z_lc_event 事件链, 实现因果可追溯.
 */
@Service
public class MaterializationEventService {

    public static final String EVENT_TYPE_MATERIALIZE = "MATERIALIZE";
    public static final String SOURCE_GENERATOR = "GENERATOR";
    private static final Logger log = LogManager.getLogger(MaterializationEventService.class);
    @Autowired
    private EventMapper eventMapper;

    /**
     * 记录一次 MATERIALIZE 事件.
     *
     * @return event_id
     */
    public String recordMaterializeEvent(MaterializationEntity m, int fileCount) {
        EventEntity e = new EventEntity();
        e.setTenantCode(m.getTenantCode());
        e.setEventId(RandomUtil.uuidCompact());
        e.setAppCode(m.getAppCode());
        e.setEntityCode(null); // MATERIALIZE 作用于多个 entity
        e.setEventType(EVENT_TYPE_MATERIALIZE);
        e.setSource(SOURCE_GENERATOR);

        Map<String, Object> data = new HashMap<>();
        data.put("materialization_id", m.getId());
        data.put("export_version", m.getExportVersion());
        data.put("materialization_path", m.getMaterializationPath());
        data.put("file_count", fileCount);
        data.put("trigger_source", m.getTriggerSource());
        e.setEventData(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(data).toString());

        e.setApplySeq(System.currentTimeMillis());
        e.setApplyTime(new Date());

        eventMapper.insert(e);
        log.info("Recorded MATERIALIZE event {} for app={} (files={})",
                e.getEventId(), m.getAppCode(), fileCount);
        return e.getEventId();
    }
}
