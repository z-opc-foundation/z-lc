package com.zifang.z.lc.sdk.spi.task.adapter;

import com.zifang.z.lc.common.dto.task.MessageDTO;
import com.zifang.z.lc.sdk.spi.task.AbstractTaskService;

/**
 * RPC 任务服务适配器 — 服务端 Provider — 蒸馏自 ace-platform-engine
 * {@code RpcTaskServiceAdapterProvider} （{@code com.c2f.ace.engine.adapter}），
 * 行为完全对齐.
 *
 * <p>本类是 TaskServiceAdapter 的「服务端实现」 — 调用方 RPC 调用时实际进入这里，
 * 然后委派给本地 {@link AbstractTaskService} 子类.
 *
 * <p>典型用法（业务方接入）：
 * <pre>{@code
 *   public class FeishuRpcProvider extends ZLcRpcTaskServiceAdapterProvider {
 *       public FeishuRpcProvider(FeishuTaskService feishuTask) {
 *           super(feishuTask);
 *       }
 *   }
 *   // 启动时:
 *   feishuRpcProvider.publishRpcService(); // 委托给具体 RPC 框架 (Dubbo / gRPC)
 * }</pre>
 *
 * @author zifang
 */
public class ZLcRpcTaskServiceAdapterProvider extends AbstractRpcTaskServiceAdapter {

    /**
     * 本地 TaskService 实现 — 调用方在构造时注入.
     */
    private final AbstractTaskService localTaskService;

    public ZLcRpcTaskServiceAdapterProvider(AbstractTaskService localTaskService) {
        this.localTaskService = localTaskService;
    }

    public AbstractTaskService getLocalTaskService() {
        return localTaskService;
    }

    @Override
    public void sendTodoMsg(MessageDTO messageDTO) {
        if (localTaskService != null) {
            localTaskService.sendTodoMsg(messageDTO);
        }
    }

    @Override
    public void sendDoneMsg(MessageDTO messageDTO) {
        if (localTaskService != null) {
            localTaskService.sendDoneMsg(messageDTO);
        }
    }

    @Override
    public void sendRevokeMsg(MessageDTO messageDTO) {
        if (localTaskService != null) {
            localTaskService.sendRevokeMsg(messageDTO);
        }
    }
}
