package com.zifang.z.lc.core.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批量导入的两段式出入参：{@code preview} 只校验不落库，{@code commit} 才写。
 * <p>
 * 为什么要服务端做：导入向导早期是"浏览器解析 CSV + 逐行 POST /runtime/create"，
 * N 行 = N 个 HTTP 往返，几千行慢到不可用，中途关页面就留下半截数据。
 */
public final class ImportDto {

    /** 单请求行数上限，避免一次把库打挂。 */
    public static final int MAX_ROWS = 2000;

    /** 返回给前端的错误条数上限（全量条数仍会体现在 total/valid 里）。 */
    public static final int MAX_ERRORS_RETURNED = 50;

    private ImportDto() {
    }

    /** records 是已经映射成 fieldCode -&gt; 值 的记录列表（映射本身仍在前端做）。 */
    public static class Request implements Serializable {
        private static final long serialVersionUID = 1L;

        private String tenantCode;
        private String appCode;
        private String entityCode;
        private List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();

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

        public List<Map<String, Object>> getRecords() {
            return records;
        }

        public void setRecords(List<Map<String, Object>> records) {
            this.records = records == null ? new ArrayList<Map<String, Object>>() : records;
        }
    }

    /** 单行错误，index 是请求 records 的下标（从 0 开始，前端能直接对回 CSV 行号）。 */
    public static class RowError implements Serializable {
        private static final long serialVersionUID = 1L;

        private int index;
        private String message;

        public RowError() {
        }

        public RowError(int index, String message) {
            this.index = index;
            this.message = message;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /** preview / commit 都可能出现的数据质量提示，不阻断写入。 */
    public static class RowWarning implements Serializable {
        private static final long serialVersionUID = 1L;

        private int index;
        private String fieldCode;
        private String message;

        public RowWarning() {
        }

        public RowWarning(int index, String fieldCode, String message) {
            this.index = index;
            this.fieldCode = fieldCode;
            this.message = message;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getFieldCode() {
            return fieldCode;
        }

        public void setFieldCode(String fieldCode) {
            this.fieldCode = fieldCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /** preview 与 commit 共用一个结果形状，省得前端分叉处理。 */
    public static class Result implements Serializable {
        private static final long serialVersionUID = 1L;

        private int total;
        private int validCount;
        private int insertedCount;
        private boolean applied;
        private boolean rolledBack;
        private List<Long> ids = new ArrayList<Long>();
        private List<RowError> errors = new ArrayList<RowError>();
        private List<RowWarning> warnings = new ArrayList<RowWarning>();
        private String message;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getValidCount() {
            return validCount;
        }

        public void setValidCount(int validCount) {
            this.validCount = validCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public void setInsertedCount(int insertedCount) {
            this.insertedCount = insertedCount;
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

        public List<RowWarning> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<RowWarning> warnings) {
            this.warnings = warnings == null ? new ArrayList<RowWarning>() : warnings;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
