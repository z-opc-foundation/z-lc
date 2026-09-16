package com.zifang.z.lc.core.adapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MQ 适配器: 调用 z-mq 的消息队列, 在 Pipeline postWrite 阶段发送异步消息.
 * <p>
 * 实现策略:
 * z-mq 当前未对外暴露 REST 接口 (BrokerController 是 Netty TCP),
 * 本适配器先实现"本地队列 + 异步落日志"的能力, 在 z-mq 提供 HTTP 端点后,
 * 替换 send() 内部为 HttpExecutor.post(brokerUrl + "/api/send") 即可.
 * <p>
 * 用法: Pipeline 在 postWrite 阶段调用 publishAsync(dictCode → mqTopic 映射),
 * 适配器不阻塞主流程 (失败仅 WARN), 保证数据写入与消息发送解耦.
 */
@Component
public class MqAdapter implements Adapter {

    public static final String NAME = "mq";
    private static final Logger log = LogManager.getLogger(MqAdapter.class);
    /**
     * 本地消息缓冲, 用于本地 dev / 单元测试 / 离线查看.
     * 实际生产应当从 z-mq NameServer 拉 Broker 地址, 走 Netty 客户端.
     */
    private final ConcurrentLinkedQueue<MqMessage> sentBuffer = new ConcurrentLinkedQueue<>();
    @Value("${z-lc.adapter.mq.broker-url:tcp://localhost:9876}")
    private String brokerUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public void init() {
        log.info("MqAdapter initialized, brokerUrl={} (z-mq has no HTTP API yet, " +
                "using local buffer + async log)", brokerUrl);
    }

    /**
     * 异步发布消息 (非阻塞, 失败仅日志).
     *
     * @param topic 主题 (例如 "data-change-notify")
     * @param tag   标签 (例如 "create" / "update" / "delete")
     * @param body  消息体
     * @return true 入队成功, false 失败
     */
    public boolean publishAsync(String topic, String tag, Object body) {
        if (topic == null || topic.isEmpty()) {
            log.warn("MqAdapter.publishAsync: topic is null, dropping message");
            return false;
        }
        MqMessage msg = new MqMessage();
        msg.topic = topic;
        msg.tag = tag == null ? "" : tag;
        msg.body = body;
        msg.timestamp = LocalDateTime.now();
        try {
            sentBuffer.add(msg);
            log.info("MqAdapter → async send topic={} tag={} (bufferSize={})",
                    topic, tag, sentBuffer.size());
            return true;
        } catch (Exception ex) {
            log.warn("MqAdapter.publishAsync failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 拉取已发送消息 (用于本地调试 / 单元测试).
     */
    public List<MqMessage> drainSent() {
        List<MqMessage> all = new ArrayList<>(sentBuffer);
        sentBuffer.clear();
        return Collections.unmodifiableList(all);
    }

    /**
     * 查看当前缓冲大小.
     */
    public int bufferSize() {
        return sentBuffer.size();
    }

    /**
     * 健康检查: z-mq 当前未提供 HTTP 端点, 始终返回 true (本地模式).
     */
    public boolean ping() {
        // TODO: z-mq 暴露 HTTP 端点后改为 doGet(brokerUrl + "/api/health")
        return true;
    }

    /**
     * 消息载体.
     */
    public static class MqMessage {
        public String topic;
        public String tag;
        public Object body;
        public LocalDateTime timestamp;

        @Override
        public String toString() {
            return "MqMessage{topic='" + topic + "', tag='" + tag
                    + "', timestamp=" + timestamp + ", body=" + body + "}";
        }
    }
}
