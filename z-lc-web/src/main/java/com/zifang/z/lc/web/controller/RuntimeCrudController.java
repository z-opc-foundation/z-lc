package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeByIdDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import com.zifang.z.lc.common.dto.ShapeQueryDTO;
import com.zifang.z.lc.core.event.EventReplayService;
import com.zifang.z.lc.core.executor.RuntimeCrudExecutor;
import com.zifang.z.lc.core.pipeline.Pipeline;
import com.zifang.z.lc.core.pipeline.Pipeline.PipelineException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 运行时 CRUD 控制器.
 * <p>
 * API 基础路径: /api/lc/runtime
 * 所属模块: z-lc-web
 * 鉴权: 无 (直连, 由上游网关 z-ctc 统一鉴权)
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/lc/runtime/{entityCode}/list   — 分页查询 (filter 走 body)</li>
 *   <li>POST /api/lc/runtime/{entityCode}/get    — 按 id 取单条</li>
 *   <li>POST /api/lc/runtime/{entityCode}/create — 新增记录</li>
 *   <li>POST /api/lc/runtime/{entityCode}/update — 按 id 更新</li>
 *   <li>POST /api/lc/runtime/{entityCode}/delete — 按 id 软删</li>
 *   <li>POST /api/lc/runtime/shape — 分组聚合成二维后再整形为任意高维结构</li>
 *   <li>POST /api/lc/runtime/delete-batch       — 批量软删 (整批预检, 一条不能删则整批不动)</li>
 * </ul>
 *
 * <p>所有端点执行流程: 1) 通过 {@link EventReplayService} 回放事件得到 {@link EntityDefDTO};
 * 2) 调用 {@link RuntimeCrudExecutor} 执行具体 SQL; 3) 触发 {@link Pipeline} 钩子 (preWrite / postRead / postReadList).
 */
@Tag(name = "低代码-运行时CRUD")
@RestController
@RequestMapping("/api/lc/runtime")
public class RuntimeCrudController {

    private static final Logger log = LogManager.getLogger(RuntimeCrudController.class);

    @Autowired
    private EventReplayService eventReplayService;

    @Autowired
    private RuntimeCrudExecutor crudExecutor;

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private com.zifang.z.lc.core.undo.UndoService undoService;

    @Autowired
    private com.zifang.z.lc.core.importer.RuntimeImportService importService;

    @Autowired
    private com.zifang.z.lc.core.deleter.RuntimeBatchDeleteService batchDeleteService;

    /**
     * 将任意对象安全转换为 Long, 失败时返回 null.
     *
     * @param o 待转换对象 (Number/String 均可)
     * @return 转换后的 Long, 转换失败或入参为 null 时返回 null
     */
    private static Long asLong(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof Number) {
            return ((Number) o).longValue();
        }

        try {
            return Long.parseLong(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 分页查询: 用 POST body 携带复杂 filter, 返回分页结果; 查询后会触发 {@code postReadList} 管道钩子.
     *
     * @param entityCode 实体编码
     * @param tenantCode 租户编码 (可覆盖 query 内的值)
     * @param appCode    应用编码 (可覆盖 query 内的值)
     * @param query      查询条件 (filter / 分页 / 排序), 可空
     * @return 分页结果 {@link PageResult}
     */
    @Operation(summary = "分页查询 (POST 形式, 用于 body 携带 filter)")
    @PostMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam String entityCode,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) String appCode,
            @RequestBody(required = false) RuntimeQueryDTO query) {
        if (query == null) {
            query = new RuntimeQueryDTO();
        }

        if (tenantCode != null) {
            query.setTenantCode(tenantCode);
        }

        if (appCode != null) {
            query.setAppCode(appCode);
        }

        query.setEntityCode(entityCode);
        EntityDefDTO def = resolveEntity(query.getAppCode(), entityCode, query.getTenantCode());
        PageResult<Map<String, Object>> page = crudExecutor.list(def, query);
        if (page != null && page.getRecords() != null) {
            pipeline.postReadList(def, page.getRecords());
        }
        return Result.success(page);
    }

    /**
     * 按 id 取单条; 查询后会触发 {@code postRead} 管道钩子.
     *
     * @param entityCode 实体编码
     * @param body       请求体, 含 id / appCode / tenantCode
     * @return 单行数据 Map; 不存在时为 null
     */
    @Operation(summary = "按 id 取单条")
    @PostMapping("/get")
    public Result<Map<String, Object>> get(@RequestParam String entityCode,
                                           @RequestBody RuntimeByIdDTO body) {
        if (body == null) {
            return Result.<Map<String, Object>>fail("body is null");
        }

        EntityDefDTO def = resolveEntity(body.getAppCode(), entityCode, body.getTenantCode());
        Map<String, Object> row = crudExecutor.get(def, body.getId(), body.getTenantCode());
        if (row == null) {
            return Result.success(null);
        }

        pipeline.postRead(def, row);
        return Result.success(row);
    }

    /**
     * 创建一条记录; 写入前会触发 {@code preWrite} 管道钩子, 钩子异常会被映射为 400.
     *
     * @param entityCode 实体编码
     * @param body       字段值请求体 (含 fieldValues / appCode / tenantCode)
     * @return 新建记录的主键 id
     */
    @Operation(summary = "创建记录")
    @PostMapping("/create")
    public Result<Long> create(@RequestParam String entityCode,
                               @RequestBody RuntimeCrudDTO body) {
        if (body == null) {
            return Result.<Long>fail("body is null");
        }

        body.setEntityCode(entityCode);
        EntityDefDTO def = resolveEntity(body.getAppCode(), entityCode, body.getTenantCode());
        try {
            pipeline.preWrite(def, body);
        } catch (PipelineException ex) {
            return Result.<Long>fail(ex.getMessage()).code(400);
        } catch (RuntimeException ex) {
            return Result.<Long>fail(ex.getMessage()).code(400);
        }
        String actor = com.zifang.z.lc.web.support.ActorResolver.resolve(null);
        Long id = crudExecutor.create(def, body, actor);
        undoService.record(body.getTenantCode(), body.getAppCode(), def, id,
                com.zifang.z.lc.core.undo.entity.DataChangeEntity.OP_CREATE,
                null, body.getFieldValues(), com.zifang.z.lc.web.support.ActorResolver.resolve(null));
        return Result.success(id);
    }

    /**
     * 分组聚合. 筛选入参与 {@code /list} 同构, 所以前端把当前视图的筛选原样发过来即可.
     *
     * @param entityCode 实体编码
     * @param body       {@link com.zifang.z.lc.common.dto.AggregateQueryDTO}
     * @return 分组行列表 (未知分组字段/非法聚合会报 400, 不静默降级)
     */
    @Operation(summary = "分组聚合 (group by + count/sum/avg/min/max)")
    @PostMapping("/aggregate")
    public Result<java.util.List<java.util.Map<String, Object>>> aggregate(
            @RequestParam String entityCode,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String tenantCode,
            @RequestBody com.zifang.z.lc.common.dto.AggregateQueryDTO body) {
        if (body == null) {
            return Result.<java.util.List<java.util.Map<String, Object>>>fail("body is null").code(400);
        }
        body.setEntityCode(entityCode);
        // 与 /list 保持同一口径: appCode/tenantCode 走 query 或 body 都行, query 优先.
        // 只认 body 的话, 前端照抄 list 的调用方式就会撞上 "appCode is required".
        String effectiveApp = appCode != null && !appCode.isEmpty() ? appCode : body.getAppCode();
        String effectiveTenant = tenantCode != null && !tenantCode.isEmpty() ? tenantCode : body.getTenantCode();
        body.setAppCode(effectiveApp);
        body.setTenantCode(effectiveTenant);
        EntityDefDTO def = resolveEntity(effectiveApp, entityCode, effectiveTenant);
        try {
            return Result.success(crudExecutor.aggregate(def, body));
        } catch (IllegalArgumentException ex) {
            // 分组/聚合字段不在白名单: 明确报 400, 不能悄悄返回一份全量统计骗人
            return Result.<java.util.List<java.util.Map<String, Object>>>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 分组聚合并整形为任意结构: 库出原始行 → 聚合出二维分组结果 → 对象语言抬成高维文档.
     * <p>
     * 存在的理由是"二维不够表达一个视图": 树形视图要 parent/children, 分组小计要 region/lines/total,
     * 透视表要行转列。这些形状属于视图, 不属于实体, 所以既不该建表也不该在前端拼。
     * shape 就是一段 JSON 程序, 由 z-util-expr-obj 执行; 引擎报的语义错误 (未知步骤/槽位取不到)
     * 一律 400 原样带回, 不能兜底成空结构骗过配置的人。
     *
     * @param entityCode 实体编码
     * @param body       {@link ShapeQueryDTO}: 聚合入参 + shape 程序
     * @return 整形产出的任意结构 (对象/数组/标量)
     */
    @Operation(summary = "分组聚合并整形为任意结构 (二维 → 高维)")
    @PostMapping("/shape")
    public Result<Object> shape(@RequestParam String entityCode,
                                @RequestParam(required = false) String appCode,
                                @RequestParam(required = false) String tenantCode,
                                @RequestBody ShapeQueryDTO body) {
        if (body == null) {
            return Result.<Object>fail("body is null").code(400);
        }
        if (body.getShape() == null) {
            return Result.<Object>fail("body.shape required: 对象整形程序 (JSON 步骤数组或对象)").code(400);
        }
        // 与 /aggregate 同口径: appCode/tenantCode 走 query 或 body 都行, query 优先.
        String effectiveApp = appCode != null && !appCode.isEmpty() ? appCode : body.getAppCode();
        String effectiveTenant = tenantCode != null && !tenantCode.isEmpty() ? tenantCode : body.getTenantCode();
        body.setAppCode(effectiveApp);
        body.setTenantCode(effectiveTenant);
        EntityDefDTO def = resolveEntity(effectiveApp, entityCode, effectiveTenant);
        try {
            java.util.List<Map<String, Object>> rows = crudExecutor.aggregate(def, body);
            return Result.success(new com.zifang.util.expr.obj.ObjEngine().shape(body.getShape(), rows));
        } catch (IllegalArgumentException | com.zifang.util.expr.obj.ObjException ex) {
            // 分组字段不在白名单 / 整形程序写错: 都是请求问题, 报 400 说清楚, 不返回一份空结构
            return Result.<Object>fail(ex.getMessage()).code(400);
        }
    }

    /**
     * 批量导入 · 第一段：只校验，零写入。
     * <p>
     * 校验走的是与单条 create 完全相同的 pipeline，所以"预览说能过"和"真写能过"是同一个口径。
     *
     * @param entityCode 实体编码
     * @param body       records 为已映射成 fieldCode -&gt; 值 的记录数组
     * @return total / validCount / 前 50 条行级错误
     */
    @Operation(summary = "批量导入 · 预检（不落库）")
    @PostMapping("/import/preview")
    public Result<com.zifang.z.lc.core.importer.ImportDto.Result> importPreview(
            @RequestParam String entityCode,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String tenantCode,
            @RequestBody com.zifang.z.lc.core.importer.ImportDto.Request body) {
        return doImport(entityCode, appCode, tenantCode, body, false);
    }

    /**
     * 批量导入 · 第二段：落库。
     * <p>
     * 保证是"只要有一行校验不过就整批不写"；写入中途意外失败则对本批已插入的行做补偿回滚。
     * 数据库事务级别的原子性这里不提供，原因见 {@code RuntimeImportService} 的类注释。
     */
    @Operation(summary = "批量导入 · 提交")
    @PostMapping("/import/commit")
    public Result<com.zifang.z.lc.core.importer.ImportDto.Result> importCommit(
            @RequestParam String entityCode,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String tenantCode,
            @RequestBody com.zifang.z.lc.core.importer.ImportDto.Request body) {
        return doImport(entityCode, appCode, tenantCode, body, true);
    }

    private Result<com.zifang.z.lc.core.importer.ImportDto.Result> doImport(
            String entityCode,
            String appCode,
            String tenantCode,
            com.zifang.z.lc.core.importer.ImportDto.Request body,
            boolean commit) {
        if (body == null) {
            return Result.<com.zifang.z.lc.core.importer.ImportDto.Result>fail("body is null").code(400);
        }
        if (body.getRecords() == null || body.getRecords().isEmpty()) {
            return Result.<com.zifang.z.lc.core.importer.ImportDto.Result>fail("records is empty").code(400);
        }
        if (body.getRecords().size() > com.zifang.z.lc.core.importer.ImportDto.MAX_ROWS) {
            return Result.<com.zifang.z.lc.core.importer.ImportDto.Result>fail(
                    "单次最多 " + com.zifang.z.lc.core.importer.ImportDto.MAX_ROWS + " 行，实际 "
                            + body.getRecords().size() + " 行，请分批导入").code(400);
        }
        body.setEntityCode(entityCode);
        // 与 /list、/aggregate 同口径：appCode/tenantCode 走 query 或 body 都认，query 优先。
        String effectiveApp = appCode != null && !appCode.isEmpty() ? appCode : body.getAppCode();
        String effectiveTenant = tenantCode != null && !tenantCode.isEmpty() ? tenantCode : body.getTenantCode();
        body.setAppCode(effectiveApp);
        body.setTenantCode(effectiveTenant);
        EntityDefDTO def = resolveEntity(effectiveApp, entityCode, effectiveTenant);
        String actor = com.zifang.z.lc.web.support.ActorResolver.resolve(null);
        com.zifang.z.lc.core.importer.ImportDto.Result result = commit
                ? importService.commit(def, body, actor)
                : importService.preview(def, body);
        return Result.success(result);
    }

    /**
     * 按 id 更新一条记录; {@code fieldValues.id} 必填, 写入前会触发 {@code preWrite} 管道钩子.
     *
     * @param entityCode 实体编码
     * @param body       字段值请求体 (含 fieldValues, 其中 id 字段会被消费)
     * @return 受影响行数
     */
    @Operation(summary = "按 id 更新")
    @PostMapping("/update")
    public Result<Integer> update(@RequestParam String entityCode,
                                  @RequestBody RuntimeCrudDTO body) {
        if (body == null || body.getFieldValues() == null || body.getFieldValues().get("id") == null) {
            return Result.<Integer>fail("body.fieldValues.id required");
        }
        Long id = asLong(body.getFieldValues().remove("id"));
        body.setEntityCode(entityCode);
        EntityDefDTO def = resolveEntity(body.getAppCode(), entityCode, body.getTenantCode());
        // 先取现值: 一份给必填校验看"改完之后的整行", 一份做 undo 的前像
        Map<String, Object> before = crudExecutor.get(def, id, body.getTenantCode());
        if (before == null) {
            // 不说清楚的话, 后面必填校验会报"某字段为必填", 而真正的原因是这条记录不存在/已删除
            return Result.<Integer>fail("record not found: entity=" + entityCode + ", id=" + id)
                    .code(404);
        }
        body.setExistingValues(before);
        try {
            pipeline.preWrite(def, body);
        } catch (PipelineException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        } catch (RuntimeException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        }
        int n = crudExecutor.update(def, id, body, null);
        if (n > 0) {
            undoService.record(body.getTenantCode(), body.getAppCode(), def, id,
                    com.zifang.z.lc.core.undo.entity.DataChangeEntity.OP_UPDATE,
                    before, crudExecutor.get(def, id, body.getTenantCode()),
                    com.zifang.z.lc.web.support.ActorResolver.resolve(null));
        }
        return Result.success(n);
    }

    /**
     * 按 id 软删一条记录.
     *
     * @param entityCode 实体编码
     * @param body       请求体, 含 id / appCode / tenantCode
     * @return 受影响行数
     */
    @Operation(summary = "按 id 删除 (软删)")
    @PostMapping("/delete")
    public Result<Integer> delete(@RequestParam String entityCode,
                                  @RequestBody RuntimeByIdDTO body) {
        if (body == null || body.getId() == null) {
            return Result.<Integer>fail("body.id required");
        }

        EntityDefDTO def = resolveEntity(body.getAppCode(), entityCode, body.getTenantCode());
        Map<String, Object> before = crudExecutor.get(def, body.getId(), body.getTenantCode());
        int n = crudExecutor.delete(def, body.getId(), body.getTenantCode());
        if (n > 0) {
            undoService.record(body.getTenantCode(), body.getAppCode(), def, body.getId(),
                    com.zifang.z.lc.core.undo.entity.DataChangeEntity.OP_DELETE,
                    before, null, com.zifang.z.lc.web.support.ActorResolver.resolve(null));
        }
        return Result.success(n);
    }

    /**
     * 批量软删：一次请求删一整批，且**先整批预检**。
     * <p>
     * 存在的理由是"删了一半"这件事：早先批量删除是浏览器 for 循环发 N 个 /delete，
     * 中间任何一个失败就留下半删现场，也没有回滚。这里任何一条 id 不存在/读不到，
     * 整批一条都不删；写入途中意外失败则把本批已删的还原（补偿回滚）。
     * <p>
     * 与 /import 同一个口径：始终返回 HTTP 200 + success 信封，成败由 body 里的
     * applied/deletedCount/errors 说明 —— 部分失败不该变成一个笼统的 4xx。
     *
     * @param entityCode 实体编码
     * @param body       请求体, 含 ids / appCode / tenantCode
     * @return 批量删除结果
     */
    @Operation(summary = "批量删除 (软删, 整批预检)")
    @PostMapping("/delete-batch")
    public Result<com.zifang.z.lc.core.deleter.BatchDeleteDto.Result> deleteBatch(
            @RequestParam String entityCode,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String tenantCode,
            @RequestBody com.zifang.z.lc.core.deleter.BatchDeleteDto.Request body) {
        if (body == null) {
            return Result.<com.zifang.z.lc.core.deleter.BatchDeleteDto.Result>fail("body is null").code(400);
        }
        if (body.getIds() == null || body.getIds().isEmpty()) {
            return Result.<com.zifang.z.lc.core.deleter.BatchDeleteDto.Result>fail("ids is empty").code(400);
        }
        if (body.getIds().size() > com.zifang.z.lc.core.deleter.BatchDeleteDto.MAX_IDS) {
            return Result.<com.zifang.z.lc.core.deleter.BatchDeleteDto.Result>fail(
                    "单次最多 " + com.zifang.z.lc.core.deleter.BatchDeleteDto.MAX_IDS + " 条，实际 "
                            + body.getIds().size() + " 条，请分批删除").code(400);
        }
        body.setEntityCode(entityCode);
        // 与 /import、/list 同口径：appCode/tenantCode 走 query 或 body 都认，query 优先。
        String effectiveApp = appCode != null && !appCode.isEmpty() ? appCode : body.getAppCode();
        String effectiveTenant = tenantCode != null && !tenantCode.isEmpty() ? tenantCode : body.getTenantCode();
        body.setAppCode(effectiveApp);
        body.setTenantCode(effectiveTenant);
        EntityDefDTO def = resolveEntity(effectiveApp, entityCode, effectiveTenant);
        return Result.success(batchDeleteService.deleteBatch(def, body,
                com.zifang.z.lc.web.support.ActorResolver.resolve(null)));
    }

    /**
     * 通过 EventReplayService 解析 entity 定义. 找不到时抛 404 异常 (ControllerAdvice 兜底).
     */
    private EntityDefDTO resolveEntity(String appCode, String entityCode, String tenantCode) {
        if (appCode == null || appCode.isEmpty()) {
            throw new IllegalArgumentException("appCode is required");
        }
        java.util.List<EntityDefDTO> entities = eventReplayService.replay(tenantCode, appCode);
        for (EntityDefDTO e : entities) {
            if (entityCode.equals(e.getEntityCode())) {
                return e;
            }
        }
        throw new IllegalArgumentException(
                "Entity not found: appCode=" + appCode + ", entityCode=" + entityCode
                        + ". 请先通过 POST /api/lc/app/{appCode}/event 提交 CREATE 事件定义实体.");
    }
}
