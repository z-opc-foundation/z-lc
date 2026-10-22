# z-lc-admin 生产镜像（卡 TASK-20261023-008）
#
# 【为什么基底是「现役镜像」而不是 jre 基础镜像】
#   本仓原先**没有任何 Dockerfile** ⇒ 线上 zopc/z-lc:local 是历史上手工构建的，
#   运行时（Temurin jdk8u504 / JAVA_HOME=/opt/java/openjdk / locale / entrypoint
#   `sh -c "exec java $JAVA_OPTS -jar /app/app.jar"`）已在线上验证过。
#   本轮只换 jar ⇒ 基底直接取现役镜像 ⇒ 运行时**零漂移**，把变量收敛到「jar 内容」一个。
#
# 【构建】
#   cd z-lc
#   mvn -B -pl z-lc-admin -am clean package -Dmaven.test.skip=true
#   docker buildx build --platform linux/amd64 --provenance=false --sbom=false --load \
#     -f deploy/Dockerfile -t zopc/z-lc:1.0.0-zboot112 .
#
# 【tag 铁律】
#   产出的 tag 必须与 deploy/k8s/02-deployment.yaml 的 `image:` 逐字一致 ——
#   本集群 imagePullPolicy=Never，k3s 按字面引用去节点本地 content store 找。
#   ⚠ 不得再用 `:local`：`:local` 不可回滚、不可追溯。
FROM zopc/z-lc:local

# Spring Boot 胖 jar（只匹配胖包，`.jar.original` 不匹配）
COPY z-lc-admin/target/z-lc-admin-*.jar /app/app.jar
