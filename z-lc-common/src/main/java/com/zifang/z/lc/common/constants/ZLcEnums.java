package com.zifang.z.lc.common.constants;

import java.util.*;

/**
 * 低代码平台常量枚举集 — 蒸馏自 ace-platform-core
 * {@code Enums} ({@code com.c2f.ace.core.common}).
 *
 * <p>包含低代码平台各子模块的常量定义:
 * 模型类型、字段类型、应用类型、构件类型、页面类型、取值策略等.
 * 蒸馏时移除了 ace 对 Lombok @Data/@AllArgsConstructor / Swagger @ApiModel/@ApiModelProperty
 * / GitLab4J 的依赖, 改为纯 JDK + 手写 getter 实现.
 *
 * @author zifang
 */
public final class ZLcEnums {

    private ZLcEnums() {
    }

    // ==================== 菜单启用标记 ====================

    /** 菜单启用标记常量. */
    public static final class MenuEnableFlag {
        public static final int ENABLE = 1;
        public static final int DISABLE = 0;
        private MenuEnableFlag() {}
    }

    // ==================== 模型类型标记 ====================

    /** 模型类型标记常量. */
    public static final class ModelTypeFlag {
        public static final int PHYSICAL_TYPE = 1;
        public static final int VIRTUAL_TYPE = 0;
        private ModelTypeFlag() {}
    }

    // ==================== 模型字段类型 ====================

    /** 模型字段类型常量. */
    public static final class ModelFieldType {
        public static final String TIME = "Time";
        public static final String NUMBER = "Number";
        public static final String TEXT = "Text";
        public static final String OBJECT = "Object";
        public static final String ARRAY = "Array";
        public static final List<String> SYS_FIELD_TYPES = Collections.unmodifiableList(
                Arrays.asList(TIME, NUMBER, TEXT));
        private ModelFieldType() {}
    }

    // ==================== 字段关联类型 ====================

    /** 字段关联类型常量. */
    public static final class FieldRelateType {
        public static final String ONE_TO_ONE = "oneToOne";
        public static final String ONE_TO_MANY = "oneToMany";
        private FieldRelateType() {}
    }

    // ==================== HTTP 请求类型 ====================

    /** HTTP 请求类型常量. */
    public static final class HttpRequestType {
        public static final String GET = "get";
        public static final String POST = "post";
        private HttpRequestType() {}
    }

    // ==================== 页面模板类型 ====================

    /** 页面模板类型. */
    public static final class PageTemplateType {
        public static final String FORM = "form";
        public static final String FORM_DESC = "表单页";
        public static final String LIST = "list";
        public static final String LIST_DESC = "列表页";
        public static final String APP_FORM = "app_form";
        public static final String APP_FORM_DESC = "移动端表单页";
        public static final String FORM_V2 = "formV2";
        public static final String FORM_V2_DESC = "表单页-新版编辑器";
        public static final String LIST_V2 = "listV2";
        public static final String LIST_V2_DESC = "列表页-新版编辑器";
        public static final String COMMON = "common";
        public static final String COMMON_DESC = "普通页";
        public static final String PRINT = "print";
        public static final String PRINT_DESC = "打印模板页";

        public static final List<String> DEFAULT_PAGE_TYPES = Collections.unmodifiableList(
                Arrays.asList(LIST, FORM, APP_FORM, FORM_V2, LIST_V2, COMMON, PRINT));
        private PageTemplateType() {}
    }

    // ==================== 默认值策略 ====================

    /** 默认值策略. */
    public static final class DefaultValueStrategy {
        public static final String CURRENT_TIME_MILLIS = "${CURRENT_TIME_MILLIS}";
        public static final String CURRENT_TIME = "${CURRENT_TIMM}";
        public static final String OPERATOR = "${OPERATOR}";
        public static final String ORG_ID = "${ORG_ID}";
        public static final String CURRENT_CAMPUS_ID = "${CURRENT_CAMPUS_ID}";
        public static final String CURRENT_APP_CODE = "${CURRENT_APP_CODE}";
        public static final String CURRENT_MODEL_CODE = "${CURRENT_MODEL_CODE}";
        public static final String CURRENT_TENANT_CODE = "${CURRENT_TENANT_CODE}";
        private DefaultValueStrategy() {}
    }

    // ==================== 取值填充策略 ====================

    /** 取值填充策略. */
    public static final class FetchValueFillStrategy {
        public static final String STATIC_VALUE = "STATIC_VALUE";
        public static final String CONTEXT_VALUE = "CONTEXT_VALUE";
        public static final String DICT_VALUE = "DICT_VALUE";
        public static final String SERVICE_VALUE = "SERVICE_VALUE";
        private FetchValueFillStrategy() {}
    }

    // ==================== 应用包类型 ====================

    /** 应用包类型. */
    public static final class AppPackageType {
        public static final String IMPORT = "import";
        public static final String OUTPUT = "output";
        private AppPackageType() {}
    }

    // ==================== 应用类型 ====================

    /** 应用类型. */
    public static final class AppType {
        public static final String LC = "LC";
        public static final String NC = "NC";
        public static final List<String> APP_TYPES = Collections.unmodifiableList(Arrays.asList(LC, NC));

        public static boolean contains(String type) {
            return APP_TYPES.contains(type);
        }
        private AppType() {}
    }

    // ==================== 应用构件类型 ====================

    /** 应用构件类型. */
    public static final class AppComponentType {
        public static final String MODEL = "model";
        public static final String PAGE = "page";
        public static final String DICT = "dict";
        public static final String WORKFLOW = "workflow";
        public static final String SERVICE = "service";
        public static final String TABLE = "table";
        public static final String MODEL_SERVICE = "modelService";
        public static final String COMMON = "common";
        public static final String PRINT = "print";
        private AppComponentType() {}
    }

    // ==================== 逻辑文件夹类型 ====================

    /** 逻辑文件夹类型. */
    public static final class LogicFolderType {
        public static final String CODE_FOLDER = "code";
        public static final String RESOURCE_FOLDER = "resources";
        private LogicFolderType() {}
    }

    // ==================== Git 文件构建类型 ====================

    /** Git 文件构建类型. */
    public static final class GitComponentType {
        public static final String FOLDER = "folder";
        public static final String FILE = "file";
        public static final String UNSUPPORT = "unSupport";
        private GitComponentType() {}
    }

    // ==================== 应用版本类型 ====================

    /** 应用版本提交类型. */
    public static final class AppVersionCommitType {
        public static final String CREATE = "create";
        public static final String REMOVE = "remove";
        public static final String UPDATE = "update";
        private AppVersionCommitType() {}
    }

    // ==================== 页面操作记录类型 ====================

    /** 页面操作记录类型. */
    public static final class PageTemplateOperateRecordType {
        public static final String PRINT = "print";
        private PageTemplateOperateRecordType() {}
    }

    // ==================== 任务成员类型 ====================

    /** 任务成员类型常量. */
    public static final class IdentityLinkType {
        public static final String ASSIGNEE = "assignee";
        public static final String CANDIDATE = "candidate";
        public static final String OWNER = "owner";
        public static final String STARTER = "starter";
        public static final String PARTICIPANT = "participant";
        private IdentityLinkType() {}
    }
}
