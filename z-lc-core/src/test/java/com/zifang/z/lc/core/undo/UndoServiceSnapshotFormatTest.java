package com.zifang.z.lc.core.undo;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.core.fieldtype.FieldTypeRegistry;
import org.junit.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 快照格式 (缺陷 #51 的第二支): 「写进日志的值」必须能被写路径原样吃回去。
 * <p>
 * 为什么要单独钉这一层: undo 的失败点不在撤销的逻辑，而在前像的**值形态**随 JDBC 驱动变 ——
 * H2 给 java.sql.Timestamp，MySQL 8 的 Connector/J 给 java.time.LocalDateTime。修复前只认 Date
 * 和 String 两支，于是同一份代码在 250 上存出 {@code "last_visit":"1937-11-05T12:00:00"}，
 * 撤销时 {@code Field requires date:} 当场失败 (dev 的 H2 永远复现不出来)。
 * <p>
 * 所以这里判的不是"字符串长什么样像我想要的"，而是**真拿写入路径的 coerce 吃一遍**：
 * 快照能被接受、且接受的还是同一个时刻，才算修好。
 */
public class UndoServiceSnapshotFormatTest {

    private EntityDefDTO entityWith(String code, String type) {
        EntityDefDTO def = new EntityDefDTO();
        def.setEntityCode("visit");
        def.setTableName("lc_crm_visit");
        List<FieldDefDTO> fields = new ArrayList<FieldDefDTO>();
        FieldDefDTO f = new FieldDefDTO();
        f.setFieldCode(code);
        f.setFieldName(code);
        f.setFieldType(type);
        fields.add(f);
        def.setFields(fields);
        return def;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> persistable(EntityDefDTO entity, Map<String, Object> row) throws Exception {
        Method m = UndoService.class.getDeclaredMethod("persistable", EntityDefDTO.class, Map.class);
        m.setAccessible(true);
        return (Map<String, Object>) m.invoke(new UndoService(), entity, row);
    }

    private static Map<String, Object> row(String code, Object value) {
        Map<String, Object> r = new LinkedHashMap<String, Object>();
        r.put(code, value);
        return r;
    }

    /** 写路径能不能吃回去 —— 这才是 undo 真正的成败点。 */
    private static Date acceptedByWritePath(Object snapshotValue, String fieldType) {
        Object coerced = FieldTypeRegistry.coerceValue(snapshotValue, fieldType);
        assertTrue(fieldType + " 字段吃不下快照里的值 (撤销会当场失败): " + snapshotValue,
                coerced instanceof Date);
        return (Date) coerced;
    }

    @Test
    public void localDateTimeSnapshotSurvivesTheWritePath() throws Exception {
        Map<String, Object> out = persistable(entityWith("last_visit", "DATETIME"),
                row("last_visit", LocalDateTime.of(1937, 11, 5, 12, 0, 0)));
        Object snap = out.get("last_visit");

        // 带 T 的 ISO 串就是 250 上那批撤不回来的日志的形状。
        assertEquals("MySQL 8 驱动给 LocalDateTime, 写快照时必须归一: " + snap,
                "1937-11-05 12:00:00", snap);
        assertEquals("吃回去还得是同一个时刻: " + snap, "1937-11-05 12:00:00",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(acceptedByWritePath(snap, "DATETIME")));
    }

    @Test
    public void localDateSnapshotSurvivesTheWritePath() throws Exception {
        Map<String, Object> out = persistable(entityWith("birthday", "DATE"),
                row("birthday", LocalDate.of(2026, 9, 26)));
        Object snap = out.get("birthday");

        assertEquals(snap, "2026-09-26");
        assertEquals("同一个日子: " + snap, "2026-09-26",
                new SimpleDateFormat("yyyy-MM-dd").format(acceptedByWritePath(snap, "DATE")));
    }

    /**
     * LocalTime.toString() 在整分时会把秒丢掉 ("12:00")，而物理列是 TIME 的话写回去就是另一个值。
     * 反向导入的表 (DbTableMapperService 把 Types.TIME 映射成 fieldType TIME) 走的就是这一支。
     */
    @Test
    public void localTimeSnapshotPadsSeconds() throws Exception {
        Map<String, Object> out = persistable(entityWith("opens_at", "TIME"),
                row("opens_at", LocalTime.of(12, 0)));
        assertEquals("整分时间不能被 toString 削成 12:00: ", "12:00:00", out.get("opens_at"));
    }

    /**
     * 修复之前写下的历史日志里存的就是带 T 的串。"改了新写入"不等于"历史撤不回来"，
     * 所以读回这一头也要归一次 —— 否则删除记录永远撤不掉。
     */
    @Test
    public void isoStringFromAnOlderSnapshotIsReplayable() throws Exception {
        Map<String, Object> out = persistable(entityWith("last_visit", "DATETIME"),
                row("last_visit", "1937-11-05T12:00:00"));
        assertEquals("1937-11-05 12:00:00", out.get("last_visit"));
        acceptedByWritePath(out.get("last_visit"), "DATETIME");
    }

    /**
     * 带偏移的那一支必须按**瞬时**换算，不能把 +00:00 直接截掉 —— 截掉等于把时刻整体平移
     * (实测 03:04:05 变成前一天 19:04:05)。断言两边是同一个瞬间，与本机时区无关。
     */
    @Test
    public void offsetAwareIsoStringKeepsTheSameInstant() throws Exception {
        Map<String, Object> out = persistable(entityWith("last_visit", "DATETIME"),
                row("last_visit", "1937-11-05T12:00:00+00:00"));
        String snapshot = String.valueOf(out.get("last_visit"));
        Date coerced = acceptedByWritePath(snapshot, "DATETIME");
        long want = OffsetDateTime.parse("1937-11-05T12:00:00+00:00").toInstant().toEpochMilli();
        assertEquals("同一个瞬间必须还是同一个瞬间 (归一时别拿本地墙钟当 UTC): " + snapshot,
                want, coerced.getTime());
    }

    /** 反向的过头: 只有日期类字段才归一，别的字段类型不许被顺手改成字符串。 */
    @Test
    public void nonDateFieldsPassThroughUntouched() throws Exception {
        LocalDateTime value = LocalDateTime.of(1937, 11, 5, 12, 0, 0);
        Map<String, Object> out = persistable(entityWith("note", "STRING"), row("note", value));
        assertNotNull(out.get("note"));
        assertTrue("STRING 字段不该被日期归一顺手改掉类型: " + out.get("note"),
                out.get("note") instanceof LocalDateTime);

        Map<String, Object> ok = persistable(entityWith("birthday", "DATE"), row("birthday", "2026-09-26"));
        assertEquals("本来就合式的值不许被改动: " + ok.get("birthday"), "2026-09-26", ok.get("birthday"));
    }

    /** 派生列 (`*_label`) 必须在快照里丢掉，否则重放时会被当成字段写进物理表。 */
    @Test
    public void derivedColumnsAreNotSnapshotted() throws Exception {
        Map<String, Object> row = row("last_visit", LocalDateTime.of(1937, 11, 5, 12, 0, 0));
        row.put("status_label", "已确认");
        Map<String, Object> out = persistable(entityWith("last_visit", "DATETIME"), row);
        assertTrue("定义里没有的栏不该进快照 (重放时会当成物理列写): " + out, !out.containsKey("status_label"));
        assertEquals(1, out.size());
    }
}
