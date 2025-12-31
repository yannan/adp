package com.eisoo.metadatamanage.web.service;

import com.eisoo.metadatamanage.db.entity.DipDataSourceEntity;
import com.github.yulichang.base.MPJBaseService;
public interface IVegaDataSourceService extends MPJBaseService<DipDataSourceEntity> {
    DipDataSourceEntity getByDataSourceId(String dsid);

    boolean clearColumnsByDsId(String dsid);
}
