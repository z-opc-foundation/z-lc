package com.zifang.z.lc.common.apppackage;

/**
 * 应用包模型 DTO — 蒸馏自 ace-platform-core
 * {@code AppPackageModel} ({@code com.c2f.ace.core.extenssion.model.define}).
 *
 * <p>用于低代码平台"应用包"管理 — 描述一个可独立打包/导出的应用单元.
 * 业务方对应用包做"导入/导出/版本管理"操作时以此 DTO 作为基础载体.
 *
 * @author zifang
 */
public class ZLcAppPackageModel {

    private Long id;
    private String packageName;
    private String packageCode;
    private String packageDesc;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }

    public String getPackageDesc() { return packageDesc; }
    public void setPackageDesc(String packageDesc) { this.packageDesc = packageDesc; }
}