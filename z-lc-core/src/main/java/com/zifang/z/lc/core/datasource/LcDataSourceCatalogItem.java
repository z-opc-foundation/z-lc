package com.zifang.z.lc.core.datasource;

/**
 * 数据源目录条目 —— 只承载非敏感元信息。
 * <p>
 * 纪律（继承 z-dms）：credentialRef 是"引用"不是口令，可以透出；
 * properties 里的任何键值一律不出现在本 DTO（前端永远拿不到连接参数）。
 */
public class LcDataSourceCatalogItem {

    private String id;
    private String name;
    /** z-dms 的类型字面量：JDBC / MEMORY / CSV / HTTP / SQL */
    private String type;
    /** 凭据引用（{@code <group>/<key>} 格式），可空 */
    private String credentialRef;

    public LcDataSourceCatalogItem() {
    }

    public LcDataSourceCatalogItem(String id, String name, String type, String credentialRef) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.credentialRef = credentialRef;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCredentialRef() {
        return credentialRef;
    }

    public void setCredentialRef(String credentialRef) {
        this.credentialRef = credentialRef;
    }
}
