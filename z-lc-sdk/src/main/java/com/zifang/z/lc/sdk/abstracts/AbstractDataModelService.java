package com.zifang.z.lc.sdk.abstracts;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.context.ExtensionServiceContextHolder;
import com.zifang.z.lc.sdk.define.DataModelService;
import com.zifang.z.lc.sdk.define.ModelDataSaveMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 低代码模型服务抽象基类 — 业务方继承本类并指定泛型，z-lc 引擎即可在运行时接管该 POJO 的 CRUD.
 *
 * <p>蒸馏自 ace-platform-engine {@code AbstractDataModelService}
 * （{@code com.c2f.ace.engine.define}），对齐 z-lc 的 RuntimeCrudExecutor 链路：
 * <ul>
 *   <li>通过本地 {@code RuntimeCrudExecutor}（而非远端 Dubbo RPC）调引擎</li>
 *   <li>不依赖任何私库 starter（走 z-opc 自己的依赖图）</li>
 *   <li>返回类型用 z-util-core（{@code PageResult}）— 统一 z-opc 风格</li>
 *   <li>appCode / modelCode 解析优先级：ThreadLocal Context &gt; {@code @DataModel} 注解 &gt; 系统兜底</li>
 * </ul>
 *
 * <p>本类实现 {@link DataModelService} 接口 — 业务方继承后即隐式实现该接口契约，
 * 不用显式 implements；但所有方法默认走「委派 RuntimeCrudExecutor」实现，
 * 由 z-lc-core 子类（典型如 {@code DefaultDataModelService}）完成绑定.
 *
 * <p>用法：
 * <pre>{@code
 *   @DataModel(appCode = "crm", modelCode = "customer")
 *   public class Customer { ... }
 *
 *   @Service
 *   public class CustomerService extends AbstractDataModelService<Customer> {
 *       public CustomerService(RuntimeCrudExecutor crud) { super(crud, Customer.class); }
 *   }
 * }</pre>
 *
 * @param <T> 业务 POJO 类型（带 {@code @DataModel} 注解）
 * @author zifang
 */
public abstract class AbstractDataModelService<T> implements DataModelService<T> {

    private static final Logger log = LogManager.getLogger(AbstractDataModelService.class);

    /**
     * 子类可重写：注入的 CRUD 执行器（z-lc RuntimeCrudExecutor）.
     */
    protected Object crudExecutor;

    /**
     * 业务 POJO 的 Class 对象（子类构造时传入）.
     */
    protected Class<?> genericType;

    protected AbstractDataModelService(Object crudExecutor, Class<?> genericType) {
        this.crudExecutor = crudExecutor;
        this.genericType = genericType;
    }

    /**
     * 取出业务 POJO 的 Class — 蒸馏自 ace ModelServiceCollector 用于 RPC 注册时取 @DataModel 元信息.
     */
    public Class<?> getGenericType() {
        return genericType;
    }

    /**
     * 后期注入 genericType — 用于 {@link com.zifang.z.lc.sdk.collector.ZLcModelServiceCollector}
     * 在启动时通过反射解析 AbstractDataModelService 子类的泛型 T 并设置.
     *
     * <p>注意：业务子类通常通过构造函数注入即可，本 setter 仅供 Collector 使用.
     */
    public void setGenericType(Class<?> genericType) {
        this.genericType = genericType;
    }

    /**
     * 解析当前调用的 appCode / modelCode.
     * 优先级：ThreadLocal Context &gt; {@code @DataModel} 注解 &gt; 系统兜底.
     *
     * @return {@code [appCode, modelCode]} 二元数组
     */
    protected String[] resolveAppAndModel() {
        String appCode = ExtensionServiceContextHolder.currentAppCode();
        String modelCode = ExtensionServiceContextHolder.currentModelCode();
        if (appCode != null && modelCode != null) {
            return new String[]{appCode, modelCode};
        }
        if (genericType.isAnnotationPresent(DataModel.class)) {
            DataModel dm = genericType.getAnnotation(DataModel.class);
            return new String[]{dm.appCode(), dm.modelCode()};
        }
        // 兜底：空 appCode + 类名小写为 modelCode
        return new String[]{"", genericType.getSimpleName().toLowerCase()};
    }

    /**
     * 暴露通用 CRUD DTO body 给子类 — 业务方子类构造 {@link RuntimeCrudDTO} 时使用.
     *
     * @param fieldValues 字段名 → 值的 Map
     * @return 构造好的 body
     */
    protected RuntimeCrudDTO newCrudBody(Map<String, Object> fieldValues) {
        RuntimeCrudDTO body = new RuntimeCrudDTO();
        body.setFieldValues(fieldValues);
        String[] am = resolveAppAndModel();
        body.setAppCode(am[0]);
        body.setEntityCode(am[1]);
        return body;
    }

    /**
     * 暴露通用查询 DTO 给子类 — 业务方子类构造 {@link RuntimeQueryDTO} 时使用.
     */
    protected RuntimeQueryDTO newQuery() {
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        String[] am = resolveAppAndModel();
        q.setAppCode(am[0]);
        q.setEntityCode(am[1]);
        return q;
    }

    // ============================== DataModelService 接口实现 ==============================
    // 默认走 not-implemented — 由 z-lc-core 子类（绑定 RuntimeCrudExecutor）覆盖。
    // 这里保留 stub 是因为 z-lc-sdk 设计为"纯 API 层"，不依赖 z-lc-core。
    // ============================================================================

    @Override
    public Long save(T t) {
        log.debug("AbstractDataModelService.save called for {}", genericType.getSimpleName());
        return defaultNotImplemented("save");
    }

    @Override
    public Long save(T t, Integer mode) {
        // 蒸馏 ace 的 save(t, mode) — mode 仅作为审计/扩展点，底层仍走 save(t)
        log.debug("AbstractDataModelService.save(t, mode={}) called for {}", mode, genericType.getSimpleName());
        return save(t);
    }

    @Override
    public void delete(Long id) {
        log.debug("AbstractDataModelService.delete({}) called for {}", id, genericType.getSimpleName());
        defaultNotImplemented("delete");
    }

    @Override
    public void delete(T t) {
        log.debug("AbstractDataModelService.delete(t) called for {}", genericType.getSimpleName());
        // 默认走 delete by id — 子类可覆盖
        throw new UnsupportedOperationException(
                "delete(T) requires reading 'id' field from " + genericType.getSimpleName()
                        + ". Override delete(T) in subclass.");
    }

    @Override
    public T queryById(Long pkId, boolean deep) {
        log.debug("AbstractDataModelService.queryById({}, deep={}) called for {}", pkId, deep, genericType.getSimpleName());
        return defaultNotImplemented("queryById");
    }

    @Override
    public List<T> queryList(RuntimeQueryDTO dto, boolean deep) {
        log.debug("AbstractDataModelService.queryList called for {}", genericType.getSimpleName());
        return Collections.emptyList();
    }

    @Override
    public PageResult<T> queryPageable(RuntimeQueryDTO dto, boolean deep) {
        log.debug("AbstractDataModelService.queryPageable called for {}", genericType.getSimpleName());
        RuntimeQueryDTO q = dto == null ? newQuery() : dto;
        int page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : q.getSize();
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    @Override
    public T dataCopy(T t) {
        // 蒸馏 ace 的 dataCopy — 默认返回源对象（业务子类应覆盖生成新主键）
        return t;
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode) {
        // 默认返回空 — 业务子类覆盖以提供字段默认值
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) {
        // 默认返回 data 自身 — 业务子类覆盖
        return data == null ? Collections.emptyMap() : data;
    }

    @Override
    public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) {
        // 默认走 init — AI 接入由 Phase 2C 子类覆盖
        return init(appCode, modelCode, data);
    }

    /**
     * 给 stub 方法的统一 not-implemented 信号 — z-lc-core 子类应覆盖所有方法.
     *
     * @param method 方法名
     * @param <R> 返回类型
     * @return 不返回（抛异常）
     */
    protected <R> R defaultNotImplemented(String method) {
        throw new UnsupportedOperationException(
                "AbstractDataModelService." + method + " not bound to executor for "
                        + genericType.getSimpleName() + ". Override in z-lc-core subclass or provide custom implementation.");
    }

    /**
     * 工具方法：判断当前保存模式是否对应「暂存」 — 业务子类在 save 后处理时可用.
     */
    protected boolean isTempSave(Integer mode) {
        return mode != null && ModelDataSaveMode.SAVE_TEMP.equals(mode);
    }

    /**
     * 工具方法：判断当前保存模式是否对应「流程中」 — 业务子类在 save 后处理时可用.
     */
    protected boolean isInProcessSave(Integer mode) {
        return mode != null && ModelDataSaveMode.SAVE_IN_PROCESS.equals(mode);
    }
}
