-- =====================================================================
-- 结构对齐回写 (缺列) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: 只加不改; 只收「两种独立解析实现都判缺」的列, 歧义区不动。
-- 用法: mysql <库名> < 本文件 (脚本用 DELIMITER, 走 mysql 客户端执行)。
-- 位置: 与本表 CREATE 语句同目录 (z-lc/z-lc-admin/src/main/resources/db)
-- =====================================================================

-- 逐列补齐: 只在列不存在时才 ALTER ⇒ 可重复执行。
-- MySQL **不支持** ALTER TABLE ... ADD COLUMN IF NOT EXISTS (那是 MariaDB 语法),
-- 所以幂等靠 information_schema 判定 + 动态 SQL, 不靠语法糖。
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-lc-z-lc-admin-src-main-resources-db`;
DELIMITER $$
CREATE PROCEDURE `zopc_sync_prod_cols_z-lc-z-lc-admin-src-main-resources-db`()
BEGIN
  DECLARE v_t VARCHAR(64);
  DECLARE v_c VARCHAR(64);
  DECLARE v_d TEXT;
  DECLARE cur CURSOR FOR SELECT tbl, col, defn FROM `_zopc_prod_sync_cols_z-lc-z-lc-admin-src-main-resources-db`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_t = NULL;
  OPEN cur;
  lp: LOOP
    FETCH cur INTO v_t, v_c, v_d;
    IF v_t IS NULL THEN LEAVE lp; END IF;
    IF EXISTS (
         SELECT 1 FROM information_schema.TABLES
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t)
       AND NOT EXISTS (
         SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t AND COLUMN_NAME = v_c) THEN
      SET @ddl = CONCAT('ALTER TABLE `', v_t, '` ADD COLUMN `', v_c, '` ', v_d);
      PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-lc-z-lc-admin-src-main-resources-db`;
CREATE TEMPORARY TABLE `_zopc_prod_sync_cols_z-lc-z-lc-admin-src-main-resources-db` (tbl VARCHAR(64), col VARCHAR(64), defn TEXT);
INSERT INTO `_zopc_prod_sync_cols_z-lc-z-lc-admin-src-main-resources-db` (tbl, col, defn) VALUES
  ('z_lc_app', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_deployment', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_dict', 'api_url', 'varchar(512) DEFAULT NULL'),
  ('z_lc_dict', 'cache_seconds', 'int DEFAULT ''300'''),
  ('z_lc_dict', 'dict_type', 'varchar(16) DEFAULT ''STATIC'''),
  ('z_lc_dict', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_dict_item', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_dict_item', 'enabled', 'tinyint(1) DEFAULT ''1'''),
  ('z_lc_entity', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_event', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_field', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_permission', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_pipeline_config', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_relation', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_view_config', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_lc_workflow_binding', 'domain_code', 'varchar(64) DEFAULT NULL');

CALL `zopc_sync_prod_cols_z-lc-z-lc-admin-src-main-resources-db`();
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-lc-z-lc-admin-src-main-resources-db`;
DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-lc-z-lc-admin-src-main-resources-db`;
