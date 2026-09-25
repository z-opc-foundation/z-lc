package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Schema 管理控制器: App / Entity 的元数据 CRUD + DDL 建表
 * <p>
 * API 路径约定:
 * <ul>
 *   <li>POST   /api/lc/admin/app/create       — 创建应用</li>
 *   <li>POST   /api/lc/admin/app/list         — 应用列表 (分页)</li>
 *   <li>GET    /api/lc/admin/app/{id}         — 应用详情</li>
 *   <li>PUT    /api/lc/admin/app/{id}         — 更新应用</li>
 *   <li>DELETE /api/lc/admin/app/{id}         — 删除应用</li>
 *   <li>POST   /api/lc/admin/app/{appCode}/entity/create — 创建实体</li>
 *   <li>GET    /api/lc/admin/app/{appCode}/entity/list   — 实体列表</li>
 *   <li>GET    /api/lc/admin/entity/{id}                 — 实体详情</li>
 *   <li>PUT    /api/lc/admin/entity/{id}                 — 更新实体</li>
 *   <li>DELETE /api/lc/admin/entity/{id}                 — 删除实体</li>
 *   <li>POST   /api/lc/admin/entity/{id}/provision       — 建表</li>
 *   <li>POST   /api/lc/admin/app/{appCode}/provision-all — 批量建表</li>
 * </ul>
 */
@Tag(name = "低代码-Schema管理")
@RestController
@RequestMapping("/api/lc/admin")
public class SchemaAdminController {

    private static final Logger log = LogManager.getLogger(SchemaAdminController.class);

    @Autowired
    private SchemaAdminService schemaAdminService;

    // ===== App =====

    /**
     * 创建一个新应用 (App), 业务校验失败时返回 400.
     *
     * @param req 应用请求体
     * @return 创建后的 {@link AppDTO}
     */
    @Operation(summary = "创建应用")
    @PostMapping("/app/create")
    public Result<AppDTO> createApp(@RequestBody AppDTO req) {
        if (req == null) {
            return Result.<AppDTO>fail("body is null");
        }

        try {
            AppDTO created = schemaAdminService.createApp(req);
            return Result.success(created);
        } catch (IllegalArgumentException ex) {
            return Result.<AppDTO>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 应用分页列表.
     *
     * @param tenantCode 租户编码, 可空 (不过滤)
     * @param page       页码, 从 1 开始
     * @param size       每页大小
     * @return {@link PageResult} 应用分页结果
     */
    @Operation(summary = "应用列表 (分页)")
    @PostMapping("/app/list")
    public Result<PageResult<AppDTO>> listApps(
            @RequestParam(required = false) String tenantCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(schemaAdminService.listApps(tenantCode, page, size));
    }

    /**
     * 按 id 获取应用详情, 不存在时返回 404.
     *
     * @param id 应用主键
     * @return {@link AppDTO}
     */
    @Operation(summary = "应用详情")
    @GetMapping("/app")
    public Result<AppDTO> getApp(@RequestParam Long id) {
        AppDTO app = schemaAdminService.getApp(id);
        return app == null ? Result.<AppDTO>fail("App not found").code(404) : Result.success(app);
    }

    /**
     * 更新指定 id 的应用.
     *
     * @param id  应用主键
     * @param req 更新内容
     * @return 受影响行数; 业务校验失败时返回 400
     */
    @Operation(summary = "更新应用")
    @PutMapping("/app")
    public Result<Integer> updateApp(@RequestParam Long id, @RequestBody AppDTO req) {
        if (req == null) {
            return Result.<Integer>fail("body is null");
        }

        try {
            return Result.success(schemaAdminService.updateApp(id, req));
        } catch (IllegalArgumentException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 删除指定 id 的应用.
     *
     * @param id 应用主键
     * @return 受影响行数
     */
    @Operation(summary = "删除应用")
    @DeleteMapping("/app")
    public Result<Integer> deleteApp(@RequestParam Long id) {
        return Result.success(schemaAdminService.deleteApp(id));
    }

    // ===== Entity =====

    /**
     * 在指定 app 下创建一个新实体 (含字段定义).
     *
     * @param appCode    应用编码
     * @param tenantCode 租户编码
     * @param req        实体定义请求体
     * @return 创建后的 {@link EntityDefDTO}; 业务校验失败返回 400
     */
    @Operation(summary = "创建实体 (含字段)")
    @PostMapping("/app/entity/create")
    public Result<EntityDefDTO> createEntity(
            @RequestParam String appCode,
            @RequestParam String tenantCode,
            @RequestBody EntityDefDTO req) {
        if (req == null) {
            return Result.<EntityDefDTO>fail("body is null");
        }

        try {
            EntityDefDTO created = schemaAdminService.createEntity(tenantCode, appCode, req);
            return Result.success(created);
        } catch (IllegalArgumentException ex) {
            return Result.<EntityDefDTO>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 列出指定 app 下的全部实体.
     *
     * @param appCode    应用编码
     * @param tenantCode 租户编码
     * @return 实体列表
     */
    @Operation(summary = "实体列表")
    @GetMapping("/app/entity/list")
    public Result<List<EntityDefDTO>> listEntities(
            @RequestParam String appCode,
            @RequestParam String tenantCode) {
        return Result.success(schemaAdminService.listEntities(tenantCode, appCode));
    }

    /**
     * 按 id 获取实体详情 (含字段定义), 不存在时返回 404.
     *
     * @param id 实体主键
     * @return {@link EntityDefDTO}
     */
    @Operation(summary = "实体详情 (含字段)")
    @GetMapping("/entity")
    public Result<EntityDefDTO> getEntity(@RequestParam Long id) {
        EntityDefDTO def = schemaAdminService.getEntity(id);
        return def == null ? Result.<EntityDefDTO>fail("Entity not found").code(404) : Result.success(def);
    }

    /**
     * 更新指定 id 的实体.
     *
     * @param id  实体主键
     * @param req 实体更新内容
     * @return 受影响行数; 业务校验失败时返回 400
     */
    @Operation(summary = "更新实体")
    @PutMapping("/entity")
    public Result<Integer> updateEntity(@RequestParam Long id, @RequestBody EntityDefDTO req) {
        if (req == null) {
            return Result.<Integer>fail("body is null");
        }

        try {
            return Result.success(schemaAdminService.updateEntity(id, req));
        } catch (IllegalArgumentException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 删除指定 id 的实体.
     *
     * @param id 实体主键
     * @return 受影响行数
     */
    @Operation(summary = "删除实体")
    @DeleteMapping("/entity")
    public Result<Integer> deleteEntity(@RequestParam Long id) {
        return Result.success(schemaAdminService.deleteEntity(id));
    }

    // ===== DDL Provisioning =====

    /**
     * 为指定实体执行 DDL 建表, 返回**这一支的结果** (状态 + DDL + 缺哪些列).
     * <p>
     * 不再自己 catch Exception 拼 {@code "DDL execution failed: " + ex.getMessage()}:
     * JDBC 的 message 里带着 {@code SQL [CREATE TABLE ...]}，那等于把物理表名和语句结构发给浏览器。
     * 交给 {@code LcExceptionHandler} 兜 (它带 clientSafe)，坏消息的载体也统一成一处。
     *
     * @param id 实体主键
     */
    @Operation(summary = "为指定实体建表")
    @PostMapping("/entity/provision")
    public Result<ProvisionReport.Item> provisionTable(@RequestParam Long id) {
        return Result.success(schemaAdminService.provisionTable(id));
    }

    /**
     * 为指定 app 下的所有实体逐个建表, 返回逐支结果 + 汇总 (建成几张、跳过几张、几张没建成).
     * <p>
     * 旧口径是 {@code Map<entityCode, DDL>} + 整体 500: 第一个坏实体把其他实体的表一起挡住，
     * 而且 Map 里有这个 entityCode 就等于宣布"建好了" —— 而 {@code IF NOT EXISTS} 对已存在的表
     * 是空操作，那份定义可能一列都没落地。现在一支坏只红自己，坏的那支带原因。
     */
    @Operation(summary = "为某应用下所有实体批量建表")
    @PostMapping("/app/provision-all")
    public Result<ProvisionReport> provisionAllTables(
            @RequestParam String appCode,
            @RequestParam String tenantCode) {
        return Result.success(schemaAdminService.provisionAllTables(tenantCode, appCode));
    }
}
