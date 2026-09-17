package com.zifang.z.lc.sdk.spi.task.adapter;

import com.zifang.z.lc.common.dto.task.MessageDTO;
import com.zifang.z.lc.sdk.spi.task.TaskServiceInfo;

/**
 * RPC 任务服务适配器 — 客户端 Invoker 抽象 — 蒸馏自 ace-platform-engine
 * {@code RpcTaskServiceAdapterInvoker} （{@code com.c2f.ace.engine.adapter}）.
 *
 * <p>本类是 TaskServiceAdapter 的「客户端代理」 — 本地业务代码调用 send*Msg 方法时，
 * 实际进入这里，由子类（具体 RPC 协议）执行跨进程调用.
 *
 * <p>子类的职责：
 * <ul>
 *   <li>实现 {@link #invokeRemote(String, Object[])} — 实际调用 RPC（Dubbo / gRPC / HTTP 等）</li>
 *   <li>从 {@link TaskServiceInfo#identityCode()} / {@link #group} 派生 version</li>
 *   <li>处理失败响应（Map 形式含 success / message）</li>
 * </ul>
 *
 * @author zifang
 */
public abstract class ZLcRpcTaskServiceAdapterInvoker extends AbstractRpcTaskServiceAdapter {

    /** identityCode — 业务方在构造时注入. */
    private final String identityCode;

    protected ZLcRpcTaskServiceAdapterInvoker(String identityCode) {
        this.identityCode = identityCode;
    }

    public String getIdentityCode() {
        return identityCode;
    }

    @Override
    public final void sendTodoMsg(MessageDTO messageDTO) {
        invokeRemote("sendTodoMsg", new Object[]{messageDTO});
    }

    @Override
    public final void sendDoneMsg(MessageDTO messageDTO) {
        invokeRemote("sendDoneMsg", new Object[]{messageDTO});
    }

    @Override
    public final void sendRevokeMsg(MessageDTO messageDTO) {
        invokeRemote("sendRevokeMsg", new Object[]{messageDTO});
    }

    /**
     * 子类实现 — 执行实际 RPC 调用.
     *
     * @param methodName 远程方法名（{@code sendTodoMsg / sendDoneMsg / sendRevokeMsg}）
     * @param args       远程方法参数
     * @return 远程响应（子类按需处理）
     */
    protected abstract Object invokeRemote(String methodName, Object[] args);

    /**
     * 子类实现 — 获取远程版本号（默认从 identityCode 派生，子类可覆盖）.
     */
    protected String deriveVersion() {
        return version(identityCode);
    }
}
