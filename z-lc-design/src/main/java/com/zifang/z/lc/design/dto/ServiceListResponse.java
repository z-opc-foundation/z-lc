package com.zifang.z.lc.design.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码服务列表响应 DTO.
 * <p>
 * 用于 /api/lc/design/services 端点, 返回已注册的低代码服务统计与列表.
 */
@Schema(description = "低代码服务列表响应")
public class ServiceListResponse {

    @Schema(description = "已注册服务总数")
    private int total;

    @Schema(description = "服务列表")
    private List<ServiceEntry> services = new ArrayList<>();

    public ServiceListResponse() {
    }

    public ServiceListResponse(int total, List<ServiceEntry> services) {
        this.total = total;
        this.services = services;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<ServiceEntry> getServices() {
        return services;
    }

    public void setServices(List<ServiceEntry> services) {
        this.services = services;
    }

    /**
     * 单个低代码服务的标识信息.
     */
    @Schema(description = "低代码服务标识")
    public static class ServiceEntry {

        @Schema(description = "服务实现类全名")
        private String clazz;

        @Schema(description = "服务 group 标识")
        private String group;

        @Schema(description = "服务 code 标识")
        private String code;

        @Schema(description = "服务名称")
        private String name;

        public ServiceEntry() {
        }

        public ServiceEntry(String clazz, String group, String code, String name) {
            this.clazz = clazz;
            this.group = group;
            this.code = code;
            this.name = name;
        }

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
