package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.metadatamanage.db.entity.DataSourceEntityDataConnection;
import com.eisoo.metadatamanage.db.mapper.DataConnectionDataSourceMapper;
import com.eisoo.metadatamanage.web.service.IDataConnectionDataSourceService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Tian.lan
 */
@Slf4j
@Service
public class DataConnectionDataSourceServiceImpl extends MPJBaseServiceImpl<DataConnectionDataSourceMapper, DataSourceEntityDataConnection> implements IDataConnectionDataSourceService {
    @Autowired(required = false)
    DataConnectionDataSourceMapper dataConnectionDataSourceMapper;

    @Override
    public DataSourceEntityDataConnection getByDataSourceId(String dsid) {
        MPJLambdaWrapper<DataSourceEntityDataConnection> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(DataSourceEntityDataConnection::getFId, dsid);
        return dataConnectionDataSourceMapper.selectOne(wrapper);
    }
    @Override
    public boolean clearColumnsByDsId(String datasourceId) {
        return dataConnectionDataSourceMapper.clearColumnsByDsId(datasourceId);
    }
}
