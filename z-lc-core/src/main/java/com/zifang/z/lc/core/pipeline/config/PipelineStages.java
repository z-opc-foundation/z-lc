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

    /**
     * 阶段类型 → 该阶段的处理器**真正会读**的 config 键。今天它是空的: 五个处理器没有一个
     * 读 {@code stages[].config} (见 {@code Pipeline#resolve} —— 解析结果只有阶段类型)。
     * <p>
     * 为什么要一张"今天为空"的表: 配置页放着一个自由 JSON 输入框, 用户填了、存成功、
     * 运行期一个字都不执行 —— 这就是 #41 那一类缺陷换了个字段重演。有了这张表,
     * 写入口可以把"引擎不读的参数"当场拒掉, 而将来真给某一档实现参数时,
     * **必须在这里登记 + 让处理器真读它**, 两处一起改。
     * <p>
     * ⚠ 表为空时, "登记的键真被读到"这条正向检查没有猎物 (真空为真), 所以它**不是**一条
     * 有测试守着的保证 —— 第一个参数落地时要连这条检查一起补, 并按 #41 的六层各钉一层。
     */
    private static final Map<String, List<String>> CONFIG_KEYS_BY_TYPE = buildConfigKeysByType();

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

    /**
     * 今天每个档都是空清单。**故意不写成 null / 不省掉这个 map**: 少了它就没有地方可以登记,
     * 而"没地方登记"的下一步一定是"在处理器里偷偷读 get(\"xxx\")" —— 那又把词表拆成两处。
     */
    private static Map<String, List<String>> buildConfigKeysByType() {
        Map<String, List<String>> m = new LinkedHashMap<String, List<String>>();
        for (String type : PROCESSOR_BY_TYPE.keySet()) {
            m.put(type, Collections.<String>emptyList());
        }
        return Collections.unmodifiableMap(m);
    }

    /** 某一档真被处理的 config 键 (今天对所有档都是空清单). */
    public static List<String> configKeysOf(String stageType) {
        List<String> keys = stageType == null ? null : CONFIG_KEYS_BY_TYPE.get(stageType.trim());
        return keys == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(keys));
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
     * **写入口**用的校验: 校验一份配置并回它实际会执行的阶段顺序。任何不合口径的地方都抛
     * {@link IllegalArgumentException} —— 消息里点名"引擎支持什么", 而不是只说"不合法";
     * 比 {@link #resolve} 多拒一条: 配了引擎不读的 {@code config} 参数。
     *
     * @param triggerEvent 配置的触发事件
     * @param stagesJson {@code [{type,config,order}]} 的 JSON 串
     * @return 按 order 排好、去重后的阶段类型列表
     */
    public static List<String> validateAndResolve(String triggerEvent, String stagesJson) {
        Resolution parsed = resolve(triggerEvent, stagesJson);
        List<String> unread = parsed.unreadConfig();
        if (!unread.isEmpty()) {
            throw new IllegalArgumentException(join(unread)
                    + " —— 现在收下它们, 就等于让用户填一份运行期一个字都不执行的参数,"
                    + " 而那份参数长得像能改变行为。真要给某一档加参数, 先登记进"
                    + " PipelineStages.CONFIG_KEYS_BY_TYPE 并让处理器真读它。");
        }
        return parsed.types();
    }

    /**
     * 运行期用的解析: 结构、词表、必填阶段、顺序全部照 {@link #validateAndResolve} 一样拒,
     * 但**不**因为"配了引擎不读的参数"而拒。
     * <p>
     * 这个不对称是有意的: 阶段类型配错 (没有执行器 / 摘掉闸门) 意味着业务写入绕过数据完整性检查,
     * 运行期必须硬拒; 而"多填了一个没人读的参数"不影响任何一行数据的对错, 为了它把某个实体的
     * **全部写入**按住, 等于让一个装饰性字段绑架业务 —— #36 演示过新加的闸把自己这边按死的后果。
     * 所以在写入口上闸之前存下的老配置行, 运行期照常工作, 只留一条 warn 日志; 谁要改那份配置,
     * 就会被 {@link #validateAndResolve} 拒。
     */
    public static Resolution resolve(String triggerEvent, String stagesJson) {
        String trigger = triggerEvent == null ? null : triggerEvent.trim();
        if (trigger == null || trigger.isEmpty()) {
            throw new IllegalArgumentException("触发事件不能为空, 引擎当前只支持 " + join(SUPPORTED_TRIGGERS));
        }
        if (!isSupportedTrigger(trigger)) {
            throw new IllegalArgumentException("触发事件 [" + trigger + "] 在引擎里没有挂接点: "
                    + "写后 (AFTER_*) 事件没有任何回调落点, 配置了也不会执行。当前只支持 "
                    + join(SUPPORTED_TRIGGERS));
        }

        Parsed parsed = parseTypes(stagesJson);
        List<String> types = parsed.types;

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

        return new Resolution(types, parsed.unreadConfig);
    }

    /**
     * 解析 stages JSON: 必须是对象数组, 每项带引擎支持的 type; order 缺省时按数组下标。
     * 排序按 (order, 数组位置) —— 位置是稳定的次级键, 所以两份 order 相同的配置结果确定。
     * <p>
     * 顺带登记"配了但引擎不读的参数": 这里只**记账**, 拒不拒由调用方决定
     * (写入口拒, 运行期只 warn —— 理由见 {@link #resolve})。
     */
    private static Parsed parseTypes(String stagesJson) {
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
        List<String> unread = new ArrayList<String>();
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

            JsonNode configNode = node.get("config");
            if (configNode != null && !configNode.isNull()) {
                if (!configNode.isObject()) {
                    // 形状都不对, 更不可能被读; 和"未知键"记在同一处, 免得拒了 A 却漏了 B
                    unread.add("阶段 [" + type + "] 的 config 不是对象 (实际: "
                            + shorten(configNode.asText()) + ")" + acceptedConfigHint(type));
                } else if (configNode.size() > 0) {
                    List<String> accepted = CONFIG_KEYS_BY_TYPE.get(type);
                    List<String> unknown = new ArrayList<String>();
                    for (Iterator<String> kit = configNode.fieldNames(); kit.hasNext(); ) {
                        String key = kit.next();
                        if (accepted == null || !accepted.contains(key)) {
                            unknown.add(key);
                        }
                    }
                    if (!unknown.isEmpty()) {
                        unread.add("阶段 [" + type + "] 里的参数 " + join(unknown)
                                + " 没有任何处理器读取" + acceptedConfigHint(type));
                    }
                }
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
        return new Parsed(types, unread);
    }

    /** 括号里那句"这一档到底读哪些参数", 由词表生成, 不手抄. */
    private static String acceptedConfigHint(String type) {
        List<String> keys = CONFIG_KEYS_BY_TYPE.get(type);
        return keys == null || keys.isEmpty()
                ? " —— 该阶段今天不读取任何参数 (引擎里参数只有一份口径: PipelineStages.CONFIG_KEYS_BY_TYPE)"
                : " —— 该阶段只读 " + join(keys);
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

    /** {@link #parseTypes} 的中间结果: 执行顺序 + "配了但没人读"的参数条目. */
    private static final class Parsed {
        private final List<String> types;
        private final List<String> unreadConfig;

        private Parsed(List<String> types, List<String> unreadConfig) {
            this.types = types;
            this.unreadConfig = unreadConfig;
        }
    }

    /**
     * 一份配置的解析结果: 真正会执行的阶段顺序, 以及"引擎不读的参数"清单。
     * <p>
     * 分开两样东西是这条规则的落点: 写入口对后者**拒**, 运行期只对它 **warn**
     * (见 {@link #validateAndResolve} 与 {@link #resolve} 的分工)。
     */
    public static final class Resolution {
        private final List<String> types;
        private final List<String> unreadConfig;

        private Resolution(List<String> types, List<String> unreadConfig) {
            this.types = Collections.unmodifiableList(new ArrayList<String>(types));
            this.unreadConfig = Collections.unmodifiableList(new ArrayList<String>(unreadConfig));
        }

        /** 按 order 排好的阶段类型 —— 也就是实际会执行的那条链. */
        public List<String> types() {
            return types;
        }

        /** 每一条都自带档名与键名, 直接可以进日志/错误消息. */
        public List<String> unreadConfig() {
            return unreadConfig;
        }
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
