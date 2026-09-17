package com.zifang.z.lc.core.event;

import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.core.event.entity.EventEntity;
import com.zifang.z.lc.mapper.event.EventMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件服务实现: append 带因果校验, 冲突 → 409
 */
@Service
public class EventBizService implements EventService {

    private static final Logger log = LogManager.getLogger(EventBizService.class);

    @Autowired
    private EventMapper eventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDTO append(String appCode, EventAppendRequest req) {
        // 1. 校验 parent_event_id (因果链)
        EventDTO last = getLastEvent(req.getTenantCode(), appCode);
        String actualLastId = last == null ? null : last.getEventId();
        if (req.getParentEventId() == null
                ? actualLastId != null
                : !req.getParentEventId().equals(actualLastId)) {
            log.warn("Event conflict for app={}: expected parent={}, actual last={}",
                    appCode, req.getParentEventId(), actualLastId);
            throw new EventConflictException(req.getParentEventId(), actualLastId);
        }

        // 2. 分配下一个 apply_seq (单调递增)
        Long nextSeq = eventMapper.maxApplySeq(req.getTenantCode(), appCode) + 1;

        // 3. 构建实体并 insert
        EventEntity entity = new EventEntity();
        entity.setTenantCode(req.getTenantCode());
        entity.setEventId(RandomUtil.uuid()); // 32 字符
        entity.setAppCode(appCode);
        entity.setEntityCode(req.getEntityCode());
        entity.setEventType(req.getEventType());
        entity.setEventData(req.getEventData());
        entity.setSource(req.getSource() == null ? "USER" : req.getSource());
        entity.setParentEventId(req.getParentEventId());
        entity.setApplySeq(nextSeq);
        entity.setApplyTime(new Date());
        eventMapper.insert(entity);

        log.info("Event appended: app={} type={} seq={} id={}",
                appCode, req.getEventType(), nextSeq, entity.getEventId());

        return toDto(entity);
    }

    @Override
    public EventDTO getLastEvent(String tenantCode, String appCode) {
        // 取最大 seq 的事件
        List<EventEntity> list = eventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .orderByDesc("apply_seq")
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : toDto(list.get(0));
    }

    @Override
    public List<EventDTO> listSince(String tenantCode, String appCode, Long fromSeq) {
        List<EventEntity> list = eventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EventEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .gt(fromSeq != null, "apply_seq", fromSeq == null ? 0 : fromSeq)
                        .orderByAsc("apply_seq"));
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private EventDTO toDto(EventEntity e) {
        EventDTO dto = new EventDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
