package com.zifang.z.lc.web.config;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 守卫 z-lc **不向 Spring 容器注册通用裸名的 mapper bean**（卡 {@code TASK-20261024-113}）。
 *
 * <p><b>它守的是哪条线</b>：MyBatis-Spring 的 {@code ClassPathMapperScanner} 用 Spring 的
 * {@code AnnotationBeanNameGenerator} 给 mapper 命名 ⇒ <b>bean 名 = 首字母小写的接口简名</b>。
 * 别的模块只要有一个 {@code @Resource} 字段名写成 {@code permissionMapper}，
 * {@code @Resource} 的「按名优先」就会把本模块的 mapper 代理抢走 ⇒ 类型对不上 ⇒
 * {@code BeanNotOfRequiredTypeException} ⇒ 主壳启动即挂。
 *
 * <p><b>生产实证（2026-10-24，z-opc-main CrashLoop）</b>：
 * <pre>
 * BeanNotOfRequiredTypeException: Bean named 'permissionMapper' is expected to be of type
 *   'com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper'
 *   but was actually of type 'jdk.proxy2.$Proxy352'
 * </pre>
 * 那个 {@code permissionMapper} 就是本仓 z-lc 的 mapper（当时叫 {@code PermissionMapper}）。
 * 详见 {@link com.zifang.z.lc.mapper.permission.LcPermissionMapper} 的类注释。
 *
 * <p><b>为什么不用「grep 一下有没有 permissionMapper」那种写法</b>：本钉**直接驱动真实的
 * {@link LcModuleDataSource} 上的 {@code @MapperScan}**（注解反射读出 basePackages ⇒ 用真实的
 * {@code ClassPathMapperScanner} 真扫一遍 ⇒ 记录它给每个接口起的 bean 名），
 * 所以它量的是「容器里到底会注册出什么 bean 名」，而不是「源码里有没有某个字符串」。
 * 任何人把 mapper 改名、加 mapper 包、或把 {@code @MapperScan} 的包列表改掉，这条钉都会跟着走。
 *
 * @see com.zifang.z.lc.mapper.permission.LcPermissionMapper
 */
class LcMapperBeanNameGuardTest {

    /** 生产上被抢走的那个 bean 名（线上异常原文里的名字）。 */
    private static final String PRODUCTION_SQUAT_NAME = "permissionMapper";

    /** z-lc 自己的权限 mapper 包 —— 真实注册点的一部分。 */
    private static final String LC_PERMISSION_MAPPER_PKG = "com.zifang.z.lc.mapper.permission";

    /** 真实注册点：{@link LcModuleDataSource} 上的 {@code @MapperScan}。 */
    private static String[] realScannedPackages() {
        MapperScan scan = LcModuleDataSource.class.getAnnotation(MapperScan.class);
        assertNotNull(scan,
                "LcModuleDataSource 上的 @MapperScan 没了 —— mapper 就不再注册，"
                        + "本钉量的「真实注册点」会失真（先确认这条钉本身还量得到东西）");
        assertTrue(scan.basePackages().length > 0, "@MapperScan.basePackages 不能为空");
        return scan.basePackages();
    }

    /**
     * 用**真实的** {@link ClassPathMapperScanner} 扫一遍真实注册点的包，
     * 记录它给每个 mapper 接口起的 bean 名。
     * <p>
     * 刻意**不 refresh 上下文**、**不建 SqlSessionFactory**：本钉要量的是
     * 「bean 名怎么起」，不是「mapper 能不能跑起来」⇒ 不碰库、不起容器、不连库。
     */
    private static Map<String, String> scanRealRegistrationSite() {
        BeanDefinitionRegistry registry = new AnnotationConfigApplicationContext();
        RecordingNameGenerator generator = new RecordingNameGenerator();

        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
        scanner.setBeanNameGenerator(generator);
        // mybatis-spring 2.0.6 的扫描器构造时是 useDefaultFilters=false，
        // 且 @Mapper 的 include filter 要显式 registerFilters() 才会加上
        // （Spring Boot 的 @MapperScan 由 MapperScannerRegistrar 代劳，这里手工驱动要自己补）。
        scanner.setAnnotationClass(Mapper.class);
        scanner.registerFilters();
        // 不把 mapper 加进 Configuration：起 SqlSessionFactory / 连库都不是本钉要量的事
        // （addToConfig 需要真实 Configuration）。
        scanner.setAddToConfig(false);
        scanner.scan(realScannedPackages());

        Map<String, String> fqnToBeanName = generator.recorded;
        assertFalse(fqnToBeanName.isEmpty(), "真实注册点一个 mapper 都没扫到 —— 尺坏了，不是「没问题」（阳性对照失败）");
        return fqnToBeanName;
    }

    /** 记录 {@code AnnotationBeanNameGenerator}（MyBatis-Spring 扫描器的默认命名器）给每个候选接口起的 bean 名。 */
    private static final class RecordingNameGenerator extends AnnotationBeanNameGenerator {
        private final Map<String, String> recorded = new LinkedHashMap<>();

        @Override
        public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
            String beanName = super.generateBeanName(beanDefinition, registry);
            recorded.put(beanDefinition.getBeanClassName(), beanName);
            return beanName;
        }
    }

    @Test
    @DisplayName("真实 @MapperScan 注册出的 bean 名里不得有 permissionMapper（线上被抢走的那个名）")
    void mustNotRegisterTheProductionSquatBeanName() {
        Map<String, String> fqnToBeanName = scanRealRegistrationSite();

        Set<String> beanNames = new TreeSet<>(fqnToBeanName.values());
        List<String> squatters = fqnToBeanName.entrySet().stream()
                .filter(e -> PRODUCTION_SQUAT_NAME.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertFalse(beanNames.contains(PRODUCTION_SQUAT_NAME), "z-lc 仍在注册 bean 名 '" + PRODUCTION_SQUAT_NAME + "'（接口 " + squatters
                        + "）⇒ 别的模块只要有 @Resource 字段名写成 " + PRODUCTION_SQUAT_NAME
                        + "，就会抢走本 mapper 的代理 ⇒ 主壳启动即挂（线上已踩：TASK-20261024-113）");

        assertTrue(beanNames.contains("lcPermissionMapper"), "z-lc 的权限 mapper 应当注册成 lcPermissionMapper（改名后的 bean 名）");
    }

    @Test
    @DisplayName("真实 @MapperScan 的包列表必须覆盖权限 mapper 包（保证上面那条钉量的是真注册点）")
    void guardIsAnchoredOnTheRealRegistrationSite() {
        Set<String> packages = new LinkedHashSet<>(Arrays.asList(realScannedPackages()));
        assertTrue(packages.contains(LC_PERMISSION_MAPPER_PKG), "真实 @MapperScan 的 basePackages 里没有 " + LC_PERMISSION_MAPPER_PKG
                        + " ⇒ 本钉量的不是实际注册点");
    }

    @Test
    @DisplayName("真实注册点里不得有两个不同 mapper 接口映射到同一个 bean 名（同名不同类型）")
    void noTwoDistinctMappersShareOneBeanName() {
        Map<String, String> fqnToBeanName = scanRealRegistrationSite();

        Map<String, String> beanNameToFqn = new LinkedHashMap<>();
        List<String> collisions = new ArrayList<>();
        for (Map.Entry<String, String> e : fqnToBeanName.entrySet()) {
            String previous = beanNameToFqn.putIfAbsent(e.getValue(), e.getKey());
            if (previous != null && !previous.equals(e.getKey())) {
                collisions.add(e.getValue() + " <= " + previous + " & " + e.getKey());
            }
        }
        assertTrue(collisions.isEmpty(), "同名不同类型的 mapper 会被 MyBatis 静默跳过其中一个（只剩 WARN，运行期才炸）："
                + collisions);
    }

    @Test
    @DisplayName("z-lc 里 @Resource 注入 mapper 的字段名必须等于该 mapper 的规范 bean 名")
    void resourceMapperFieldNamesMustMatchCanonicalBeanNames() {
        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : classesUnder("com.zifang.z.lc")) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(Resource.class) == null) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (!fieldType.isInterface() || fieldType.getAnnotation(Mapper.class) == null) {
                    continue;
                }
                String canonical = java.beans.Introspector.decapitalize(fieldType.getSimpleName());
                if (!canonical.equals(field.getName())) {
                    violations.add(clazz.getName() + "#" + field.getName()
                            + " 的类型是 " + fieldType.getName()
                            + "（规范 bean 名 = " + canonical + "）");
                }
            }
        }

        assertTrue(violations.isEmpty(), "@Resource 是按名优先注入：字段名与 mapper 的规范 bean 名不一致时，"
                        + "字段名一旦是通用裸名就会被别的模块的同名 bean 抢走（线上已踩：TASK-20261024-113）"
                        + "；违规项：" + violations);
    }

    /** 扫出某个包下的所有类（走 classpath）。 */
    private static List<Class<?>> classesUnder(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            org.springframework.core.io.Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + ClassUtils.convertClassNameToResourcePath(basePackage)
                            + "/**/*.class");
            for (org.springframework.core.io.Resource resource : resources) {
                String path = resource.getURL().getPath();
                String marker = basePackage.replace('.', '/');
                int idx = path.indexOf(marker);
                if (idx < 0) {
                    continue;
                }
                String relative = path.substring(idx + marker.length() + 1);
                relative = relative.substring(0, relative.length() - ".class".length());
                String className = basePackage + "." + relative.replace('/', '.');
                try {
                    classes.add(Class.forName(className, false,
                            Thread.currentThread().getContextClassLoader()));
                } catch (Throwable ignored) {
                    // 扫不到的（可选依赖等）不参与判定
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫 " + basePackage + " 下的类失败 —— 尺坏了", e);
        }
        assertFalse(classes.isEmpty(), "扫 " + basePackage + " 一个类都没扫到 —— 尺坏了（阳性对照失败）");
        return classes;
    }
}
