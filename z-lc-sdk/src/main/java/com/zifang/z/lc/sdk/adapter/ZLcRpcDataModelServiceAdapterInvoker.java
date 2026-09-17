package com.zifang.z.lc.sdk.adapter;

import com.zifang.util.core.lang.exception.BusinessException;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;
import com.zifang.z.lc.common.enums.ZLcEngineStatusCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC 模型服务适配器 — 客户端 Invoker 抽象 — 蒸馏自 ace-platform-engine
 * {@code RpcDataModelServiceAdapterInvoker} （{@code com.c2f.ace.engine.adapter}），
 * 行为完全对齐.
 *
 * <p>本类是 {@link ServiceAdapter} 的「客户端代理」 — 本地业务代码调用 CRUD 方法时，
 * 实际进入这里，由子类（具体 RPC 协议：Dubbo / gRPC / HTTP）执行跨进程调用.
 *
 * <p>11 个方法全部 {@code final}，强制走 RPC 分发；子类只需实现：
 * <ul>
 *   <li>{@link #invokeRemote(String, Object[])} — 实际 RPC 调用，返回远程响应（通常是 {@code Map<String,Object>} 或 POJO）</li>
 * </ul>
 *
 * <p>响应处理：ace 的 RPC 失败响应是 {@code {success=false, message="..."}} 的 Map，
 * 本类在 {@link #failHandle(Object)} 把它转成 {@link BusinessException}.
 *
 * <p>典型用法（Dubbo 实现）：
 * <pre>{@code
 *   public class DubboRpcInvoker extends ZLcRpcDataModelServiceAdapterInvoker {
 *       public DubboRpcInvoker(String appCode, String modelCode) {
 *           super(appCode, modelCode);
 *       }
 *       protected Object invokeRemote(String methodName, Object[] args) {
 *           // 调 Dubbo genericService 即可
 *           return dubboGenericService.invoke(methodName, args);
 *       }
 *   }
 * }</pre>
 *
 * @author zifang
 */
public abstract class ZLcRpcDataModelServiceAdapterInvoker extends AbstractRpcDataModelServiceAdapter {

    /** 调用方应用编码（用于 RPC 路由） */
    private final String callerAppCode;
    /** 服务端应用编码 — version 模板使用 */
    private final String serviceAppCode;
    /** 模型编码 — version 模板使用 */
    private final String modelCode;

    /**
     * @param callerAppCode  调用方应用编码（用于上下文埋点等）
     * @param serviceAppCode 服务端应用编码（用于 RPC version）
     * @param modelCode      模型编码（用于 RPC version）
     */
    public ZLcRpcDataModelServiceAdapterInvoker(String callerAppCode, String serviceAppCode, String modelCode) {
        this.callerAppCode = callerAppCode;
        this.serviceAppCode = serviceAppCode;
        this.modelCode = modelCode;
    }

    public String getCallerAppCode() {
        return callerAppCode;
    }

    public String getServiceAppCode() {
        return serviceAppCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    // ============================== 11 个 RPC 分发方法（final） ==============================

    @Override
    public final Long save(Map<String, Object> data) {
        Object o = invokeRemote("save", new Object[]{data});
        return unwrapId(o);
    }

    @Override
    public final Long save(Map<String, Object> data, Integer mode) {
        Object o = invokeRemote("save", new Object[]{data, mode});
        return unwrapId(o);
    }

    @Override
    public final PageResult<Map<String, Object>> queryPageable(ModelDataPageableQueryDTO query, boolean deep) {
        Object o = invokeRemote("queryPageable", new Object[]{query, deep});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        failHandle(map);
        // 蒸馏 ace 的 Pageable 反序列化 — 这里只做最低限度的字段拷贝
        // z-lc 用 PageResult 替换 Pageable，调用方一般只看 records/total/pageNum/pageSize
        PageResult<Map<String, Object>> re = new PageResult<>();
        Object records = map.get("records");
        if (records instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) records;
            re.setRecords(list);
        }
        Object total = map.get("total");
        if (total != null) re.setTotal(total instanceof Number ? ((Number) total).longValue() : 0L);
        Object pageNum = map.get("pageNum");
        if (pageNum != null) re.setPageNum(((Number) pageNum).longValue());
        Object pageSize = map.get("pageSize");
        if (pageSize != null) re.setPageSize(((Number) pageSize).longValue());
        return re;
    }

    @Override
    public final void delete(Long id) {
        Object o = invokeRemote("delete", new Object[]{id});
        failHandle(o);
    }

    @Override
    public final void delete(Map<String, Object> data) {
        Object o = invokeRemote("delete", new Object[]{data});
        failHandle(o);
    }

    @Override
    public final Map<String, Object> queryById(Long id, boolean deep) {
        Object o = invokeRemote("queryById", new Object[]{id, deep});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        return map;
    }

    @Override
    public final List<Map<String, Object>> queryList(ModelDataQueryDTO query, boolean deep) {
        Object o = invokeRemote("queryList", new Object[]{query, deep});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) o;
        return list;
    }

    @Override
    public final Map<String, Object> init(String appCode, String modelCode) {
        Object o = invokeRemote("init", new Object[]{appCode, modelCode});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        failHandle(map);
        return map;
    }

    @Override
    public final Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) {
        Object o = invokeRemote("init", new Object[]{appCode, modelCode, data});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        failHandle(map);
        return map;
    }

    @Override
    public final Map<String, Object> dataCopy(Map<String, Object> data) {
        Object o = invokeRemote("dataCopy", new Object[]{data});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        failHandle(map);
        return map == null ? new HashMap<>() : map;
    }

    @Override
    public final Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) {
        Object o = invokeRemote("aiInit", new Object[]{appCode, modelCode, data});
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        failHandle(map);
        return map == null ? new HashMap<>() : map;
    }

    // ============================== 子类抽象 + 工具方法 ==============================

    /**
     * Invoker 子类实现 — 执行实际 RPC 调用.
     *
     * <p>子类应返回：
     * <ul>
     *   <li>{@link Map} 形式响应（服务端用 {@code {success, message, data}} 包装）</li>
     *   <li>或裸 POJO / {@link List}（如服务端直接返回对象）</li>
     * </ul>
     *
     * <p>返回值类型由子类决定 — 本类仅做最低限度的类型检查.
     *
     * @param methodName 远程方法名（{@code save/queryPageable/delete/queryById/...}）
     * @param args       远程方法参数
     * @return 远程响应（子类按需处理）
     */
    protected abstract Object invokeRemote(String methodName, Object[] args);

    /**
     * 派生版本号 — 默认从 serviceAppCode + modelCode 生成，
     * 子类可覆盖（如换成不同模板）.
     */
    protected String deriveVersion() {
        return version(serviceAppCode, modelCode);
    }

    /**
     * 把 RPC 返回值（可能是 {@code Map} 包含 {@code data} 字段，或裸 {@link Long}）
     * 拆出主键 ID.
     */
    private Long unwrapId(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) o;
            failHandle(map);
            Object r = map.get("data");
            if (r instanceof Number) {
                return ((Number) r).longValue();
            }
            if (r != null) {
                return Long.parseLong(String.valueOf(r));
            }
        }
        throw new BusinessException(ZLcEngineStatusCode.BIZ_EXCEPTION,
                "RpcDataModelServiceAdapterInvoker.unwrapId: 期望 Number 或 Map，但得到 " + (o == null ? "null" : o.getClass().getName()));
    }

    /**
     * 处理 Map 形式失败响应 — 抛 {@link BusinessException}.
     */
    private void failHandle(Object o) {
        if (!(o instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) o;
        Object success = map.get("success");
        if (success != null && !Boolean.TRUE.equals(success)) {
            String message = map.get("message") == null ? "rpc fail" : String.valueOf(map.get("message"));
            throw new BusinessException(ZLcEngineStatusCode.BIZ_EXCEPTION, message);
        }
    }
}
