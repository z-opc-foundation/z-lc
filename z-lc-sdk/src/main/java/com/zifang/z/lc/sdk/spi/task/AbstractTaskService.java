package com.zifang.z.lc.sdk.spi.task;

import com.zifang.z.lc.common.dto.task.MessageDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 任务服务 SPI 抽象基类 — 蒸馏自 ace-platform-engine {@code AbstractTaskService}
 * （{@code com.c2f.ace.engine.define}），行为完全对齐.
 *
 * <p>业务方继承本类并标 {@link TaskServiceInfo} 注解 — 默认实现仅打日志，
 * 业务方按需覆盖 3 个 send*Msg 方法.
 *
 * <p>典型用法（业务方接入飞书 / 钉钉 / 邮件）：
 * <pre>{@code
 *   @Service
 *   @TaskServiceInfo(identityCode = "feishu", exportRpc = true)
 *   public class FeishuTaskService extends AbstractTaskService {
 *       &#64;Override
 *       public void sendTodoMsg(MessageDTO dto) {
 *           feishuClient.sendCard(dto.getTaskInfo().getAssigneeList(), ...);
 *       }
 *   }
 * }</pre>
 *
 * @author xuhf (distilled by zifang)
 */
public abstract class AbstractTaskService implements TaskService {

    private static final Logger log = LogManager.getLogger(AbstractTaskService.class);

    @Override
    public void sendTodoMsg(MessageDTO messageDTO) {
        log.info("AbstractTaskService.sendTodoMsg default impl — taskId={}, assigneeCount={}",
                messageDTO != null && messageDTO.getTaskInfo() != null
                        ? messageDTO.getTaskInfo().getTaskId() : null,
                messageDTO != null && messageDTO.getTaskInfo() != null
                        && messageDTO.getTaskInfo().getAssigneeList() != null
                        ? messageDTO.getTaskInfo().getAssigneeList().size() : 0);
    }

    @Override
    public void sendDoneMsg(MessageDTO messageDTO) {
        log.info("AbstractTaskService.sendDoneMsg default impl — taskId={}",
                messageDTO != null && messageDTO.getTaskInfo() != null
                        ? messageDTO.getTaskInfo().getTaskId() : null);
    }

    @Override
    public void sendRevokeMsg(MessageDTO messageDTO) {
        log.info("AbstractTaskService.sendRevokeMsg default impl — taskId={}",
                messageDTO != null && messageDTO.getTaskInfo() != null
                        ? messageDTO.getTaskInfo().getTaskId() : null);
    }
}
