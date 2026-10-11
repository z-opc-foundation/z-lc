package com.zifang.z.report.common.schema;

import java.util.Map;

/**
 * 数据源定义 (数据源板块的管理单元): 连接标识 + 类型 + 属性。
 * 敏感属性 (密码等) 的加密存储为 M2 事项, M1 内存态不落盘。
 */
public class DataSourceDef {

    private String id;
    private String name;
    private DataSourceType type;
    private Map<String, String> properties;

    public DataSourceDef() {
    }

    public DataSourceDef(String id, String name, DataSourceType type, Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataSourceType getType() {
        return type;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
