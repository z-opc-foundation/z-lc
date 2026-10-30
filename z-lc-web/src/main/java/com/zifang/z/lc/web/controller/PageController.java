package com.zifang.z.lc.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.PageCreateReq;
import com.zifang.z.lc.common.dto.PageDTO;
import com.zifang.z.lc.common.dto.PageUpdateReq;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.page.PageService;
import com.zifang.z.lc.report.common.schema.DatasetSchema;
import com.zifang.z.lc.report.common.schema.QueryResult;
import com.zifang.z.lc.report.common.schema.WidgetSpec;
import com.zifang.z.lc.report.dataset.DatasetExecutor;
import com.zifang.z.lc.report.render.EChartsRenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面控制器 (V2 页面域: Page 一等公民 + 渲染, lead 013 §四)。
 * <p>
 * CRUD 走 {@link PageService}；渲染消费报表执行链 ({@link DatasetExecutor} +
 * {@link EChartsRenderService})——报表链未装配的宿主里 CRUD 仍可用，render 报 503 语义。
 * 渲染产物与 z-frontend-render 的 {@code RenderData} 逐字段对齐；
 * FORM 部件的字段清单由 {@link EventReplayService} 回放实体得到，前端照单画表单、
 * 提交走既有 RuntimeCrud（零脚本缺省实现）。
 */
@Tag(name = "低代码-页面")
// 显式 bean 名: z-opc 同 JVM 防撞名 (MetaController 2026-10-03 实测教训)
@RestController("lcPageController")
@RequestMapping("/api/lc/page")
public class PageController {

    private static final Logger log = LogManager.getLogger(PageController.class);

    private static final String DEFAULT_TENANT = "default";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private PageService pageService;

    @Resource
    private EventReplayService eventReplayService;

    @Resource
    private ObjectProvider<DatasetExecutor> datasetExecutorProvider;

    @Resource
    private ObjectProvider<EChartsRenderService> renderServiceProvider;

    @Operation(summary = "创建页面")
    @PostMapping("/create")
    public Result<PageDTO> create(@RequestBody PageCreateReq request) {
        return Result.success(pageService.createPage(request));
    }

    @Operation(summary = "更新页面 (schema/pageName)")
    @PostMapping("/update")
    public Result<?> update(@RequestBody PageUpdateReq request) {
        PageDTO dto = pageService.updatePage(request);
        if (dto == null) {
            return Result.fail("页面不存在: id=" + request.getId()).code(404);
        }
        return Result.success(dto);
    }

    @Operation(summary = "删除页面 (逻辑删)")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody Map<String, Long> body) {
        pageService.deletePage(body.get("id"));
        return Result.success(true);
    }

    @Operation(summary = "页面列表 (按应用)")
    @GetMapping("/list")
    public Result<List<PageDTO>> list(@RequestParam String appCode) {
        return Result.success(pageService.listPages(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "页面详情")
    @GetMapping("/{appCode}/{pageCode}")
    public Result<PageDTO> get(@PathVariable("appCode") String appCode,
                               @PathVariable("pageCode") String pageCode) {
        return Result.success(pageService.getPage(DEFAULT_TENANT, appCode, pageCode));
    }

    /**
     * 页面渲染: schema → {layout, options, forms}。
     * options 供 UniversalRenderer 直接消费；forms 供前端画实体表单。
     */
    @Operation(summary = "页面渲染 (组件树 → 渲染产物)")
    @GetMapping("/{appCode}/{pageCode}/render")
    public ResponseEntity<Result<Object>> render(@PathVariable("appCode") String appCode,
                                                              @PathVariable("pageCode") String pageCode) {
        PageDTO page = pageService.getPage(DEFAULT_TENANT, appCode, pageCode);
        if (page == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail("页面不存在: " + appCode + "/" + pageCode).code(404));
        }
        DatasetExecutor executor = datasetExecutorProvider.getIfAvailable();
        EChartsRenderService renderService = renderServiceProvider.getIfAvailable();
        if (executor == null || renderService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.fail("页面渲染链未装配 (报表 DatasetExecutor/RenderService 缺席)").code(503));
        }
        try {
            return ResponseEntity.ok(Result.<Object>success(renderPage(appCode, page, executor, renderService)));
        } catch (IllegalArgumentException e) {
            // schema 落库后校验规则演进/手工改库等导致的坏结构: 明确报 400, 不落 500
            log.warn("Page render schema invalid: app={} page={}: {}", appCode, pageCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.fail("页面 schema 非法: " + e.getMessage()).code(400));
        } catch (Exception e) {
            log.error("Page render failed: app={} page={}", appCode, pageCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("页面渲染失败: " + e.getMessage()).code(500));
        }
    }

    private Map<String, Object> renderPage(String appCode, PageDTO page,
                                           DatasetExecutor executor, EChartsRenderService renderService)
            throws Exception {
        Map<String, Object> root = MAPPER.readValue(page.getSchema(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });

        @SuppressWarnings("unchecked")
        Map<String, DatasetSchema> datasets = root.containsKey("datasets")
                ? MAPPER.convertValue(root.get("datasets"),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, DatasetSchema>>() { })
                : Collections.<String, DatasetSchema>emptyMap();

        List<Map<String, Object>> widgets = root.containsKey("widgets")
                ? castList(root.get("widgets")) : Collections.<Map<String, Object>>emptyList();

        Map<String, Object> options = new LinkedHashMap<>();
        Map<String, Object> forms = new LinkedHashMap<>();
        for (Map<String, Object> widget : widgets) {
            String id = String.valueOf(widget.get("id"));
            String type = String.valueOf(widget.get("type"));
            if ("FORM".equals(type)) {
                forms.put(id, formMeta(appCode, widget));
                continue;
            }
            WidgetSpec spec = MAPPER.convertValue(widget, WidgetSpec.class);
            DatasetSchema ds = datasets.get(spec.getDatasetId());
            if (ds == null) {
                throw new IllegalArgumentException("widget [" + id + "] 引用的 dataset ["
                        + spec.getDatasetId() + "] 未定义");
            }
            QueryResult data = executor.execute(ds);
            Map<String, Object> option = renderService.render(spec, data);
            option.put("_widgetType", type);
            option.put("_title", spec.getTitle());
            options.put(id, option);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("pageCode", page.getPageCode());
        resp.put("pageName", page.getPageName());
        resp.put("title", page.getPageName());
        resp.put("version", root.get("version"));
        resp.put("layout", root.get("layout"));
        resp.put("options", options);
        resp.put("forms", forms);
        return resp;
    }

    /** FORM 部件元数据: entityCode + 回放出的字段清单 (前端画表单的唯一真相源) */
    private Map<String, Object> formMeta(String appCode, Map<String, Object> widget) {
        String entityCode = String.valueOf(widget.get("entityCode"));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("entityCode", entityCode);
        meta.put("title", widget.get("title"));
        List<Map<String, Object>> fields = new ArrayList<>();
        for (EntityDefDTO def : eventReplayService.replay(DEFAULT_TENANT, appCode)) {
            if (!entityCode.equals(def.getEntityCode())) {
                continue;
            }
            meta.put("entityName", def.getEntityName());
            if (def.getFields() != null) {
                for (com.zifang.z.lc.common.dto.FieldDefDTO f : def.getFields()) {
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("code", f.getFieldCode());
                    field.put("name", f.getFieldName() == null ? f.getFieldCode() : f.getFieldName());
                    field.put("type", f.getFieldType());
                    field.put("required", Boolean.TRUE.equals(f.getRequired()));
                    fields.add(field);
                }
            }
            break;
        }
        meta.put("fields", fields);
        return meta;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : (List<Object>) value) {
            out.add((Map<String, Object>) item);
        }
        return out;
    }
}
