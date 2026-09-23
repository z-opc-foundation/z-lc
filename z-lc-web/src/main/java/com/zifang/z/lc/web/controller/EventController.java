package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.EventDTO;
import com.zifang.z.lc.core.event.EventConflictException;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.event.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件 / Schema 控制器:
 * <ul>
 *   <li>GET  /api/lc/app/{appCode}/schema  → 折叠事件链为当前实体定义列表</li>
 *   <li>POST /api/lc/app/{appCode}/event   → 追加事件 (parent_event_id 因果校验, 冲突 → 409)</li>
 * </ul>
 */
@Tag(name = "低代码-事件/Schema")
@RestController
@RequestMapping("/api/lc/app")
public class EventController {

    private static final Logger log = LogManager.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @Autowired
    private EventReplayService eventReplayService;

    /**
     * 获取指定 app 的当前 schema: 通过回放该 app 的全部事件链折叠出最新的实体定义列表.
     *
     * @param appCode    应用编码
     * @param tenantCode 租户编码, 可空 (缺省时使用默认租户)
     * @return 当前生效的 {@link EntityDefDTO} 列表
     */
    @Operation(summary = "获取 app 当前 schema (回放事件链)")
    @GetMapping("/schema")
    public Result<List<EntityDefDTO>> schema(@RequestParam String appCode,
                                             @RequestParam(required = false) String tenantCode) {
        List<EntityDefDTO> entities = eventReplayService.replay(tenantCode, appCode);
        return Result.success(entities);
    }

    /**
     * 事件链头节点: 客户端要追加事件就得先拿到它, 否则没法填 parentEventId.
     * <p>
     * 之前这个查询口缺失, 而 parentEventId 是因果校验的必填项, 结果就是除了内部
     * {@code SchemaAdminBizService} 之外, 任何外部调用者追加事件都必然撞 409.
     */
    @Operation(summary = "获取 app 事件链的最后一个事件 (追加前用来取 parentEventId)")
    @GetMapping("/event/last")
    public Result<EventDTO> lastEvent(@RequestParam String appCode,
                                      @RequestParam(required = false) String tenantCode) {
        return Result.success(eventService.getLastEvent(tenantCode, appCode));
    }

    /**
     * 追加一个 schema 事件.
     * <p>
     * 事件追加时会进行 {@code parent_event_id} 因果校验, 冲突时返回 code=409.
     *
     * @param appCode 应用编码
     * @param req     事件追加请求体 (含 tenantCode, eventType, payload, parentEventId 等)
     * @return 单层 {@link Result}: 因果冲突 → success:false + code 409; 参数缺失 → code 400
     */
    @Operation(summary = "追加 schema 事件")
    @PostMapping("/event")
    public Result<EventDTO> append(@RequestParam String appCode,
                                   @RequestBody EventAppendRequest req) {
        if (req == null) {
            return Result.<EventDTO>fail("body is null").code(400);
        }
        if (req.getTenantCode() == null || req.getTenantCode().isEmpty()) {
            return Result.<EventDTO>fail("tenantCode is required").code(400);
        }
        if (req.getEventType() == null) {
            return Result.<EventDTO>fail("eventType is required").code(400);
        }
        try {
            return Result.success(eventService.append(appCode, req));
        } catch (EventConflictException ex) {
            log.warn("Event conflict: app={}, expected={}, actual={}",
                    appCode, ex.getExpectedParentEventId(), ex.getActualLastEventId());
            // 不能写成 Result.success(Result.fail(...)): 外层信封 success=true 会让任何
            // 只看外层判成功的客户端 (网关 / SDK / curl) 把一次失败写入当成成功, 静默丢写.
            return Result.<EventDTO>fail(ex.getMessage()).code(409);
        } catch (RuntimeException ex) {
            log.warn("Append event failed: app={}, msg={}", appCode, ex.getMessage());
            return Result.<EventDTO>fail(ex.getMessage()).code(400);
        }
    }
}
