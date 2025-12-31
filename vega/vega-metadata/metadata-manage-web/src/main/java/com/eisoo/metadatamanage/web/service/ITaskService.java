package com.eisoo.metadatamanage.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.TaskEntity;
import com.eisoo.metadatamanage.lib.dto.LiveDdlDto;
import com.eisoo.metadatamanage.lib.dto.FillMetaDataDTO;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.standardization.common.api.Result;

import java.util.Date;
import java.util.List;

public interface ITaskService extends IService<TaskEntity> {
    Result<?> fillMetaData(String dsid);
    Result<?> fillMetaDataByVirtual(String taskId);
    Result<?> fillMetaData(FillMetaDataDTO fillMetaDataDTO);
    Result<?> fillMetaDataByVirtual(FillMetaDataDTO fillMetaDataDTO);
    void fillMetaDataExec(DataSourceEntity dataSourceEntity, TaskEntity taskEntity);

    /**
     * 查询数据元ID集合校验,通过校验时返回ID列表
     * @param ids
     * @return
     */
    CheckVo<String> checkID(String ids);

    Result<List<TaskEntity>> getList(Date start_time, Date end_time, String keyword, Integer task_status, Integer offset, Integer limit, String sort, String direction);

    Result<?> updateRowNum(Long tableId);

    Result<?> getLog(Long taskId);

    CheckVo<TaskEntity> checkID(Long id);

    void updateSchemaByVirtual(DataSourceEntity dataSourceEntity);
    void updateTableByVirtual(List<DataSourceEntity> dataSourceEntity, List<LiveDdlDto> ddlDtoList);

    Result<?> updateMetaData();
}
