package com.zifang.z.lc.core.deleter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端批量删除的出入参。
 * <p>
 * 为什么要服务端做：批量删除早先是浏览器 for 循环发 N 个 {@code POST /runtime/delete}，
 * 于是有三笔对不上的账 —— ① N 个往返，勾选几百行要点几百次 HTTP；② 中途某一个失败
 * 就留下"删了一半"的现场，而且没人回滚；③ 前端只能拿"发出去几个请求"当"删掉了几个"报给用户。
 * 这里给的是删除版的 all-or-nothing：先整批预检，有一行不能删就整批不动。
 */
public final class BatchDeleteDto {

    /** 单请求 id 上限，和批量导入同一量级。 */
    public static final int MAX_IDS = 2000;

    /** 返回给前端的错误条数上限（全量条数体现在 total 里）。 */
    public static final int MAX_ERRORS_RETURNED = 50;

    private BatchDeleteDto() {
    }

    public static class Request implements Serializable {
        private static final long serialVersionUID = 1L;

        private String tenantCode;
        private String appCode;
        private String entityCode;
        private List<Long> ids = new ArrayList<Long>();

        public String getTenantCode() {
            return tenantCode;
        }

        public void setTenantCode(String tenantCode) {
            this.tenantCode = tenantCode;
        }

        public String getAppCode() {
            return appCode;
        }

        public void setAppCode(String appCode) {
            this.appCode = appCode;
        }

        public String getEntityCode() {
            return entityCode;
        }

        public void setEntityCode(String entityCode) {
            this.entityCode = entityCode;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids == null ? new ArrayList<Long>() : ids;
        }
    }

    /** 单条错误。index 是去重后 ids 的下标，id 冗余带回，前端不用再对一次下标。 */
    public static class RowError implements Serializable {
        private static final long serialVersionUID = 1L;

        private int index;
        private Long id;
        private String message;

        public RowError() {
        }

        public RowError(int index, Long id, String message) {
            this.index = index;
            this.id = id;
            this.message = message;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class Result implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 去重后的待删条数。报给用户的"N 条"以这个为准，不是浏览器发了几个请求。 */
        private int total;
        private int deletedCount;
        private boolean applied;
        private boolean rolledBack;
        private List<Long> ids = new ArrayList<Long>();
        private List<RowError> errors = new ArrayList<RowError>();
        private String message;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }

        public boolean isApplied() {
            return applied;
        }

        public void setApplied(boolean applied) {
            this.applied = applied;
        }

        public boolean isRolledBack() {
            return rolledBack;
        }

        public void setRolledBack(boolean rolledBack) {
            this.rolledBack = rolledBack;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids == null ? new ArrayList<Long>() : ids;
        }

        public List<RowError> getErrors() {
            return errors;
        }

        public void setErrors(List<RowError> errors) {
            this.errors = errors == null ? new ArrayList<RowError>() : errors;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
