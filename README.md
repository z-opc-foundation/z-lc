# z-lc

> 低代码平台 (Low-Code)

DB 驱动的通用 CRUD 引擎 (Phase 1), 后续扩展为可视化低代码平台

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **项目** | z-lc |
| **分类** | 业务应用 · 低代码 |
| **父项目** | z-opc (com.zifang:z-opc:1.0.0-SNAPSHOT) |
| **默认端口** | `8088` |
| **文档维护** | z-opc-foundation |
| **最近更新** | 2026-09-06 |

---

## 🎯 核心功能

DB 驱动的通用 CRUD 引擎 (Phase 1), 后续扩展为可视化低代码平台

详细功能特性详见各子模块 README 或源码注释。

---

## 🏗️ 项目结构

```
z-lc/
├── pom.xml                      # 根 POM (引用 z-opc 父项目)
├── README.md                    # 本文档
├── MODULE_NOTE.md               # 来源说明 (从 z-opc 拆分)
```

子模块列表:

| 模块 | 职责 |
|------|------|
| `z-lc-common/` | 公共抽象 |
| `z-lc-sdk/` | 扩展点 SDK |
| `z-lc-core/` | 通用 CRUD 引擎 |
| `z-lc-design/` | 可视化设计器 |
| `z-lc-web/` | Web API 层 |

---

## 🔧 技术栈

- Java 8+
- Spring Boot
- DB 元数据驱动

---

## 🚀 快速开始

### 前置条件

- JDK 8+ (推荐 JDK 17)
- Maven 3.6+
- 端口 `8088` 未被占用

### 编译

```bash
# 在 z-opc 父项目下编译 (推荐)
cd /Users/zifang/workplace/idea_workplace/z-opc
mvn clean install -pl :z-opc -am -DskipTests

# 单独编译本模块 (需 ../pom.xml 父项目可用)
cd /Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc
mvn clean compile
```

### 运行

```bash
# 启动主服务 (根据项目类型选择)
mvn -pl <启动模块> spring-boot:run
# 或
java -jar <启动模块>/target/*.jar
```

---

## 📦 模块说明

z-lc 由以下子模块组成:

| `z-lc-common/` | 公共抽象 |
| `z-lc-sdk/` | 扩展点 SDK |
| `z-lc-core/` | 通用 CRUD 引擎 |
| `z-lc-design/` | 可视化设计器 |
| `z-lc-web/` | Web API 层 |

各模块职责详见各子目录下的 `pom.xml` 和源码。

---

## 🧪 测试

```bash
mvn test
```

测试覆盖:
- 单元测试: 各核心服务类
- 集成测试: 端到端调用链路
- 性能测试: 详见 `/src/test` 下的 `*PerformanceTest.java`

---

## 🔌 API 接口

API 接口定义在各子模块的 `controller` 包下。

启动后访问 `http://localhost:8088/swagger-ui.html` 或 `/doc.html` (knife4j) 查看完整 API 文档。

---

## 🐳 部署

### Docker

```bash
# 构建镜像
docker build -t z-lc:latest .

# 运行容器
docker run -d -p 8088:8088 --name z-lc z-lc:latest
```

### 配置

主要配置文件:
- `application.yml` - Spring Boot 配置
- `logback.xml` - 日志配置
- 环境变量: `JAVA_OPTS`, `SPRING_PROFILES_ACTIVE`

---

## 📚 相关文档

- [MODULE_NOTE.md](./MODULE_NOTE.md) - 从 z-opc 拆分说明
- [z-opc 父项目](https://github.com/yuku123/z-opc) - 完整源码

---

## 📝 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2026-09-06 | 从 z-opc monorepo 拆分独立仓, 文档补齐 |

---

## 📄 License

Internal use only. 版权属于 z-biz。

_Maintained by z-opc-foundation organization._


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档 (项目总览 / 模块结构 / 接口清单 / DB schema / 前端 / 能力 / roadmap):
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md)

各文档详细说明见各子目录。
