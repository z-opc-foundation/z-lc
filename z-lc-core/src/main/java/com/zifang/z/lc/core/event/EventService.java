package com.zifang.z.lc.core.event;

import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.EventDTO;

import java.util.List;

/**
 * 事件服务接口 (append + replay)
 */
public interface EventService {

    /**
     * 追加事件 (带 parent_event_id 因果校验, 冲突抛 EventConflictException)
     */
    EventDTO append(String appCode, EventAppendRequest req);

    /**
     * 取某 app 的末次事件 (parent_event_id 校验基准)
     */
    EventDTO getLastEvent(String tenantCode, String appCode);

    /**
     * 取从 apply_seq=N 之后的所有事件 (用于增量同步)
     */
    List<EventDTO> listSince(String tenantCode, String appCode, Long fromSeq);
}
