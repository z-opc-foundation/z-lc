package com.zifang.z.lc.schema.model;

/**
 * 物料/子 schema 依赖声明。运行时据此按需加载物料实现。
 */
public class SchemaImport {

    /** 导出的组件名 */
    private String name;

    /** 版本 */
    private String version;

    /** 包坐标 / URL */
    private String packageName;

    /** component=组件物料；schema=子 schema（内嵌页面） */
    private String type = "component";

    /** 是否异步加载 */
    private boolean async;

    public SchemaImport() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
