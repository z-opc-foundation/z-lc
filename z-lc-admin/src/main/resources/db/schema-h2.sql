-- =====================================================================
-- z-lc 开发/端到端验证 schema (H2 2.x, MODE=MySQL)
--
-- 与 z-opc/_doc/004_sql/{z-opc-schema.sql,z-lc.sql} 的关系:
-- 那两份 MySQL DDL 排版被打散 (列名/类型跨多行)、且含 AFTER 等 H2 不认的语法,
-- 无法直接机读维护; 本文件按 MyBatis-Plus Entity 的真实字段列表重写, 是 dev 环境的权威 schema.
-- 生产 MySQL 仍以 _doc/004_sql 为准.
--
-- 两处修正了 _doc/004_sql 的真实缺陷 (均由 E2E 实测暴露):
--   1) z_lc_app.icon            — 基础建表语句里没有, 只有一条 ALTER 补列; 这里直接建出来.
--   2) z_lc_relation.relation_type VARCHAR(8) -> VARCHAR(32)
--      ONE_TO_MANY(11) / MANY_TO_MANY(12) 根本存不下: H2 直接报错,
--      MySQL 非严格模式会被静默截断成 "ONE_TO_MA" —— 关系类型从此永久错乱.
--
-- 设计原则: MyBatis-Plus 按 Entity 字段生成显式列表, 库里多列无害、缺列必炸,
-- 因此对所有表一律给出 deleted / create_time / update_time, 且枚举列一律给足宽度.
-- 全量 IF NOT EXISTS, 幂等, 可重复执行.
-- =====================================================================

CREATE TABLE IF NOT EXISTS z_lc_app (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_code`     VARCHAR(64)  NOT NULL,
  `app_code`        VARCHAR(64)  NOT NULL,
  `app_name`        VARCHAR(128),
  `description`     TEXT,
  `icon`            VARCHAR(64),
  `status`          VARCHAR(32)  DEFAULT 'DRAFT',
  `current_version` BIGINT       DEFAULT 0,
  `deleted`         TINYINT      DEFAULT 0,
  `create_time`     DATETIME,
  `update_time`     DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_app_tenant_code UNIQUE (`tenant_code`, `app_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_entity (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`     VARCHAR(64) NOT NULL,
  `app_code`        VARCHAR(64) NOT NULL,
  `entity_code`     VARCHAR(64) NOT NULL,
  `entity_name`     VARCHAR(128),
  `table_name`      VARCHAR(128),
  `description`     TEXT,
  `current_version` BIGINT      DEFAULT 0,
  `deleted`         TINYINT     DEFAULT 0,
  `create_time`     DATETIME,
  `update_time`     DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_entity_tenant_app_code UNIQUE (`tenant_code`, `app_code`, `entity_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_field (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`  VARCHAR(64) NOT NULL,
  `entity_id`    BIGINT      NOT NULL,
  `field_code`   VARCHAR(64) NOT NULL,
  `field_name`   VARCHAR(128),
  `field_type`   VARCHAR(32) DEFAULT 'STRING',
  `required`     TINYINT     DEFAULT 0,
  `default_value` VARCHAR(255),
  `dict_code`    VARCHAR(64),
  `ref_entity`   VARCHAR(128),
  `field_length` INT,
  `scale`        INT,
  `sort_order`   INT         DEFAULT 0,
  `description`  VARCHAR(512),
  `deleted`      TINYINT     DEFAULT 0,
  `create_time`  DATETIME,
  `update_time`  DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_field_entity_code UNIQUE (`entity_id`, `field_code`)
);

-- 事件溯源表: append-only, 无 deleted; schema 由 /api/lc/app/schema 回放得到
CREATE TABLE IF NOT EXISTS z_lc_event (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`     VARCHAR(64) NOT NULL,
  `event_id`        VARCHAR(64) NOT NULL,
  `app_code`        VARCHAR(64) NOT NULL,
  `entity_code`     VARCHAR(64),
  `event_type`      VARCHAR(32) NOT NULL,
  `event_data`      TEXT,
  `source`          VARCHAR(64),
  `parent_event_id` VARCHAR(64),
  `apply_seq`       BIGINT,
  `apply_time`      DATETIME,
  `create_time`     DATETIME,
  `update_time`     DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_event_event_id UNIQUE (`event_id`)
);

CREATE TABLE IF NOT EXISTS z_lc_dict (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code` VARCHAR(64) NOT NULL,
  `dict_code`   VARCHAR(64) NOT NULL,
  `dict_name`   VARCHAR(128) NOT NULL,
  `description` VARCHAR(256),
  `status`      VARCHAR(32) DEFAULT 'ENABLED',
  `deleted`     TINYINT     DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_dict_tenant_code UNIQUE (`tenant_code`, `dict_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_dict_item (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code` VARCHAR(64) NOT NULL,
  `dict_code`   VARCHAR(64) NOT NULL,
  `item_code`   VARCHAR(64) NOT NULL,
  `item_label`  VARCHAR(128),
  `item_value`  VARCHAR(256),
  `sort_order`  INT         DEFAULT 0,
  `description` VARCHAR(256),
  `deleted`     TINYINT     DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_relation (
  `id`                 BIGINT      NOT NULL AUTO_INCREMENT,
  `relation_code`      VARCHAR(64) NOT NULL,
  `relation_name`      VARCHAR(128),
  `source_entity_code` VARCHAR(64) NOT NULL,
  `target_entity_code` VARCHAR(64) NOT NULL,
  `relation_type`      VARCHAR(32) NOT NULL,
  `source_field_code`  VARCHAR(64),
  `through_table`      VARCHAR(128),
  `tenant_code`        VARCHAR(64) NOT NULL,
  `app_code`           VARCHAR(64) NOT NULL,
  `deleted`            TINYINT     DEFAULT 0,
  `create_time`        DATETIME,
  `update_time`        DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_relation_tenant_code UNIQUE (`tenant_code`, `app_code`, `relation_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_view_config (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `entity_code` VARCHAR(64) NOT NULL,
  `app_code`    VARCHAR(64) NOT NULL,
  `view_type`   VARCHAR(32) NOT NULL,
  `config`      TEXT,
  `tenant_code` VARCHAR(64) NOT NULL,
  `deleted`     TINYINT     DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_pipeline_config (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `entity_code`   VARCHAR(64) NOT NULL,
  `app_code`      VARCHAR(64) NOT NULL,
  `trigger_event` VARCHAR(32) NOT NULL,
  `stages`        TEXT,
  `enabled`       TINYINT     DEFAULT 0,
  `tenant_code`   VARCHAR(64) NOT NULL,
  `deleted`       TINYINT     DEFAULT 0,
  `create_time`   DATETIME,
  `update_time`   DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_workflow_binding (
  `id`                      BIGINT      NOT NULL AUTO_INCREMENT,
  `entity_code`             VARCHAR(64) NOT NULL,
  `app_code`                VARCHAR(64) NOT NULL,
  `trigger_event`           VARCHAR(32),
  `process_definition_key`  VARCHAR(128),
  `auto_submit`             TINYINT     DEFAULT 0,
  `tenant_code`             VARCHAR(64) NOT NULL,
  `deleted`                 TINYINT     DEFAULT 0,
  `create_time`             DATETIME,
  `update_time`             DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_deployment (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `app_code`          VARCHAR(64) NOT NULL,
  `materialization_id` BIGINT,
  `deploy_type`       VARCHAR(32) NOT NULL,
  `status`            VARCHAR(32) DEFAULT 'PENDING',
  `deploy_log`        TEXT,
  `version`           VARCHAR(64),
  `tenant_code`       VARCHAR(64) NOT NULL,
  `deleted`           TINYINT     DEFAULT 0,
  `create_time`       DATETIME,
  `update_time`       DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_permission (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `app_code`    VARCHAR(64) NOT NULL,
  `entity_code` VARCHAR(64),
  `role_code`   VARCHAR(64) NOT NULL,
  `permission`  VARCHAR(64) NOT NULL,
  `tenant_code` VARCHAR(64) NOT NULL,
  `deleted`     TINYINT     DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_materialization (
  `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`          VARCHAR(64) NOT NULL,
  `app_code`             VARCHAR(64) NOT NULL,
  `materialization_path` VARCHAR(512),
  `entity_codes`         TEXT,
  `export_version`       VARCHAR(64),
  `status`               VARCHAR(32) DEFAULT 'PENDING',
  `file_count`           INT         DEFAULT 0,
  `description`          VARCHAR(512),
  `error_message`        TEXT,
  `trigger_source`       VARCHAR(64),
  `event_id`             VARCHAR(64),
  `deleted`              TINYINT     DEFAULT 0,
  `create_time`          DATETIME,
  `update_time`          DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT uk_materialization_event_id UNIQUE (`event_id`)
);

-- 运行态数据变更日志 (undo/redo). 由 UndoService 写入, 前像/后像只存真实列,
-- 查询期 JOIN 出来的 *_label / *_name 派生列不进快照.
CREATE TABLE IF NOT EXISTS z_lc_data_change (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`  VARCHAR(64) NOT NULL,
  `app_code`     VARCHAR(64) NOT NULL,
  `entity_code`  VARCHAR(64) NOT NULL,
  `record_id`    BIGINT      NOT NULL,
  `operation`    VARCHAR(32) NOT NULL,
  `before_image` TEXT,
  `after_image`  TEXT,
  `undone_by`    BIGINT,
  `actor`        VARCHAR(128),
  `trace_id`     VARCHAR(64),
  `create_time`  DATETIME,
  `update_time`  DATETIME,
  `deleted`      TINYINT     DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 流程绑定的发起结局账 (缺陷 #61). 一条记录写成功后每次「去发起流程」都在这里留一行:
-- STARTED 带 z-wf 的实例 id, FAILED 带为什么 (引擎不可达/拒绝/超时/并发额度用尽)。
-- 没有这张表的话, "绑定已保存" 与 "单真的提了" 之间没有任何可回读的证据。
CREATE TABLE IF NOT EXISTS z_lc_workflow_fire (
  `id`                     BIGINT      NOT NULL AUTO_INCREMENT,
  `tenant_code`            VARCHAR(64) NOT NULL,
  `app_code`               VARCHAR(64) NOT NULL,
  `entity_code`            VARCHAR(64) NOT NULL,
  `record_id`              BIGINT      NOT NULL,
  `binding_id`             BIGINT,
  `trigger_event`          VARCHAR(32),
  `process_definition_key` VARCHAR(128),
  `status`                 VARCHAR(16) NOT NULL,
  `instance_id`            VARCHAR(128),
  `detail`                 VARCHAR(512),
  `create_time`            DATETIME,
  `update_time`            DATETIME,
  `deleted`                TINYINT     DEFAULT 0,
  PRIMARY KEY (`id`)
);
