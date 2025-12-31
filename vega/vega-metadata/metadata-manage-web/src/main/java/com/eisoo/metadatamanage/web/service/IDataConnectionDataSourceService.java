package com.eisoo.metadatamanage.web.service;

import com.eisoo.metadatamanage.db.entity.DataSourceEntityDataConnection;
import com.github.yulichang.base.MPJBaseService;

/**
 * @author Tian.lan
 */
public interface IDataConnectionDataSourceService extends MPJBaseService<DataSourceEntityDataConnection> {
    DataSourceEntityDataConnection getByDataSourceId(String dsid);
    boolean clearColumnsByDsId(String dsid);
}
