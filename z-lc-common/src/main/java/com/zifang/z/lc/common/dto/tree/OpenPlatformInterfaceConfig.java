package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.List;

/**
 * 开放平台接口配置.
 *
 * <p>蒸馏自 ace-platform-client {@code OpenPlatformInterfaceConfig}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「开放平台 API」调用配置 — 类似 HttpInterfaceConfig 但额外包含 appId /
 * apiId 标识开放平台侧资源.
 *
 * @author lufei.lhw (distilled by zifang)
 */
public class OpenPlatformInterfaceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id（开放平台侧应用标识）.
     */
    private Long appId;

    /**
     * 接口 id（开放平台侧 API 标识）.
     */
    private Long apiId;

    /**
     * 接口 URL.
     */
    private String requestUrl;

    /**
     * 请求类型（GET / POST）.
     */
    private String requestType;

    /**
     * 请求体（POST 时使用）.
     */
    private String requestBody;

    /**
     * 请求参数列表.
     */
    private List<OpenRequestParam> requestParamList;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Long getApiId() {
        return apiId;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public List<OpenRequestParam> getRequestParamList() {
        return requestParamList;
    }

    public void setRequestParamList(List<OpenRequestParam> requestParamList) {
        this.requestParamList = requestParamList;
    }

    /**
     * 开放平台接口入参 — 相比 {@link HttpRequestParam} 更复杂，支持嵌套子节点.
     */
    public static class OpenRequestParam implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 主键 id.
         */
        private Long id;

        /**
         * 索引顺序.
         */
        private Integer index;

        /**
         * 开放平台侧参数名称.
         */
        private String openParamsName;

        /**
         * 插入参数名称（z-lc 侧）.
         */
        private String insertParamsName;

        /**
         * 参数类型.
         */
        private String paramsType;

        /**
         * 扩展类型.
         */
        private String expandType;

        /**
         * 是否必填.
         */
        private Boolean mustWrite;

        /**
         * 示例值.
         */
        private String exampleValue;

        /**
         * 描述.
         */
        private String description;

        /**
         * 是否可编辑.
         */
        private Boolean isEdit;

        /**
         * 是否必传新增.
         */
        private Boolean isAdd;

        /**
         * 能否操作.
         */
        private Boolean isCanOperate;

        /**
         * 子节点（递归结构 — 表达嵌套参数）.
         */
        private List<OpenRequestParam> childNode;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getOpenParamsName() {
            return openParamsName;
        }

        public void setOpenParamsName(String openParamsName) {
            this.openParamsName = openParamsName;
        }

        public String getInsertParamsName() {
            return insertParamsName;
        }

        public void setInsertParamsName(String insertParamsName) {
            this.insertParamsName = insertParamsName;
        }

        public String getParamsType() {
            return paramsType;
        }

        public void setParamsType(String paramsType) {
            this.paramsType = paramsType;
        }

        public String getExpandType() {
            return expandType;
        }

        public void setExpandType(String expandType) {
            this.expandType = expandType;
        }

        public Boolean getMustWrite() {
            return mustWrite;
        }

        public void setMustWrite(Boolean mustWrite) {
            this.mustWrite = mustWrite;
        }

        public String getExampleValue() {
            return exampleValue;
        }

        public void setExampleValue(String exampleValue) {
            this.exampleValue = exampleValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsEdit() {
            return isEdit;
        }

        public void setIsEdit(Boolean isEdit) {
            this.isEdit = isEdit;
        }

        public Boolean getIsAdd() {
            return isAdd;
        }

        public void setIsAdd(Boolean isAdd) {
            this.isAdd = isAdd;
        }

        public Boolean getIsCanOperate() {
            return isCanOperate;
        }

        public void setIsCanOperate(Boolean isCanOperate) {
            this.isCanOperate = isCanOperate;
        }

        public List<OpenRequestParam> getChildNode() {
            return childNode;
        }

        public void setChildNode(List<OpenRequestParam> childNode) {
            this.childNode = childNode;
        }
    }
}
