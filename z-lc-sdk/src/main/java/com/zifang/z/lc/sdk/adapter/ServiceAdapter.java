package com.zifang.z.lc.sdk.adapter;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;

import java.util.List;
import java.util.Map;

/**
 * 通用 CRUD 服务适配器（Map 版本）.
 *
 * <p>蒸馏自 ace-platform-engine {@code ServiceAdapter}
 * （{@code com.c2f.ace.engine.adapter}），字段语义完全对齐.
 *
 * <p>与 {@link com.zifang.z.lc.sdk.define.DataModelService} 的区别：
 * <ul>
 *   <li>{@code DataModelService<T>} — POJO 泛型版本（编译期类型安全）</li>
 *   <li>{@code ServiceAdapter} — {@code Map<String, Object>} 版本（运行时灵活，
 *       用于 RPC / 跨进程 / 动态类型场景）</li>
 * </ul>
 *
 * <p>典型用法：
 * <ul>
 *   <li>RPC Provider — 把 {@code DataModelService<T>} 适配为 RPC 接口
 *       （{@code RpcDataModelServiceAdapterProvider}）</li>
 *   <li>Dispatcher 路由 — 从多个 adapter 中选一个执行请求
 *       （{@code DispatcherModelDataService}）</li>
 *   <li>动态字段场景 — 字段类型运行时才确定（如前端自定义表单）</li>
 * </ul>
 *
 * <p>实现类（典型）：
 * <ul>
 *   <li>{@link InnerDataModelServiceAdapter} — 把 {@code DataModelService<T>} 包装成 Map 形式</li>
 *   <li>{@code RpcDataModelServiceAdapterInvoker} — 通过 Dubbo RPC 跨进程调用</li>
 *   <li>业务方自定义 — 直接实现本接口做内部覆盖</li>
 * </ul>
 *
 * @author zifang
 */
public interface ServiceAdapter {

    /**
     * 保存 / 更新 — 自动判定（PK 存在则 UPDATE，否则 INSERT）.
     *
     * @param data 字段值 Map（fieldCode → value）
     * @return 自增主键
     */
    Long save(Map<String, Object> data);

    /**
     * 保存（指定 mode）— 用于区分普通保存 / 暂存 / 流程中.
     *
     * @param data 字段值 Map
     * @param mode 保存模式（{@link com.zifang.z.lc.sdk.define.ModelDataSaveMode}）
     * @return 自增主键
     */
    @Deprecated
    Long save(Map<String, Object> data, Integer mode);

    /**
     * 分页查询.
     *
     * @param query 查询条件（含 pageCode / queryBeans / orderBean / queryCondition）
     * @param deep  是否深度递归（截断子模型）
     * @return 分页结果
     */
    PageResult<Map<String, Object>> queryPageable(ModelDataPageableQueryDTO query, boolean deep);

    /**
     * 按主键删除（软删）.
     */
    void delete(Long id);

    /**
     * 按数据行删除（软删）.
     */
    void delete(Map<String, Object> data);

    /**
     * 按主键查询.
     *
     * @param id   主键
     * @param deep 是否深度递归
     * @return 数据行（不存在返回 null）
     */
    Map<String, Object> queryById(Long id, boolean deep);

    /**
     * 列表查询（不分页）.
     */
    List<Map<String, Object>> queryList(ModelDataQueryDTO query, boolean deep);

    /**
     * 表单数据初始化 — 引擎返回字段默认值.
     */
    Map<String, Object> init(String appCode, String modelCode);

    /**
     * 表单数据初始化（带上下文数据 — 用于 AI 推断 / 字段联动）.
     */
    Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data);

    /**
     * 数据复制（从源数据衍生新草稿）.
     */
    Map<String, Object> dataCopy(Map<String, Object> data);

    /**
     * AI 初始化（Phase 2C 接入）.
     */
    Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data);
}
