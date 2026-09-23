-- =====================================================================
-- z-lc-web 集成测试种子数据 (幂等 MERGE, 双 DataSource 重复执行安全)
-- 租户统一 default, 应用 demo, 实体 order -> 物理表 lc_demo_order.
-- =====================================================================

MERGE INTO z_lc_app (id, tenant_code, app_code, app_name, status, current_version, deleted)
  KEY(id) VALUES (1, 'default', 'demo', 'Demo App', 'DRAFT', 0, 0);

-- 事件溯源: 回放后折叠出 order 实体 (name/amount/status/due_date/closed_at)
-- due_date 是 DATE、closed_at 是 DATETIME —— 时间分桶聚合 (/runtime/aggregate?timeGroup) 靠这两列做真实执行测试
MERGE INTO z_lc_event (id, tenant_code, event_id, app_code, entity_code, event_type, event_data, source, apply_seq)
  KEY(id) VALUES (1, 'default', 'evt-order-create', 'demo', 'order', 'CREATE',
  '{"entityCode":"order","entityName":"Order","tableName":"lc_demo_order","fields":[{"fieldCode":"name","fieldName":"name","fieldType":"STRING","required":true,"fieldLength":32},{"fieldCode":"amount","fieldName":"amount","fieldType":"DECIMAL"},{"fieldCode":"status","fieldName":"status","fieldType":"STRING","dictCode":"order_status"},{"fieldCode":"due_date","fieldName":"due_date","fieldType":"DATE"},{"fieldCode":"closed_at","fieldName":"closed_at","fieldType":"DATETIME"}]}',
  'test', 1);

MERGE INTO z_lc_dict (id, tenant_code, dict_code, dict_name, status, deleted)
  KEY(id) VALUES (1, 'default', 'order_status', 'Order Status', 'ENABLED', 0);

MERGE INTO z_lc_dict_item (id, tenant_code, dict_code, item_code, item_label, item_value, sort_order, deleted)
  KEY(id) VALUES (1, 'default', 'order_status', 'OK', 'Done', 'OK', 1, 0);
MERGE INTO z_lc_dict_item (id, tenant_code, dict_code, item_code, item_label, item_value, sort_order, deleted)
  KEY(id) VALUES (2, 'default', 'order_status', 'PENDING', 'Pending', 'PENDING', 2, 0);

MERGE INTO z_lc_view_config (id, entity_code, app_code, view_type, config, tenant_code, deleted)
  KEY(id) VALUES (1, 'order', 'demo', 'LIST', '{"columns":["name","amount"]}', 'default', 0);

MERGE INTO z_lc_relation (id, relation_code, relation_name, source_entity_code, target_entity_code, relation_type, tenant_code, app_code, deleted)
  KEY(id) VALUES (1, 'r-order-customer', 'Order-Customer', 'order', 'customer', 'MANY_TO_ONE', 'default', 'demo', 0);

MERGE INTO lc_demo_order (id, tenant_code, name, amount, status, due_date, closed_at, deleted)
  KEY(id) VALUES (1, 'default', 'Alice', 100.00, 'OK', DATE '2026-01-15', TIMESTAMP '2026-01-15 09:30:00', 0);
MERGE INTO lc_demo_order (id, tenant_code, name, amount, status, due_date, closed_at, deleted)
  KEY(id) VALUES (2, 'default', 'Bob', 50.00, 'PENDING', DATE '2026-02-03', TIMESTAMP '2026-02-03 18:00:00', 0);
