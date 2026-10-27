package com.zifang.z.lc.core.page;

import com.zifang.z.lc.common.dto.PageCreateReq;
import com.zifang.z.lc.common.dto.PageDTO;
import com.zifang.z.lc.common.dto.PageUpdateReq;

import java.util.List;

/**
 * 页面服务接口 (V2 页面域: Page 一等公民, lead 013 §四)。
 */
public interface PageService {

    PageDTO createPage(PageCreateReq req);

    PageDTO updatePage(PageUpdateReq req);

    int deletePage(Long id);

    PageDTO getPage(String tenantCode, String appCode, String pageCode);

    List<PageDTO> listPages(String tenantCode, String appCode);
}
