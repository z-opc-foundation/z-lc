package com.zifang.z.lc.sdk.spi.task;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务服务收集器 — 蒸馏自 ace-platform-engine {@code TaskServiceCollector}
 * （{@code com.c2f.ace.engine}），行为对齐.
 *
 * <p>启动时扫描所有 {@link AbstractTaskService} 子类，按 {@link TaskServiceInfo#identityCode()}
 * 索引到 Map. 业务方可通过 {@link #findByIdentityCode} 取具体服务.
 *
 * <p>相比 ace 的版本，去除了 RpcPublisher 依赖（z-lc 默认进程内调用，
 * 业务方如需 RPC 可继承基类后自行包装）.
 *
 * @author zifang
 */
@Component
public class TaskServiceCollector implements ApplicationContextAware {

    /**
     * identityCode → AbstractTaskService 子类 的索引表.
     */
    private final Map<String, AbstractTaskService> byIdentityCode = new LinkedHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, AbstractTaskService> beans = applicationContext.getBeansOfType(AbstractTaskService.class);
        for (AbstractTaskService svc : beans.values()) {
            TaskServiceInfo info = AnnotatedElementUtils.findMergedAnnotation(svc.getClass(), TaskServiceInfo.class);
            if (info == null) {
                throw new IllegalStateException(
                        "@TaskServiceInfo 注解缺失: " + svc.getClass().getName()
                                + " 必须标注 @TaskServiceInfo(identityCode=...)");
            }
            String identityCode = info.identityCode();
            if (identityCode == null || identityCode.isEmpty()) {
                throw new IllegalStateException(
                        "@TaskServiceInfo.identityCode 不能为空: " + svc.getClass().getName());
            }
            if (byIdentityCode.containsKey(identityCode)) {
                throw new IllegalStateException(
                        "找到两个相同的 TaskService.identityCode: " + identityCode
                                + " (existing=" + byIdentityCode.get(identityCode).getClass().getName()
                                + ", new=" + svc.getClass().getName() + ")");
            }
            byIdentityCode.put(identityCode, svc);
        }
    }

    /**
     * 按 identityCode 取具体服务实现.
     *
     * @param identityCode {@link TaskServiceInfo#identityCode()}
     * @return 对应服务；未注册返回 null
     */
    public AbstractTaskService findByIdentityCode(String identityCode) {
        if (identityCode == null) {
            return null;
        }
        return byIdentityCode.get(identityCode);
    }

    /**
     * 当前已注册的所有 identityCode.
     */
    public java.util.Set<String> registeredIdentityCodes() {
        return new java.util.LinkedHashSet<>(byIdentityCode.keySet());
    }

    /**
     * 已注册的服务总数.
     */
    public int size() {
        return byIdentityCode.size();
    }
}
