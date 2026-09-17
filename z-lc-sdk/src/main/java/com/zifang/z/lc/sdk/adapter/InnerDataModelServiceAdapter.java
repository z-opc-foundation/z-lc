package com.zifang.z.lc.sdk.adapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataPageableQueryDTO;
import com.zifang.z.lc.common.dto.query.ModelDataQueryBean;
import com.zifang.z.lc.common.dto.query.ModelDataQueryDTO;
import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import com.zifang.z.lc.sdk.reflect.LcReflectHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部 ServiceAdapter — 把 {@link AbstractDataModelService}{@code <T>}
 * 包装成 {@link ServiceAdapter}（Map 形式）.
 *
 * <p>蒸馏自 ace-platform-engine {@code InnerDataModelServiceAdapter}
 * （{@code com.c2f.ace.engine.adapter}），行为完全对齐.
 *
 * <p>关键转换：
 * <ul>
 *   <li>入参 {@code Map<String, Object>} → 反射转换为 POJO {@code T}（用 Jackson ObjectMapper），
 *       再调 {@code AbstractDataModelService} 对应方法</li>
 *   <li>出参 POJO {@code T} → 转换为 {@code Map<String, Object>}</li>
 * </ul>
 *
 * <p>JSON 转换：本类自带 {@link ObjectMapper}（不依赖 z-lc-design Json 工具），
 * 配置 {@code FAIL_ON_UNKNOWN_PROPERTIES=false}，避免 DB 多余字段导致反序列化失败.
 *
 * <p>典型用法：
 * <pre>{@code
 *   AbstractDataModelService&lt;Customer&gt; abs = ...;
 *   ServiceAdapter adapter = new InnerDataModelServiceAdapter();
 *   ((InnerDataModelServiceAdapter) adapter).setAbstractDataModelService(abs);
 *
 *   Map&lt;String, Object&gt; data = Map.of("name", "张三", "age", 30);
 *   Long id = adapter.save(data);  // 自动转换 Map → Customer → save
 * }</pre>
 *
 * @author zifang
 */
public class InnerDataModelServiceAdapter implements ServiceAdapter {

    /**
     * JSON 转换用 — 配置与 z-lc-design Json 工具保持一致.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 业务 POJO Service 实现 — 通过 setter 注入.
     * <p>声明为 raw type 以绕过 {@code AbstractDataModelService<?>} 的泛型擦除限制 —
     * 这样可以调用 {@code save(T) / delete(T)} 等方法，否则编译器拒绝 Object 实参.
     */
    @SuppressWarnings("rawtypes")
    private AbstractDataModelService abstractDataModelService;

    /**
     * 注入业务 Service — 调用方应在使用前调用此 setter.
     */
    @SuppressWarnings("rawtypes")
    public void setAbstractDataModelService(AbstractDataModelService svc) {
        this.abstractDataModelService = svc;
    }

    /**
     * 取出当前注入的 Service（raw type — 调用方应自行 cast）.
     */
    @SuppressWarnings("rawtypes")
    public AbstractDataModelService getAbstractDataModelService() {
        return abstractDataModelService;
    }

    @Override
    public Long save(Map<String, Object> data) {
        Object pojo = toPojo(data);
        return abstractDataModelService.save(pojo);
    }

    @Override
    @Deprecated
    public Long save(Map<String, Object> data, Integer mode) {
        Object pojo = toPojo(data);
        return abstractDataModelService.save(pojo, mode);
    }

    @Override
    public PageResult<Map<String, Object>> queryPageable(ModelDataPageableQueryDTO query, boolean deep) {
        RuntimeQueryDTO rq = toRuntimeQuery(query);
        PageResult<?> page = abstractDataModelService.queryPageable(rq, deep);
        if (page == null) {
            return new PageResult<>(Collections.emptyList(), 0L, 1L, 20L);
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        if (page.getRecords() != null) {
            for (Object o : page.getRecords()) {
                maps.add(toMap(o));
            }
        }
        return new PageResult<>(maps, page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    public void delete(Long id) {
        abstractDataModelService.delete(id);
    }

    @Override
    public void delete(Map<String, Object> data) {
        Object pojo = toPojo(data);
        abstractDataModelService.delete(pojo);
    }

    @Override
    public Map<String, Object> queryById(Long pkId, boolean deep) {
        Object pojo = abstractDataModelService.queryById(pkId, deep);
        return toMap(pojo);
    }

    @Override
    public List<Map<String, Object>> queryList(ModelDataQueryDTO query, boolean deep) {
        RuntimeQueryDTO rq = toRuntimeQuery(query);
        List<?> list = abstractDataModelService.queryList(rq, deep);
        List<Map<String, Object>> maps = new ArrayList<>();
        if (list != null) {
            for (Object o : list) {
                maps.add(toMap(o));
            }
        }
        return maps;
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode) {
        return abstractDataModelService.init(appCode, modelCode);
    }

    @Override
    public Map<String, Object> init(String appCode, String modelCode, Map<String, Object> data) {
        return abstractDataModelService.init(appCode, modelCode, data);
    }

    @Override
    public Map<String, Object> dataCopy(Map<String, Object> data) {
        Object pojo = toPojo(data);
        Object result = abstractDataModelService.dataCopy(pojo);
        return toMap(result);
    }

    @Override
    public Map<String, Object> aiInit(String appCode, String modelCode, Map<String, Object> data) {
        return abstractDataModelService.aiInit(appCode, modelCode, data);
    }

    // —— 私有转换助手 ——

    /**
     * Map → POJO（用反射拿到泛型 T 的 Class，再 Jackson 反序列化）.
     */
    private Object toPojo(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Class<?> genericType = resolveGenericType();
        if (genericType == null) {
            return data;
        }
        return MAPPER.convertValue(data, genericType);
    }

    /**
     * POJO → Map（Jackson ObjectMapper 转换）.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object pojo) {
        if (pojo == null) {
            return null;
        }
        if (pojo instanceof Map) {
            return (Map<String, Object>) pojo;
        }
        return MAPPER.convertValue(pojo, Map.class);
    }

    /**
     * 解析 AbstractDataModelService 的泛型 T.
     */
    private Class<?> resolveGenericType() {
        if (abstractDataModelService == null) {
            return null;
        }
        return LcReflectHelper.resolveGenericType(abstractDataModelService);
    }

    /**
     * ModelDataPageableQueryDTO → RuntimeQueryDTO（适配 z-lc RuntimeCrudExecutor 入参）.
     */
    private RuntimeQueryDTO toRuntimeQuery(ModelDataPageableQueryDTO q) {
        RuntimeQueryDTO rq = new RuntimeQueryDTO();
        if (q == null) {
            return rq;
        }
        rq.setAppCode(q.getAppCode());
        rq.setEntityCode(q.getModelCode());
        rq.setPage(q.getCurrent() == null ? 1 : q.getCurrent().intValue());
        rq.setSize(q.getSize() == null ? 20 : q.getSize().intValue());
        if (q.getQueryBeans() != null) {
            Map<String, Object> filters = new LinkedHashMap<>();
            for (ModelDataQueryBean bean : q.getQueryBeans()) {
                filters.put(bean.getFieldCode(), bean.getValue());
            }
            rq.setFilters(filters);
        }
        if (q.getOrderBean() != null && q.getOrderBean().getFieldCodes() != null) {
            String orderType = q.getOrderBean().getOrderType() == null ? "ASC" : q.getOrderBean().getOrderType().toUpperCase();
            rq.setOrderBy(q.getOrderBean().getFieldCodes().get(0) + " " + orderType);
        }
        return rq;
    }

    /**
     * ModelDataQueryDTO → RuntimeQueryDTO.
     */
    private RuntimeQueryDTO toRuntimeQuery(ModelDataQueryDTO q) {
        RuntimeQueryDTO rq = new RuntimeQueryDTO();
        if (q == null) {
            return rq;
        }
        rq.setAppCode(q.getAppCode());
        rq.setEntityCode(q.getModelCode());
        if (q.getQueryBeans() != null) {
            Map<String, Object> filters = new LinkedHashMap<>();
            for (ModelDataQueryBean bean : q.getQueryBeans()) {
                filters.put(bean.getFieldCode(), bean.getValue());
            }
            rq.setFilters(filters);
        }
        if (q.getOrderBean() != null && q.getOrderBean().getFieldCodes() != null) {
            String orderType = q.getOrderBean().getOrderType() == null ? "ASC" : q.getOrderBean().getOrderType().toUpperCase();
            rq.setOrderBy(q.getOrderBean().getFieldCodes().get(0) + " " + orderType);
        }
        return rq;
    }
}
