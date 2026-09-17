package com.zifang.z.lc.common.utils;

import java.net.InetAddress;

/**
 * 编号生成器 — 蒸馏自 ace-platform-core
 * {@code OrderNoGenerateUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>基于雪花算法 (Snowflake) 生成唯一编号, 支持 Base62 编码后拼接前缀.
 * 适用于流程工单号、审批单号、数据记录编号等场景.
 *
 * <p>编号格式: {@code 前缀 + Base62(SnowflakeId)}
 * 示例: {@code APP-3Kx7mP2q} (前缀 APP-, 后接 Base62 编码的雪花 ID)
 *
 * <p>蒸馏时移除了对外部 SnowflakeId 类的依赖, 内嵌轻量级雪花实现.
 *
 * @author zifang
 */
public final class ZLcOrderNoGenerator {

    private ZLcOrderNoGenerator() {
    }

    /**
     * 生成编号 = 前缀 + Base62(雪花ID).
     *
     * @param prefix 业务前缀 (如 "WF-", "TASK-")
     * @return 唯一编号
     */
    public static String generate(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        return prefix + Base62.encode(SnowflakeId.nextId());
    }

    // ==================== Base62 编码 ====================

    static class Base62 {
        private static final char[] ALPHABET =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

        static String encode(long value) {
            if (value == 0) {
                return "0";
            }
            StringBuilder sb = new StringBuilder();
            long v = value;
            while (v > 0) {
                int idx = (int) (v % 62);
                sb.append(ALPHABET[idx]);
                v /= 62;
            }
            return sb.reverse().toString();
        }
    }

    // ==================== 雪花 ID 生成器 ====================

    static class SnowflakeId {
        private static final long EPOCH = 1704067200000L; // 2024-01-01
        private static final long NODE_BITS = 10L;
        private static final long SEQ_BITS = 12L;
        private static final long MAX_NODE = ~(-1L << NODE_BITS);
        private static final long MAX_SEQ = ~(-1L << SEQ_BITS);
        private static final long NODE_SHIFT = SEQ_BITS;
        private static final long TIME_SHIFT = SEQ_BITS + NODE_BITS;

        private static final long NODE_ID = initNodeId();
        private static long lastTs = -1L;
        private static long seq = 0L;

        static synchronized long nextId() {
            long ts = System.currentTimeMillis();
            if (ts < lastTs) {
                ts = waitUntil(lastTs);
            }
            if (ts == lastTs) {
                seq = (seq + 1) & MAX_SEQ;
                if (seq == 0) {
                    ts = waitUntil(lastTs + 1);
                }
            } else {
                seq = 0L;
            }
            lastTs = ts;
            return ((ts - EPOCH) << TIME_SHIFT) | (NODE_ID << NODE_SHIFT) | seq;
        }

        private static long waitUntil(long target) {
            long t = System.currentTimeMillis();
            while (t < target) {
                t = System.currentTimeMillis();
            }
            return t;
        }

        private static long initNodeId() {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                int h = Math.abs(host.hashCode());
                return h % (MAX_NODE + 1);
            } catch (Exception e) {
                return (long) (Math.random() * (MAX_NODE + 1));
            }
        }
    }
}
