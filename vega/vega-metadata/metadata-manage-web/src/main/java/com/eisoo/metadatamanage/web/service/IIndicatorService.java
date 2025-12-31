package com.eisoo.metadatamanage.web.service;

import com.eisoo.metadatamanage.lib.dto.IndicatorCreateDTO;
import com.eisoo.metadatamanage.db.entity.IndicatorEntity;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseService;

import java.util.Date;
import java.util.List;

public interface IIndicatorService extends MPJBaseService<IndicatorEntity> {
    Result<?> innerTask();
    Boolean create(IndicatorCreateDTO dto);
    Boolean deleteByTime(Date startTime, Date endTime);
    /**
     * 查询数据元ID集合校验,通过校验时返回ID列表
     * @param ids
     * @return
     */
    CheckVo<String> checkID(String ids);
    CheckVo<IndicatorEntity> checkID(Long id);
    Result<List<IndicatorEntity>> getList(Date start_time, Date end_time, String keyword, String type, Integer offset, Integer limit, String direction);
}
