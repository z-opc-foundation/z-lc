package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.exception.ZLcPermissionLimitException;
import com.zifang.util.core.encrypt.RsaUtil;
import com.zifang.util.core.io.IOUtil;
import com.zifang.util.core.io.file.FileContentUtil;
import com.zifang.util.core.io.file.FileDirUtil;
import com.zifang.util.core.json.JsonUtil;
import com.zifang.util.core.lang.CollectionUtil;
import com.zifang.util.core.lang.Retry;
import com.zifang.util.core.lang.StringUtil;
import com.zifang.util.core.lang.concurrency.KeyAffinityExecutor;
import com.zifang.util.core.lang.concurrency.MdcThreadPoolExecutor;
import com.zifang.util.core.time.DateUtil;
import com.zifang.util.core.time.LocalDateUtil;
import com.zifang.util.core.time.LocalDateTimeUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * z-util 1.0.13 收口"尺"——防重复实现的回归锁.
 *
 * <p>2026-09 把 {@code z-lc-common/utils} 与 z-util 1.0.13 逐个方法、逐个边界实测后：
 *
 * <h3>第 1 组（已删本地实现, 本锁保证 z-util 不漂移）</h3>
 * <ul>
 *   <li>{@link StringUtil#isEmpty(String)} / {@code isNotEmpty(String)} —— 顶掉
 *       本地 ZLcStringUtil.isBlank / isNotBlank（两者只判 null 与 "",
 *       与 z-util 的 isBlank（含纯空白）不同名不同义, 所以映射到 isEmpty）</li>
 *   <li>{@link DateUtil} / {@link LocalDateTimeUtil} / {@link LocalDateUtil} —— 顶掉
 *       ZLcTimeUtil 的 7 个转换/格式化方法</li>
 *   <li>{@link MdcThreadPoolExecutor} —— 顶掉本地 ZLcMDCThreadPoolExecutor
 *       （构造参数一致; 池内线程 MDC 传播实测一致; 本地版在 CallerRunsPolicy 下
 *       会把调用线程的 MDC 清掉, z-util 版恢复原值, 只强不弱）</li>
 * </ul>
 *
 * <h3>第 2 组（保留本地实现, 本锁钉住差异, 不许拿 z-util 同名方法直接替换）</h3>
 * 详见各用例注释. 结论汇总在 z-lc 收口报告里.
 *
 * @author zifang
 */
class ZLcUtilDedupEquivalenceTest {

    // ==================================================================
    // 第 1 组：已收口项的等价锁
    // ==================================================================

    /** 顶掉 ZLcStringUtil.isBlank/isNotBlank：null 与 "" 为空, 纯空白不算空. */
    @Test
    void blankCheckEquivalentToStrippedLocalImpl() {
        // 本地语义: isBlank(s) == (s == null || s.isEmpty())
        String[] inputs = {null, "", " ", "  ", "\t", "\n", "hello", " a "};
        for (String s : inputs) {
            boolean localIsBlank = (s == null || s.isEmpty());
            assertThat(StringUtil.isEmpty(s))
                    .as("isEmpty 必须等价于已删的 ZLcStringUtil.isBlank, input=%s", s)
                    .isEqualTo(localIsBlank);
            assertThat(StringUtil.isNotEmpty(s))
                    .as("isNotEmpty 必须等价于已删的 ZLcStringUtil.isNotBlank, input=%s", s)
                    .isEqualTo(!localIsBlank);
        }
        // 反证：不能改成 z-util 的 isBlank —— 它把纯空白也算空
        assertThat(StringUtil.isBlank(" ")).isTrue();
        assertThat(StringUtil.isEmpty(" ")).isFalse();
    }

    /** 顶掉 ZLcTimeUtil 的 7 个方法：含 null 入参逐一对齐. */
    @Test
    void timeConversionsEquivalentToStrippedLocalImpl() {
        LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Date date = new Date(1700000000000L);
        LocalDate ld = LocalDate.of(2024, 1, 15);

        assertThat(DateUtil.fromLocalDateTime(ldt))
                .isEqualTo(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
        assertThat(DateUtil.fromLocalDateTime((LocalDateTime) null)).isNull();
        assertThat(DateUtil.toLocalDateTime(date)).isEqualTo(LocalDateTime.ofInstant(
                date.toInstant(), ZoneId.systemDefault()));
        assertThat(DateUtil.toLocalDateTime((Date) null)).isNull();

        // 本地 formatFromDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
        assertThat(DateUtil.format(date))
                .isEqualTo(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        assertThat(DateUtil.format((Date) null)).isNull();

        assertThat(LocalDateTimeUtil.format(ldt)).isEqualTo("2024-01-15 10:30:00");
        assertThat(LocalDateTimeUtil.format((LocalDateTime) null)).isNull();
        assertThat(LocalDateUtil.format(ld)).isEqualTo("2024-01-15");
        assertThat(LocalDateUtil.format((LocalDate) null)).isNull();

        long millis = 1700000000123L;
        Date fromEpoch = DateUtil.fromEpochMilli(millis);
        assertThat(DateUtil.format(fromEpoch)).isEqualTo(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        assertThat(LocalDateUtil.format(fromEpoch.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate())).isEqualTo(java.time.Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    /** z-util 的 todayStartStr/todayEndStr 不是当日起止 —— 这就是 ZLcTimeUtil 只留两个方法的原因. */
    @Test
    void localTodayStartEndStillNeededBecauseZUtilReturnsNow() {
        String local = ZLcTimeUtil.todayStart();
        assertThat(local).endsWith(" 00:00:00");
        assertThat(ZLcTimeUtil.todayEnd()).endsWith(" 23:59:59");

        // DateUtil.getTodayStartStr()/getTodayEndStr() 实测都是 format(new Date(), 默认格式),
        // 即"当前时刻", 且两个方法实现完全相同 —— 不能用来顶掉本地方法.
        assertThat(DateUtil.getTodayStartStr()).isEqualTo(DateUtil.getTodayEndStr());
        assertThat(ZLcTimeUtil.todayStart()).isNotEqualTo(DateUtil.getTodayStartStr());
    }

    /** 顶掉 ZLcMDCThreadPoolExecutor：构造签名与池内传播一致, 且不再污染调用线程 MDC. */
    @Test
    void mdcThreadPoolExecutorEquivalentAndSafer() throws Exception {
        MdcThreadPoolExecutor executor = new MdcThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100),
                new java.util.concurrent.ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r);
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        try {
            MDC.clear();
            assertThat(executor).isInstanceOf(ThreadPoolExecutor.class);

            // 原 ZLcMDCThreadPoolExecutorTest 的 4 个行为用例, 逐条在 z-util 实现上复验
            assertThat(runCapturingMdc(executor, "traceId", "test-trace-123")).isEqualTo("test-trace-123");
            assertThat(runCapturingMdc(executor, null, null)).isNull();
            assertThat(runCapturingMdc(executor, "traceId", "test-trace")).isEqualTo("test-trace");
            assertThat(runPlainTask(executor)).isEqualTo("executed");

            // 差异锁：池满 + CallerRunsPolicy 时, 本地版收尾 MDC.clear() 会把调用线程的上下文清掉,
            // z-util 版恢复原值. 这条只许往好的方向走.
            assertThat(callerRunsPreservesMdc()).isEqualTo("CALLER");
            MDC.clear();
        } finally {
            executor.shutdownNow();
        }
    }

    /** 单线程 + 1 格队列 + CallerRunsPolicy, 第三个任务必然在调用线程就地执行. */
    private String callerRunsPreservesMdc() throws Exception {
        MdcThreadPoolExecutor tiny = new MdcThreadPoolExecutor(
                1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1),
                new java.util.concurrent.ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r);
                    }
                }, new ThreadPoolExecutor.CallerRunsPolicy());
        try {
            MDC.clear();
            MDC.put("traceId", "CALLER");
            CountDownLatch blocker = new CountDownLatch(1);
            tiny.execute(runnables(blocker));      // 占住唯一工作线程
            tiny.execute(runnables(null));         // 占满队列
            MDC.put("traceId", "CALLER");
            tiny.execute(new Runnable() {
                public void run() {
                    /* 队列已满 -> 在调用线程就地执行 */
                }
            });
            return MDC.get("traceId");
        } finally {
            // shutdownNow 会中断还在等 latch 的占位任务, 不留悬线程
            tiny.shutdownNow();
            MDC.clear();
        }
    }

    private Runnable runnables(final CountDownLatch latch) {
        return new Runnable() {
            public void run() {
                if (latch != null) {
                    try {
                        latch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }

    private String runCapturingMdc(MdcThreadPoolExecutor executor, String key, String value)
            throws Exception {
        MDC.clear();
        if (key != null) {
            MDC.put(key, value);
        }
        final List<String> seen = new ArrayList<String>();
        final CountDownLatch done = new CountDownLatch(1);
        executor.execute(new Runnable() {
            public void run() {
                seen.add(MDC.get("traceId"));
                done.countDown();
            }
        });
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        MDC.clear();
        return seen.get(0);
    }

    private String runPlainTask(MdcThreadPoolExecutor executor) throws Exception {
        MDC.clear();
        final List<String> seen = new ArrayList<String>();
        final CountDownLatch done = new CountDownLatch(1);
        executor.execute(new Runnable() {
            public void run() {
                seen.add("executed");
                done.countDown();
            }
        });
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        return seen.get(0);
    }

    // ==================================================================
    // 第 2 组：保留项的差异锁
    // ==================================================================




    /**
     * ZLcRSAUtil 保留的四条实测依据：密钥字符串可互换、短载荷双向可解,
     * 但 (1) z-util 只能生成 1024 位、(2) 本地不能分段加密超 117B 载荷、
     * (3) z-util 加密用平台默认 charset 而解密写死 UTF-8.
     */
    @Test
    void rsaLocalAndZUtilKeyStringsInterchangeableButLimitsDiffer() throws Exception {
        Map<String, Object> zl1024 = ZLcRSAUtil.initKey(1024);
        Map<String, Object> utilKeys = RsaUtil.genKeyPair();

        // 同一份 KeyMap 上, Base64 密钥字符串两侧字节相同（standard base64, 无换行）
        assertThat(ZLcRSAUtil.getPublicKeyStr(zl1024)).isEqualTo(RsaUtil.getPublicKey(zl1024));
        assertThat(ZLcRSAUtil.getPrivateKeyStr(zl1024)).isEqualTo(RsaUtil.getPrivateKey(zl1024));
        assertThat(ZLcRSAUtil.getPublicKeyStr(utilKeys)).isEqualTo(RsaUtil.getPublicKey(utilKeys));
        assertThat(ZLcRSAUtil.getPrivateKeyStr(utilKeys)).isEqualTo(RsaUtil.getPrivateKey(utilKeys));
        // 密钥 map 的槽位名也一致（RSAPublicKey / RSAPrivateKey）
        assertThat(zl1024.keySet()).containsExactlyInAnyOrderElementsOf(utilKeys.keySet());

        String pub = ZLcRSAUtil.getPublicKeyStr(zl1024);
        String pri = ZLcRSAUtil.getPrivateKeyStr(zl1024);

        // 验签闭环：本地生成的密钥对, 在 z-util 上签 + 验必须成立
        byte[] preSign = "z-lc-approval-preSign".getBytes(StandardCharsets.UTF_8);
        assertThat(RsaUtil.verify(preSign, pub, RsaUtil.sign(preSign, pri))).isTrue();
        assertThat(RsaUtil.verify("tampered".getBytes(StandardCharsets.UTF_8), pub,
                RsaUtil.sign(preSign, pri))).isFalse();

        // 跨实现闭环：本地加密 -> z-util 解密（本地写 UTF-8, z-util 解密也读 UTF-8, 平台无关）
        String ascii = "hello-rsa";
        String cn = "低代码平台-中文载荷";
        assertThat(RsaUtil.decryptDataWithPriKey(ZLcRSAUtil.encrypt(ascii, pub), pri)).isEqualTo(ascii);
        assertThat(RsaUtil.decryptDataWithPriKey(ZLcRSAUtil.encrypt(cn, pub), pri)).isEqualTo(cn);
        // z-util 加密 -> 本地解密（只在默认 charset=UTF-8 时成立, 见下方条件断言）
        assertThat(ZLcRSAUtil.decrypt(RsaUtil.encryptedDataWithPubKey(ascii, pub), pri))
                .isEqualTo(ascii);
        assertThat(ZLcRSAUtil.decrypt(ZLcRSAUtil.encrypt(cn, pub), pri)).isEqualTo(cn);
        if (Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
            // z-util 的 encryptedDataWithPubKey 用 data.getBytes()（平台默认）,
            // decryptDataWithPriKey 却写死 UTF-8 —— 非 UTF-8 默认平台上会乱码, 这是 z-util 侧缺陷.
            assertThat(RsaUtil.decryptDataWithPriKey(
                    RsaUtil.encryptedDataWithPubKey(cn, pub), pri)).isEqualTo(cn);
        }

        // 长度上限：本地单次 Cipher.doFinal, 超 117B 直接失败; z-util 分段可过
        final StringBuilder big = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            big.append("0123456789");
        }
        final String longPayload = big.toString();
        final String fpub = pub;
        final String fpri = pri;
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() throws Exception {
                ZLcRSAUtil.encrypt(longPayload, fpub);
            }
        }).isInstanceOf(IllegalBlockSizeException.class);
        assertThat(RsaUtil.decryptDataWithPriKey(
                RsaUtil.encryptedDataWithPubKey(longPayload, fpub), fpri)).isEqualTo(longPayload);

        // 密钥长度：z-util 只有 genKeyPair() 写死 1024, 本地按入参给
        assertThat(((RSAPublicKey) ZLcRSAUtil.initKey(2048).get("RSAPublicKey"))
                .getModulus().bitLength()).isEqualTo(2048);
        assertThat(((RSAPublicKey) utilKeys.get("RSAPublicKey")).getModulus().bitLength())
                .isEqualTo(1024);
    }

    /**
     * ZLcIOUtil / ZLcExportTxtUtil 的错误契约与 z-util 相反：本地吞异常返回 null/""/false,
     * z-util 一律抛 IOException; 建父目录、删不存在目录的行为也不一致.
     */
    @Test
    void ioAndExportErrorContractsDiffer() throws Exception {
        File dir = Files.createTempDirectory("zlc-dedup-io").toFile();
        try {
            File existing = new File(dir, "a.txt");
            Files.write(existing.toPath(), "line1\nline2".getBytes(StandardCharsets.UTF_8));

            // 正常读写字节一致
            assertThat(ZLcIOUtil.readFile(existing.getAbsolutePath()))
                    .isEqualTo(FileContentUtil.readString(existing.getAbsolutePath()))
                    .isEqualTo("line1\nline2");
            InputStream in = new ByteArrayInputStream("abc中文".getBytes(StandardCharsets.UTF_8));
            assertThat(ZLcIOUtil.readStream(in))
                    .isEqualTo(IOUtil.readString(new ByteArrayInputStream(
                            "abc中文".getBytes(StandardCharsets.UTF_8)), "UTF-8"));

            // 差异 1：读流出错 —— 本地返回 null, z-util 抛 IOException
            assertThat(ZLcIOUtil.readStream(boomStream())).isNull();
            assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
                public void call() throws Exception {
                    IOUtil.readString(boomStream(), "UTF-8");
                }
            }).isInstanceOf(IOException.class);

            // 差异 2：文件不存在 —— 本地返回 "", z-util 抛 FileNotFoundException
            String missing = new File(dir, "nope.txt").getAbsolutePath();
            assertThat(ZLcIOUtil.readFile(missing)).isEqualTo("");
            assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
                public void call() throws Exception {
                    FileContentUtil.readString(missing);
                }
            }).isInstanceOf(FileNotFoundException.class);

            // 差异 3：写入时父目录不存在 —— 本地抛 RuntimeException, z-util 自动建父目录
            File deepA = new File(dir, "no/such/x.txt");
            File deepB = new File(dir, "no/such/y.txt");
            assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
                public void call() {
                    ZLcIOUtil.generateFile(deepA.getAbsolutePath(), "x");
                }
            }).isInstanceOf(RuntimeException.class);
            FileContentUtil.writeString(deepB, "x");
            assertThat(deepB).exists();

            // 写字节一致（本地 FileWriter 走平台默认 charset, z-util 写死 UTF-8;
            // 本机默认 UTF-8 才一致 —— 这条断言只在 UTF-8 默认平台上成立）
            File wa = new File(dir, "w-a.txt");
            File wb = new File(dir, "w-b.txt");
            ZLcExportTxtUtil.exportTxtLocal("中文内容", wa.getAbsolutePath());
            FileContentUtil.writeString(wb, "中文内容");
            if (Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
                assertThat(Files.readAllBytes(wa.toPath()))
                        .isEqualTo(Files.readAllBytes(wb.toPath()));
            }

            // 差异 4：递归删 —— 本地返回 boolean 且吞错, z-util 返回 void 且抛 IOException
            File t1 = new File(dir, "t1/inner");
            t1.mkdirs();
            Files.write(new File(t1, "x").toPath(), new byte[]{1});
            assertThat(ZLcIOUtil.deleteDir(t1)).isTrue();
            assertThat(t1).doesNotExist();
            File t2 = new File(dir, "t2/inner");
            t2.mkdirs();
            Files.write(new File(t2, "x").toPath(), new byte[]{1});
            FileDirUtil.deleteDir(t2);
            assertThat(t2).doesNotExist();
            assertThat(ZLcIOUtil.deleteDir(new File(dir, "ghost"))).isFalse();
            FileDirUtil.deleteDir(new File(dir, "ghost2"));   // 不抛

            // 差异 5：建目录 —— 本地忽略失败, z-util 抛 IOException
            File notADir = new File(dir, "a-file");
            Files.write(notADir.toPath(), new byte[]{1});   // 同名普通文件挡路 -> 建不出来
            ZLcExportTxtUtil.createDirectory(new File(dir, "c1/c2").getAbsolutePath());
            assertThat(new File(dir, "c1/c2")).isDirectory();
            assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
                public void call() throws Exception {
                    FileDirUtil.mkdirs(new File(notADir, "sub").getAbsolutePath());
                }
            }).isInstanceOf(IOException.class);
            ZLcExportTxtUtil.createDirectory(new File(notADir, "sub").getAbsolutePath());
            assertThat(notADir).isFile();   // 本地静默失败, 不抛

            // 无对应：classpath 读取 + user.home 目录约定 + .txt 后缀约定
            assertThat(ZLcExportTxtUtil.getFileName("report")).isEqualTo("report.txt");
            assertThat(ZLcExportTxtUtil.getLocalDirectoryPath("z-lc-export"))
                    .isEqualTo(System.getProperty("user.home") + File.separator + "z-lc-export");
        } finally {
            ZLcIOUtil.deleteDir(dir);
        }
    }

    /**
     * ZLcKeyOrderedExecutor 的路由算法与同 key 串行性和 z-util KeyAffinityExecutor 完全一致,
     * 但 null 任务处理相反（本地抛 NPE, z-util 静默丢弃 = 会丢任务）,
     * 且 z-util 打开了 allowCoreThreadTimeOut. 所以本类保留.
     */

    /**
     * ZLcTaskExecutionUtil 两份实现同名不同义, 且都不能直接换成 z-util Retry：
     * utils 那份 ignoreExceptions = "重试这些"（与 Retry.retryOn 同极性, 但会对 cause 解包后再判类型）,
     * aspect 那份 ignoreExceptions = "命中就立刻抛, 不再重试"（极性相反）.
     */
    @Test
    void taskExecutionIgnorePolarityAndCauseUnwrapDifferFromRetry() throws Exception {
        // (1) 重试次数语义一致：3 次尝试、第 3 次成功
        final int[] hits = {0};
        assertThat(ZLcTaskExecutionUtil.execute(new ZLcTaskExecutionUtil.ZLcTask<String>() {
            public String execute() {
                hits[0]++;
                if (hits[0] < 3) {
                    throw new IllegalStateException("x");
                }
                return "ok";
            }
        }, 3, 1L)).isEqualTo("ok");
        assertThat(hits[0]).isEqualTo(3);

        final int[] uHits = {0};
        assertThat(Retry.builder().maxAttempts(3).backoff(new Retry.FixedBackoff(1L)).build()
                .execute(new Retry.ThrowingSupplier<String>() {
                    public String get() {
                        uHits[0]++;
                        if (uHits[0] < 3) {
                            throw new IllegalStateException("x");
                        }
                        return "ok";
                    }
                })).isEqualTo("ok");
        assertThat(uHits[0]).isEqualTo(3);

        // (2) cause 解包：包装成 RuntimeException(cause=ISE) 时, 本地拿 cause 判类型 -> 认为是"可重试",
        //     跑满 2 次后抛原始 RuntimeException; z-util 拿抛出类型判 -> 第 1 次就抛. 重试次数不同.
        final int[] wrapped = {0};
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                ZLcTaskExecutionUtil.execute(new ZLcTaskExecutionUtil.ZLcTask<String>() {
                    public String execute() {
                        wrapped[0]++;
                        throw new RuntimeException(new IllegalStateException("wrapped"));
                    }
                }, 2, 1L, IllegalStateException.class);
            }
        }).isInstanceOf(RuntimeException.class);
        assertThat(wrapped[0]).isEqualTo(2);
        final int[] utilWrapped = {0};
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() throws Exception {
                Retry.builder().maxAttempts(2).backoff(new Retry.FixedBackoff(1L))
                        .retryOn(IllegalStateException.class).build()
                        .execute(new Retry.ThrowingSupplier<String>() {
                            public String get() {
                                utilWrapped[0]++;
                                throw new RuntimeException(new IllegalStateException("wrapped"));
                            }
                        });
            }
        }).isInstanceOf(RuntimeException.class);
        assertThat(utilWrapped[0]).isEqualTo(1);

        // (3) aspect 那份的 ignoreExceptions 是"命中即抛", 与上面相反（ZLcRetryAspect 在用）
        final int[] aspectHits = {0};
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() throws Exception {
                com.zifang.z.lc.common.aspect.ZLcTaskExecutionUtil.execute(
                        new com.zifang.z.lc.common.aspect.ZLcTaskExecutionUtil.ZLcTask<String>() {
                            public String execute() throws Exception {
                                aspectHits[0]++;
                                throw new IllegalArgumentException("bad");
                            }
                        }, 3, 1L, IllegalArgumentException.class);
            }
        }).isInstanceOf(IllegalArgumentException.class);
        assertThat(aspectHits[0]).isEqualTo(1);   // 命中即抛, 一次都不重试

        // (4) attempts=0：本地按 1 次跑, z-util 直接拒绝构造
        final int[] zero = {0};
        ZLcTaskExecutionUtil.execute(new ZLcTaskExecutionUtil.ZLcTask<String>() {
            public String execute() {
                zero[0]++;
                return "done";
            }
        }, 0, 0L);
        assertThat(zero[0]).isEqualTo(1);
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                Retry.builder().maxAttempts(0);
            }
        }).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * ZLcErrorUtil / ZLcExtendUtil / ZLcThreadPoolUtil 无 z-util 对应或异常类型不同：
     * check 抛业务异常 ZLcPermissionLimitException（z-util Assert 只抛
     * IllegalArgumentException）、requireNonNull 的消息是原文（z-util 会拼 " must not be null"）、
     * autoFillExtend 的"空则建默认实例"语义 z-util 没有、三个预置线程池的容量配置 z-util 没有.
     */
    @Test
    void errorExtendAndThreadPoolHaveNoEquivalent() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                ZLcErrorUtil.check(true, "无权限");
            }
        }).isInstanceOf(ZLcPermissionLimitException.class).hasMessage("无权限");

        // z-util Assert.isTrue 抛 IllegalArgumentException —— 异常类型不同, 业务 catch 会被漏掉
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                com.zifang.util.core.lang.Assert.isTrue(false, "无权限");
            }
        }).isInstanceOf(IllegalArgumentException.class).hasMessage("无权限");

        // requireNonNull：两侧都抛 IllegalArgumentException, 但消息文本不同（调用点/用例按原文断言）
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                ZLcErrorUtil.requireNonNull(null, "对象为空");
            }
        }).isInstanceOf(IllegalArgumentException.class).hasMessage("对象为空");
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            public void call() {
                com.zifang.util.core.lang.Assert.notNull(null, "对象为空");
            }
        }).isInstanceOf(IllegalArgumentException.class).hasMessage("对象为空 must not be null");

        // autoFillExtend：空/"null"/非法 JSON 三态, z-util 无对应方法
        assertThat(ZLcExtendUtil.autoFillExtend((String) null, Sample.class)).isNotNull();
        assertThat(ZLcExtendUtil.autoFillExtend("null", Sample.class)).isNotNull();
        assertThat(ZLcExtendUtil.autoFillExtend("{\"name\":\"x\"}", Sample.class).name).isEqualTo("x");
        assertThat(ZLcExtendUtil.autoFillExtend((Sample) null, Sample.class)).isNotNull();
        Sample had = new Sample();
        assertThat(ZLcExtendUtil.autoFillExtend(had, Sample.class)).isSameAs(had);

        // 预置线程池：容量/拒绝策略是本仓业务口径, z-util 无同名常量
        assertThat(ZLcThreadPoolUtil.SIMPLE_EXECUTOR).isInstanceOf(ThreadPoolExecutor.class);
        assertThat(((ThreadPoolExecutor) ZLcThreadPoolUtil.SIMPLE_EXECUTOR).getCorePoolSize())
                .isEqualTo(30);
        assertThat(((ThreadPoolExecutor) ZLcThreadPoolUtil.SIMPLE_EXECUTOR).getMaximumPoolSize())
                .isEqualTo(300);
        assertThat(ZLcThreadPoolUtil.IO_EXECUTOR).isNotNull();
        assertThat(ZLcThreadPoolUtil.SCHEDULED_EXECUTOR).isNotNull();
    }

    /** autoFillExtend 用例载体（无 Lombok, 手写无参构造 + public 字段）. */
    public static class Sample {
        public String name;
    }

    private static InputStream boomStream() {
        return new InputStream() {
            private int i = 0;

            @Override
            public int read() throws IOException {
                if (i++ > 2) {
                    throw new IOException("boom");
                }
                return 'x';
            }
        };
    }

}
