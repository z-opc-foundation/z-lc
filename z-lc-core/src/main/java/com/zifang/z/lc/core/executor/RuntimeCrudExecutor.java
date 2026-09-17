package com.zifang.z.lc.core.executor;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 运行时 CRUD 执行器: 直接用 JdbcTemplate 拼 SQL (来自 DynamicSqlBuilder)
 * <p>
 * 不依赖 MyBatis Plus (因 SQL 动态构造); DataSource 来自 Spring 自动装配.
 */
@Component
public class RuntimeCrudExecutor {

    private static final Logger log = LogManager.getLogger(RuntimeCrudExecutor.class);
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private DynamicSqlBuilder sqlBuilder;

    public RuntimeCrudExecutor(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static long longOf(Integer v, int def) {
        if (v == null) {
            return def;
        }

        return v.longValue();
    }

    /**
     * 分页查询
     */
    public PageResult<Map<String, Object>> list(EntityDefDTO entity, RuntimeQueryDTO query) {
        if (entity == null) {
            throw new IllegalArgumentException("entity is null");
        }

        // 1) count
        DynamicSqlBuilder.SqlAndParams countSql = sqlBuilder.buildCountSql(entity, query);
        Long total = jdbcTemplate.queryForObject(countSql.sql, Long.class, countSql.params.toArray());
        if (total == null || total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L,
                    longOf(query.getPage(), 1), longOf(query.getSize(), 20));
        }
        // 2) list
        DynamicSqlBuilder.SqlAndParams listSql = sqlBuilder.buildListSql(entity, query);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql.sql, listSql.params.toArray());
        return new PageResult<>(rows, total,
                longOf(query.getPage(), 1), longOf(query.getSize(), 20));
    }

    /**
     * 按 id 取单条
     */
    public Map<String, Object> get(EntityDefDTO entity, Long id, String tenantCode) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        DynamicSqlBuilder.SqlAndParams sql = sqlBuilder.buildGetSql(entity, id, tenantCode);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.sql, sql.params.toArray());
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * INSERT (返回生成的主键 id)
     */
    public Long create(EntityDefDTO entity, RuntimeCrudDTO body, String currentUser) {
        DynamicSqlBuilder.SqlAndParams sql = sqlBuilder.buildInsertSql(entity, body, currentUser);
        KeyHolder kh = new GeneratedKeyHolder();
        int affected = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql.sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < sql.params.size(); i++) {
                ps.setObject(i + 1, sql.params.get(i));
            }
            return ps;
        }, kh);
        if (affected == 0) {
            log.warn("INSERT affected 0 rows for entity={}", entity.getEntityCode());
            return null;
        }
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    /**
     * UPDATE by id (返回受影响行数)
     */
    public int update(EntityDefDTO entity, Long id, RuntimeCrudDTO body, String currentUser) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        DynamicSqlBuilder.SqlAndParams sql = sqlBuilder.buildUpdateSql(entity, id, body, currentUser);
        return jdbcTemplate.update(sql.sql, sql.params.toArray());
    }

    /**
     * 软删 by id
     */
    public int delete(EntityDefDTO entity, Long id, String tenantCode) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        DynamicSqlBuilder.SqlAndParams sql = sqlBuilder.buildDeleteSql(entity, id, tenantCode);
        return jdbcTemplate.update(sql.sql, sql.params.toArray());
    }

    /**
     * 简单参数查询 (用于 RefCheckProcessor 之类的单值校验)
     */
    public boolean existsById(EntityDefDTO entity, Long id, String tenantCode) {
        Map<String, Object> r = get(entity, id, tenantCode);
        return r != null;
    }

    /**
     * 单元测试 / 健康检查: 直接执行任意 SQL (SELECT)
     */
    public List<Map<String, Object>> rawQuery(String sql, List<Object> params) {
        if (sql == null || !sql.trim().toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT allowed in rawQuery");
        }
        List<Object> p = params == null ? new ArrayList<>() : params;
        return jdbcTemplate.queryForList(sql, p.toArray());
    }
}
