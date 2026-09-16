package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.core.dict.DictAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public Result<DictDTO> create(@RequestBody DictDTO request) {
        DictDTO result = dictAdminService.createDict(request);
        return Result.success(result);
    }

    @Operation(summary = "更新字典")
    @PostMapping("/update")
    public Result<DictDTO> update(@RequestBody DictDTO request) {
        DictDTO result = dictAdminService.updateDict(request);
        return Result.success(result);
    }

    @Operation(summary = "删除字典")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody DictDTO request) {
        dictAdminService.deleteDict(request.getId());
        return Result.success(true);
    }

    @Operation(summary = "字典列表")
    @GetMapping("/list")
    public Result<List<DictDTO>> list(@RequestParam(required = false) String appCode) {
        List<DictDTO> list = dictAdminService.listDicts(DEFAULT_TENANT);
        return Result.success(list);
    }

    @Operation(summary = "字典详情（含字典项）")
    @GetMapping("/detail")
    public Result<DictDTO> getByCode(@RequestParam String dictCode) {
        DictDTO result = dictAdminService.getDictByCode(DEFAULT_TENANT, dictCode);
        return Result.success(result);
    }

    @Operation(summary = "字典项列表")
    @GetMapping("/items")
    public Result<List<DictItemDTO>> listItems(@RequestParam String dictCode) {
        List<DictItemDTO> items = dictAdminService.listItems(DEFAULT_TENANT, dictCode);
        return Result.success(items);
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/items/create")
    public Result<DictItemDTO> createItem(@RequestParam String dictCode, @RequestBody DictItemDTO request) {
        request.setDictCode(dictCode);
        List<DictItemDTO> result = dictAdminService.saveItems(DEFAULT_TENANT, dictCode, java.util.Collections.singletonList(request));
        return Result.success(result != null && !result.isEmpty() ? result.get(0) : null);
    }

    @Operation(summary = "更新字典项")
    @PostMapping("/items/update")
    public Result<DictItemDTO> updateItem(@RequestParam String dictCode, @RequestBody DictItemDTO request) {
        request.setDictCode(dictCode);
        List<DictItemDTO> result = dictAdminService.saveItems(DEFAULT_TENANT, dictCode, java.util.Collections.singletonList(request));
        return Result.success(result != null && !result.isEmpty() ? result.get(0) : null);
    }

    @Operation(summary = "删除字典项")
    @PostMapping("/items/delete")
    public Result<Boolean> deleteItem(@RequestBody DictItemDTO request) {
        dictAdminService.deleteItem(request.getId());
        return Result.success(true);
    }
}
