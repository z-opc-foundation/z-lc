package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.undo.UndoService;
import com.zifang.z.lc.core.undo.entity.DataChangeEntity;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行态数据的撤销 / 重做 / 变更历史.
 * <p>
 * 鉴权: 与 runtime CRUD 一致, 直连模式不拦截, 由上游网关 z-ctc 统一负责.
 */
@Tag(name = "低代码-撤销重做")
@RestController
@RequestMapping("/api/lc/undo")
public class UndoController {

    private static final Logger log = LogManager.getLogger(UndoController.class);

    @Autowired
    private UndoService undoService;

    @Autowired
    private EventReplayService eventReplayService;

    /**
     * 撤销该实体作用域内最近一次未被撤销的变更.
     */
    @Operation(summary = "撤销上一次数据变更")
    @PostMapping("/undo")
    @BizApi(summary = "撤销该实体作用域内最近一次未被撤销的变更", description = "撤销该实体作用域内最近一次未被撤销的变更.", tags = { "z-lc" })
    public Result<Map<String, Object>> undo(@RequestParam @BizParam(name = "entityCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String entityCode,
                                            @RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String appCode = body == null ? null : body.get("appCode");
        String tenantCode = body == null ? null : body.get("tenantCode");
        EntityDefDTO def = resolveEntity(appCode, entityCode, tenantCode);
        String actor = com.zifang.z.lc.web.support.ActorResolver.resolve(bodyActor(body));
        UndoService.UndoOutcome outcome = undoService.undo(tenantCode, appCode, def, actor, ownerOnly(body));
        return Result.success(view(outcome));
    }

    /**
     * 重做最近一次被撤销的变更.
     * <p>
     * 注意: 重做「新建」时自增主键会变, 原 recordId 仍留在变更日志里以便追溯.
     */
    @Operation(summary = "重做上一次被撤销的变更")
    @PostMapping("/redo")
    @BizApi(summary = "重做最近一次被撤销的变更", description = "重做最近一次被撤销的变更. 注意: 重做「新建」时自增主键会变, 原 recordId 仍留在变更日志里以便追溯.", tags = { "z-lc" })
    public Result<Map<String, Object>> redo(@RequestParam @BizParam(name = "entityCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String entityCode,
                                            @RequestBody @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, String> body) {
        String appCode = body == null ? null : body.get("appCode");
        String tenantCode = body == null ? null : body.get("tenantCode");
        EntityDefDTO def = resolveEntity(appCode, entityCode, tenantCode);
        String actor = com.zifang.z.lc.web.support.ActorResolver.resolve(bodyActor(body));
        UndoService.UndoOutcome outcome = undoService.redo(tenantCode, appCode, def, actor, ownerOnly(body));
        return Result.success(view(outcome));
    }

    /**
     * 变更历史 (倒序). 返回前像/后像便于 UI 展示"将要回退成什么".
     */
    @Operation(summary = "数据变更历史")
    @GetMapping("/history")
    @BizApi(summary = "变更历史 (倒序). 返回前像/后像便于 UI 展示\"将要回退成什么\"", description = "变更历史 (倒序). 返回前像/后像便于 UI 展示\"将要回退成什么\".", tags = { "z-lc" })
    public Result<List<Map<String, Object>>> history(@RequestParam @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String appCode,
                                                     @RequestParam(required = false)
                                                     @BizParam(name = "entityCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String entityCode,
                                                     @RequestParam(required = false)
                                                     @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String tenantCode,
                                                     @RequestParam(required = false, defaultValue = "50")
                                                     @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) int limit,
                                                     @RequestParam(required = false)
                                                     @BizParam(name = "owner", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String owner) {
        List<DataChangeEntity> rows = undoService.history(tenantCode, appCode, entityCode, limit,
                "mine".equalsIgnoreCase(owner) ? com.zifang.z.lc.web.support.ActorResolver.resolve(null) : null);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (DataChangeEntity row : rows) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", row.getId());
            item.put("entityCode", row.getEntityCode());
            item.put("recordId", row.getRecordId());
            item.put("operation", row.getOperation());
            item.put("actor", row.getActor());
            item.put("createTime", row.getCreateTime());
            item.put("undone", row.getUndoneBy() != null);
            item.put("beforeImage", row.getBeforeImage());
            item.put("afterImage", row.getAfterImage());
            out.add(item);
        }
        return Result.success(out);
    }

    private static String bodyActor(Map<String, String> body) {
        return body == null ? null : body.get("actor");
    }

    /** 默认只撤自己的改动; 只有显式 scope=all 才允许跨人撤销. */
    private static boolean ownerOnly(Map<String, String> body) {
        return body == null || !"all".equalsIgnoreCase(String.valueOf(body.get("scope")));
    }

    private static Map<String, Object> view(UndoService.UndoOutcome outcome) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("applied", outcome.isApplied());
        map.put("changeId", outcome.getChangeId());
        map.put("operation", outcome.getOperation());
        map.put("recordId", outcome.getRecordId());
        map.put("message", outcome.describe());
        return map;
    }

    /** 与 RuntimeCrudController 同样的事件回放解析口径. */
    private EntityDefDTO resolveEntity(String appCode, String entityCode, String tenantCode) {
        if (appCode == null || appCode.isEmpty()) {
            throw new IllegalArgumentException("appCode is required");
        }
        List<EntityDefDTO> entities = eventReplayService.replay(tenantCode, appCode);
        for (EntityDefDTO entity : entities) {
            if (entityCode != null && entityCode.equals(entity.getEntityCode())) {
                return entity;
            }
        }
        throw new IllegalArgumentException("Entity not found: appCode=" + appCode + ", entityCode=" + entityCode);
    }
}



