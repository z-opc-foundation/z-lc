package com.zifang.z.lc.common.constance;

/**
 * z-lc 平台常量 — 蒸馏自 ace-platform-core
 * {@code Constance} ({@code com.c2f.ace.core.common}).
 *
 * <p>包含低代码平台流程引擎、MQ 消息、系统上下文、模型标识等核心常量.
 * 业务方引用本类获取平台级常量，无需硬编码字符串.
 *
 * @author zifang
 */
public final class ZLcConstance {

    private ZLcConstance() {
    }

    // ==================== 产品标识 ====================

    /** 产品编码. */
    public static final String PRODUCT_CODE = "z-lc";

    /** 应用编码变量名. */
    public static final String APP_CODE = "appCode";

    /** 模型编码变量名. */
    public static final String MODEL_CODE = "modelCode";

    // ==================== 会签前缀 ====================

    /** 会签独占前缀. */
    public static final String MONOPOLIZE_PREFIX = "Monopolize:";

    /** 会签标记变量名. */
    public static final String MONOPOLIZE_FLAG = "MONOPOLIZEFLAG";

    /** 会签人数前缀 (Camunda nrOfCompletedInstances 等). */
    public static final String NROF_PREFIX = "nrOf";

    // ==================== 电子签名 ====================

    /** 电子签名开关标识. */
    public static final String ELECTRONIC_SIGN_FLAG = "ELECTRONIC_SIGN_FLAG";

    /** 电子签名图片关联 ID (task 级上下文). */
    public static final String TASK_ELECTRONIC_SIGN_IMAGE_RELATION_ID =
            "T_ELECTRONIC_SIGN_IMAGE_RELATION_ID";

    // ==================== MQ 消息主题 ====================

    /** MQ 主题模板 — ace:%s. */
    public static final String TOPIC = "ace:%s";

    /** 流程事件主题模板 — process-event:%s. */
    public static final String EVENT_TOPIC = "process-event:%s";

    /** 任务事件主题模板 — task-event:%s. */
    public static final String TASK_EVENT_TOPIC = "task-event:%s";

    // ==================== 流程事件类型 ====================

    /** 流程已启动. */
    public static final String PROCESS_STARTED = "processStarted";

    /** 流程已完成. */
    public static final String PROCESS_COMPLETED = "processCompleted";

    /** 流程已删除. */
    public static final String PROCESS_DELETED = "processDeleted";

    /** 流程已挂起. */
    public static final String PROCESS_SUSPEND = "processSuspend";

    // ==================== 任务事件类型 ====================

    /** 任务已完成. */
    public static final String TASK_COMPLETED = "taskCompleted";

    /** 任务已拒绝. */
    public static final String TASK_REFUSED = "taskRefused";

    /** 任务已创建. */
    public static final String TASK_CREATED = "taskCreated";

    /** 任务已同意. */
    public static final String TASK_AGREE = "taskAgree";

    // ==================== 消息通知 ====================

    /** 消息通知开关. */
    public static final String MSG_NOTIFY_ENABLE_FLAG = "MEG_NOTIFY_ENABLE_FLAG";

    // ==================== 流程上下文 ====================

    /** 系统上下文 — 动作类型. */
    public static final String PROCESS_CONTEXT_ACTION_TYPE = "SYSTEM_CONTEXT_ACTION_TYPE";

    /** 系统上下文 — 是否自动审批. */
    public static final String PROCESS_CONTEXT_IS_AUTO = "SYSTEM_CONTEXT_IS_AUTO";

    /** 逐级驳回模式 — 回退到驳回人. */
    public static final Integer REJECT_MODEL_STEP = 1;

    /** 直接驳回模式 — 跳到驳回人. */
    public static final Integer REJECT_MODEL_JUMP = 2;

    /** 系统上下文 — 最后一次驳回时间. */
    public static final String LAST_REJECT_TIME = "LAST_REJECT_TIME";

    // ==================== 自动审批 ====================

    /** 无审批人时自动审批人 ID. */
    public static final String AUTO_APPROVAL_USER_ID = "-1";

    /** 无审批人时自动审批人名称. */
    public static final String AUTO_APPROVAL_USER_NAME = "无审批人自动审批";

    /** 自动审批缓存 key 模板. */
    public static final String AUTO_APPROVAL_FLAG_CACHE_KEY = "AUTO_AUDIT_TASK:%s";

    /** 任务完成标记. */
    public static final String DONE_TASK_FLAG = "doneTaskFlag";

    /** 发起人节点定义 key. */
    public static final String INIT_TASK_DEFINITION_KEY = "user_static_initate";

    // ==================== HTTP Header ====================

    /** ACE 平台来源 Header. */
    public static final String X_LC_ORIGIN = "X-LC-ORIGIN";

    // ==================== 数据版本 ====================

    /** 数据版本 cookie 有效时长 (秒). */
    public static final int DATA_VERSION_COOKIE_EXPIRY = 86400;

    // ==================== 前缀 ====================

    /** 外部列表页面前缀. */
    public static final String EXTERNAL_LIST_PAGE_CODE_PREFIX = "external_list@@@";

    /** 外部详情页面前缀. */
    public static final String EXTERNAL_DETAIL_PAGE_CODE_PREFIX = "external_detail@@@";

    /** 数据模型列表页面前缀. */
    public static final String DATA_LIST_PAGE_CODE_PREFIX = "data_list@@@";

    /** 数据模型 ADS 列表页面前缀. */
    public static final String DATA_ADS_LIST_PAGE_CODE_PREFIX = "data_list@@@ads_";

    /** 数据模型详情页面前缀. */
    public static final String DATA_DETAIL_PAGE_CODE_PREFIX = "data_detail@@@";

    /** HDOS 模型前缀. */
    public static final String HDOS_MODEL_PREFIX = "hdos@";

    /** HDOS ADS 模型前缀. */
    public static final String HDOS_ADS_MODEL_PREFIX = "hdos@ads_origin_";

    /** ODS 原始层表前缀. */
    public static final String ODS_PREFIX = "ods_";

    /** ADS 聚合层表前缀. */
    public static final String ADS_PREFIX = "ads_";

    /** ADS 原始层表前缀. */
    public static final String ADS_ORIGIN_PREFIX = "ads_origin_";

    /** 标签模型字段前缀. */
    public static final String LABEL_PREFIX = "label_";

    // ==================== 字典编码 ====================

    /** 待办排序类型字典. */
    public static final String BIG_ORDER_TYPE_DICT_CODE = "todo_order_type";

    /** 是否紧急字典. */
    public static final String IS_URGENT_DICT_CODE = "isUrgent";

    /** 业务域字典. */
    public static final String BIZ_DOMAIN_DICT_CODE = "biz_domain";

    // ==================== 缓存 key ====================

    /** 更新表标记缓存前缀. */
    public static final String UPDATE_TABLE_FLAG_CACHE = "UPDATE_TABLE_FLAG_CACHE:";

    /** 身份关联缓存前缀. */
    public static final String IDENTITY_LINK_CACHE = "identity_Link_cache:";

    // ==================== 系统字段配置类型 ====================

    /** 模型个性化系统字段. */
    public static final String MODEL_SYSTEM_FIELD = "model_system_field";

    /** ODS 层通用系统字段. */
    public static final String ODS_NORMAL_SYSTEM_FIELD = "ods_normal_system_field";

    /** BIZ 层通用系统字段. */
    public static final String BIZ_NORMAL_SYSTEM_FIELD = "biz_normal_system_field";

    /** ADS 层通用系统字段. */
    public static final String ADS_NORMAL_SYSTEM_FIELD = "ads_normal_system_field";

    // ==================== 应用扩展 ====================

    /** 催办频率扩展字段 key. */
    public static final String APP_EXTEND_URGE_FREQUENCY_KEY = "urge_frequency";

    /** 催办次数扩展字段 key. */
    public static final String APP_EXTEND_URGE_NUM_KEY = "urge_num";

    /** 业务上下文 UUID. */
    public static final String BUSINESS_CONTEXT_UUID = "business_context_uuid";
}
