package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.core.dict.DictAdminService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理 Controller - F035 T6
 * <p>
 * API: /api/lc/dict
 * <ul>
 *   <li>POST /create — 创建字典</li>
 *   <li>POST /update — 更新字典</li>
 *   <li>POST /delete — 删除字典</li>
 *   <li>GET  /list — 字典列表</li>
 *   <li>GET  /{dictCode} — 字典详情（含字典项）</li>
 *   <li>POST /{dictCode}/items/create — 新增字典项</li>
 *   <li>POST /{dictCode}/items/update — 更新字典项</li>
 *   <li>POST /{dictCode}/items/delete — 删除字典项</li>
 *   <li>GET  /{dictCode}/items — 字典项列表</li>
 * </ul>
 */
@Tag(name = "低代码-字典管理")
@RestController
@RequestMapping("/api/lc/dict")
public class DictAdminController {

    private static final String DEFAULT_TENANT = "default";

    @Resource
    private DictAdminService dictAdminService;

    @Operation(summary = "创建字典")
    @PostMapping("/create")
    @BizApi(summary = "create", description = "", tags = { "z-lc" })
    public Result<DictDTO> create(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictDTO request) {
        DictDTO result = dictAdminService.createDict(request);
        return Result.success(result);
    }

    @Operation(summary = "更新字典")
    @PostMapping("/update")
    @BizApi(summary = "update", description = "", tags = { "z-lc" })
    public Result<DictDTO> update(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictDTO request) {
        DictDTO result = dictAdminService.updateDict(request);
        return Result.success(result);
    }

    @Operation(summary = "删除字典")
    @PostMapping("/delete")
    @BizApi(summary = "delete", description = "", tags = { "z-lc" })
    public Result<Boolean> delete(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictDTO request) {
        dictAdminService.deleteDict(request.getId());
        return Result.success(true);
    }

    @Operation(summary = "字典列表")
    @GetMapping("/list")
    @BizApi(summary = "list", description = "", tags = { "z-lc" })
    public Result<List<DictDTO>> list(@RequestParam(required = false) @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String appCode) {
        List<DictDTO> list = dictAdminService.listDicts(DEFAULT_TENANT);
        return Result.success(list);
    }

    @Operation(summary = "字典详情（含字典项）")
    @GetMapping("/detail")
    @BizApi(summary = "getByCode", description = "", tags = { "z-lc" })
    public Result<DictDTO> getByCode(@RequestParam @BizParam(name = "dictCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dictCode) {
        DictDTO result = dictAdminService.getDictByCode(DEFAULT_TENANT, dictCode);
        return Result.success(result);
    }

    @Operation(summary = "字典项列表")
    @GetMapping("/items")
    @BizApi(summary = "listItems", description = "", tags = { "z-lc" })
    public Result<List<DictItemDTO>> listItems(@RequestParam @BizParam(name = "dictCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dictCode) {
        List<DictItemDTO> items = dictAdminService.listItems(DEFAULT_TENANT, dictCode);
        return Result.success(items);
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/items/create")
    @BizApi(summary = "createItem", description = "", tags = { "z-lc" })
    public Result<DictItemDTO> createItem(@RequestParam @BizParam(name = "dictCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dictCode, @RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictItemDTO request) {
        // 注意: 这里绝不能走 saveItems —— 那是"整表替换"语义, 会把同字典下其它字典项全部软删.
        return Result.success(dictAdminService.addItem(DEFAULT_TENANT, dictCode, request));
    }

    @Operation(summary = "更新字典项")
    @PostMapping("/items/update")
    @BizApi(summary = "updateItem", description = "", tags = { "z-lc" })
    public Result<DictItemDTO> updateItem(@RequestParam @BizParam(name = "dictCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dictCode, @RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictItemDTO request) {
        return Result.success(dictAdminService.updateItem(DEFAULT_TENANT, dictCode, request));
    }

    @Operation(summary = "整体替换字典项 (先软删该字典下全部项, 再按入参重建)")
    @PostMapping("/items/save-all")
    @BizApi(summary = "saveItems", description = "", tags = { "z-lc" })
    public Result<List<DictItemDTO>> saveItems(@RequestParam @BizParam(name = "dictCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.QUERY) String dictCode, @RequestBody @BizParam(name = "items", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) List<DictItemDTO> items) {
        return Result.success(dictAdminService.saveItems(DEFAULT_TENANT, dictCode, items));
    }

    @Operation(summary = "删除字典项")
    @PostMapping("/items/delete")
    @BizApi(summary = "deleteItem", description = "", tags = { "z-lc" })
    public Result<Boolean> deleteItem(@RequestBody @BizParam(name = "request", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) DictItemDTO request) {
        dictAdminService.deleteItem(request.getId());
        return Result.success(true);
    }
}



