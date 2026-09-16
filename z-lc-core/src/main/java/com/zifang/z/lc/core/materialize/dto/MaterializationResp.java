package com.zifang.z.lc.core.materialize.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 物化状态/结果响应.
 */
public class MaterializationResp {

    private Long id;
    private String appCode;
    private String materializationPath;
    private String exportVersion;
    private String status;
    private Integer fileCount;
    private String description;
    private String errorMessage;
    private String triggerSource;
    private String createTime;
    private String updateTime;

    /**
     * 本次生成的文件清单 (前端可下载/预览)
     */
    private List<GeneratedFile> files = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        this.id = v;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String v) {
        this.appCode = v;
    }

    public String getMaterializationPath() {
        return materializationPath;
    }

    public void setMaterializationPath(String v) {
        this.materializationPath = v;
    }

    public String getExportVersion() {
        return exportVersion;
    }

    public void setExportVersion(String v) {
        this.exportVersion = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer v) {
        this.fileCount = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String v) {
        this.errorMessage = v;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String v) {
        this.triggerSource = v;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String v) {
        this.createTime = v;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String v) {
        this.updateTime = v;
    }

    public List<GeneratedFile> getFiles() {
        return files;
    }

    public void setFiles(List<GeneratedFile> v) {
        this.files = v;
    }

    public static class GeneratedFile {
        private String entityCode;
        private String type;        // Entity / Mapper / Service / Controller / ReactPage
        private String relativePath;
        private String content;
        private long sizeBytes;

        public GeneratedFile() {
        }

        public GeneratedFile(String entityCode, String type, String rel, String content) {
            this.entityCode = entityCode;
            this.type = type;
            this.relativePath = rel;
            this.content = content;
            this.sizeBytes = content == null ? 0 : content.getBytes().length;
        }

        public String getEntityCode() {
            return entityCode;
        }

        public void setEntityCode(String v) {
            this.entityCode = v;
        }

        public String getType() {
            return type;
        }

        public void setType(String v) {
            this.type = v;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String v) {
            this.relativePath = v;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String v) {
            this.content = v;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long v) {
            this.sizeBytes = v;
        }
    }
}
