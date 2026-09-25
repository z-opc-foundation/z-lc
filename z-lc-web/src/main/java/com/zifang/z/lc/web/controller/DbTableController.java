package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.core.mapper.DbTableMapperService;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DB 表逆向映射控制器: 扫描已有 DB 表 → 低代码模型
 *
 * <p>API:
 * <ul>
 *   <li>GET  /api/lc/admin/db/tables?prefix=oc_              — 扫描表</li>
 *   <li>GET  /api/lc/admin/db/table/{tableName}                — 单表映射预览</li>
 *   <li>POST /api/lc/admin/db/table/{tableName}/import         — 导入为低代码模型</li>
 *   <li>POST /api/lc/admin/db/tables/batch-import              — 批量导入</li>
 * </ul>
 *
 * <p>示例:
 * <pre>{@code
 * // 扫描 oc_ 开头的所有表
 * GET /api/lc/admin/db/tables?prefix=oc_
 *
 * // 预览单表映射
 * GET /api/lc/admin/db/table/oc_order
 *
 * // 导入为模型 (自动创建 entity + 触发 CREATE 事件)
 * POST /api/lc/admin/db/table/oc_order/import?tenantCode=DEFAULT&appCode=crm
 * }</pre>
 */
@Tag(name = "低代码-DB表映射")
@RestController
@RequestMapping("/api/lc/admin/db")
public class DbTableController {

    private static final Logger log = LogManager.getLogger(DbTableController.class);

    @Autowired
    private DbTableMapperService tableMapper;

    @Autowired
    private SchemaAdminService schemaAdminService;

    /**
     * 扫描数据库表 (前缀过滤), 即将表结构逆向映射为低代码模型预览.
     *
     * @param prefix 表名前缀过滤, 默认空字符串表示扫描全部
     * @param schema schema 名, 可空
     * @return 命中的表对应的 {@link EntityDefDTO} 列表
     */
    @Operation(summary = "扫描数据库表 (前缀过滤)")
    @GetMapping("/tables")
    public Result<List<EntityDefDTO>> scanTables(
            @RequestParam(required = false, defaultValue = "") String prefix,
            @RequestParam(required = false) String schema) {
        List<EntityDefDTO> entities = tableMapper.scanTables(prefix, schema);
        return Result.success(entities);
    }

    /**
     * 预览单张表的逆向映射结果 (不落库, 仅做结构转换).
     *
     * @param tableName 表名
     * @param schema    schema 名, 可空
     * @return 映射后的 {@link EntityDefDTO}; 表不存在返回 404
     */
    @Operation(summary = "单表映射预览 (不落库)")
    @GetMapping("/table")
    public Result<EntityDefDTO> previewTable(@RequestParam String tableName,
                                             @RequestParam(required = false) String schema) {
        try {
            EntityDefDTO entity = tableMapper.mapTable(tableName, schema);
            return entity == null
                    ? Result.<EntityDefDTO>fail("Table not found: " + tableName).code(404)
                    : Result.success(entity);
        } catch (Exception e) {
            return Result.<EntityDefDTO>fail("Map table failed: " + e.getMessage()).code(500);
        }
    }

    /**
     * 将单张 DB 表导入为低代码模型.
     * <p>
     * 流程: 1) 逆向映射表结构; 2) 通过 {@link SchemaAdminService#createEntity} 注册为低代码模型,
     * 内部会触发 CREATE 事件以使 {@code RuntimeCrudController} 可用; 3) 若 {@code autoProvision=true},
     * 尝试建表 (表已存在会被忽略).
     *
     * @param tableName     表名
     * @param tenantCode    租户编码
     * @param appCode       应用编码
     * @param schema        schema 名, 可空
     * @param entityCode    自定义实体编码, 缺省时使用表名
     * @param autoProvision 是否在导入后自动建表
     * @return 导入后的 {@link EntityDefDTO}; 表不存在返回 404, 业务校验失败返回 400
     */
    @Operation(summary = "导入单张 DB 表为低代码模型")
    @PostMapping("/table/import")
    public Result<EntityDefDTO> importTable(@RequestParam String tableName,
                                            @RequestParam String tenantCode,
                                            @RequestParam String appCode,
                                            @RequestParam(required = false) String schema,
                                            @RequestParam(required = false) String entityCode,
                                            @RequestParam(required = false, defaultValue = "false") boolean autoProvision) {
        try {
            // 1. 逆向映射
            EntityDefDTO mapped = tableMapper.mapTable(tableName, schema);
            if (mapped == null) {
                return Result.<EntityDefDTO>fail("Table not found: " + tableName).code(404);
            }
            // 支持覆盖 entityCode
            if (entityCode != null && !entityCode.isEmpty()) {
                mapped.setEntityCode(entityCode);
            }

            // 2. 注册为低代码模型 (触发 CREATE 事件，RuntimeCrudController 即可用)
            EntityDefDTO created = schemaAdminService.createEntity(tenantCode, appCode, mapped);
            log.info("[DbTableController] Imported table {} → entity={} (id={})",
                    tableName, created.getEntityCode(), created.getId());

            // 3. 可选：自动建表（如果表已存在则跳过）
            if (autoProvision) {
                try {
                    // 结论必须读回来：`CREATE TABLE IF NOT EXISTS` 对一张已在的表是空操作，
                    // 旧写法把 provisionTable 的返回值丢掉、无论如何都 log "Provisioned table" ——
                    // 那是把"调用过一条没报错的 DDL"说成"这张表按这份定义建起来了" (缺陷 #43)。
                    ProvisionReport.Item prov = schemaAdminService.provisionTable(created.getId());
                    if (ProvisionReport.FAILED.equals(prov.getStatus())) {
                        log.warn("[DbTableController] Provision FAILED for entity {}: {}",
                                created.getEntityCode(), prov.getMessage());
                    } else {
                        log.info("[DbTableController] Provision {} for entity {}",
                                prov.getStatus(), created.getEntityCode());
                    }
                } catch (Exception ddl) {
                    log.warn("[DbTableController] Provision skipped (table may already exist): {}",
                            ddl.getMessage());
                }
            }

            return Result.success(created);
        } catch (IllegalArgumentException ex) {
            return Result.<EntityDefDTO>fail(ex.getMessage()).code(400);
        } catch (Exception ex) {
            log.error("[DbTableController] Import failed for table={}", tableName, ex);
            return Result.<EntityDefDTO>fail("Import failed: " + ex.getMessage()).code(500);
        }
    }

    /**
     * 批量将指定前缀的 DB 表导入为低代码模型.
     * <p>
     * 流程: 1) 按前缀扫描; 2) 逐张注册为低代码模型 (单表失败不中断整体);
     * 3) 若 {@code autoProvision=true} 同步建表 (表已存在忽略).
     *
     * @param tenantCode    租户编码
     * @param appCode       应用编码
     * @param prefix        表名前缀
     * @param schema        schema 名, 可空
     * @param autoProvision 是否在导入后自动建表
     * @return 成功导入的 {@link EntityDefDTO} 列表; 扫描为空时返回 404
     */
    @Operation(summary = "批量导入 DB 表为低代码模型")
    @PostMapping("/tables/batch-import")
    public Result<java.util.List<EntityDefDTO>> batchImport(
            @RequestParam String tenantCode,
            @RequestParam String appCode,
            @RequestParam String prefix,
            @RequestParam(required = false) String schema,
            @RequestParam(required = false, defaultValue = "false") boolean autoProvision) {
        try {
            // 1. 扫描
            List<EntityDefDTO> scanned = tableMapper.scanTables(prefix, schema);
            if (scanned.isEmpty()) {
                return Result.<java.util.List<EntityDefDTO>>fail("No tables found with prefix: " + prefix).code(404);
            }

            // 2. 批量注册
            List<EntityDefDTO> imported = new java.util.ArrayList<>();
            List<String> errors = new java.util.ArrayList<>();
            for (EntityDefDTO src : scanned) {
                try {
                    EntityDefDTO created = schemaAdminService.createEntity(tenantCode, appCode, src);
                    if (autoProvision) {
                        try {
                            ProvisionReport.Item prov = schemaAdminService.provisionTable(created.getId());
                            if (ProvisionReport.FAILED.equals(prov.getStatus())) {
                                errors.add(src.getTableName() + " 元数据收下但表没落地: " + prov.getMessage());
                            }
                        } catch (Exception ddl) {
                            // 表已存在时忽略
                            errors.add(src.getTableName() + " provision 异常: " + ddl.getMessage());
                        }
                    }
                    imported.add(created);
                } catch (Exception ex) {
                    errors.add(src.getTableName() + ": " + ex.getMessage());
                    log.warn("[DbTableController] Skip table {}: {}", src.getTableName(), ex.getMessage());
                }
            }

            log.info("[DbTableController] Batch imported {}/{} tables (errors={})",
                    imported.size(), scanned.size(), errors.size());

            if (!errors.isEmpty()) {
                log.warn("Import errors: {}", errors);
            }

            return Result.success(imported);
        } catch (Exception ex) {
            return Result.<java.util.List<EntityDefDTO>>fail("Batch import failed: " + ex.getMessage()).code(500);
        }
    }
}
