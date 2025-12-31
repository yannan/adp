package com.eisoo.metadatamanage.web.service;

import java.util.List;

import com.eisoo.metadatamanage.db.entity.DictEntity;
import com.eisoo.metadatamanage.lib.vo.CatagoryItemVo;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseService;

public interface ICatagoryService extends MPJBaseService<DictEntity> {
    Result<List<CatagoryItemVo>> getList(Integer includeDeleted);
}
