import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import { PERMISSION_KEYS } from './permission';

/**
 * 权限项这份词表此前有两份平行副本，而且两边都不是闸：后端只在 `PermissionEntity` 的注释里
 * 写了 READ / WRITE / DELETE / ADMIN，前端矩阵的列头是另外五个词，而 `grant` 对
 * `permission` 一个字都不校验。于是矩阵能给出一个 `/check` 永远答"拒绝"的词。
 *
 * 这一层不再由人抄一遍列头：直接读 `PermissionKeys.java` 里那个 `ALL` 清单，
 * 顺序也要一起对 —— 那一列就是矩阵的列顺序。
 */

const here = dirname(fileURLToPath(import.meta.url));
const KEYS_JAVA = resolve(
  here,
  '../../../z-lc-core/src/main/java/com/zifang/z/lc/core/permission/PermissionKeys.java',
);
const PAGE_TSX = resolve(here, '../views/admin/PermissionsPage.tsx');

/** `public static final String VIEW = "VIEW";` → 常量名到字面量。 */
function constants(text: string): Map<string, string> {
  const map = new Map<string, string>();
  for (const m of text.matchAll(/public static final String (\w+) = "([^"]+)"/g)) {
    if (m[1] && m[2]) map.set(m[1], m[2]);
  }
  expect(map.size, '没解析出任何 String 常量，说明正则或被读的文件不对').toBeGreaterThan(0);
  return map;
}

/** `Arrays.asList(VIEW, CREATE, ...)` 里点名的常量，按书写顺序。 */
function declaredOrder(text: string, consts: Map<string, string>): string[] {
  const start = text.search(/Arrays\.asList\(/);
  expect(start, '找不到 PermissionKeys.ALL 那份清单').toBeGreaterThanOrEqual(0);
  // 结束括号要留在切片里，否则末项后面没有 `,`/`)` 可 lookahead，会凭空消失
  const slice = text.slice(start, text.indexOf(')', start) + 1);
  const names = [...slice.matchAll(/([A-Z_]+)\s*(?=[,)])/g)].map((m) => m[1] ?? '');
  // 空参照集不能算通过：解析出 0 项会让下面的"两边一致"变成 vacuous truth
  expect(names.length, 'PermissionKeys.ALL 里一项都没解析出来，词表搬去别处了').toBeGreaterThan(0);
  return names.map((name) => {
    const value = consts.get(name);
    expect(value, `${name} 在 PermissionKeys.java 里没有对应的 String 常量`).toBeTruthy();
    return value as string;
  });
}

function javaKeys(): string[] {
  let text: string;
  try {
    text = readFileSync(KEYS_JAVA, 'utf8');
  } catch (err) {
    throw new Error(`读不到后端词表 PermissionKeys.java (${KEYS_JAVA}) —— 参照集为空时断言会全部空转: ${err}`);
  }
  expect(text.length, 'PermissionKeys.java 是空文件').toBeGreaterThan(0);
  const consts = constants(text);
  // 常量声明本身也要在词表里（"两个清单"式漂移的另一半：声明了却没进 ALL）
  const declared = declaredOrder(text, consts);
  for (const key of declared) {
    expect(consts.get(key), `ALL 里的 ${key} 与它自己的常量声明不一致`).toBe(key);
  }
  expect(new Set(declared).size, `后端词表有重复项: ${declared.join(' / ')}`).toBe(declared.length);
  return declared;
}

describe('权限项词表与后端同源', () => {
  it('矩阵列头就是 PermissionKeys.ALL，一项不多一项不少，顺序也一致', () => {
    expect([...PERMISSION_KEYS]).toEqual(javaKeys());
  });

  it('页面里不再有第二份抄出来的权限词', () => {
    const page = readFileSync(PAGE_TSX, 'utf8');
    // 先钉"列头确实是从词表 map 出来的"，再钉"没有手写数组"——只留后半句会把整列删掉也全绿
    expect(page.includes('PERMISSION_KEYS.map'), '矩阵列头不再由词表生成').toBe(true);
    for (const ghost of ['READ', 'WRITE', 'ADMIN']) {
      expect(page.includes(`'${ghost}'`), `${ghost} 是后端没登记的说法，矩阵不许再列它`).toBe(false);
    }
    expect(page.includes("['VIEW'"), '页面自己抄了一份权限清单').toBe(false);
  });
});
