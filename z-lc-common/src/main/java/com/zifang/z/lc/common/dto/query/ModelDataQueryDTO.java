package com.zifang.z.lc.common.dto.query;

import java.io.Serializable;
import java.util.List;

/**
 * 模型数据查询 DTO（不分页）.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataQueryDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：业务方调用 {@code DataModelService.queryList(ModelDataQueryDTO, boolean)} 时入参 —
 * 与 {@code RuntimeQueryDTO} 互补：后者走 z-lc 原生 RuntimeCrudExecutor，
 * 本 DTO 走 ace 风格的 queryBeans 列表 + 顺序 bean 格式.
 *
 * <p>典型用法：
 * <pre>{@code
 *   ModelDataQueryDTO q = new ModelDataQueryDTO();
 *   q.setAppCode("crm");
 *   q.setModelCode("customer");
 *   q.setQueryBeans(Arrays.asList(
 *       ModelDataQueryBean.eq("status", "ACTIVE"),
 *       ModelDataQueryBean.like("name", "张")));
 *   ModelDataOrderBean order = new ModelDataOrderBean();
 *   order.setFieldCodes(Arrays.asList("create_time"));
 *   order.setOrderType("desc");
 *   q.setOrderBean(order);
 *   List<MyPojo> rows = service.queryList(q, false);
 * }</pre>
 *
 * @author zifang
 */
public class ModelDataQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型 code.
     */
    private String modelCode;

    /**
     * 字段等值条件列表（多条件并列 — 行为类似 AND）.
     */
    private List<ModelDataQueryBean> queryBeans;

    /**
     * 字段顺序.
     */
    private ModelDataOrderBean orderBean;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public List<ModelDataQueryBean> getQueryBeans() {
        return queryBeans;
    }

    public void setQueryBeans(List<ModelDataQueryBean> queryBeans) {
        this.queryBeans = queryBeans;
    }

    public ModelDataOrderBean getOrderBean() {
        return orderBean;
    }

    public void setOrderBean(ModelDataOrderBean orderBean) {
        this.orderBean = orderBean;
    }
}
