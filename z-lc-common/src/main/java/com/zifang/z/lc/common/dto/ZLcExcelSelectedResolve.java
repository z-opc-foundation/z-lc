package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * Excel 下拉选择解析配置 — 蒸馏自 ace-platform-core
 * {@code ExcelSelectedResolve} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>定义 Excel 下拉框的数据源、起始行、结束行等配置.
 * 蒸馏时移除了 ace 对 Lombok @Data / @Slf4j 的依赖, 改为手写 getter/setter.
 *
 * @author zifang
 */
public class ZLcExcelSelectedResolve implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下拉内容数组. */
    private String[] source;

    /** 下拉框起始行 (默认为第二行). */
    private int firstRow;

    /** 下拉框结束行 (默认为最后一行). */
    private int lastRow;

    public ZLcExcelSelectedResolve() {
    }

    public String[] getSource() {
        return source;
    }

    public void setSource(String[] source) {
        this.source = source;
    }

    public int getFirstRow() {
        return firstRow;
    }

    public void setFirstRow(int firstRow) {
        this.firstRow = firstRow;
    }

    public int getLastRow() {
        return lastRow;
    }

    public void setLastRow(int lastRow) {
        this.lastRow = lastRow;
    }
}
