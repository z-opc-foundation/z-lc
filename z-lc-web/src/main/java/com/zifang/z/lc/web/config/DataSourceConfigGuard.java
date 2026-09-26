package com.zifang.z.lc.web.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 启动时校验数据源配置，配置不成串就拒绝起来.
 * <p>
 * 为什么要有这个类（09-26 在部署件上实测的三种"配错但照样起来"的形态）：
 * <ul>
 *   <li>{@code SPRING_DATASOURCE_URL} 没设 ⇒ url 解析成 application.yml 的占位字面量
 *       {@code HIDE_IN_REPO}；Druid 默认 {@code initial-size=0}，init 阶段一条连接都不试，
 *       于是打出 {@code {dataSource-1} inited} 就起来了，{@code /api/lc/health} 回 200 UP，
 *       第一条业务查询才 {@code http=500}。把 {@code initial-size} 压成 1 同一条命令立刻
 *       炸在 {@code rawConn is null} —— 证明"起得来"和"连得上"本来就没关系。</li>
 *   <li>{@code SPRING_DATASOURCE_URL=} 设成空串 ⇒ Boot 见 classpath 上有 h2 jar 就自己生成一个
 *       {@code jdbc:h2:mem:<随机>}（实测：只把驱动换成 org.h2.Driver 就能干净启动 2.937s），
 *       数据写进一个每次重启就蒸发的库，而健康检查照样 UP。</li>
 *   <li>驱动与 url 对不上（把 LC 池的 url 换成 {@code jdbc:mysql://…} 却留着 dev 的
 *       {@code org.h2.Driver}）⇒ {@code driver.connect(url, …)} 对不受理的 url 是<b>返回 null
 *       而不是抛异常</b>，活的 Druid 拿到 null 就在建连线程里 NPE 且<b>无退避</b>死循环：实测
 *       8 分钟刷出 13 GB 日志。这一支必须拦在建池之前，见 postProcessBeanFactory 上那段。</li>
 * </ul>
 * 判据只钉"配置形状"，不钉"连不连得上"：库暂时起不来（主从切换、维护窗口）时进程要能起来并
 * 由 {@link com.zifang.z.lc.web.health.DataSourceHealthProber} 报 DOWN；但一个根本不是
 * {@code jdbc:} 串、或者是空串的值，无论库在不在都永远连不上，属于纯粹的配错，没有"等一等就好"的
 * 余地，所以直接 fail fast。
 * <p>
 * 属性完全没出现（例如只靠 classpath 自动配置出内嵌库的测试上下文）时不拦：那是 Boot 的正常行为，
 * 拦了会把测试上下文一起搞挂。这里只拦"配了，但配成了一个永远连不上的值"。
 */
@Component
public class DataSourceConfigGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    /** 占位字面量：application.yml 用它兜底，一旦真的解析到这个串就说明环境变量没设上. */
    static final String PLACEHOLDER = "HIDE_IN_REPO";

    private Environment env;

    /**
     * 容器走这条：BeanFactoryPostProcessor 的实例化发生在 {@code AutowiredAnnotationBeanPostProcessor}
     * 注册<b>之前</b>，所以 @Autowired 的构造器注入对它不生效（实测报
     * {@code No default constructor found} 直接把上下文搞挂）。Environment 由
     * {@code prepareBeanFactory} 就挂好的 {@code ApplicationContextAwareProcessor} 递过来。
     */
    public DataSourceConfigGuard() {
    }

    /** 单测走这条：直接喂 MockEnvironment（装配层另有一例验 Aware 那条路真的接通）。 */
    DataSourceConfigGuard(Environment env) {
        this.env = env;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    /**
     * 校验挂在 BeanFactoryPostProcessor 而不是 @PostConstruct：@PostConstruct 只在<b>这个 bean 自己</b>
     * 被创建之后跑，而 bean 的创建顺序由容器决定 —— 主数据源那个 Druid 池完全可能排在它前面先
     * init()。那正是我在 java 层撞过一次 13 GB 日志的形状（把 HIDE_IN_REPO 交给活的 Druid 建连，
     * driver.connect() 返回 null ⇒ 建连线程在 rawConn is null 上无退避死循环）。
     * BeanFactoryPostProcessor 在<b>任何</b>单例被实例化之前跑完，所以"闸在池之前"是结构保证
     * 而不是运气；装配层的顺序断言在 LcStartupRefusalTest。
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (env == null) {
            // 宁可拒起也不静默放行：这条如果反过来写（env 没接到就当没事），整道闸就成了一句空话。
            throw new IllegalStateException(
                    "z-lc 拒绝启动：DataSourceConfigGuard 没有拿到 Environment，"
                            + "于是没法判断 spring.datasource.url 是不是一个真的 JDBC 串。");
        }
        validateMainDataSourceUrl();
    }

    void validateMainDataSourceUrl() {
        if (!env.containsProperty("spring.datasource.url")) {
            return;
        }
        String url = env.getProperty("spring.datasource.url");
        if (!isUsable(url)) {
            throw new IllegalStateException(
                    "z-lc 拒绝启动：主数据源 spring.datasource.url 不是一个可用的 JDBC 串，当前值 = " + describe(url)
                            + "。这个值来自 ${SPRING_DATASOURCE_URL:" + PLACEHOLDER + "}，说明环境变量 SPRING_DATASOURCE_URL"
                            + " 没设上（或被设成了空串）。");
        }
        String driver = env.getProperty("spring.datasource.driver-class-name");
        String mismatch = driverUrlMismatch(driver, url);
        if (mismatch != null) {
            throw new IllegalStateException(
                    "z-lc 拒绝启动：主数据源的驱动与 JDBC 串对不上 —— " + mismatch
                            + "。这一支必须拦在建池之前：驱动对不受理的 url 是返回 null 而不是抛异常，实测那样活的"
                            + " Druid 建连线程会在 rawConn is null 上无退避死循环（8 分钟 13 GB 日志）。"
                            + " 改 spring.datasource.driver-class-name，或者把 url 换回这个驱动受理的那一种。");
        }
    }

    /** 值是不是"至少像个 JDBC 串"：非空、去空白后以 jdbc: 开头. */
    public static boolean isUsable(String url) {
        return url != null && url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:");
    }

    /**
     * 驱动与 url 配不配套；<b>只有两边都认得、且明显对不上</b>才判不匹配，返回 null 表示"不拦"。
     * <p>
     * 放过任何一侧不认识的值是有意的：这条判据两头的失效代价不对称 —— 误拦会让一个本来能跑的部署
     * 起不来（还是在客户机器上），漏判只是回到"让 Druid 自己去撞"。所以对没见过的驱动名（各家
     * shaded 驱动）一律不瞎猜；非 ASCII 环境（tr_TR 的 {@code toLowerCase} 会把 I 变成 ı）最坏也
     * 只落到"不判"这一侧，不会凭空拦下一个本来能跑的串。
     */
    public static String driverUrlMismatch(String driverClassName, String url) {
        String family = driverFamily(driverClassName);
        String scheme = urlScheme(url);
        if (family == null || scheme == null || family.equals(scheme)) {
            return null;
        }
        // mariadb 驱动受理 jdbc:mysql://（它自己的兼容档），这一对不算对不上。
        if ("mariadb".equals(family) && "mysql".equals(scheme)) {
            return null;
        }
        return "驱动 " + driverClassName + " 受理 " + family + " 系列的 url，而这里配的是 "
                + scheme + " 系列：" + mask(url);
    }

    /** 驱动类名里的"家族"段：按非字母数字切开后<b>整段</b>匹配，绝不用子串（shaded 包名会自己撞上来）. */
    static String driverFamily(String driverClassName) {
        if (driverClassName == null) {
            return null;
        }
        for (String seg : driverClassName.trim().toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if ("mysql".equals(seg) || "mariadb".equals(seg) || "h2".equals(seg)
                    || "postgresql".equals(seg)) {
                return seg;
            }
        }
        return null;
    }

    /** url 的子协议（jdbc:&lt;scheme&gt;:）；只认这一族用过的那几种，其余回 null（= 不判）。 */
    static String urlScheme(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (!u.startsWith("jdbc:")) {
            return null;
        }
        int end = u.indexOf(':', 5);
        String scheme = end < 0 ? u.substring(5) : u.substring(5, end);
        if ("mysql".equals(scheme) || "mariadb".equals(scheme) || "h2".equals(scheme)
                || "postgresql".equals(scheme)) {
            return scheme;
        }
        return null;
    }

    /** 给报错用的值描述：绝不把带凭证的原串抄进异常/日志. */
    public static String describe(String url) {
        if (url == null) {
            return "（未配置）";
        }
        if (url.trim().isEmpty()) {
            return "（空串）";
        }
        return mask(url);
    }

    /**
     * 抹掉 JDBC 串里的凭证：查询参数（{@code ?}/{@code ;} 之后）和 {@code scheme://user:pass@} 的
     * userinfo 段一律去掉，只留 scheme + host + port + 库名。
     */
    public static String mask(String url) {
        if (url == null) {
            return "unknown";
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return "unknown";
        }
        int cut = u.length();
        int q = u.indexOf('?');
        if (q >= 0) {
            cut = Math.min(cut, q);
        }
        int s = u.indexOf(';');
        if (s >= 0) {
            cut = Math.min(cut, s);
        }
        u = u.substring(0, cut);
        int at = u.lastIndexOf('@');
        int scheme = u.indexOf("://");
        if (at >= 0 && scheme >= 0 && scheme < at) {
            u = u.substring(0, scheme + 3) + u.substring(at + 1);
        }
        return u;
    }
}
