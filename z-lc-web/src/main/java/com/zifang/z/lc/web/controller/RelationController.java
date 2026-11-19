package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;
import com.zifang.z.lc.core.relation.RelationService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "低代码-实体关系")
@RestController
@RequestMapping("/api/lc/relation")
public class RelationController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private RelationService relationService;

    @Operation(summary = "关系列表")
    @GetMapping("/list")
    @BizApi(summary = "list", description = "", tags = { "z-lc" })
    public Result<List<RelationDTO>> list(@RequestParam @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String appCode,
                                          @RequestParam(required = false)
                                          @BizParam(name = "entityCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String entityCode) {
        if (entityCode != null) {
            return Result.success(relationService.listRelationsByEntity(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(relationService.listRelationsByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "创建关系")
    @PostMapping("/create")
    @BizApi(summary = "create", description = "", tags = { "z-lc" })
    public Result<RelationDTO> create(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) RelationCreateReq request) {
        return Result.success(relationService.createRelation(request));
    }

    @Operation(summary = "更新关系")
    @PostMapping("/update")
    @BizApi(summary = "update", description = "", tags = { "z-lc" })
    public Result<RelationDTO> update(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) RelationUpdateReq request) {
        return Result.success(relationService.updateRelation(request));
    }

    @Operation(summary = "删除关系")
    @PostMapping("/delete")
    @BizApi(summary = "delete", description = "", tags = { "z-lc" })
    public Result<Boolean> delete(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) RelationDTO request) {
        relationService.deleteRelation(request.getId());
        return Result.success(true);
    }
}



