package com.zifang.z.lc.core.adapter;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 适配器注册中心: 收集所有 Adapter, 按 priority 升序排列, 支持按 name 查找
 */
@Component
public class AdapterRegistry {

    private final Map<String, Adapter> byName = new LinkedHashMap<>();

    public AdapterRegistry(List<Adapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            return;
        }
        // 按 priority 升序, 同 priority 保持注册顺序
        adapters.stream()
                .sorted((a, b) -> Integer.compare(a.priority(), b.priority()))
                .forEach(a -> {
                    Adapter prev = byName.put(a.name(), a);
                    if (prev != null) {
                        throw new IllegalStateException(
                                "Duplicate adapter name: " + a.name() + " (classes: "
                                        + prev.getClass().getName() + " vs " + a.getClass().getName() + ")");
                    }
                });
    }

    /**
     * 按名取 Adapter
     */
    public Adapter get(String name) {
        return byName.get(name);
    }

    /**
     * 列出所有 Adapter (只读)
     */
    public List<Adapter> all() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(byName.values()));
    }
}
