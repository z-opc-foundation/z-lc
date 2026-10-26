package com.zifang.z.lc.datasource.config;

import com.zifang.z.dms.core.model.DataSourceDef;
import com.zifang.z.dms.core.model.DataSourceType;
import com.zifang.z.dms.core.service.DataSourceService;
import com.zifang.z.dms.core.service.RegistrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开发自检种子（仅 {@code z.lc.dms.seed=true} 时装配）。
 * <p>
 * 往 z-dms 目录注册 {@code dev-h2}：与 z-lc-admin dev profile 的主库（H2 mem）
 * <b>同一实例</b>，用于端到端验证"按数据源 id 扫表/看字段"这条新链路。
 * 凭据走 {@code z.dms.credential.dev.h2}（Environment 解析器，JSON 形态），
 * 本类零口令落地。幂等：目录里已有 dev-h2 就跳过。
 */
public class DmsDevSeed implements ApplicationRunner {

    public static final String SEED_ID = "dev-h2";

    private static final Logger log = LoggerFactory.getLogger(DmsDevSeed.class);

    private final ObjectProvider<DataSourceService> serviceProvider;
    private final Environment environment;

    public DmsDevSeed(ObjectProvider<DataSourceService> serviceProvider, Environment environment) {
        this.serviceProvider = serviceProvider;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        DataSourceService service = serviceProvider.getIfAvailable();
        if (service == null) {
            log.warn("z.lc.dms.seed=true 但 z-dms DataSourceService 不在位，种子跳过");
            return;
        }
        if (service.find(SEED_ID) != null) {
            log.info("z-dms 目录已有 [{}]，种子跳过", SEED_ID);
            return;
        }
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:h2:")) {
            log.warn("种子期望 dev H2 主库（spring.datasource.url 以 jdbc:h2: 开头），实际不满足，跳过");
            return;
        }
        Map<String, String> props = new LinkedHashMap<>();
        props.put("jdbc-url", url);
        props.put("driver-class-name", "org.h2.Driver");
        DataSourceDef def = new DataSourceDef(SEED_ID, "孵化自检 H2（与 dev 主库同实例）",
                DataSourceType.JDBC, "dev/h2", props);
        try {
            RegistrationResult result = service.register(def);
            log.info("z-dms 种子源 [{}] 注册完成，探活 {}", SEED_ID, result.isUp() ? "UP" : "DOWN(见 z-dms 日志)");
        } catch (Exception e) {
            log.error("z-dms 种子源 [{}] 注册失败: {}", SEED_ID, e.getMessage());
        }
    }
}
