package com.zifang.z.report.common.schema;

/**
 * 多源 join 定义: 将 sourceId 指向的数据源读入内存,
 * 以 leftKeys = rightKeys 多键等值 join 追加到当前数据集。
 * <p>
 * 语义: join 键中任一侧含 null 的行不参与匹配 (对齐 SQL)。
 */
public class JoinDef {

    private String sourceId;
    /** 被 join 的源内数据单元 (表名/文件名/源内标识) */
    private String table;
    private JoinType type;
    private String[] leftKeys;
    private String[] rightKeys;

    public JoinDef() {
    }

    public JoinDef(String sourceId, JoinType type, String[] leftKeys, String[] rightKeys) {
        this.sourceId = sourceId;
        this.type = type;
        this.leftKeys = leftKeys;
        this.rightKeys = rightKeys;
    }

    public JoinDef(String sourceId, String table, JoinType type, String[] leftKeys, String[] rightKeys) {
        this.sourceId = sourceId;
        this.table = table;
        this.type = type;
        this.leftKeys = leftKeys;
        this.rightKeys = rightKeys;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public JoinType getType() {
        return type;
    }

    public void setType(JoinType type) {
        this.type = type;
    }

    public String[] getLeftKeys() {
        return leftKeys;
    }

    public void setLeftKeys(String[] leftKeys) {
        this.leftKeys = leftKeys;
    }

    public String[] getRightKeys() {
        return rightKeys;
    }

    public void setRightKeys(String[] rightKeys) {
        this.rightKeys = rightKeys;
    }
}
