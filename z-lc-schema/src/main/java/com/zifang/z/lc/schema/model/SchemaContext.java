package com.zifang.z.lc.schema.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面上下文声明：对外暴露的变量/事件/方法 + 扩展点 + 全局配置。
 *
 * <p>这是页面与外部宿主、以及组件之间的“通信协议”。组件在运行时通过约定的 key
 * （{@code <nodeId>-variable} / {@code <nodeId>-methods}）读写上下文。</p>
 */
public class SchemaContext {

    /** 外部可注入的变量 */
    private List<Declaration> variableList = new ArrayList<>();

    /** 组件可触发的事件 */
    private List<Declaration> eventList = new ArrayList<>();

    /** 组件对外暴露的方法 */
    private List<Declaration> contextMethods = new ArrayList<>();

    /** 扩展点：beanName → 扩展定义 */
    private Map<String, Object> extensions = new LinkedHashMap<>();

    /** 全局配置（样式、服务等） */
    private Map<String, Object> global = new LinkedHashMap<>();

    public SchemaContext() {
    }

    /** 一个变量/事件/方法的声明：name + description + 可选参数 */
    public static class Declaration {
        private String name;
        private String description;
        private List<Map<String, Object>> params = new ArrayList<>();

        public Declaration() {
        }

        public Declaration(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Map<String, Object>> getParams() {
            return params;
        }

        public void setParams(List<Map<String, Object>> params) {
            this.params = params == null ? new ArrayList<>() : params;
        }
    }

    public List<Declaration> getVariableList() {
        return variableList;
    }

    public void setVariableList(List<Declaration> variableList) {
        this.variableList = variableList == null ? new ArrayList<>() : variableList;
    }

    public List<Declaration> getEventList() {
        return eventList;
    }

    public void setEventList(List<Declaration> eventList) {
        this.eventList = eventList == null ? new ArrayList<>() : eventList;
    }

    public List<Declaration> getContextMethods() {
        return contextMethods;
    }

    public void setContextMethods(List<Declaration> contextMethods) {
        this.contextMethods = contextMethods == null ? new ArrayList<>() : contextMethods;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions == null ? new LinkedHashMap<>() : extensions;
    }

    public Map<String, Object> getGlobal() {
        return global;
    }

    public void setGlobal(Map<String, Object> global) {
        this.global = global == null ? new LinkedHashMap<>() : global;
    }
}
