package com.zifang.z.lc.core.pipeline.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 写路径流水线的唯一口径: 哪些触发事件真有挂接点、哪些阶段类型真有执行器、哪些阶段不许摘。
 * <p>
 * 存在的理由是配置面与执行面此前完全脱节: {@code z_lc_pipeline_config} 里的 order/enabled/stages
 * 一个都不影响运行时 (Pipeline 跑的是按类名字典序排好的全部 Spring bean), 而配置页对外宣传
 * "校验、字典解析、外部通知等, 按 order 顺序执行" —— 其中 WEBHOOK / SCRIPT 连执行器都不存在,
 * AFTER_* 连挂接点都没有。所以这里把"引擎能兑现什么"钉成一份数据, 让
 * {@link PipelineConfigService} 的写入口校验与 {@code Pipeline} 的执行链解析共用同一份,
 * 两边不再各写一套清单。
 * <p>
 * 新增一个阶段类型的正确姿势: 先有 {@code FieldProcessor} 实现并确定它挂在写前还是写后,
 * 再登记到 {@link #PROCESSOR_BY_TYPE}; 只有登记在这里的类型才可能被配置接受。
 */
public final class PipelineStages {

    /** 引擎里唯一的写前挂接点: 创建一条记录之前. */
    public static final String BEFORE_CREATE = "BEFORE_CREATE";
    /** 引擎里唯一的写前挂接点: 更新一条记录之前 (部分更新也走这里). */
    public static final String BEFORE_UPDATE = "BEFORE_UPDATE";

    public static final String DICT_RESOLVE = "DICT_RESOLVE";
    public static final String REF_CHECK = "REF_CHECK";
    public static final String REQUIRED_CHECK = "REQUIRED_CHECK";
    public static final String TYPE_CONVERT = "TYPE_CONVERT";
    public static final String VALUE_VALIDATE = "VALUE_VALIDATE";

    /**
     * 阶段类型 → 处理器名 ({@code FieldProcessor.name()})。
     * <p>
     * 迭代顺序就是**没有配置时**的默认执行顺序 —— 沿用 Pipeline 历史上按类名字典序排出来的那条链
     * (DictResolve, RefCheck, RequiredCheck, TypeConvert, ValueValidate), 这样"新建一份等于现状"
     * 的配置不会悄悄改变任何实体的行为。
     */
    private static final Map<String, String> PROCESSOR_BY_TYPE = buildProcessorByType();

    /** 引擎真有挂接点的触发事件。AFTER_* 一个都不在这里: 写后没有任何回调落点。 */
    private static final List<String> SUPPORTED_TRIGGERS =
            Collections.unmodifiableList(new ArrayList<String>() {{
                add(BEFORE_CREATE);
                add(BEFORE_UPDATE);
            }});

    /**
     * 摘掉就会让写入变坏的阶段。REF_CHECK / DICT_RESOLVE 不在其中 —— 它们俩在写路径上
     * 目前不改变数据 (见 {@link #NO_OP_ON_WRITE}), 所以允许不配。
     */
    private static final Set<String> MANDATORY = orderedSet(REQUIRED_CHECK, TYPE_CONVERT, VALUE_VALIDATE);

    /** 写路径上什么都不做、只在读路径 (或压根没实现) 的阶段。 */
    private static final Set<String> NO_OP_ON_WRITE = orderedSet(DICT_RESOLVE, REF_CHECK);

    private static final ObjectMapper JSON = new ObjectMapper();

    private PipelineStages() {
    }

    private static Map<String, String> buildProcessorByType() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(DICT_RESOLVE, "DictResolve");
        m.put(REF_CHECK, "RefCheck");
        m.put(REQUIRED_CHECK, "RequiredCheck");
        m.put(TYPE_CONVERT, "TypeConvert");
        m.put(VALUE_VALIDATE, "ValueValidate");
        return Collections.unmodifiableMap(m);
    }

    private static Set<String> orderedSet(String... values) {
        Set<String> s = new LinkedHashSet<String>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    /** 没有配置时引擎跑的那条默认链 (阶段类型, 按执行顺序). */
    public static List<String> defaultOrder() {
        return Collections.unmodifiableList(new ArrayList<String>(PROCESSOR_BY_TYPE.keySet()));
    }

    public static List<String> supportedTriggers() {
        return SUPPORTED_TRIGGERS;
    }

    public static List<String> supportedTypes() {
        return defaultOrder();
    }

    public static List<String> mandatoryTypes() {
        return Collections.unmodifiableList(new ArrayList<String>(MANDATORY));
    }

    public static List<String> noOpOnWriteTypes() {
        return Collections.unmodifiableList(new ArrayList<String>(NO_OP_ON_WRITE));
    }

    public static boolean isSupportedTrigger(String triggerEvent) {
        return triggerEvent != null && SUPPORTED_TRIGGERS.contains(triggerEvent.trim());
    }

    public static boolean isSupportedType(String stageType) {
        return stageType != null && PROCESSOR_BY_TYPE.containsKey(stageType.trim());
    }

    /** 阶段类型对应的处理器名; 未登记的类型返回 null (调用方必须拒, 不能跳过). */
    public static String processorNameFor(String stageType) {
        return stageType == null ? null : PROCESSOR_BY_TYPE.get(stageType.trim());
    }

    /**
     * 校验一份配置并回它**实际会执行**的阶段顺序。任何不合口径的地方都抛
     * {@link IllegalArgumentException} —— 消息里点名"引擎支持什么", 而不是只说"不合法"。
     *
     * @param triggerEvent 配置的触发事件
     * @param stagesJson {@code [{type,config,order}]} 的 JSON 串
     * @return 按 order 排好、去重后的阶段类型列表
     */
    public static List<String> validateAndResolve(String triggerEvent, String stagesJson) {
        String trigger = triggerEvent == null ? null : triggerEvent.trim();
        if (trigger == null || trigger.isEmpty()) {
            throw new IllegalArgumentException("触发事件不能为空, 引擎当前只支持 " + join(SUPPORTED_TRIGGERS));
        }
        if (!isSupportedTrigger(trigger)) {
            throw new IllegalArgumentException("触发事件 [" + trigger + "] 在引擎里没有挂接点: "
                    + "写后 (AFTER_*) 事件没有任何回调落点, 配置了也不会执行。当前只支持 "
                    + join(SUPPORTED_TRIGGERS));
        }

        List<String> types = parseTypes(stagesJson);

        Set<String> seen = new LinkedHashSet<String>();
        for (String type : types) {
            if (!seen.add(type)) {
                throw new IllegalArgumentException("阶段 [" + type + "] 重复配置, 同一个阶段在一次写入里只会执行一次");
            }
        }

        List<String> missing = new ArrayList<String>();
        for (String required : MANDATORY) {
            if (!seen.contains(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("阶段链缺少必填阶段 " + join(missing)
                    + " —— 摘掉它们等于让写入绕过必填校验/类型转换/值校验");
        }

        int convert = types.indexOf(TYPE_CONVERT);
        int validate = types.indexOf(VALUE_VALIDATE);
        if (convert > validate) {
            throw new IllegalArgumentException("VALUE_VALIDATE 必须排在 TYPE_CONVERT 之后: "
                    + "值校验的长度规则只对字符串单元格生效, 放到转换之前会把合法的数字按字符长度拒掉");
        }

        return types;
    }

    /**
     * 解析 stages JSON: 必须是对象数组, 每项带引擎支持的 type; order 缺省时按数组下标。
     * 排序按 (order, 数组位置) —— 位置是稳定的次级键, 所以两份 order 相同的配置结果确定。
     */
    private static List<String> parseTypes(String stagesJson) {
        if (stagesJson == null || stagesJson.trim().isEmpty()) {
            throw new IllegalArgumentException("阶段链不能为空, 可配置的阶段类型: " + join(PROCESSOR_BY_TYPE.keySet()));
        }

        JsonNode root;
        try {
            root = JSON.readTree(stagesJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("阶段链不是合法 JSON: " + shorten(ex.getMessage()));
        }
        if (root == null || !root.isArray() || root.size() == 0) {
            throw new IllegalArgumentException("阶段链必须是非空 JSON 数组, 形如 "
                    + "[{\"type\":\"TYPE_CONVERT\",\"order\":1}]");
        }

        List<Stage> stages = new ArrayList<Stage>();
        int index = 0;
        for (Iterator<JsonNode> it = root.elements(); it.hasNext(); index++) {
            JsonNode node = it.next();
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("阶段链第 " + (index + 1) + " 项不是对象, "
                        + "每一项必须形如 {\"type\":\"TYPE_CONVERT\",\"order\":1}");
            }

            JsonNode typeNode = node.get("type");
            String type = typeNode == null || typeNode.isNull() ? null : typeNode.asText().trim();
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("阶段链第 " + (index + 1) + " 项没有 type, "
                        + "可配置的阶段类型: " + join(PROCESSOR_BY_TYPE.keySet()));
            }
            if (!PROCESSOR_BY_TYPE.containsKey(type)) {
                throw new IllegalArgumentException("阶段 [" + type + "] 在引擎里没有执行器, 配置了也不会执行。"
                        + "当前支持的阶段类型: " + join(PROCESSOR_BY_TYPE.keySet()));
            }

            int order = index;
            JsonNode orderNode = node.get("order");
            if (orderNode != null && !orderNode.isNull()) {
                if (!orderNode.isIntegralNumber()) {
                    throw new IllegalArgumentException("阶段 [" + type + "] 的 order 必须是整数, 实际: "
                            + shorten(orderNode.asText()));
                }
                order = orderNode.intValue();
            }
            stages.add(new Stage(type, order, index));
        }

        Collections.sort(stages);

        List<String> types = new ArrayList<String>(stages.size());
        for (Stage s : stages) {
            types.add(s.type);
        }
        return types;
    }

    public static String join(Iterable<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(v);
        }
        return sb.toString();
    }

    private static String shorten(String raw) {
        if (raw == null) {
            return "null";
        }
        return raw.length() > 160 ? raw.substring(0, 160) : raw;
    }

    /** 一条阶段的排序键: order 优先, 数组位置兜底 (保证可预测). */
    private static final class Stage implements Comparable<Stage> {
        private final String type;
        private final int order;
        private final int position;

        private Stage(String type, int order, int position) {
            this.type = type;
            this.order = order;
            this.position = position;
        }

        @Override
        public int compareTo(Stage o) {
            if (order != o.order) {
                return order < o.order ? -1 : 1;
            }
            return position - o.position;
        }
    }
}
