package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeByIdDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
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
        Long id = crudExecutor.create(def, body, null);
        return Result.success(id);
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
        try {
            pipeline.preWrite(def, body);
        } catch (PipelineException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        } catch (RuntimeException ex) {
            return Result.<Integer>fail(ex.getMessage()).code(400);
        }
        int n = crudExecutor.update(def, id, body, null);
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
        int n = crudExecutor.delete(def, body.getId(), body.getTenantCode());
        return Result.success(n);
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
