package com.zifang.z.lc.common.seal;

/**
 * 签章入参文件类型枚举 — 蒸馏自 ace-platform-core
 * {@code SealFileType} ({@code com.c2f.ace.core.seal.support}).
 *
 * <p>用于低代码平台"电子签章"模块标识入参文件的类型.
 * SDK 不自动识别, 由 URL 后缀或文件头判断.
 *
 * <ul>
 *   <li>{@link #PDF} — PDF 文件</li>
 *   <li>{@link #WORD} — Word 文档</li>
 *   <li>{@link #UNKNOWN} — 未知/不支持的文件类型</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcSealFileType {

    /** PDF 文件. */
    PDF,

    /** Word 文档. */
    WORD,

    /** 未知/不支持的文件类型. */
    UNKNOWN
}