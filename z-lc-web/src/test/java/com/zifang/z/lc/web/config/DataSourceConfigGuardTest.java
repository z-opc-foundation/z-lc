package com.zifang.z.lc.web.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 缺陷 #52 的配置层：坏得一眼看出来的数据源配置必须在启动时就拒绝.
 * <p>
 * 实测过的两种"起来了但其实什么都干不了"：
 * {@code SPRING_DATASOURCE_URL} 没设 ⇒ url 是字面量 {@code HIDE_IN_REPO}，Druid
 * {@code initial-size=0} 于是 {@code inited} 照打、health 照回 UP、第一条业务查询 500；
 * 设成空串 ⇒ Boot 见 classpath 有 h2 就自己造一个 {@code jdbc:h2:mem:<随机>}，
 * 数据写进一个重启就蒸发的库。
 * 另一条是 {@code dataSourceLc} 在三个 jdbc-url 都没配时偷偷退回
 * {@code jdbc:mysql://localhost:3306/}（库名为空、用户 root、密码空）。
 */
class DataSourceConfigGuardTest {

    @Test
    @DisplayName("缺陷#52：url 是占位字面量 HIDE_IN_REPO ⇒ 拒绝启动，报错点名是哪个环境变量")
    void rejectsPlaceholderUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", "HIDE_IN_REPO");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new DataSourceConfigGuard(env).validateMainDataSourceUrl());

        assertTrue(ex.getMessage().contains("SPRING_DATASOURCE_URL"),
                "报错要直接说出该设哪个环境变量: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("HIDE_IN_REPO"),
                "报错要回显解析到的坏值, 否则运维无从对照: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：url 设成空串 ⇒ 同样拒绝（那种情况 Boot 会偷偷造一个用完即弃的内存库）")
    void rejectsBlankUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new DataSourceConfigGuard(env).validateMainDataSourceUrl());
        assertTrue(ex.getMessage().contains("空串"), "要说清是空串而不是没配: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：属性压根没出现时不拦 —— 只靠 classpath 起内嵌库的测试上下文是正常形态")
    void leavesAbsentPropertyAlone() {
        MockEnvironment env = new MockEnvironment();
        new DataSourceConfigGuard(env).validateMainDataSourceUrl();
    }

    @Test
    @DisplayName("缺陷#52：正常的 jdbc:mysql / jdbc:h2 串一律放行（闸只管形状，不管连不连得上）")
    void acceptsRealJdbcUrls() {
        MockEnvironment mysql = new MockEnvironment();
        mysql.setProperty("spring.datasource.url",
                "jdbc:mysql://127.0.0.1:33061/z_lc?useSSL=false&serverTimezone=UTC");
        new DataSourceConfigGuard(mysql).validateMainDataSourceUrl();

        MockEnvironment h2 = new MockEnvironment();
        h2.setProperty("spring.datasource.url", "jdbc:h2:mem:zlc;MODE=MySQL");
        new DataSourceConfigGuard(h2).validateMainDataSourceUrl();
    }

    @Test
    @DisplayName("缺陷#52：判「像个连接串」的口径本身也要钉住，别让它漂成什么都算")
    void usablePredicateIsPinned() {
        assertTrue(DataSourceConfigGuard.isUsable("jdbc:mysql://h/db"));
        assertTrue(DataSourceConfigGuard.isUsable("  JDBC:H2:MEM:x  "), "大小写不该影响判定");
        assertFalse(DataSourceConfigGuard.isUsable(null));
        assertFalse(DataSourceConfigGuard.isUsable(""));
        assertFalse(DataSourceConfigGuard.isUsable("   "), "纯空白不是串");
        assertFalse(DataSourceConfigGuard.isUsable("HIDE_IN_REPO"));
        assertFalse(DataSourceConfigGuard.isUsable("mysql://h/db"), "少了 jdbc: 前缀就不是 JDBC 串");
    }

    @Test
    @DisplayName("缺陷#52：报错/健康检查里回显 url 时，凭证一个字都不许跟着出去")
    void describeNeverEchoesCredentials() {
        String masked = DataSourceConfigGuard.describe(
                "jdbc:mysql://root:S3cr3tInUserinfo@127.0.0.1:33061/z_lc?password=S3cr3tInParams");
        assertFalse(masked.contains("S3cr3tInUserinfo"), masked);
        assertFalse(masked.contains("S3cr3tInParams"), masked);
        assertEquals("jdbc:mysql://127.0.0.1:33061/z_lc", masked);
        assertEquals("（空串）", DataSourceConfigGuard.describe("  "));
        assertEquals("（未配置）", DataSourceConfigGuard.describe(null));
    }

    /* ------------------------------------------------------------------ */

    private static MockEnvironment emptyLcEnv() {
        return new MockEnvironment();
    }

    @Test
    @DisplayName("缺陷#52：三个 jdbc-url 都没配 ⇒ dataSourceLc 拒绝启动，不再偷偷去连 localhost:3306")
    void lcPoolRefusesToFabricateLocalhostUrl() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new LcModuleDataSource().dataSource(emptyLcEnv()));

        assertTrue(ex.getMessage().contains("z.base.db.lc.jdbc-url"),
                "要点名该配哪个属性: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("localhost:3306"),
                "要说破那条被偷偷编出来的串: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：占位字面量传到 LC 池时同样拒，且不回显密码")
    void lcPoolRejectsPlaceholderWithoutLeakingPassword() {
        MockEnvironment env = emptyLcEnv();
        env.setProperty("spring.datasource.url", "mysql://root:S3cr3tInUserinfo@h/db?password=S3cr3tInParams");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new LcModuleDataSource().dataSource(env));
        assertFalse(ex.getMessage().contains("S3cr3tInUserinfo"), "异常信息也是对外面: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("S3cr3tInParams"), "异常信息也是对外面: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：单库部署（只写 spring.datasource.*，例如 local profile 示例）LC 池要接得上")
    void lcPoolFallsBackToMainDataSourceUrl() {
        MockEnvironment env = emptyLcEnv();
        env.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:33061/z_lc?useSSL=false");
        env.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        env.setProperty("spring.datasource.username", "zlc");

        DataSource ds = new LcModuleDataSource().dataSource(env);

        DruidDataSource druid = (DruidDataSource) ds;
        assertEquals("jdbc:mysql://127.0.0.1:33061/z_lc?useSSL=false", druid.getUrl());
        assertEquals("com.mysql.cj.jdbc.Driver", druid.getDriverClassName());
        assertEquals("zlc", druid.getUsername());
    }

    @Test
    @DisplayName("缺陷#52：显式的 z.base.db.lc.jdbc-url 优先级最高，且驱动/凭证按模块走")
    void lcPoolPrefersExplicitModuleUrl() {
        MockEnvironment env = emptyLcEnv();
        env.setProperty("spring.datasource.url", "jdbc:mysql://should-not-be-used:3306/x");
        env.setProperty("z.base.db.default.jdbc-url", "jdbc:mysql://neither-nor:3306/y");
        env.setProperty("z.base.db.lc.jdbc-url", "jdbc:h2:mem:lc;MODE=MySQL");
        env.setProperty("z.base.db.lc.driver-class-name", "org.h2.Driver");
        env.setProperty("z.base.db.lc.username", "sa");

        DruidDataSource druid = (DruidDataSource) new LcModuleDataSource().dataSource(env);

        assertEquals("jdbc:h2:mem:lc", druid.getUrl().substring(0, "jdbc:h2:mem:lc".length()));
        assertEquals("org.h2.Driver", druid.getDriverClassName());
        assertEquals("sa", druid.getUsername());
    }

    @Test
    @DisplayName("缺陷#52：运维显式给了 host + database 时仍按 z-boot 模板拼串（那是他要的串，不是我们猜的）")
    void explicitHostAndDatabaseStillUsesTemplate() {
        MockEnvironment env = emptyLcEnv();
        env.setProperty("z.base.db.lc.host", "rds.internal");
        env.setProperty("z.base.db.lc.database", "z_lc");

        DruidDataSource druid = (DruidDataSource) new LcModuleDataSource().dataSource(env);

        assertTrue(druid.getUrl().startsWith("jdbc:mysql://rds.internal:3306/z_lc?"),
                "模板路径要照显式值拼: " + druid.getUrl());
    }

    /* ------------------------------------------------------------------ */
    /* 驱动与 url 的配套性：不受理对方的两半都能凑出"活 Druid + null 连接" */

    @Test
    @DisplayName("缺陷#52：主池的驱动与 url 对不上（mysql 串 + h2 驱动）⇒ 拒绝，且不跟着 url 出凭证")
    void mainPoolRefusesDriverUrlMismatch() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url",
                "jdbc:mysql://root:S3cr3tInUserinfo@127.0.0.1:33061/z_lc?password=S3cr3tInParams");
        env.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new DataSourceConfigGuard(env).validateMainDataSourceUrl());
        assertTrue(ex.getMessage().contains("driver-class-name"),
                "要点名该改哪个属性: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("S3cr3tInUserinfo"), "异常也是对外面: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("S3cr3tInParams"), "异常也是对外面: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：反方向同样拒（h2 串 + mysql 驱动）—— 这一对实测会让 driver.connect() 返回 null")
    void lcPoolRefusesDriverUrlMismatchTheOtherWay() {
        MockEnvironment env = emptyLcEnv();
        env.setProperty("z.base.db.lc.jdbc-url", "jdbc:h2:mem:lc;MODE=MySQL");
        env.setProperty("z.base.db.lc.driver-class-name", "com.mysql.cj.jdbc.Driver");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new LcModuleDataSource().dataSource(env));
        assertTrue(ex.getMessage().contains("z.base.db.lc.driver-class-name"),
                "要说破是哪一条链上的驱动: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("rawConn is null"),
                "要写清为什么拦在池起来之前: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：fallback 链凑出来的不匹配也要拒（只换 LC 的 url、驱动跟着 dev 走 h2）")
    void mismatchViaFallbackChainIsAlsoRefused() {
        MockEnvironment env = emptyLcEnv();
        // 单库 fallback 拿来了 mysql 串，而 dev profile 的 spring.datasource.driver-class-name 是 h2。
        env.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:33061/z_lc?useSSL=false");
        env.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        assertThrows(IllegalStateException.class, () -> new LcModuleDataSource().dataSource(env));
    }

    @Test
    @DisplayName("缺陷#52：判据的失效方向要落在「不拦」那一边 —— 没见过的驱动/子协议一律放过")
    void unknownDriverOrSchemeIsLeftAlone() {
        // 各家 shaded 驱动：名字里没有认得的段 ⇒ 不瞎猜（误拦会把能跑的部署挡在门外）。
        assertNull(DataSourceConfigGuard.driverUrlMismatch("com.acme.shaded.MyDriver", "jdbc:mysql://h/db"));
        assertNull(DataSourceConfigGuard.driverUrlMismatch(null, "jdbc:mysql://h/db"));
        assertNull(DataSourceConfigGuard.driverUrlMismatch("oracle.jdbc.OracleDriver", "jdbc:h2:mem:x"));
        assertNull(DataSourceConfigGuard.driverUrlMismatch("org.h2.Driver", "jdbc:oracle:thin:@h:1521/db"));
        // mariadb 驱动受理 jdbc:mysql://（兼容档），这一对不算对不上。
        assertNull(DataSourceConfigGuard.driverUrlMismatch("org.mariadb.jdbc.Driver", "jdbc:mysql://h/db"));
        // 认得且对不上的两支，必须判出来：
        assertEquals("h2", DataSourceConfigGuard.driverFamily("org.h2.Driver"));
        assertEquals("mysql", DataSourceConfigGuard.driverFamily("com.mysql.cj.jdbc.Driver"));
        assertEquals("mysql", DataSourceConfigGuard.urlScheme("JDBC:MySQL://h/db"));
        assertNotNull(DataSourceConfigGuard.driverUrlMismatch("org.h2.Driver", "jdbc:mysql://h/db"));
    }

    @Test
    @DisplayName("缺陷#52：闸自己没接到 Environment 时必须拒起，不能当成「没什么要查的」静默放行")
    void refusesToRunBlindWithoutEnvironment() {
        // BeanFactoryPostProcessor 在容器的 @Autowired 基础设施就绪之前就要实例化（实测
        // No default constructor found），所以 Environment 走的是 EnvironmentAware 那条接线。
        // 那条一旦没接上，env 就是 null —— 若按"null 就跳过"写，整道闸会退化成一句空话而全绿。
        DataSourceConfigGuard blind = new DataSourceConfigGuard();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> blind.postProcessBeanFactory(null));
        assertTrue(ex.getMessage().contains("Environment"), "要说破是闸自己没接上: " + ex.getMessage());
    }

    @Test
    @DisplayName("缺陷#52：配套的正常组合一律放行（三种真用得到的写法）")
    void consistentPairsAreLetThrough() {
        assertNull(DataSourceConfigGuard.driverUrlMismatch("com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://127.0.0.1:33061/z_lc?useSSL=false"));
        assertNull(DataSourceConfigGuard.driverUrlMismatch("org.h2.Driver", "jdbc:h2:mem:zlc;MODE=MySQL"));
        assertNull(DataSourceConfigGuard.driverUrlMismatch("org.postgresql.Driver", "jdbc:postgresql://h/db"));
    }
}
