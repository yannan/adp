package com.eisoo.metadatamanage.web.service;

import java.util.List;

import com.eisoo.metadatamanage.db.entity.TableFieldEntity;
import com.eisoo.metadatamanage.lib.dto.AdvancedDTO;
import com.eisoo.metadatamanage.lib.dto.TableFieldDTO;
import com.eisoo.metadatamanage.lib.vo.FieldVo;
import com.eisoo.metadatamanage.web.util.FieldCheckError;
import com.github.yulichang.base.MPJBaseService;

public interface ITableFieldService extends MPJBaseService<TableFieldEntity> {
    List<FieldCheckError> tableFieldsCheck(Integer dataSourceType, Long tableId, List<TableFieldDTO> tfList);
    boolean saveOrUpdateBatch(Long tableId, List<TableFieldEntity> tfList);
    List<FieldVo> getVoList(List<TableFieldEntity> entityList, Integer dataSourceType) ;
    AdvancedDTO getAdvancedParamByKey(TableFieldEntity field, String key);

    int logicalDeleteByTableId(Long tableId);
}
