package com.zifang.z.lc.sdk.spi.sign;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 签名任务服务收集器 — 蒸馏自 ace-platform-engine {@code AssignServiceCollector}
 * （{@code com.c2f.ace.engine}），行为对齐.
 *
 * <p>启动时扫描所有 {@link AbstractAssignService} 子类，按 {@link AssignServiceInfo#identityCode()}
 * 索引到 Map. 业务方可通过 {@link #findByIdentityCode} 取具体服务.
 *
 * @author zifang
 */
@Component
public class AssignServiceCollector implements ApplicationContextAware {

    /**
     * identityCode → AbstractAssignService 子类 的索引表.
     */
    private final Map<String, AbstractAssignService> byIdentityCode = new LinkedHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, AbstractAssignService> beans = applicationContext.getBeansOfType(AbstractAssignService.class);
        for (AbstractAssignService svc : beans.values()) {
            AssignServiceInfo info = AnnotatedElementUtils.findMergedAnnotation(svc.getClass(), AssignServiceInfo.class);
            if (info == null) {
                throw new IllegalStateException(
                        "@AssignServiceInfo 注解缺失: " + svc.getClass().getName()
                                + " 必须标注 @AssignServiceInfo(identityCode=...)");
            }
            String identityCode = info.identityCode();
            if (identityCode == null || identityCode.isEmpty()) {
                throw new IllegalStateException(
                        "@AssignServiceInfo.identityCode 不能为空: " + svc.getClass().getName());
            }
            if (byIdentityCode.containsKey(identityCode)) {
                throw new IllegalStateException(
                        "找到两个相同的 AssignService.identityCode: " + identityCode
                                + " (existing=" + byIdentityCode.get(identityCode).getClass().getName()
                                + ", new=" + svc.getClass().getName() + ")");
            }
            byIdentityCode.put(identityCode, svc);
        }
    }

    /**
     * 按 identityCode 取具体服务实现.
     *
     * @param identityCode {@link AssignServiceInfo#identityCode()}
     * @return 对应服务；未注册返回 null
     */
    public AbstractAssignService findByIdentityCode(String identityCode) {
        if (identityCode == null) {
            return null;
        }
        return byIdentityCode.get(identityCode);
    }

    /**
     * 当前已注册的所有 identityCode.
     */
    public Set<String> registeredIdentityCodes() {
        return new LinkedHashSet<>(byIdentityCode.keySet());
    }

    /**
     * 已注册的服务总数.
     */
    public int size() {
        return byIdentityCode.size();
    }
}
