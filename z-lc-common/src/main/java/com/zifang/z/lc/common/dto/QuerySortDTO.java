package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 结构化排序: {fieldCode, dir} — dir ∈ asc | desc.
 * <p>
 * fieldCode 必须命中实体已声明字段白名单, 否则整体查询被拒绝 (不拼任何 SQL).
 */
public class QuerySortDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段编码 (白名单校验)
     */
    private String fieldCode;

    /**
     * 排序方向: asc / desc (大小写不敏感), 缺省 asc
     */
    private String dir;

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
