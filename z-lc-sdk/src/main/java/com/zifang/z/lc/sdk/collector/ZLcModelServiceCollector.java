package com.zifang.z.lc.sdk.collector;

import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import com.zifang.z.lc.sdk.adapter.InnerDataModelServiceAdapter;
import com.zifang.z.lc.sdk.adapter.ServiceAdapter;
import com.zifang.z.lc.sdk.adapter.ZLcRpcDataModelServiceAdapterProvider;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.annotation.DataModelServiceInfo;
import com.zifang.z.lc.sdk.reflect.LcReflectHelper;
import com.zifang.z.lc.sdk.reflect.QlExpressionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * z-lc 模型服务 Collector — 蒸馏自 ace-platform-engine
 * {@code ModelServiceCollector} （{@code com.c2f.ace.engine}}，
 * 行为完全对齐.
 *
 * <p>启动时扫描 Spring 容器中所有 {@link AbstractDataModelService} Bean，
 * 通过 {@link DataModelServiceInfo @DataModelServiceInfo} 注解 + 泛型 T 的
 * {@link DataModel @DataModel} 注解提取 appCode/modelCode，
 * 注册到内部 {@code appCode:modelCode → AbstractDataModelService} 索引.
 *
 * <p>功能：
 * <ul>
 *   <li>{@link #aim(String, String, Map)} — 根据 appCode/modelCode + 上下文，
 *       用 QL 表达式从所有 Service 中选出匹配的 Service，返回其 {@link ServiceAdapter}
 *       形式（用于引擎动态路由调用）</li>
 *   <li>{@link #collectRpc()} — 找出 {@code exportRpc=true} 的 Service，
 *       注册为 RPC Provider（服务端）— 等价于 ace 的 doRegisterRpc()</li>
 *   <li>{@link #collect()} — 仅做收集（不发布 RPC）— 等价于 ace 的 doCollect()</li>
 * </ul>
 *
 * @author zifang
 */
@Component
public class ZLcModelServiceCollector implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LogManager.getLogger(ZLcModelServiceCollector.class);

    /** key = {@code "appCode:modelCode"}, value = 业务方实现的 Service. */
    private final Map<String, AbstractDataModelService<?>> byAppModel = new LinkedHashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        collect();
        // collectRpc() 需 RPC 框架支持（z-rpc），由业务方按需调用；
        // 默认不自动发布，避免在 SDK 阶段引入 RPC 框架依赖.
    }

    /**
     * 收集所有 {@link AbstractDataModelService} Bean 到 {@link #byAppModel}.
     *
     * <p>与 ace {@code doCollect()} 行为一致：
     * <ol>
     *   <li>取 Bean 上 {@link DataModelServiceInfo @DataModelServiceInfo} 注解</li>
     *   <li>通过反射解析子类泛型 T 的 Class</li>
     *   <li>若 T 是 {@code Map.class}：用注解上的 appCode/modelCode 作 key</li>
     *   <li>否则：从 T 的 {@link DataModel @DataModel} 注解取 appCode/modelCode 作 key</li>
     *   <li>同一 key 重复注册抛异常（防止业务方误配）</li>
     * </ol>
     */
    public void collect() {
        if (applicationContext == null) {
            log.warn("ZLcModelServiceCollector: applicationContext is null, skip collecting");
            return;
        }
        Map<String, AbstractDataModelService> beansOfType =
                applicationContext.getBeansOfType(AbstractDataModelService.class);
        for (Map.Entry<String, AbstractDataModelService> entry : beansOfType.entrySet()) {
            @SuppressWarnings("rawtypes")
            AbstractDataModelService<?> dataModelService = entry.getValue();

            DataModelServiceInfo info = AnnotatedElementUtils.findMergedAnnotation(
                    dataModelService.getClass(), DataModelServiceInfo.class);
            if (info == null) {
                throw new IllegalStateException("@DataModelServiceInfo annotation missing on " +
                        dataModelService.getClass().getName());
            }

            // 通过反射解析子类泛型 T 的 Class
            Class<?> genericType = LcReflectHelper.resolveGenericType(dataModelService);
            dataModelService.setGenericType(genericType);

            String appCode;
            String modelCode;
            if (Map.class.isAssignableFrom(genericType)) {
                appCode = info.appCode();
                modelCode = info.modelCode();
            } else {
                if (!genericType.isAnnotationPresent(DataModel.class)) {
                    throw new IllegalStateException("泛型 T " + genericType.getName() +
                            " 必须带 @DataModel 注解或 T=Map.class");
                }
                DataModel dm = genericType.getAnnotation(DataModel.class);
                appCode = dm.appCode();
                modelCode = dm.modelCode();
            }

            String key = appCode + ":" + modelCode;
            if (byAppModel.containsKey(key)) {
                throw new IllegalStateException(
                        "@DataModelServiceInfo 重复注册：appCode=" + appCode +
                                ", modelCode=" + modelCode);
            }
            byAppModel.put(key, dataModelService);
            log.info("ZLcModelServiceCollector: registered {} -> {}",
                    key, dataModelService.getClass().getSimpleName());
        }
        log.info("ZLcModelServiceCollector: collected {} model services", byAppModel.size());
    }

    /**
     * 把 {@code exportRpc=true} 的 Service 注册为 RPC Provider（服务端）.
     *
     * <p>与 ace {@code doRegisterRpc()} 行为一致：
     * <ul>
     *   <li>遍历所有 {@link AbstractDataModelService}</li>
     *   <li>读 {@link DataModelServiceInfo#exportRpc()} — true 则发布</li>
     *   <li>用 {@code appCode + modelCode} 派生 RPC version（{@code "1.0.0_<appCode>_<modelCode>"}）</li>
     *   <li>用 {@link ZLcRpcDataModelServiceAdapterProvider} 包装本地 Service</li>
     * </ul>
     *
     * <p>注：本方法需调用方接入 RPC 发布能力（z-rpc / Dubbo / gRPC）后自行调用，
     * 业务方可重写本方法或订阅 Collector 的事件把 Provider 投递到 RPC 框架.
     *
     * @return 已发布 RPC 的 ServiceAdapter Provider 列表（key = appCode:modelCode, value = adapter）
     */
    public Map<String, ZLcRpcDataModelServiceAdapterProvider> collectRpc() {
        Map<String, ZLcRpcDataModelServiceAdapterProvider> published = new LinkedHashMap<>();
        for (AbstractDataModelService<?> service : byAppModel.values()) {
            DataModelServiceInfo info = AnnotatedElementUtils.findMergedAnnotation(
                    service.getClass(), DataModelServiceInfo.class);
            if (info == null || !info.exportRpc()) {
                continue;
            }
            DataModel dm = service.getGenericType().getAnnotation(DataModel.class);
            String appCode = dm.appCode();
            String modelCode = dm.modelCode();
            String version = ZLcRpcDataModelServiceAdapterProvider.version(appCode, modelCode);

            @SuppressWarnings("rawtypes")
            AbstractDataModelService raw = service;
            ZLcRpcDataModelServiceAdapterProvider provider =
                    new ZLcRpcDataModelServiceAdapterProvider(appCode, modelCode, raw);

            String key = appCode + ":" + modelCode;
            published.put(key, provider);
            log.info("ZLcModelServiceCollector: published RPC provider {} version={}",
                    key, version);
        }
        return published;
    }

    /**
     * 引擎调用入口 — 等价于 ace {@code ModelServiceCollector.aim()}.
     *
     * <p>遍历所有已注册 Service，按 {@link DataModelServiceInfo#expression()}
     * 用 {@link QlExpressionUtil} 求值，选出第一个匹配的 Service，返回其
     * {@link InnerDataModelServiceAdapter}（Map 形式 ServiceAdapter）.
     *
     * <p>典型场景：低代码平台调用一个未指定 Service 的模型时，
     * 引擎按上下文（如「按业务类型匹配」）动态路由到对应业务 Service.
     *
     * @param appCode 应用 code（保留参数 — ace 兼容签名；当前 z-lc 不做 appCode 过滤）
     * @param modelCode 模型 code（保留参数 — 同上）
     * @param context 调用上下文（用于 QL 表达式求值）
     * @return 匹配 Service 的 InnerAdapter（Map 形式 ServiceAdapter）；无匹配返回 null
     */
    public ServiceAdapter aim(String appCode, String modelCode, Map<String, Object> context) {
        for (AbstractDataModelService<?> service : byAppModel.values()) {
            DataModelServiceInfo info = AnnotatedElementUtils.findMergedAnnotation(
                    service.getClass(), DataModelServiceInfo.class);
            if (info == null) {
                continue;
            }
            String expression = info.expression();
            if (QlExpressionUtil.executeRule(context == null ? Collections.emptyMap() : context, expression)) {
                @SuppressWarnings("rawtypes")
                AbstractDataModelService raw = service;
                InnerDataModelServiceAdapter inner = new InnerDataModelServiceAdapter();
                inner.setAbstractDataModelService(raw);
                return inner;
            }
        }
        return null;
    }

    /**
     * 按 appCode:modelCode 直接获取 Service — 比 {@link #aim} 更精确.
     */
    public AbstractDataModelService<?> getByAppModel(String appCode, String modelCode) {
        return byAppModel.get(appCode + ":" + modelCode);
    }

    /**
     * 返回所有已注册 appCode:modelCode key（不可变快照）.
     */
    public java.util.Set<String> allKeys() {
        return Collections.unmodifiableSet(byAppModel.keySet());
    }

    /**
     * 已注册 Service 数量.
     */
    public int size() {
        return byAppModel.size();
    }

    /**
     * 清空索引 — 主要用于测试.
     */
    public void clear() {
        byAppModel.clear();
    }

    /**
     * 取全部 Service 列表（不可变快照）.
     */
    public List<AbstractDataModelService<?>> all() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(byAppModel.values()));
    }
}
