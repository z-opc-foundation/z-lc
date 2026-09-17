package com.zifang.z.lc.sdk.adapter;

import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;

/**
 * RPC 模型服务适配器 — 服务端 Provider — 蒸馏自 ace-platform-engine
 * {@code RpcDataModelServiceAdapterProvider} （{@code com.c2f.ace.engine.adapter}），
 * 行为完全对齐.
 *
 * <p>本类是 {@link ServiceAdapter} 的「服务端代理」 — 把本地
 * {@link AbstractDataModelService}{@code <T>} 暴露为 RPC 接口，
 * 供 {@link ZLcRpcDataModelServiceAdapterInvoker} 跨进程调用.
 *
 * <p>典型用法（z-lc 的 Dubbo/gRPC Provider 实现可继承本类）：
 * <pre>{@code
 *   @Service
 *   public class MyProvider extends ZLcRpcDataModelServiceAdapterProvider {
 *       public MyProvider() {
 *           super("myapp", "order", customerService);  // appCode / modelCode / 本地 Service
 *       }
 *   }
 * }</pre>
 *
 * <p>版本号：根据构造时传入的 {@code appCode} + {@code modelCode} 自动派生为
 * {@code "1.0.0_<appCode>_<modelCode>"}（{@link AbstractRpcDataModelServiceAdapter#version}），
 * 调用方 Invoker 用同样的 appCode + modelCode 就能路由到本 Provider.
 *
 * @author zifang
 */
public class ZLcRpcDataModelServiceAdapterProvider extends AbstractRpcDataModelServiceAdapter {

    private final String appCode;
    private final String modelCode;

    @SuppressWarnings("rawtypes")
    private final AbstractDataModelService localService;

    /**
     * @param appCode       应用编码 — 用于派生 RPC version
     * @param modelCode     模型编码 — 用于派生 RPC version
     * @param localService  本地 {@link AbstractDataModelService} 实现（被本 Provider 代理）
     */
    @SuppressWarnings("rawtypes")
    public ZLcRpcDataModelServiceAdapterProvider(String appCode, String modelCode, AbstractDataModelService localService) {
        this.appCode = appCode;
        this.modelCode = modelCode;
        this.localService = localService;
    }

    public String getAppCode() {
        return appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    /** 暴露本地 Service — 业务方子类（如做埋点）可访问. */
    @SuppressWarnings("rawtypes")
    public AbstractDataModelService getLocalService() {
        return localService;
    }

    @Override
    protected InnerDataModelServiceAdapter resolveInnerAdapter() {
        InnerDataModelServiceAdapter inner = new InnerDataModelServiceAdapter();
        @SuppressWarnings("rawtypes")
        AbstractDataModelService svc = localService;
        inner.setAbstractDataModelService(svc);
        return inner;
    }
}
