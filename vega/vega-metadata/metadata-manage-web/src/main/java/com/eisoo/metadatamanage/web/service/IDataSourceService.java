package com.eisoo.metadatamanage.web.service;

import java.util.List;

import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.lib.dto.DataSourceLiveUpdateStatusDto;
import com.eisoo.metadatamanage.lib.dto.DataSourceStatusDTO;
import com.eisoo.metadatamanage.lib.dto.DataSourceDTO;
import com.eisoo.metadatamanage.lib.dto.FillMetaDataDTO;
import com.eisoo.metadatamanage.lib.vo.DataSourceVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceCatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceItemVo;
import com.eisoo.metadatamanage.web.extra.model.DataSource;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseService;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public interface IDataSourceService extends MPJBaseService<DataSourceEntity> {
    List<DataSourceCatagoryItemVo> getListForCatagory(Integer includeDeleted);
    boolean isExisted(Long id);
    Result<List<DataSourceItemVo>> getList(Integer enableStatus, Integer connectStatus, Integer includeDeleted, Integer dataSourceType, String keyword, Integer offset, Integer limit, String sort, String direction);
    Result<DataSourceVo> getDetail(Long id);
    void create(DataSourceDTO params);
    void update(Long id, DataSourceDTO params);
    void updateEnableStatus(DataSourceStatusDTO params, List<Long> ids);
    Result<?> delete(List<Long> ids);
    Result<?> checkNameConflict(Integer dataSourceType, String dataSourceName);
    Result<?> fillMetaData(Long dsid);
    Result<?> fillMetaData(FillMetaDataDTO fillMetaDataDTO);
    DataSource getDataSource(DataSourceEntity dataSourceEntity);
    Boolean MQHandle(ConsumerRecords<String,String> data);
    Boolean MQDDLHandle(ConsumerRecords<String,String> data);
    void logicDelete(List<Long> ids);
    List<DataSourceEntity> queryByIdList(List<Long> dsIdList);
    DataSourceLiveUpdateStatusDto setliveUpdateStatus(DataSourceLiveUpdateStatusDto liveUpdateStatusDto);
    boolean clearColumnsByDsId(Long datasourceId);
    void stopMysqlBinlog (DataSourceEntity dataSourceEntity);
}
