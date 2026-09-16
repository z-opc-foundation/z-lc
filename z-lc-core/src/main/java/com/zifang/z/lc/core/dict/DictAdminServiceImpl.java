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
        if (entity.getTenantCode() == null) entity.setTenantCode("default");
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        dictMapper.insert(entity);
        log.info("Dict created: code={}", entity.getDictCode());
        return toDTO(entity);
    }

    @Override
    public DictDTO updateDict(DictDTO req) {
        DictEntity entity = dictMapper.selectById(req.getId());
        if (entity == null) return null;
        if (req.getDictName() != null) entity.setDictName(req.getDictName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        entity.setUpdateTime(new Date());
        dictMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public int deleteDict(Long id) {
        DictEntity entity = dictMapper.selectById(id);
        if (entity == null) return 0;
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
        if (entity == null) return 0;
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return dictItemMapper.updateById(entity);
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
