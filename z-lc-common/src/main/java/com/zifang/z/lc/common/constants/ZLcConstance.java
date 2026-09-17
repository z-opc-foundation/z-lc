package com.zifang.z.lc.common.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-lc 核心常量 — 蒸馏自 ace-platform-core
 * {@code Constance} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>本类集中定义 z-lc 引擎用到的产品标识、缓存前缀、流程事件名、字典 code 等核心常量，
 * 避免散落在各处的字符串字面量难以维护.
 *
 * @author zifang
 */
public final class ZLcConstance {

    /** 产品标识 — 与 ace {@code PRODUCT_CODE = "ace-platform"} 对应. */
    public static final String PRODUCT_CODE = "z-lc";

    /** appCode 字段名（通用 HTTP Header / 流程变量 / SQL 列名） */
    public static final String APP_CODE = "appCode";

    /** modelCode 字段名 */
    public static final String MODEL_CODE = "modelCode";

    /** 独占锁前缀. */
    public static final String MONOPOLIZE_PREFIX = "Monopolize:";

    /** 独占锁标记字段. */
    public static final String MONOPOLIZE_FLAG = "MONOPOLIZEFLAG";

    /** 流程变量计数前缀（nOfCompleted 等） */
    public static final String NROF_PREFIX = "nrOf";

    /** 电子签名开关标识. */
    public static final String ELECTRONIC_SIGN_FLAG = "ELECTRONIC_SIGN_FLAG";

    /** MQ topic 模板. */
    public static final String MQ_TOPIC = "z-lc:%s";

    /** 流程事件 topic 模板. */
    public static final String MQ_PROCESS_EVENT_TOPIC = "process-event:%s";

    /** 任务事件 topic 模板. */
    public static final String MQ_TASK_EVENT_TOPIC = "task-event:%s";

    /** 流程事件类型 — 流程已发起. */
    public static final String PROCESS_STARTED = "processStarted";

    /** 流程事件类型 — 流程已完成. */
    public static final String PROCESS_COMPLETED = "processCompleted";

    /** 任务事件类型 — 任务已完成. */
    public static final String TASK_COMPLETED = "taskCompleted";

    /** 任务事件类型 — 任务被拒绝. */
    public static final String TASK_REFUSED = "taskRefused";

    /** 任务事件类型 — 任务已创建. */
    public static final String TASK_CREATED = "taskCreated";

    /** 任务事件类型 — 任务已同意. */
    public static final String TASK_AGREE = "taskAgree";

    /** 消息通知开关标识. */
    public static final String MSG_NOTIFY_ENABLE_FLAG = "MSG_NOTIFY_ENABLE_FLAG";

    /** 任务内部的流程上下文坑位 — 电子签图片关系 ID. */
    public static final String T_ELECTRONIC_SIGN_IMAGE_RELATION_ID = "T_ELECTRONIC_SIGN_IMAGE_RELATION_ID";

    /** 流程删除事件. */
    public static final String PROCESS_DELETED = "processDeleted";

    /** 流程挂起事件. */
    public static final String PROCESS_SUSPEND = "processSuspend";

    /** 已完成任务标记. */
    public static final String DONE_TASK_FLAG = "doneTaskFlag";

    /** 发起人节点定义 key. */
    public static final String INIT_TASK_DEFINITION_KEY = "user_static_initate";

    /** 无审批人自动审批人 ID（系统占位）. */
    public static final String AUTO_APPROVAL_USER_ID = "-1";

    /** 无审批人自动审批人名. */
    public static final String AUTO_APPROVAL_USER_NAME = "无审批人自动审批";

    /** 流程上下文 — 动作类型字段名. */
    public static final String PROCESS_CONTEXT_ACTION_TYPE = "SYSTEM_CONTEXT_ACTION_TYPE";

    /** 流程上下文 — 是否自动审批字段名. */
    public static final String PROCESS_CONTEXT_IS_AUTO = "SYSTEM_CONTEXT_IS_AUTO";

    /** 驳回模式 — 逐级驳回. */
    public static final Integer REJECT_MODEL_STEP = 1;

    /** 驳回模式 — 直接驳回. */
    public static final Integer REJECT_MODEL_JUMP = 2;

    /** ace 自定义 HTTP Header — 标识来源系统. */
    public static final String X_ZLC_ORIGIN = "X-ZLC-ORIGIN";

    /** 流程上下文 — 最后一次驳回时间字段名. */
    public static final String LAST_REJECT_TIME = "LAST_REJECT_TIME";

    /** 应用扩展 — 自动签名有效时长（秒）. */
    public static final String APP_EXTEND_AUTO_SIGN_TIME_REGION = "autoSignTimeRegion";

    /** 自动签名最大有效时长 — 24 小时. */
    public static final Long AUTO_SIGN_MAX_TIME_REGION = 86400L;

    /** 数据版本 cookie 有效时长（秒） */
    public static final int DATA_VERSION_COOKIE_EXPIRY = 86400;

    /** 外部模型列表页 code 前缀. */
    public static final String EXTERNAL_LIST_PAGE_CODE_PREFIX = "external_list@@@";

    /** 外部模型详情页 code 前缀. */
    public static final String EXTERNAL_DETAIL_PAGE_CODE_PREFIX = "external_detail@@@";

    /** 数据模型列表页 code 前缀. */
    public static final String DATA_LIST_PAGE_CODE_PREFIX = "data_list@@@";

    /** 数据模型 ADS 列表页 code 前缀. */
    public static final String DATA_ADS_LIST_PAGE_CODE_PREFIX = "data_list@@@ads_";

    /** 数据模型详情页 code 前缀. */
    public static final String DATA_DETAIL_PAGE_CODE_PREFIX = "data_detail@@@";

    /** 原始层表前缀. */
    public static final String ODS_PREFIX = "ods_";

    /** ADS 层表前缀. */
    public static final String ADS_PREFIX = "ads_";

    /** ADS 原始层表前缀. */
    public static final String ADS_ORIGIN_PREFIX = "ads_origin_";

    /** 标签模型字段前缀. */
    public static final String LABEL_PREFIX = "label_";

    /** 应用扩展 — 催办频率 key. */
    public static final String APP_EXTEND_URGE_FREQUENCY_KEY = "urge_frequency";

    /** 应用扩展 — 催办次数 key. */
    public static final String APP_EXTEND_URGE_NUM_KEY = "urge_num";

    /** 业务上下文 UUID 字段名. */
    public static final String BUSINESS_CONTEXT_UUID = "business_context_uuid";

    /** 系统字段配置类型 — 模型个性化系统字段. */
    public static final String MODEL_SYSTEM_FIELD = "model_system_field";

    /** 系统字段配置类型 — ODS 层通用系统字段. */
    public static final String ODS_NORMAL_SYSTEM_FIELD = "ods_normal_system_field";

    /** 系统字段配置类型 — BIZ 层通用系统字段. */
    public static final String BIZ_NORMAL_SYSTEM_FIELD = "biz_normal_system_field";

    /** 系统字段配置类型 — ADS 层通用系统字段. */
    public static final String ADS_NORMAL_SYSTEM_FIELD = "ads_normal_system_field";

    /** 表结构更新标记缓存前缀. */
    public static final String UPDATE_TABLE_FLAG_CACHE = "UPDATE_TABLE_FLAG_CACHE:";

    /** 大订单类型字典 code. */
    public static final String BIG_ORDER_TYPE_DICT_CODE = "todo_order_type";

    /** 是否紧急字典 code. */
    public static final String IS_URGENT_DICT_CODE = "isUrgent";

    /** IdentityLink 缓存前缀. */
    public static final String IDENTITY_LINK_CACHE = "identity_Link_cache:";

    /** 业务域字典 code. */
    public static final String BIZ_DOMAIN_DICT_CODE = "biz_domain";

    /** 自动审批缓存 key 模板. */
    public static final String AUTO_APPROVAL_FLAG_CACHE_KEY = "AUTO_AUDIT_TASK:%s";

    /** 全部事件类型常量（不可变列表） */
    public static final List<String> ALL_PROCESS_EVENT_TYPES = Collections.unmodifiableList(
            Arrays.asList(PROCESS_STARTED, PROCESS_COMPLETED, PROCESS_DELETED, PROCESS_SUSPEND));

    /** 全部任务事件类型常量（不可变列表） */
    public static final List<String> ALL_TASK_EVENT_TYPES = Collections.unmodifiableList(
            Arrays.asList(TASK_CREATED, TASK_AGREE, TASK_REFUSED, TASK_COMPLETED));

    /** 自动签名时长范围 map — key = 类型（int/varchar/...）, value = 默认长度. */
    public static final Map<String, String> DEFAULT_FIELD_LENGTH_MAP;

    static {
        DEFAULT_FIELD_LENGTH_MAP = new HashMap<>();
        DEFAULT_FIELD_LENGTH_MAP.put("int", "10");
        DEFAULT_FIELD_LENGTH_MAP.put("varchar", "255");
        DEFAULT_FIELD_LENGTH_MAP.put("double", "14,2");
        DEFAULT_FIELD_LENGTH_MAP.put("float", "14,2");
        DEFAULT_FIELD_LENGTH_MAP.put("bigint", "19");
        DEFAULT_FIELD_LENGTH_MAP.put("smallint", "5");
        DEFAULT_FIELD_LENGTH_MAP.put("decimal", "14,2");
    }

    private ZLcConstance() {
        // 常量类，禁止实例化
    }
}
