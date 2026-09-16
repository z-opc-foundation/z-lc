package com.zifang.z.lc.sdk.abstracts;

import com.zifang.util.core.meta.Result;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.context.ExtensionServiceContextHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 低代码模型服务抽象基类.
 * <p>
 * 设计哲学:
 * 业务侧继承本类并指定泛型, z-lc 引擎即可在运行时接管该 POJO 的 CRUD.
 * <p>
 * 设计选择:
 * <ul>
 *   <li>通过本地 RuntimeCrudExecutor (而非远端 RPC) 调引擎 — 复用 z-lc-core 已有的执行器</li>
 *   <li>不依赖任何私库 starter (走 z-opc 自己的依赖图)</li>
 *   <li>返回类型用 z-util-core (Result / PageResult) — 统一 z-opc 风格</li>
 *   <li>appCode / modelCode 解析优先级: ThreadLocal Context > @DataModel 注解 > 系统兜底</li>
 * </ul>
 * <p>
 * 用法:
 * <pre>{@code
 *   @DataModel(appCode = "crm", modelCode = "customer")
 *   public class Customer { ... }
 *
 *   @Service
 *   public class CustomerService extends AbstractDataModelService<Customer> {
 *       public CustomerService(RuntimeCrudExecutor crud) { super(crud, Customer.class); }
 *   }
 * }</pre>
 */
public abstract class AbstractDataModelService<T> {

    private static final Logger log = LogManager.getLogger(AbstractDataModelService.class);

    /**
     * 子类可重写: 注入的 CRUD 执行器 (z-lc RuntimeCrudExecutor)
     */
    protected Object crudExecutor;

    /**
     * 业务 POJO 的 Class 对象 (子类构造时传入)
     */
    protected Class<?> genericType;

    protected AbstractDataModelService(Object crudExecutor, Class<?> genericType) {
        this.crudExecutor = crudExecutor;
        this.genericType = genericType;
    }

    /**
     * 解析当前调用的 appCode / modelCode.
     * 优先级: ThreadLocal Context > @DataModel 注解 > 系统兜底.
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
        // 兜底: 空字符串
        return new String[]{"", genericType.getSimpleName().toLowerCase()};
    }

    /**
     * 保存 (新增/更新) — 默认实现返回 not-implemented, 业务子类应覆盖
     * 或由 z-lc-core 提供 default impl (与 RuntimeCrudExecutor 绑定).
     */
    public Result<Long> save(T t) {
        log.debug("AbstractDataModelService.save called for {}", genericType.getSimpleName());
        return Result.<Long>fail("AbstractDataModelService.save not bound to executor. Override in subclass.").code(501);
    }

    /**
     * 按 id 删除 — 默认实现返回 not-implemented
     */
    public Result<Object> delete(Long id) {
        log.debug("AbstractDataModelService.delete called for {} id={}", genericType.getSimpleName(), id);
        return Result.fail("AbstractDataModelService.delete not bound to executor. Override in subclass.").code(501);
    }

    /**
     * 按 id 查询 — 默认实现返回 not-implemented
     */
    public Result<T> queryById(Long id, boolean deep) {
        log.debug("AbstractDataModelService.queryById called for {} id={}", genericType.getSimpleName(), id);
        return Result.<T>fail("AbstractDataModelService.queryById not bound to executor. Override in subclass.").code(501);
    }

    /**
     * 列表查询
     */
    public Result<List<T>> queryList(Map<String, Object> conditions, boolean deep) {
        log.debug("AbstractDataModelService.queryList called for {}", genericType.getSimpleName());
        return Result.success(Collections.emptyList());
    }

    /**
     * 分页查询
     */
    public Result<PageResult<T>> queryPageable(Map<String, Object> conditions, int current, int size, boolean deep) {
        log.debug("AbstractDataModelService.queryPageable called for {}", genericType.getSimpleName());
        return Result.success(new PageResult<>(Collections.emptyList(), 0L, current, size));
    }
}
