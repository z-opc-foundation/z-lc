package com.zifang.z.lc.sdk.define;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;

import java.util.List;
import java.util.Map;

/**
 * 低代码模型通用 CRUD 契约接口.
 *
 * <p>蒸馏自 ace-platform-engine {@code DataModelService}（{@code com.c2f.ace.engine.define}），
 * 但把 ace 的 {@code ModelDataQueryDTO / ModelDataPageableQueryDTO} 适配为 z-lc 已有的
 * {@link RuntimeQueryDTO} / {@link RuntimeCrudDTO} / {@link PageResult} —
 * 保留「同一接口不同实现」的抽象能力，但对接 z-lc 现有的 {@code RuntimeCrudExecutor} 链路.
 *
 * <p>业务方继承 {@code AbstractDataModelService<T>} 即隐式实现本接口 —
 * 不需要业务方重复声明方法签名.
 *
 * <p>实现路径（由引擎注册表索引）：
 * <ul>
 *   <li>InnerAdapter — 进程内有匹配的 Spring Bean（继承 {@code AbstractDataModelService}）</li>
 *   <li>RpcAdapter  — 通过 {@code @DataModelServiceInfo(exportRpc=true)} 暴露的 Dubbo 服务</li>
 *   <li>DefaultAdapter — 兜底，走 z-lc-core 的 {@code RuntimeCrudExecutor} + {@code DynamicSqlBuilder}，
 *       无需任何业务方实现</li>
 * </ul>
 *
 * <p>三种 adapter 的优先级与 ace-platform 的
 * {@code DispatcherModelDataServiceImpl.aimInner/aimOuter/ModelDataService} 三级调度保持一致.
 *
 * @param <T> 业务 POJO 类型（带 {@code @DataModel} 注解）
 * @author zifang
 */
public interface DataModelService<T> {

    /**
     * 新增/更新 — 自动判定走 INSERT 还是 UPDATE（按 PK 是否存在）.
     *
     * @param t 业务对象
     * @return 主键 id
     */
    Long save(T t);

    /**
     * 指定保存模式的新增/更新.
     *
     * @param t    业务对象
     * @param mode 保存模式：{@link ModelDataSaveMode#SAVE_COMMON} / {@link ModelDataSaveMode#SAVE_TEMP} /
     *             {@link ModelDataSaveMode#SAVE_IN_PROCESS}
     * @return 主键 id
     */
    Long save(T t, Integer mode);

    /**
     * 按主键 id 删除（软删 — 标记 {@code deleted=1}）.
     *
     * @param id 主键
     */
    void delete(Long id);

    /**
     * 按业务对象的主键删除（软删）.
     *
     * @param t 业务对象（读取其 id 字段）
     */
    void delete(T t);

    /**
     * 字符串 id 的便捷重载（解析失败时抛 IllegalArgumentException）.
     *
     * @param id 字符串形式主键
     */
    default void delete(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is empty");
        }
        try {
            delete(Long.parseLong(id));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id is not a valid Long: " + id, e);
        }
    }

    /**
     * 按主键查询单条.
     *
     * @param pkId 主键 id
     * @param deep 是否深度递归（false 仅查本表；true 时一并查关联子模型 — 字典 / refEntity JOIN）
     * @return 单条数据（不存在返回 null）
     */
    T queryById(Long pkId, boolean deep);

    /**
     * 不分页查询 — 返回所有命中条件的记录（注意：引擎内部仍会限制 maxSize 防全表扫描）.
     *
     * @param dto  查询条件（filters / orderBy）
     * @param deep 是否深度递归
     * @return 命中的所有数据
     */
    List<T> queryList(RuntimeQueryDTO dto, boolean deep);

    /**
     * 分页查询.
     *
     * @param dto 查询条件（含 filters / orderBy / page / size）
     * @param deep 是否深度递归
     * @return 分页结果（含 total / records）
     */
    PageResult<T> queryPageable(RuntimeQueryDTO dto, boolean deep);

    /**
     * 数据复制（从源对象衍生一条新草稿 — 通常用于「复制并新建」场景）.
     * <p>z-lc 默认实现：调用 {@code T.clone()}；业务子类可覆盖以重置主键 / 业务编码.
     *
     * @param t 源数据
     * @return 复制后的对象（带新主键）
     */
    T dataCopy(T t);

    /**
     * 表单初始化 — 引擎返回字段默认值（基于 FieldDefDTO + dictCode）.
     *
     * @param appCode   应用编码
     * @param modelCode 模型编码
     * @return 字段名 → 默认值 的 Map
     */
    Map<String, Object> init(String appCode, String modelCode);

    /**
     * 表单初始化（带上下文数据）.
     *
     * @param appCode   应用编码
     * @param modelCode 模型编码
     * @param data      已有数据（用于 AI 推断 / 字段联动）
     * @return 字段名 → 默认值 的 Map
     */
    Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data);

    /**
     * AI 初始化（z-lc Phase 2C 接入 AI 时实现）.
     * <p>当前 z-lc 默认实现：返回 data 自身，不做任何加工.
     *
     * @param appCode   应用编码
     * @param modelCode 模型编码
     * @param data      已有数据
     * @return AI 加工后的数据
     */
    Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data);
}
