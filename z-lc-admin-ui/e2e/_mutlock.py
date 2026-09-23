"""注入自证脚本的互斥锁。

为什么需要：每一支 mutate_* 脚本都会**就地改写**源文件（注入 → 跑测试 → 按字节还原）。
两个脚本同时在飞时，A 的还原会把 B 正在判定的那一份源码换掉 —— B 于是拿到一个不属于
自己注入的结果。实测踩过：两支同时跑 designer 的两份脚本，基线报出 1 条红
（"字段表里不该预置引擎自建列 expected 3 to be 0"），那是另一支脚本的 F9 注入形状，
不是产品坏了。假红还能回头查，**假绿更糟**（B 期待的注入被 A 悄悄还原，红集合对不上
时的解释方向就全错了）。

锁按仓库根而不是按脚本：不同脚本改的是不同文件，但都往同一个 vitest 报告目录、
同一份 node_modules/.vite 缓存里写，且最容易犯的错正是"顺手再开一支"。
"""

import os
import sys
from pathlib import Path

LOCK = Path("/tmp/zlc_mutate_harness.lock")


def acquire(script: str) -> None:
    """独占这把锁；拿不到就直接退出（不是排队等待）。

    排队会让两轮的源码互相插队，比失败更难诊断。
    """
    try:
        fd = os.open(str(LOCK), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o644)
    except FileExistsError:
        holder = "unknown"
        try:
            holder = LOCK.read_text(encoding="utf-8").strip() or "unknown"
        except OSError:
            pass
        print(
            f"!! 已有注入脚本在飞: {holder}\n"
            f"   本次 {script} 拒绝启动 —— 两支同时改写源文件会让判定结果不属于任何一方。\n"
            f"   确认对方已经死掉再删 {LOCK}",
            file=sys.stderr,
        )
        raise SystemExit(2)
    os.write(fd, f"{script} pid={os.getpid()}\n".encode("utf-8"))
    os.close(fd)


def release() -> None:
    try:
        LOCK.unlink()
    except OSError:
        pass
