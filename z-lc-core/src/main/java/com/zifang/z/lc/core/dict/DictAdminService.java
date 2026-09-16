package com.zifang.z.lc.core.dict;

import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;

import java.util.List;

/**
 * 字典管理服务接口 (F035 T6: Dict Management)
 */
public interface DictAdminService {

    DictDTO createDict(DictDTO req);

    DictDTO updateDict(DictDTO req);

    int deleteDict(Long id);

    List<DictDTO> listDicts(String tenantCode);

    DictDTO getDictByCode(String tenantCode, String dictCode);

    List<DictItemDTO> saveItems(String tenantCode, String dictCode, List<DictItemDTO> items);

    List<DictItemDTO> listItems(String tenantCode, String dictCode);

    int deleteItem(Long id);
}
