package com.zifang.z.lc.core.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 角色能拿到哪些操作权限的唯一口径 (F035 的 ACL 那一族)。
 * <p>
 * 存在的理由和 {@code PipelineStages} 一模一样: 这份清单此前有两份平行副本，而两边都不是闸 ——
 * 后端只在 {@link PermissionEntity#getPermission()} 的注释里写了四个词
 * (READ / WRITE / DELETE / ADMIN)，前端矩阵的列头是另外五个词
 * (VIEW / CREATE / UPDATE / DELETE / EXPORT)，而 {@link PermissionService#grant} 对
 * {@code permission} 一个字都不校验: 实测 {@code "WIBBLE_不是词表里的"} 存得进去。
 * 一份没人校验的词表等于没有词表 —— 上游网关按它做判定时，拼错一个字母的那一行永远匹配不上，
 * 而矩阵里它看着是"已授予"。
 * <p>
 * 采前端矩阵已经在给用户看的那五个词 (它们与运行时真有的一端一一对应: 列表/详情、建单、
 * 内联编辑与表单保存、单条与批量删除、CSV 导出)。{@code PermissionEntity} 注释里的
 * READ / WRITE / ADMIN 是另一套没有落地的说法，随这一份收编。
 * <p>
 * ⚠ 这一张表**不**代表 z-lc 自己会拦截什么: 引擎的读写路径今天一次都不查它
 * (鉴权在网关 z-ctc，见 {@code PermissionsPage} 的说明)。这里管的是"策略数据本身别再乱" ——
 * 存进去的词必须落在这张表里，否则这份策略谁都读不准。
 */
public final class PermissionKeys {

    public static final String VIEW = "VIEW";
    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String EXPORT = "EXPORT";

    /** 顺序就是前端矩阵的列顺序 —— 抄清单的人不该再自己排一遍。 */
    private static final List<String> ALL = Collections.unmodifiableList(
            Arrays.asList(VIEW, CREATE, UPDATE, DELETE, EXPORT));

    private PermissionKeys() {
    }

    public static List<String> all() {
        return ALL;
    }

    public static boolean isKnown(String permission) {
        if (permission == null) {
            return false;
        }
        String wanted = permission.trim().toUpperCase(Locale.ROOT);
        for (String key : ALL) {
            if (key.equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 归一成一个规范形态: 去空白 + 大写。
     * <p>
     * 大小写要参与归一，因为比较是在 SQL 里做的，而**实测这套 dev 库 (H2, MODE=MySQL) 的
     * {@code =} 分大小写**: 存进去的 {@code 'view'} 对 {@code permission = 'VIEW'} 比不中，
     * 于是同一个逻辑授权在库里成了并排两行 (各自都"查重成功")，而矩阵与网关只认大写那一行 ——
     * 正是"存下了、查不到、界面上却显示已授予"。这个后果不必等某个特定排序规则: 上面那两行
     * 就是在 dev 默认配置下量出来的。
     *
     * @throws IllegalArgumentException 不在词表里；消息点名允许的词，好让人不必去猜
     */
    public static String canonical(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException("permission 是必填的，允许: " + String.join(" / ", ALL));
        }
        String wanted = permission.trim().toUpperCase(Locale.ROOT);
        if (!ALL.contains(wanted)) {
            throw new IllegalArgumentException("未知的权限项: " + permission.trim()
                    + "，允许: " + String.join(" / ", ALL));
        }
        return wanted;
    }
}
