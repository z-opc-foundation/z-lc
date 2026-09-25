package com.zifang.z.lc.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * provision 的真实结果。
 * <p>
 * 存在的理由: "DDL 没报错" 和 "这张表按这份定义建出来了" 是两件事。
 * {@code CREATE TABLE IF NOT EXISTS} 对一张已经存在的表是**空操作** —— 旧口径把 entityCode 塞进
 * 返回 Map 就等于宣布"建好了"，而它声明的列可能一列都没有 (两个实体抢同一张物理表时正是这样)。
 * 所以这里逐项带状态，并在建表之后回读物理列核对，缺列就判失败。
 */
public class ProvisionReport {

    /** 表原来不存在，这次按这份 DDL 建出来了。 */
    public static final String CREATED = "CREATED";
    /** 表本来就在，而且这份定义里的列**一列不缺** —— 跳过是安全的。 */
    public static final String EXISTS_INTACT = "EXISTS_INTACT";
    /**
     * 表本来就在，但定义跑到表前面了 (加过栏) —— 这次按定义**只补了列**。
     * <p>
     * 单独一个状态是因为"补了 3 列"和"什么都没做"是两件不同的事，把它们都报成
     * {@code EXISTS_INTACT} 等于让界面说"表好好的"，而它刚才其实被改过 (缺陷 #47)。
     * 补列只做 ADD：不动已有列、不改类型、不删任何东西。
     */
    public static final String ALTERED = "ALTERED";
    /** 没建好: DDL 报错、这张表不归我们 (缺引擎自建列或被别的实体占着)、或补列没补上。 */
    public static final String FAILED = "FAILED";

    /** 一个实体的结果。 */
    public static class Item {
        private String entityCode;
        private String tableName;
        private String status;
        private String ddl;
        private String message;
        private List<String> missingColumns = new ArrayList<String>();
        /** 这一次真的被 ALTER 加出来的列 (小写，与 {@link #missingColumns} 同一口径)。 */
        private List<String> addedColumns = new ArrayList<String>();

        public Item() {
        }

        public Item(String entityCode, String tableName, String status, String ddl, String message) {
            this.entityCode = entityCode;
            this.tableName = tableName;
            this.status = status;
            this.ddl = ddl;
            this.message = message;
        }

        public String getEntityCode() {
            return entityCode;
        }

        public void setEntityCode(String entityCode) {
            this.entityCode = entityCode;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDdl() {
            return ddl;
        }

        public void setDdl(String ddl) {
            this.ddl = ddl;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getMissingColumns() {
            return missingColumns;
        }

        public void setMissingColumns(List<String> missingColumns) {
            this.missingColumns = missingColumns;
        }

        public List<String> getAddedColumns() {
            return addedColumns;
        }

        public void setAddedColumns(List<String> addedColumns) {
            this.addedColumns = addedColumns;
        }
    }

    private String appCode;
    private int total;
    private int created;
    private int unchanged;
    private int altered;
    private List<Item> items = new ArrayList<Item>();

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public void setUnchanged(int unchanged) {
        this.unchanged = unchanged;
    }

    public int getAltered() {
        return altered;
    }

    public void setAltered(int altered) {
        this.altered = altered;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public int getFailedCount() {
        int n = 0;
        for (Item item : items) {
            if (FAILED.equals(item.getStatus())) {
                n++;
            }
        }
        return n;
    }

    /** 界面拿它决定是"全部成功"还是"部分成功，这些没建成"。 */
    public boolean isAllOk() {
        return getFailedCount() == 0;
    }
}
