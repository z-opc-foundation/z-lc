package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;
import com.zifang.z.lc.core.viewconfig.ViewConfigService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "低代码-视图配置")
@RestController
@RequestMapping("/api/lc/view-config")
public class ViewConfigController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private ViewConfigService viewConfigService;

    @Operation(summary = "视图配置列表")
    @GetMapping("/list")
    @BizApi(summary = "list", description = "", tags = { "z-lc" })
    public Result<List<ViewConfigDTO>> list(@RequestParam @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String appCode,
                                            @RequestParam(required = false)
                                            @BizParam(name = "entityCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String entityCode) {
        if (entityCode != null) {
            return Result.success(viewConfigService.listViewConfigs(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(viewConfigService.listViewConfigsByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "创建视图配置")
    @PostMapping("/create")
    @BizApi(summary = "create", description = "", tags = { "z-lc" })
    public Result<ViewConfigDTO> create(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) ViewConfigCreateReq request) {
        return Result.success(viewConfigService.createViewConfig(request));
    }

    @Operation(summary = "更新视图配置")
    @PostMapping("/update")
    @BizApi(summary = "update", description = "", tags = { "z-lc" })
    public Result<ViewConfigDTO> update(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) ViewConfigUpdateReq request) {
        return Result.success(viewConfigService.updateViewConfig(request));
    }

    @Operation(summary = "删除视图配置")
    @PostMapping("/delete")
    @BizApi(summary = "delete", description = "", tags = { "z-lc" })
    public Result<Boolean> delete(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) ViewConfigDTO request) {
        viewConfigService.deleteViewConfig(request.getId());
        return Result.success(true);
    }
}



