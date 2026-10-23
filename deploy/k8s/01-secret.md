# z-lc · Secret 键位说明（`z-lc-db`）

> ⚠ **本文件不含任何真值**：口令与 JDBC URL 的真值只在 `lead/004_重要秘钥/`（不入仓）
> 与集群里已存在的 Secret 里，**明文零落盘**。

## 1. Secret 与键位

| Secret | 命名空间 | 键 | 内容 | 消费方 |
|---|---|---|---|---|
| `z-lc-db` | `z-lc` | `password` | 共享 RDS `oc` 库的口令 | `SPRING_DATASOURCE_PASSWORD` |
| `z-lc-db` | `z-lc` | `db-url` | 共享 RDS 的**整条 JDBC URL**（`jdbc:mysql://<主机名>:3306/oc?...`） | `SPRING_DATASOURCE_URL` |

**两个键都必须齐全。** `deploy/k8s/02-deployment.yaml` 里 `SPRING_DATASOURCE_URL` 已改为
`secretKeyRef: z-lc-db / db-url`（与 `z-wf` / `z-opc` 主启动器的 `db-url` 口径同构：
JDBC URL 整条走 secret，**仓内不留云端地址字面量**）。

⚠ **为什么 `z-lc` 用 `db-url` 而 `z-agent` 用 `db-host`**：两者的 env 形态不同 ——
`z-agent` 的 5 个 `Z_BASE_DB_CONFIG_*` 是**拆开**的（host/port/database/username/password 各自一个 env），
所以它只把**主机名**那一个值走 secret；`z-lc` 是**整条 JDBC URL** 一个 env，
按 `z-wf`（`deploy/kubernetes/z-wf.yaml`）的既有口径 ⇒ 整条走 secret，**不依赖 env 展开语义**。

⚠ **口令型与地址型是两条独立的边界**：本仓只解决**地址字面量**，口令本来就走 secret，
本次一个字都没动。

## 2. 灌 Secret 的命令（运维执行；本仓只出清单，不代跑）

⚠ **本命令取代 `deploy/k8s/README.md` §3 里那条「只写 `password` 一个键」的旧命令**
（README 不在本卡的允许改动路径内，故未同步改；**以本文件为准**）。

```bash
# ⚠ 值由运维从口令源现取（lead/004_重要秘钥/ + 集群现况），不写进本仓、不写进 commit
kubectl -n z-lc create secret generic z-lc-db \
  --from-literal=password='<从 lead/004_重要秘钥/ 取真值>' \
  --from-literal=db-url='jdbc:mysql://<RDS 主机名>:3306/oc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
```

### 2.1 集群里该 Secret 已存在时（当前线上就是这种状态）

`kubectl create secret generic` 对**已存在**的同名 Secret 会报 `AlreadyExists`
⇒ 补 `db-url` 这个新键时用**增量 patch**（只加新键，**不碰**已有的 `password` 真值）：

```bash
kubectl -n z-lc patch secret z-lc-db --type=merge \
  -p '{"stringData":{"db-url":"jdbc:mysql://<RDS 主机名>:3306/oc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"}}'
```

⚠ `db-url` 的**主机名段必须与线上现状一字不差**，其余参数（`useUnicode` /
`characterEncoding` / `serverTimezone` / `useSSL` / `allowPublicKeyRetrieval`）**逐字照抄**
上面的模板 ⇒ 只换主机名来源，不改连接语义。

## 3. ⚠ apply 的先后顺序（顺序错了 ⇒ Pod 起不来）

1. **先**灌好 Secret（`password` + `db-url` 两个键齐全）；
2. **再** apply Deployment：`kubectl apply -f deploy/k8s/`。

反过来（先 apply Deployment、Secret 里还没有 `db-url`）⇒ Pod 直接卡
`CreateContainerConfigError`（找不到 secret key）⇒ **不是**静默回落 localhost。

⚠ 同理，`SPRING_DATASOURCE_*` 这 3 个 env 一个都不能少。
本卡只把 `SPRING_DATASOURCE_URL` 的**取值来源**从明文改成 secret，**env 键名一字未改**
（`SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`），
所以 Spring relaxed binding（`SPRING_DATASOURCE_URL` → `spring.datasource.url`）照旧成立。

## 4. 边界

- 本文件与 `02-deployment.yaml` 的改动**只影响仓内清单**；**零 kubectl 写 / 零 apply /
  零生产改动** ⇒ 线上跑的 Pod 仍是旧配置，直到有人按 §2 灌完 Secret 并 apply。
- Secret 真值**永不入仓**：任何一次提交（含 commit message）里都不得出现主机名/口令明文。
