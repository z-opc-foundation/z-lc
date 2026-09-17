package com.zifang.z.lc.common.dto.query;

import java.io.Serializable;
import java.util.List;

/**
 * 模型数据排序 bean — 多字段排序.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataOrderBean}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>典型用法（前端「自定义排序」UI）：
 * <pre>{@code
 *   ModelDataOrderBean order = new ModelDataOrderBean();
 *   order.setFieldCodes(Arrays.asList("create_time", "id"));
 *   order.setOrderType("desc");
 * }</pre>
 *
 * @author zifang
 */
public class ModelDataOrderBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排序字段列表（按数组顺序拼接，如 {@code ORDER BY create_time DESC, id DESC}）.
     */
    private List<String> fieldCodes;

    /**
     * 排序类型（{@code asc} / {@code desc}）.
     */
    private String orderType;

    public List<String> getFieldCodes() {
        return fieldCodes;
    }

    public void setFieldCodes(List<String> fieldCodes) {
        this.fieldCodes = fieldCodes;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}
