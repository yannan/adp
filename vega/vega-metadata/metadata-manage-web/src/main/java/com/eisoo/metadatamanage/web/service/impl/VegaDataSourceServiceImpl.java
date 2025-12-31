package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.metadatamanage.db.entity.*;
import com.eisoo.metadatamanage.db.mapper.DipDataSourceMapper;
import com.eisoo.metadatamanage.web.service.*;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VegaDataSourceServiceImpl extends MPJBaseServiceImpl<DipDataSourceMapper, DipDataSourceEntity> implements IVegaDataSourceService {
    @Autowired(required = false)
    DipDataSourceMapper dipDataSourceMapper;

    @Override
    public DipDataSourceEntity getByDataSourceId(String dsid) {
        MPJLambdaWrapper<DipDataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(DipDataSourceEntity::getId, dsid);
        return dipDataSourceMapper.selectOne(wrapper);
    }

    @Override
    public boolean clearColumnsByDsId(String datasourceId) {
        return dipDataSourceMapper.clearColumnsByDsId(datasourceId);
    }
}
