package com.zifang.z.lc.sdk.define;

/**
 * 模型数据保存模式常量.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataSaveMode}（{@code com.c2f.ace.engine.define}）。
 *
 * <p>三种保存模式在底层均映射为一次 INSERT（数据主键由存储层生成），
 * 区别仅在于上层业务处理：
 * <ul>
 *   <li>{@link #SAVE_COMMON}：常规提交（如表单提交）</li>
 *   <li>{@link #SAVE_TEMP}：暂存（草稿状态，可继续编辑）</li>
 *   <li>{@link #SAVE_IN_PROCESS}：流程运行中编辑保存（流程引擎持有锁）</li>
 * </ul>
 *
 * <p>典型用法（业务方继承 {@code AbstractDataModelService} 时调用）：
 * <pre>{@code
 *   Long pkId = modelService.save(entity, ModelDataSaveMode.SAVE_TEMP);
 * }</pre>
 *
 * @author zifang
 */
public final class ModelDataSaveMode {

    /**
     * 普通模式（默认）：正常插入/更新.
     */
    public static final Integer SAVE_COMMON = 0;

    /**
     * 暂存模式：本质仍是正常插入，但业务层视为草稿，可继续编辑后再提交.
     */
    public static final Integer SAVE_TEMP = 1;

    /**
     * 流程运行中编辑保存：流程引擎持有编辑锁，保存时记录工作流快照.
     */
    public static final Integer SAVE_IN_PROCESS = 2;

    private ModelDataSaveMode() {
        // 工具类，禁止实例化
    }
}
