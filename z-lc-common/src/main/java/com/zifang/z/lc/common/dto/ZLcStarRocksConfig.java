package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * StarRocks 连接配置 — 蒸馏自 ace-platform-core
 * {@code StarRocksConfig} ({@code com.c2f.ace.core.utils}).
 *
 * <p>封装 StarRocks 数据库的连接参数: 主机、端口、用户名、密码、数据库、表名.
 * 蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为手写 getter/setter.
 *
 * @author zifang
 */
public class ZLcStarRocksConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String host;
    private Integer port;
    private String user;
    private String password;
    private String db;
    private String table;

    public ZLcStarRocksConfig() {
    }

    public ZLcStarRocksConfig(String host, Integer port, String user,
                               String password, String db, String table) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.db = db;
        this.table = table;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
