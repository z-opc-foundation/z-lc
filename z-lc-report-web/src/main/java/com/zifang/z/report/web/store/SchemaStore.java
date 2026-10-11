package com.zifang.z.report.web.store;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.ViewSchema;

import java.util.List;

/**
 * 契约存储 SPI (M2 接口化): 数据集/视图 schema 的持久化抽象。
 * <p>
 * M2 内置实现: {@link InMemorySchemaStore} (重启即失)。
 * DB 实现约定 (同伴接手, 见 _doc/008_troubleshooting/失败要点与坑.md): MyBatis-Plus + JSON 列存 schema 全文
 * (对齐 z-lc viewconfig 模式), 表结构见 _doc/002_deploy/init/001_schema.sql;
 * 只需提供同名 Bean 替换 InMemorySchemaStore 的 @Component 注册即可整体切换。
 * <p>
 * 语义约定:
 * - require*: 不存在抛 IllegalArgumentException (面向渲染/执行主链路, fail-fast)
 * - get*: 不存在返回 null (面向幂等探测/AI 装配)
 * - put*: id 为 null 抛 IllegalArgumentException; 同 id 覆盖 (upsert 语义)
 */
public interface SchemaStore {

    void putDataset(DatasetSchema schema);

    DatasetSchema requireDataset(String id);

    DatasetSchema getDataset(String id);

    List<DatasetSchema> listDatasets();

    void putView(ViewSchema view);

    ViewSchema requireView(String id);

    ViewSchema getView(String id);

    List<ViewSchema> listViews();
}
