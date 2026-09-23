package com.zifang.z.lc.core.dict;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.core.dict.entity.DictEntity;
import com.zifang.z.lc.core.dict.entity.DictItemEntity;
import com.zifang.z.lc.mapper.dict.DictItemMapper;
import com.zifang.z.lc.mapper.dict.DictMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictAdminServiceImpl implements DictAdminService {

    private static final Logger log = LogManager.getLogger(DictAdminServiceImpl.class);

    @Resource
    private DictMapper dictMapper;

    @Resource
    private DictItemMapper dictItemMapper;

    @Override
    public DictDTO createDict(DictDTO req) {
        DictEntity entity = new DictEntity();
        BeanUtils.copyProperties(req, entity);
        if (entity.getTenantCode() == null) {
            entity.setTenantCode("default");
        }
        requireDictCodeFree(entity.getTenantCode(), entity.getDictCode());

        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        dictMapper.insert(entity);
        log.info("Dict created: code={}", entity.getDictCode());
        return toDTO(entity);
    }

    /**
     * (tenant_code, dict_code) 上有唯一索引 uk_dict_tenant_code，而该索引不含 deleted 列：
     * 软删掉的字典依然占着这个 code。所以预检必须连 deleted=1 一起查 —— 只查 deleted=0
     * 会放过去，insert 仍旧撞索引，出来的是 500 + 一层 JDBC 包装信息，而不是"字典已存在"。
     */
    private void requireDictCodeFree(String tenantCode, String dictCode) {
        if (dictCode == null) {
            return;
        }
        List<DictEntity> hits = dictMapper.selectList(new QueryWrapper<DictEntity>()
                .eq("tenant_code", tenantCode)
                .eq("dict_code", dictCode));
        if (hits.isEmpty()) {
            return;
        }
        DictEntity hit = hits.get(0);
        boolean softDeleted = hit.getDeleted() != null && hit.getDeleted() == 1;
        throw new IllegalArgumentException("字典已存在: dictCode=" + dictCode + (softDeleted
                ? "（同编码的字典此前已被删除，唯一索引仍占用该 code，请换一个编码）"
                : "（同租户下 dictCode 必须唯一）"));
    }

    @Override
    public DictDTO updateDict(DictDTO req) {
        DictEntity entity = dictMapper.selectById(req.getId());
        if (entity == null) {
            return null;
        }

        if (req.getDictName() != null) {
            entity.setDictName(req.getDictName());
        }

        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }

        entity.setUpdateTime(new Date());
        dictMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public int deleteDict(Long id) {
        DictEntity entity = dictMapper.selectById(id);
        if (entity == null) {
            return 0;
        }

        // soft-delete all items by dictCode
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DictItemEntity> itemUpdateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DictItemEntity>()
                        .eq("dict_code", entity.getDictCode())
                        .eq("tenant_code", entity.getTenantCode())
                        .set("deleted", 1);
        dictItemMapper.update(null, itemUpdateWrapper);
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        dictMapper.updateById(entity);
        return 1;
    }

    @Override
    public List<DictDTO> listDicts(String tenantCode) {
        List<DictEntity> list = dictMapper.selectList(
                new QueryWrapper<DictEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("deleted", 0)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public DictDTO getDictByCode(String tenantCode, String dictCode) {
        DictEntity entity = dictMapper.selectOne(
                new QueryWrapper<DictEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("dict_code", dictCode)
                        .eq("deleted", 0));
        return entity == null ? null : toDTO(entity);
    }

    @Override
    public List<DictItemDTO> saveItems(String tenantCode, String dictCode, List<DictItemDTO> items) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (DictItemDTO item : items) {
            String code = item.getItemCode();
            if (code != null && !code.isEmpty() && !seen.add(code)) {
                throw new IllegalArgumentException("字典项重复: dictCode=" + dictCode + ", itemCode=" + code);
            }
        }
        // soft-delete existing items first
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DictItemEntity> saveUpdateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DictItemEntity>()
                        .eq("dict_code", dictCode)
                        .eq("tenant_code", tenantCode)
                        .set("deleted", 1);
        dictItemMapper.update(null, saveUpdateWrapper);
        // insert new items
        for (DictItemDTO item : items) {
            DictItemEntity entity = new DictItemEntity();
            BeanUtils.copyProperties(item, entity);
            entity.setDictCode(dictCode);
            entity.setTenantCode(tenantCode);
            entity.setDeleted(0);
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
            dictItemMapper.insert(entity);
        }
        // return all active items
        List<DictItemEntity> list = dictItemMapper.selectList(
                new QueryWrapper<DictItemEntity>()
                        .eq("dict_code", dictCode)
                        .eq("tenant_code", tenantCode)
                        .eq("deleted", 0)
                        .orderByAsc("sort_order"));
        return list.stream().map(this::toItemDTO).collect(Collectors.toList());
    }

    @Override
    public DictItemDTO addItem(String tenantCode, String dictCode, DictItemDTO item) {
        assertItemCodeFree(tenantCode, dictCode, item.getItemCode(), null);
        DictItemEntity entity = new DictItemEntity();
        BeanUtils.copyProperties(item, entity);
        entity.setId(null);
        entity.setDictCode(dictCode);
        entity.setTenantCode(tenantCode);
        entity.setDeleted(0);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        dictItemMapper.insert(entity);
        return toItemDTO(entity);
    }

    @Override
    public DictItemDTO updateItem(String tenantCode, String dictCode, DictItemDTO item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("dict item id required for update");
        }
        DictItemEntity exists = dictItemMapper.selectById(item.getId());
        if (exists == null || !dictCode.equals(exists.getDictCode())) {
            throw new IllegalArgumentException("dict item not found: " + item.getId());
        }
        assertItemCodeFree(exists.getTenantCode(), dictCode, item.getItemCode(), item.getId());
        // 只覆盖前端会编辑的字段, tenant/dict 归属不允许被改写
        exists.setItemCode(item.getItemCode());
        exists.setItemLabel(item.getItemLabel());
        exists.setItemValue(item.getItemValue());
        exists.setSortOrder(item.getSortOrder());
        exists.setDescription(item.getDescription());
        exists.setUpdateTime(new Date());
        dictItemMapper.updateById(exists);
        return toItemDTO(exists);
    }

    @Override
    public List<DictItemDTO> listItems(String tenantCode, String dictCode) {
        List<DictItemEntity> list = dictItemMapper.selectList(
                new QueryWrapper<DictItemEntity>()
                        .eq("dict_code", dictCode)
                        .eq("tenant_code", tenantCode)
                        .eq("deleted", 0)
                        .orderByAsc("sort_order"));
        return list.stream().map(this::toItemDTO).collect(Collectors.toList());
    }

    @Override
    public int deleteItem(Long id) {
        DictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            return 0;
        }

        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return dictItemMapper.updateById(entity);
    }

    /**
     * 同一个 (tenant, dict) 下 item_code 必须唯一 (仅统计未软删的行).
     * <p>
     * z_lc_dict 有 uk_dict_tenant_code, 但字典项不能加同样的唯一索引 —— 软删后重新添加
     * 同一个 code 会撞索引。所以唯一性由这一层守住。违反它的后果不是脏数据而已:
     * {@code DynamicSqlBuilder} 按 {@code item_code} LEFT JOIN 字典项取 label, 重复项会让
     * 每条业务记录被 fan-out 成 N 行, 分页 total 与页脚统计同步虚高。
     */
    private void assertItemCodeFree(String tenantCode, String dictCode, String itemCode, Long selfId) {
        if (itemCode == null || itemCode.isEmpty()) {
            return;
        }

        QueryWrapper<DictItemEntity> dup = new QueryWrapper<DictItemEntity>()
                .eq("dict_code", dictCode)
                .eq("item_code", itemCode)
                .eq("deleted", 0);
        if (tenantCode != null) {
            dup.eq("tenant_code", tenantCode);
        }
        if (selfId != null) {
            dup.ne("id", selfId);
        }

        List<DictItemEntity> hits = dictItemMapper.selectList(dup);
        if (!hits.isEmpty()) {
            throw new IllegalArgumentException("字典项已存在: dictCode=" + dictCode + ", itemCode=" + itemCode);
        }
    }

    private DictDTO toDTO(DictEntity e) {
        DictDTO dto = new DictDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    private DictItemDTO toItemDTO(DictItemEntity e) {
        DictItemDTO dto = new DictItemDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
