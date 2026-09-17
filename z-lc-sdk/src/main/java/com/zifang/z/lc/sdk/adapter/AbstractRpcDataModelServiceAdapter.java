package com.zifang.z.lc.sdk.adapter;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;
import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;

import java.util.List;
import java.util.Map;

/**
 * RPC 模型服务适配器抽象基类 — 蒸馏自 ace-platform-engine
 * {@code AbstractRpcDataModelServiceAdapter} （{@code com.c2f.ace.engine.adapter}），
 * 行为完全对齐.
 *
 * <p>包含：
 * <ul>
 *   <li>{@link #group} — RPC 服务分组（默认 {@code "z-lc-extensions"}）</li>
 *   <li>{@link #version(String, String)} — 根据 appCode + modelCode 生成版本号
 *       （{@code "1.0.0_<appCode>_<modelCode>"}）</li>
 * </ul>
 *
 * <p>与 {@link AbstractRpcTaskServiceAdapter} / {@link com.zifang.z.lc.sdk.spi.sign.adapter.AbstractRpcAssignServiceAdapter}
 * 的区别：本基类的 version 是 {@code appCode_modelCode} 二段模板，
 * 因为 DataModel 的 RPC 路由粒度更细（每个 app+model 一条版本线）.
 *
 * <p>本基类已经实现 {@link ServiceAdapter} 的全部 11 个方法 — 通过委派给
 * {@link #resolveInnerAdapter()} 返回的 {@link InnerDataModelServiceAdapter} 完成.
 * 之所以采用委派模式而非直接继承：
 * <ul>
 *   <li>复用 InnerDataModelServiceAdapter 的 Map↔POJO 转换逻辑（避免重复）</li>
 *   <li>Provider 直接持有本地 Service，Invoker 通过 RPC 拿到结果后也用同一个 Adapter 反序列化</li>
 *   <li>子类（Provider/Invoker）只需覆写 {@link #resolveInnerAdapter()} 即可注入不同的 Service 来源</li>
 * </ul>
 *
 * @author zifang
 */
public abstract class AbstractRpcDataModelServiceAdapter implements ServiceAdapter {

    /** RPC 服务分组. */
    protected String group = "z-lc-extensions";

    /** 版本号模板（{@code %s} 占位符依次为 appCode / modelCode） */
    private static final String VERSION_TEMPLATE = "1.0.0_%s_%s";

    /** 缓存 InnerDataModelServiceAdapter — 首次访问时通过 {@link #resolveInnerAdapter()} 解析. */
    private InnerDataModelServiceAdapter cachedInner;

    /**
     * 根据 appCode + modelCode 生成版本号.
     *
     * @param appCode  应用编码
     * @param modelCode 模型编码
     * @return 版本号字符串（如 {@code "1.0.0_myapp_order"}）
     */
    public static String version(String appCode, String modelCode) {
        return String.format(VERSION_TEMPLATE, appCode, modelCode);
    }

    /**
     * 子类实现 — 返回当前 RPC Adapter 背后的 {@link InnerDataModelServiceAdapter}.
     *
     * <ul>
     *   <li>Provider — 直接返回包装了本地 {@link AbstractDataModelService} 的 InnerAdapter</li>
     *   <li>Invoker — 在每次 RPC 调用时把远程响应反序列化为 POJO，
     *       再用 InnerAdapter 转为 Map 返回 — 此场景需要子类在每次调用前后重建 InnerAdapter</li>
     * </ul>
     */
    protected abstract InnerDataModelServiceAdapter resolveInnerAdapter();

    /** 委托 — 解析后缓存. */
    private InnerDataModelServiceAdapter inner() {
        if (cachedInner == null) {
            cachedInner = resolveInnerAdapter();
        }
        return cachedInner;
    }

    @Override
    public Long save(Map<String, Object> data) {
        return inner().save(data);
    }

    @Override
    public Long save(Map<String, Object> data, Integer mode) {
        return inner().save(data, mode);
    }

    @Override
    public PageResult<Map<String, Object>> queryPageable(ModelDataPageableQueryDTO query, boolean deep) {
        return inner().queryPageable(query, deep);
    }

    @Override
    public void delete(Long id) {
        inner().delete(id);
    }

    @Override
    public void delete(Map<String, Object> data) {
        inner().delete(data);
    }

    @Override
    public Map<String, Object> queryById(Long pkId, boolean deep) {
        return inner().queryById(pkId, deep);
    }

    @Override
    public List<Map<String, Object>> queryList(ModelDataQueryDTO query, boolean deep) {
        return inner().queryList(query, deep);
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode) {
        return inner().init(appCode, modelCode);
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) {
        return inner().init(appCode, modelCode, data);
    }

    @Override
    public Map<String, Object> dataCopy(Map<String, Object> data) {
        return inner().dataCopy(data);
    }

    @Override
    public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) {
        return inner().aiInit(appCode, modelCode, data);
    }
}
