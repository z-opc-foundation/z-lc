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

    /**
     * 追加单个字典项 (不动其它已存在的项).
     * <p>
     * 与 {@link #saveItems} 的区别: saveItems 是"整表替换"语义 (先软删该字典下全部项再插入),
     * 早期 controller 把 items/create 与 items/update 也接到 saveItems 上, 导致
     * "新增一个字典项会把同字典下其它字典项全部删掉" —— 实测 /dict/items 只剩最后一次写入的那条.
     * 单项增改必须走下面这两个方法.
     */
    DictItemDTO addItem(String tenantCode, String dictCode, DictItemDTO item);

    /** 按 id 更新单个字典项 (不动其它已存在的项). */
    DictItemDTO updateItem(String tenantCode, String dictCode, DictItemDTO item);

    List<DictItemDTO> listItems(String tenantCode, String dictCode);

    int deleteItem(Long id);
}
