package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;
import com.zifang.z.lc.core.viewconfig.ViewConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public Result<List<ViewConfigDTO>> list(@RequestParam String appCode,
                                            @RequestParam(required = false) String entityCode) {
        if (entityCode != null) {
            return Result.success(viewConfigService.listViewConfigs(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(viewConfigService.listViewConfigsByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "创建视图配置")
    @PostMapping("/create")
    public Result<ViewConfigDTO> create(@RequestBody ViewConfigCreateReq request) {
        return Result.success(viewConfigService.createViewConfig(request));
    }

    @Operation(summary = "更新视图配置")
    @PostMapping("/update")
    public Result<ViewConfigDTO> update(@RequestBody ViewConfigUpdateReq request) {
        return Result.success(viewConfigService.updateViewConfig(request));
    }

    @Operation(summary = "删除视图配置")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody ViewConfigDTO request) {
        viewConfigService.deleteViewConfig(request.getId());
        return Result.success(true);
    }
}
