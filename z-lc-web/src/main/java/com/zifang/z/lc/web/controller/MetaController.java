package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.core.app.AppAdminService;
import com.zifang.z.lc.core.dict.DictAdminService;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.fieldtype.FieldTypeRegistry;
import com.zifang.z.lc.core.relation.RelationService;
import com.zifang.z.lc.core.viewconfig.ViewConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据聚合控制器 (纯新增, 不改动任何既有端点):
 * <ul>
 *   <li>GET /api/lc/meta/field-types —  fieldType 注册表自描述 (前端渲染表单/表格的唯一真相源)</li>
 *   <li>GET /api/lc/meta/bundle      —  一次拉齐 app + entities + dicts + views + relations + fieldTypes</li>
 * </ul>
 * <p>
 * 全部复用既有服务: {@link AppAdminService} / {@link EventReplayService} / {@link DictAdminService} /
 * {@link ViewConfigService} / {@link RelationService} / {@link FieldTypeRegistry}.
 * bundle 不带 appCode 时仅返回 {fieldTypes}, 且任何情况下都不会 500.
 */
@Tag(name = "低代码-元数据聚合")
@RestController
@RequestMapping("/api/lc/meta")
public class MetaController {

    private static final Logger log = LogManager.getLogger(MetaController.class);

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private FieldTypeRegistry fieldTypeRegistry;

    @Resource
    private AppAdminService appAdminService;

    @Resource
    private EventReplayService eventReplayService;

    @Resource
    private DictAdminService dictAdminService;

    @Resource
    private ViewConfigService viewConfigService;

    @Resource
    private RelationService relationService;

    /**
     * fieldType 注册表自描述. 每行 key 顺序即契约:
     * fieldType/cellValueType/label/dbType/widget/sortable/groupable/filterable/inlineEditable/operators.
     *
     * @return 至少 10 种逻辑类型的元数据
     */
    @Operation(summary = "字段类型元数据")
    @GetMapping("/field-types")
    public Result<List<Map<String, Object>>> fieldTypes() {
        return Result.success(fieldTypeRegistry.describe());
    }

    /**
     * 应用元数据包. appCode 为空 → 仅返回 {fieldTypes}; 否则补齐 app/entities/dicts/views/relations.
     * 单个下游聚合失败时降级为空集合, 绝不整体 500.
     *
     * @param appCode    应用编码, 可空
     * @param tenantCode 租户编码, 可空 (缺省 default)
     * @return Result 包裹的聚合 Map
     */
    @Operation(summary = "应用元数据聚合包")
    @GetMapping("/bundle")
    public Result<Map<String, Object>> bundle(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String tenantCode) {

        String tenant = (tenantCode == null || tenantCode.trim().isEmpty()) ? DEFAULT_TENANT : tenantCode.trim();

        Map<String, Object> data = new LinkedHashMap<>();
        // fieldTypes 永远在最前, 即使 appCode 缺失 / 下游全挂也保证该端点恒有产出
        data.put("fieldTypes", safeFieldTypes());

        if (appCode == null || appCode.trim().isEmpty()) {
            return Result.success(data);
        }

        String code = appCode.trim();
        data.put("app", safeApp(tenant, code));
        data.put("entities", safeEntities(tenant, code));
        data.put("dicts", safeDicts(tenant));
        data.put("views", safeViews(tenant, code));
        data.put("relations", safeRelations(tenant, code));
        return Result.success(data);
    }

    // ===== 降级封装: 每个子聚合独立 try/catch, 失败返回空值, 保证端点不 500 =====

    private List<Map<String, Object>> safeFieldTypes() {
        try {
            return fieldTypeRegistry.describe();
        } catch (RuntimeException ex) {
            log.error("meta.bundle fieldTypes failed", ex);
            return Collections.emptyList();
        }
    }

    private AppDTO safeApp(String tenant, String appCode) {
        try {
            return appAdminService.getAppByCode(tenant, appCode);
        } catch (RuntimeException ex) {
            log.warn("meta.bundle app resolve failed: app={}, msg={}", appCode, ex.getMessage());
            return null;
        }
    }

    private List<EntityDefDTO> safeEntities(String tenant, String appCode) {
        try {
            List<EntityDefDTO> entities = eventReplayService.replay(tenant, appCode);
            return entities == null ? new ArrayList<EntityDefDTO>() : entities;
        } catch (RuntimeException ex) {
            log.warn("meta.bundle entities failed: app={}, msg={}", appCode, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DictDTO> safeDicts(String tenant) {
        try {
            List<DictDTO> dicts = dictAdminService.listDicts(tenant);
            List<DictDTO> out = new ArrayList<>();
            if (dicts == null) {
                return out;
            }
            for (DictDTO d : dicts) {
                if (d == null) {
                    continue;
                }
                try {
                    d.setItems(dictAdminService.listItems(tenant, d.getDictCode()));
                } catch (RuntimeException ex) {
                    log.warn("meta.bundle dict items failed: dict={}, msg={}", d.getDictCode(), ex.getMessage());
                    d.setItems(Collections.emptyList());
                }
                out.add(d);
            }
            return out;
        } catch (RuntimeException ex) {
            log.warn("meta.bundle dicts failed: msg={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ViewConfigDTO> safeViews(String tenant, String appCode) {
        try {
            List<ViewConfigDTO> views = viewConfigService.listViewConfigsByApp(tenant, appCode);
            return views == null ? new ArrayList<ViewConfigDTO>() : views;
        } catch (RuntimeException ex) {
            log.warn("meta.bundle views failed: app={}, msg={}", appCode, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<RelationDTO> safeRelations(String tenant, String appCode) {
        try {
            List<RelationDTO> relations = relationService.listRelationsByApp(tenant, appCode);
            return relations == null ? new ArrayList<RelationDTO>() : relations;
        } catch (RuntimeException ex) {
            log.warn("meta.bundle relations failed: app={}, msg={}", appCode, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
