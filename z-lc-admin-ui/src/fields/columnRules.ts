/**
 * 字段编码 = 物理列名，写进 DDL 的反引号里。这里的规则与后端
 * `SchemaAdminBizService.FIELD_CODE_RE` / `SYSTEM_COLUMN_CODES` 一一对应：
 * 后端是真正的写入口（`entity/create`、`entity/update`、逆向映射都过它），
 * 前端这一份只是让用户在点保存之前就看到毛病，两边必须同一个口径，
 * 否则界面会拦下后端收得下的东西（或者反过来）。
 */

export const FIELD_CODE_RE = /^[A-Za-z][A-Za-z0-9_]*$/;

/**
 * 引擎给每张受管表自建的列 —— 同 previewDdl 里那几行，也同后端 `buildCreateTableDdl`。
 * 这五个全是**合法标识符**，所以 `FIELD_CODE_RE` 放不住它们；而后果不是"名字不优雅"：
 * 后端只在写元数据，撞名要等 provision 建表才炸，且 provision-all 会在第一个坏实体上抛，
 * 把同一个应用里其他实体的表一起挡住。所以必须在保存前就拦住。
 */
export const SYSTEM_COLUMN_CODES = ['id', 'tenant_code', 'deleted', 'create_time', 'update_time'];

/** 一个字段编码的毛病，没有毛病返回 null。保存前逐字段过一遍。 */
export function fieldCodeProblem(code?: string): string | null {
  const value = code ?? '';
  if (!value.trim()) return '字段编码不能为空';
  if (!FIELD_CODE_RE.test(value)) {
    return `字段编码「${value}」不合法：需以字母开头，仅含字母/数字/下划线`;
  }
  if (SYSTEM_COLUMN_CODES.includes(value.toLowerCase())) {
    return `字段编码「${value}」撞了引擎自建列（${SYSTEM_COLUMN_CODES.join(' / ')}），建表会直接 Duplicate column name`;
  }
  return null;
}
