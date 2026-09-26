package com.zifang.z.lc.web.it;

import com.zifang.z.lc.web.config.DataSourceConfigGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 #52 的装配层：闸「存在」和闸「真的在启动路径上」是两件事.
 * <p>
 * 单测直接调 {@code validateMainDataSourceUrl()} 只能证明这个方法会拒绝；把闸从
 * {@code BeanFactoryPostProcessor} 挪回 {@code @PostConstruct}（或者干脆摘掉），产品照样带病
 * 启动而单测一支不红 —— 这正是当初 {@code /api/lc/health} 硬编码 UP 能活很久的机制。所以这里
 * 验的是"闸在启动路径上，而且跑在池前面"：主池那两闸用不含 DataSource 的微上下文跑（真上下文
 * 跑过一次，代价见下面 guardRunsBeforeAnyDataSourceIsCreated 的注释），模块池那一闸用真装配 +
 * 命令行参数（优先级最高，压过测试用 application.properties）实测。
 * <p>
 * 第三例是阳性对照：同一套装配在配置正常时必须起得来。没有它，前两例的「拒绝」
 * 可能是任何别的原因造成的假绿。
 */
class LcStartupRefusalTest {

    private static String H2(String name) {
        return "jdbc:h2:mem:" + name + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }

    /** 把异常链上所有 message 串起来 —— Spring 会把闸的报错包进 BeanCreationException. */
    private static String chain(Throwable t) {
        List<String> seen = new ArrayList<>();
        for (Throwable cur = t; cur != null && seen.size() < 20; cur = cur.getCause()) {
            seen.add(cur.getClass().getSimpleName() + ": " + cur.getMessage());
        }
        return String.join(" <- ", seen);
    }

    @Test
    @DisplayName("缺陷#52：主池那道闸必须挂在 Bean 生命周期上（摘掉 @PostConstruct 就得红）")
    void guardRunsAsPartOfTheBeanLifecycle() {
        // 刻意不把 HIDE_IN_REPO 喂给一个活的 Druid 去起完整上下文：实测那样 driver.connect() 返回 null,
        // Druid 的 create-connection 线程会 NPE 且**无退避**地死循环（8 分钟刷出 13 GB 日志）。
        // 闸的价值恰恰是别让那种池子建起来，所以这里用一个不含任何 DataSource 的微上下文验装配。
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "defect52", Collections.<String, Object>singletonMap("spring.datasource.url", "HIDE_IN_REPO")));
        ctx.register(DataSourceConfigGuard.class);

        Throwable t = assertThrows(Throwable.class, ctx::refresh);
        String msg = chain(t);
        assertTrue(msg.contains("SPRING_DATASOURCE_URL"),
                "报错要说出该修哪个环境变量，否则运维只能对着 stack trace 猜: " + msg);
        assertTrue(msg.contains("拒绝启动"), "闸要说破自己拒了什么: " + msg);
    }

    @Test
    @DisplayName("缺陷#52：闸必须跑在任何 DataSource 被创建之前（BeanFactoryPostProcessor，不是 @PostConstruct）")
    void guardRunsBeforeAnyDataSourceIsCreated() {
        // "闸在不在启动路径上"和"闸在不在池前面"是两条不同的账。@PostConstruct 那一版只满足前者：
        // 它跑在<b>这个 bean 自己</b>创建之后，而 bean 顺序由容器决定 —— 真事故就是把 HIDE_IN_REPO
        // 交给活的 Druid 先 init（driver.connect() 返回 null ⇒ 建连线程无退避死循环，8 分钟 13 GB
        // 日志），闸后跑到只是没脸见人了。所以这里把一个"被实例化就记一笔"的 DataSource 抢在闸
        // 前面注册：走 @PostConstruct 的话 created 一定是 true（本例判红），只有
        // BeanFactoryPostProcessor 能在任何单例出生之前就拒掉。
        final boolean[] created = {false};
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "defect52order", Collections.<String, Object>singletonMap("spring.datasource.url", "HIDE_IN_REPO")));
        ctx.registerBean(javax.sql.DataSource.class, () -> {
            created[0] = true;
            // 不设 url：万一它真被创建，也不会去连任何库（这一例要量的是"有没有被创建"，不是连接）。
            return new com.alibaba.druid.pool.DruidDataSource();
        });
        ctx.register(DataSourceConfigGuard.class);

        assertThrows(Throwable.class, ctx::refresh);
        assertTrue(!created[0], "数据源在闸之前就出生了 —— 那 13 GB 日志的形状就是这么来的");
    }

    @Test
    @DisplayName("缺陷#52：LC 池的 url 配成占位字面量时真上下文必须拒绝，并点名 z.base.db.lc.jdbc-url")
    void refusesPlaceholderLcUrlAtStartup() {
        Throwable t = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(LcTestApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + H2("refuse_lc"),
                        // 主池给正常串，坏值只放在模块池上 —— 红的一定是模块池那道闸
                        "--z.base.db.lc.jdbc-url=HIDE_IN_REPO"));
        String msg = chain(t);
        assertTrue(msg.contains("dataSourceLc"), "要点名是哪一个池: " + msg);
        assertTrue(msg.contains("z.base.db.lc.jdbc-url"), "要点名该修哪个属性: " + msg);
        assertTrue(msg.contains("localhost:3306"), "要说破那条被偷偷编出来的串: " + msg);
    }

    @Test
    @DisplayName("阳性对照：同一套装配在两个池都配成真实 JDBC 串时必须起得来（否则前两例是空跑）")
    void bootsWhenBothPoolsAreConfigured() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(LcTestApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + H2("okctl"),
                        "--z.base.db.lc.jdbc-url=" + H2("okctl"));
        try {
            assertTrue(ctx.getBeansOfType(javax.sql.DataSource.class).size() >= 2,
                    "上下文里两个池都该在: " + ctx.getBeansOfType(javax.sql.DataSource.class).keySet());
        } finally {
            ctx.close();
        }
    }
}
