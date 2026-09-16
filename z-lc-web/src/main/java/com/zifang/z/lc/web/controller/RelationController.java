package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;
import com.zifang.z.lc.core.relation.RelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public Result<List<RelationDTO>> list(@RequestParam String appCode,
                                          @RequestParam(required = false) String entityCode) {
        if (entityCode != null) {
            return Result.success(relationService.listRelationsByEntity(DEFAULT_TENANT, appCode, entityCode));
        }
        return Result.success(relationService.listRelationsByApp(DEFAULT_TENANT, appCode));
    }

    @Operation(summary = "创建关系")
    @PostMapping("/create")
    public Result<RelationDTO> create(@RequestBody RelationCreateReq request) {
        return Result.success(relationService.createRelation(request));
    }

    @Operation(summary = "更新关系")
    @PostMapping("/update")
    public Result<RelationDTO> update(@RequestBody RelationUpdateReq request) {
        return Result.success(relationService.updateRelation(request));
    }

    @Operation(summary = "删除关系")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody RelationDTO request) {
        relationService.deleteRelation(request.getId());
        return Result.success(true);
    }
}
