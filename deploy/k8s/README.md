# z-lc 部署清单（deploy/k8s）

本目录是 **z-lc** 在家庭 k3s 集群（节点 `zifang001` / `zifang002`）的部署清单。

这批 Deployment 当初是手工 `kubectl apply` 上去的，仓里查不到「它是怎么部署的」——
集群无法从代码复现，发版只能靠记忆和手记。本目录把**线上实况**（`kubectl get deploy,svc -o yaml` 的真实导出）固化回代码。

> ⚠ 本目录**只产出清单，不代跑**。`kubectl apply` 会改动生产集群，需要人工确认后执行。

## 0. 清单内容

| 文件 | 对象 | 线上对应 |
|---|---|---|
| `00-namespace.yaml` | Namespace `z-lc` | 已存在 |
| `01-service.yaml` | Service `z-lc-admin`（NodePort 31890 → 8080） | 已存在 |
| `02-deployment.yaml` | Deployment `z-lc-admin`（镜像 `zopc/z-lc:local`，replicas 1） | 已存在 |
| `README.md` | 本文件 | — |

线上实况要点（与基线导出逐条对齐）：

- Deployment 名 `z-lc-admin`，容器名 `z-lc`，容器端口 `8080`（端口名 `http`）
- Service `z-lc-admin`，`type: NodePort`，`port 8080 → targetPort http`，`nodePort 31890`
- 镜像 `zopc/z-lc:local`，`imagePullPolicy: Never`
- env：`JAVA_OPTS=-Xms256m -Xmx1024m` + `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`
- resources：requests `300m` / `1536Mi`，limits `2` CPU / `2Gi`
- 本域**无 PVC**

## 1. 怎么构建镜像

### 现状（如实说明）

**本仓没有 Dockerfile。** 全仓 grep 无 `Dockerfile` / `*.Dockerfile`（`z-lc/deploy/` 目录是本任务新建的）。
⇒ **「把源码变成 `zopc/z-lc:local`」这一步目前不在仓内**，
线上那份镜像是当初在节点上手工构建后喂进 containerd 的，仓里没有留下可复现的构建定义。

线上镜像口径：裸 tag `zopc/z-lc:local`，只存在于两个节点的 containerd，不在任何 registry。

### 建议命令（可执行；Dockerfile 需先补进本仓）

```bash
# 1) 打 jar —— 启动类是 z-lc-admin 里的 ZLcBootstrapApplication
#    （本仓 z-lc-admin 声明了 spring-boot-maven-plugin ⇒ 产出的是可执行 boot jar）
cd z-lc
mvn -pl z-lc-admin -am clean package -DskipTests
# 产物：z-lc-admin/target/z-lc-admin-1.0.0.jar

# 2) 本仓当前没有 Dockerfile，建议新增 deploy/Dockerfile（Java 8 口径与本仓
#    maven.compiler.source/target=8 一致；参考 z-task/Dockerfile 的写法）：
#
#    FROM eclipse-temurin:8-jre
#    WORKDIR /app
#    COPY z-lc-admin/target/z-lc-admin-1.0.0.jar app.jar
#    ENV SERVER_PORT=8080
#    EXPOSE 8080
#    ENTRYPOINT ["java", "-jar", "app.jar"]
#
#    ⚠ z-lc-admin 的 application.yml 里 server 段被移除（注释写明
#      「admin 模块不参与 main 构建」），端口走 Spring Boot 默认 8080，
#      与线上容器端口 8080 一致 ⇒ Dockerfile 里显式钉 SERVER_PORT=8080 更稳。

# 3) 构建（tag 必须与 02-deployment.yaml 里的 image: 逐字一致）
docker build -t zopc/z-lc:local -f deploy/Dockerfile .
```

## 2. 怎么喂 containerd

铁律三条：

1. **裸 tag，禁 Nexus 前缀**——本集群 `imagePullPolicy: Never`，k3s 不会去 registry 拉，
   它按 `image:` 的字面引用去节点本地 content store 找。喂进去的 tag 带前缀
   （如 `nexus.internal/zopc/z-lc:local`）而清单里写的是裸 tag（或反过来）⇒ **ErrImageNeverPull**。
2. **必须用 `cat` 管道喂 `k3s ctr images import -`**——`ctr images import` 从 **stdin** 读，
   直接把文件路径当参数会报 `unrecognized image format`。
3. **两个节点都要喂**——两个节点都是工作节点，Pod 可能落在任一节点；只喂一个会出现
   `ErrImageNeverPull` + Pod 反复重建。

```bash
# 1) 在开发机上导出镜像（build 可在开发机做，不必上节点）
docker save zopc/z-lc:local > /tmp/z-lc-local.tar
scp /tmp/z-lc-local.tar zifang@192.168.31.231:/tmp/     # zifang001
scp /tmp/z-lc-local.tar zifang@192.168.31.136:/tmp/     # zifang002

# 2) 喂 zifang001（192.168.31.231）——该节点 sudo 需要口令，会交互提示
ssh zifang@192.168.31.231 'cat /tmp/z-lc-local.tar | sudo k3s ctr images import -'

# 3) 喂 zifang002（192.168.31.136）——该节点 sudo -n 可免密
ssh zifang@192.168.31.136 'cat /tmp/z-lc-local.tar | sudo -n k3s ctr images import -'

# 4) 核验（两个节点都要看到，且 tag 必须与清单一致）
ssh zifang@192.168.31.231 'sudo k3s ctr images ls | grep z-lc'
ssh zifang@192.168.31.136 'sudo -n k3s ctr images ls | grep z-lc'
```

一步到位的写法（不落临时文件，效果等价）：

```bash
docker save zopc/z-lc:local | ssh zifang@192.168.31.231 'cat - | sudo k3s ctr images import -'
docker save zopc/z-lc:local | ssh zifang@192.168.31.136 'cat - | sudo -n k3s ctr images import -'
```

## 3. 怎么 apply

```bash
# ⚠ 会改动生产集群，需要人工确认后再执行；本仓只产出清单，不代跑。
kubectl apply -f deploy/k8s/
```

执行顺序与前置条件（apply 前置检查）：

1. **secret 先在**：`z-lc-db`（键 `password`）必须已存在于 ns `z-lc`，
   否则 Pod 会卡 `CreateContainerConfigError`（找不到 secret key）。
   集群里这个 secret 已经存在；重建集群时按下面补灌（口令真值在
   `lead/004_重要秘钥/`，不入仓）：

   ```bash
   kubectl -n z-lc create secret generic z-lc-db \
     --from-literal=password='<从 lead/004_重要秘钥/ 取真值>'
   ```

2. **namespace 先在**：`00-namespace.yaml` 已包含在目录里，`kubectl apply -f deploy/k8s/`
   会按文件名顺序（00 → 01 → 02）依次 apply，namespace 自动先建。
3. **镜像先在**：见 §2。两个节点都喂完再 apply，否则 Pod 起不来。

apply 后自检：

```bash
kubectl -n z-lc get deploy,svc,pods
kubectl -n z-lc describe deploy z-lc-admin | tail -30
```

## 4. 怎么回滚

```bash
# 看历史版本
kubectl -n z-lc rollout history deploy/z-lc-admin

# 回滚到上一个版本
kubectl -n z-lc rollout undo deploy/z-lc-admin

# 回滚到指定版本
kubectl -n z-lc rollout undo deploy/z-lc-admin --to-revision=<N>

# 看回滚是否完成
kubectl -n z-lc rollout status deploy/z-lc-admin
```

⚠ **PVC 数据不会自动回滚**：本域没有 PVC（纯无状态 Deployment），
但 z-lc 用的存储在**共享 RDS** 上，回滚 Pod **不会**回滚数据库 schema / 数据。
镜像里带 DDL 变更时，回滚镜像后旧 schema 仍在，需要人工确认兼容性。

⚠ **回滚的前提是节点上有旧镜像**：回滚只改 Deployment 里的 `image:` 字段指向的旧 tag，
该 tag 必须仍在这两个节点的 containerd 里（`k3s ctr images ls` 能看到）。旧镜像被清掉就回滚不动。

## 5. 本清单与线上的差异（抄写时发现）

| 项 | 线上 | 本清单 | 原因 |
|---|---|---|---|
| readinessProbe | `httpGet /doc.html`（Swagger 文档页） | `httpGet /api/lc/health`（readiness + liveness） | `/doc.html` 只证明 Web 容器活着，不证明业务健康；`/api/lc/health` 本域**实测 200**，是真实可用的业务健康端点 |
| labels | 资源上只有 `app.kubernetes.io/name` | 额外加 `app.kubernetes.io/part-of` | 仅加标签，不影响 selector 匹配；`apply` 是幂等合并 |
| SA 令牌自动挂载 | `automountServiceAccountToken: false` | 不含该字段 | 该收敛由**独立清单**执行（卡 `TASK-20261021-021` 交付、用户亲自执行），刻意与各域部署清单分开，避免抢同一批字段。⚠ `apply` 本清单**不会回退**这项加固（字段既不在本清单、也不在 last-applied ⇒ 三向合并不构成删除动作，已用 `kubectl apply --dry-run=server` 逐域实测：该字段仍为 `false`） |

## 6. 待办（不在本任务范围内）

- 本仓**缺 Dockerfile**，镜像构建不可复现（§1 已给出建议内容，需人工补进仓并提交）。
- 数据库地址/账号目前是明文（照线上现状抄）。若要按「仓里不留明文主机名」的更严口径
  收口，需与 z-wf 一样改成 secretKeyRef（引用 secret `aliyun-db` 的 `db-url` / `db-username`），
  前提是那个 secret 里确实有这两个键——这是一次独立的口径变更，不在本任务范围内。
