package com.zifang.z.lc.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.PageDTO;
import com.zifang.z.lc.core.page.PageService;
import com.zifang.z.lc.schema.model.PageSchema;
import com.zifang.z.lc.schema.util.SchemaCodec;
import com.zifang.z.lc.schema.util.WidgetSchemaAdapter;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 页面运行时只读接口。
 *
 * <p>设计态（CRUD）走 {@code /api/lc/page/**}，需要登录；运行态是给终端用户看的页面，
 * 走 {@code /api/lc/runtime/page/**}，与既有 {@code /api/lc/runtime/**} 同一条
 * 免登录口径（SSO 白名单里本就有 {@code /z-team/lc/api/lc/runtime/**}）。</p>
 *
 * <p>返回的是归一化后的标准 schema（elements + layout.structure）：不管这条页面当初是
 * 用设计器搭的还是按旧报表写法存的，消费方拿到的永远是同一种结构。</p>
 */
@Tag(name = "低代码-页面运行时")
@RestController("lcPageRuntimeController")
@RequestMapping("/api/lc/runtime/page")
public class PageRuntimeController {

    private static final Logger log = LogManager.getLogger(PageRuntimeController.class);

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private PageService pageService;

    @Operation(summary = "读取页面运行态 schema")
    @GetMapping("/{appCode}/{pageCode}")
    @BizApi(summary = "get", description = "", tags = { "z-lc" })
    public ResponseEntity<Result<Object>> get(@PathVariable("appCode") @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String appCode,
                                              @PathVariable("pageCode")
                                              @BizParam(name = "pageCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String pageCode) {
        PageDTO page = pageService.getPage(DEFAULT_TENANT, appCode, pageCode);
        if (page == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail("页面不存在: " + appCode + "/" + pageCode).code(404));
        }
        if (page.getSchema() == null || page.getSchema().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail("页面尚未保存内容: " + appCode + "/" + pageCode).code(404));
        }
        try {
            JsonNode tree = SchemaCodec.mapper().readTree(page.getSchema());
            PageSchema schema = WidgetSchemaAdapter.normalize(tree);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", page.getId());
            body.put("appCode", page.getAppCode());
            body.put("pageCode", page.getPageCode());
            body.put("pageName", page.getPageName());
            body.put("title", page.getPageName());
            body.put("version", schema.getVersion());
            body.put("schema", schema);
            return ResponseEntity.ok(Result.<Object>success(body));
        } catch (IllegalArgumentException e) {
            log.warn("Page runtime schema invalid: app={} page={}: {}", appCode, pageCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.fail("页面 schema 非法: " + e.getMessage()).code(400));
        } catch (Exception e) {
            log.error("Page runtime load failed: app={} page={}", appCode, pageCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("页面加载失败: " + e.getMessage()).code(500));
        }
    }
}



