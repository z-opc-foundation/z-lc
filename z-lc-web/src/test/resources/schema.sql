-- =====================================================================
-- z-lc-web 集成测试 schema (H2 2.x, MODE=MySQL)
-- 覆盖 MetaController.bundle 触及的元数据表 + 运行时 CRUD 演示表.
-- 列布局对齐 MyBatis-Plus Entity (库里多列无害, 缺列必炸), 全量 IF NOT EXISTS 幂等.
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

-- 运行时 CRUD 演示物理表 (由 app=demo / entity=order 的 CREATE 事件指向)
CREATE TABLE IF NOT EXISTS lc_demo_order (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_code` VARCHAR(64)  NOT NULL,
  `name`        VARCHAR(255),
  `amount`      DECIMAL(18,2),
  `status`      VARCHAR(255),
  `due_date`    DATE,
  `closed_at`   DATETIME,
  `deleted`     TINYINT      DEFAULT 0,
  `create_time` DATETIME,
  `update_time` DATETIME,
  PRIMARY KEY (`id`)
);


-- ---- 其余平台表 (与 z-lc-admin/src/main/resources/db/schema-h2.sql 保持一致) ----
-- 缺列会让 mapper 直接报错、多列无害, 所以一律带 deleted/create_time/update_time.

CREATE TABLE IF NOT EXISTS z_lc_entity (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `entity_code` VARCHAR(64) NOT NULL, `entity_name` VARCHAR(128), `table_name` VARCHAR(128),
  `description` TEXT, `current_version` BIGINT DEFAULT 0, `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME, `update_time` DATETIME, PRIMARY KEY (`id`),
  CONSTRAINT uk_t_entity UNIQUE (`tenant_code`, `app_code`, `entity_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_field (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_code` VARCHAR(64) NOT NULL, `entity_id` BIGINT NOT NULL,
  `field_code` VARCHAR(64) NOT NULL, `field_name` VARCHAR(128), `field_type` VARCHAR(32) DEFAULT 'STRING',
  `required` TINYINT DEFAULT 0, `default_value` VARCHAR(255), `dict_code` VARCHAR(64), `ref_entity` VARCHAR(128),
  `field_length` INT, `scale` INT, `sort_order` INT DEFAULT 0, `description` VARCHAR(512),
  `deleted` TINYINT DEFAULT 0, `create_time` DATETIME, `update_time` DATETIME, PRIMARY KEY (`id`),
  CONSTRAINT uk_t_field UNIQUE (`entity_id`, `field_code`)
);

CREATE TABLE IF NOT EXISTS z_lc_pipeline_config (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `entity_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `trigger_event` VARCHAR(32) NOT NULL, `stages` TEXT, `enabled` TINYINT DEFAULT 0,
  `tenant_code` VARCHAR(64) NOT NULL, `deleted` TINYINT DEFAULT 0, `create_time` DATETIME, `update_time` DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_workflow_binding (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `entity_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `trigger_event` VARCHAR(32), `process_definition_key` VARCHAR(128), `auto_submit` TINYINT DEFAULT 0,
  `tenant_code` VARCHAR(64) NOT NULL, `deleted` TINYINT DEFAULT 0, `create_time` DATETIME, `update_time` DATETIME,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_deployment (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `app_code` VARCHAR(64) NOT NULL, `materialization_id` BIGINT,
  `deploy_type` VARCHAR(32) NOT NULL, `status` VARCHAR(32) DEFAULT 'PENDING', `deploy_log` TEXT,
  `version` VARCHAR(64), `tenant_code` VARCHAR(64) NOT NULL, `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME, `update_time` DATETIME, PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_permission (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `app_code` VARCHAR(64) NOT NULL, `entity_code` VARCHAR(64),
  `role_code` VARCHAR(64) NOT NULL, `permission` VARCHAR(64) NOT NULL, `tenant_code` VARCHAR(64) NOT NULL,
  `deleted` TINYINT DEFAULT 0, `create_time` DATETIME, `update_time` DATETIME, PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS z_lc_materialization (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `materialization_path` VARCHAR(512), `entity_codes` TEXT, `export_version` VARCHAR(64),
  `status` VARCHAR(32) DEFAULT 'PENDING', `file_count` INT DEFAULT 0, `description` VARCHAR(512),
  `error_message` TEXT, `trigger_source` VARCHAR(64), `event_id` VARCHAR(64), `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME, `update_time` DATETIME, PRIMARY KEY (`id`)
);

-- 运行态数据变更日志 (undo/redo)
CREATE TABLE IF NOT EXISTS z_lc_data_change (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `entity_code` VARCHAR(64) NOT NULL, `record_id` BIGINT NOT NULL, `operation` VARCHAR(32) NOT NULL,
  `before_image` TEXT, `after_image` TEXT, `undone_by` BIGINT, `actor` VARCHAR(128), `trace_id` VARCHAR(64),
  `create_time` DATETIME, `update_time` DATETIME, `deleted` TINYINT DEFAULT 0, PRIMARY KEY (`id`)
);

-- 流程绑定的发起结局账 (缺陷 #61): 每次写后发起留一行 STARTED/FAILED, FAILED 带原因。
CREATE TABLE IF NOT EXISTS z_lc_workflow_fire (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_code` VARCHAR(64) NOT NULL, `app_code` VARCHAR(64) NOT NULL,
  `entity_code` VARCHAR(64) NOT NULL, `record_id` BIGINT NOT NULL, `binding_id` BIGINT,
  `trigger_event` VARCHAR(32), `process_definition_key` VARCHAR(128), `status` VARCHAR(16) NOT NULL,
  `instance_id` VARCHAR(128), `detail` VARCHAR(512), `create_time` DATETIME, `update_time` DATETIME,
  `deleted` TINYINT DEFAULT 0, PRIMARY KEY (`id`)
);
