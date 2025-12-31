package com.eisoo.metadatamanage.web.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.eisoo.metadatamanage.lib.dto.AdvancedDTO;
import com.eisoo.metadatamanage.lib.vo.FieldVo;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.druid.support.json.JSONUtils;
import com.eisoo.metadatamanage.db.entity.TableFieldEntity;
import com.eisoo.metadatamanage.db.mapper.TableFieldMapper;
import com.eisoo.metadatamanage.lib.dto.TableFieldDTO;
import com.eisoo.metadatamanage.web.service.IDictService;
import com.eisoo.metadatamanage.web.service.ITableFieldService;
import com.eisoo.metadatamanage.web.util.FieldCheckError;
import com.eisoo.metadatamanage.web.util.ValidationUtil;
import com.github.yulichang.base.MPJBaseServiceImpl;

@Service
public class TableFieldServiceImpl extends MPJBaseServiceImpl<TableFieldMapper, TableFieldEntity> implements ITableFieldService {
    @Autowired(required = false)
    IDictService dictService;

    @Autowired(required = false)
    TableFieldMapper tableFieldMapper;

    @Override
    public List<FieldCheckError> tableFieldsCheck(Integer dataSourceType, Long tableId, List<TableFieldDTO> tfList) {
        List<FieldCheckError> fieldErrs = new ArrayList<>();
        if (tfList != null) {
            Map<String, Integer> tmpMap = new HashMap<>();
            for (Integer i = 0; i < tfList.size(); i++) {
                FieldCheckError err = new FieldCheckError(i, tfList.get(i).getFieldName(), new ArrayList<>());
                if (tmpMap.containsKey(tfList.get(i).getFieldName().toLowerCase())) {
                    err.addError("字段名称冲突");
                } else {
                    tmpMap.put(tfList.get(i).getFieldName().toLowerCase(), null);
                }
                
                // 校验字段配置是否合法(仅校验单个属性是否符合约束)
                err.addError(ValidationUtil.validateBean(tfList.get(i)));

                // 校验字段配置是否合理(多属性关联关系校验)                
                err.addError(tfList.get(i).validate(dictService.getDictKeySet(dataSourceType+1)));
    
                if (!err.getErrors().isEmpty() || !StringUtils.isEmpty(tfList.get(i).getErrorMsg())) {
                    err.addError(tfList.get(i).getErrorMsg());
                    fieldErrs.add(err);
                    tfList.get(i).setErrorMsg(JSONUtils.toJSONString(err.getErrors()));
                }
                tfList.get(i).setTableId(tableId);
                // 字段注释统一处理
                tfList.get(i).setFieldComment(tfList.get(i).getFieldComment() == null ? "" : tfList.get(i).getFieldComment());
            }
        }
        return fieldErrs;
    }

    @Override
    public boolean saveOrUpdateBatch(Long tableId, List<TableFieldEntity> tfList) {
//        if (tfList == null) {
//            return false;
//        }
//
//        List<TableFieldEntity> insertList = new ArrayList<>();
//        Map<String, TableFieldEntity> updateList = new HashMap<>();
//        for (TableFieldEntity tfe : tfList) {
//            if (tfe.getTableId() != null) {
//                if (tfe.getTableId() > 0) {
//                    updateList.put(tfe.getFieldName(), tfe);
//                    continue;
//                }
//                tfe.setTableId(null);
//            }
//            insertList.add(tfe);
//        }
//
//        boolean ret = true;
//        LambdaUpdateWrapper<TableFieldEntity> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(TableFieldEntity::getTableId, tableId);
//        wrapper.notIn(!updateList.keySet().isEmpty(), TableFieldEntity::getFieldName, updateList.keySet());
//
//        tableFieldMapper.delete(wrapper);
//        if (!insertList.isEmpty()) {
//            ret &= saveBatch(insertList, insertList.size());
//        }
//
//        if (ret && !updateList.isEmpty()) {
//            ret &= updateBatchById(updateList.values(), updateList.size());
//        }

//        return ret;
        return false;
    }

    @Override
    public List<FieldVo> getVoList(List<TableFieldEntity> entityList, Integer dataSourceType) {
        if (AiShuUtil.isEmpty(entityList)) {
            return null;
        }
        return entityList.stream().map(entity -> {
            FieldVo fieldVo = new FieldVo();
            AiShuUtil.copyProperties(entity, fieldVo);
            fieldVo.setFieldTypeName(entity.getFieldType());
//            List<AdvancedDTO> advancedDTOList = JsonUtils.json2List(entity.getAdvancedParams(), AdvancedDTO.class);
//            List<AdvancedDTO> voAdvancedDTOList = new ArrayList<>();
//            if (AiShuUtil.isNotEmpty(advancedDTOList)) {
//                for (AdvancedDTO advancedDTO : advancedDTOList) {
//                    if (advancedDTO.getKey().equals("ORIGIN_FIELD_TYPE")) {
//                        advancedDTO.setKey(DataSourceConstants.ORIGIN_FIELD_TYPE);
//                        advancedDTO.setValue(StringUtils.lowerCase(advancedDTO.getValue()));
//                    }
//                    if (advancedDTO.getKey().equals("VIRTUAL_FIELD_TYPE")) {
//                        advancedDTO.setKey(DataSourceConstants.VIRTUAL_FIELD_TYPE);
//                        advancedDTO.setValue(StringUtils.lowerCase(advancedDTO.getValue()));
//                    }
//                    voAdvancedDTOList.add(advancedDTO);
//                }
//            }
//            fieldVo.setAdvancedParams(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(voAdvancedDTOList));
            return fieldVo;
        }).collect(Collectors.toList());
    }

    @Override
    public AdvancedDTO getAdvancedParamByKey(TableFieldEntity field, String key) {
        List<AdvancedDTO> advancedParams = com.eisoo.metadatamanage.web.util.JSONUtils.toList(field.getAdvancedParams(), AdvancedDTO.class);
        AdvancedDTO primaryKeys = advancedParams.stream().filter(param -> key.equals(param.getKey())).findFirst().orElse(null);
        return primaryKeys;
    }

    @Override
    public int logicalDeleteByTableId(Long tableId) {
        return tableFieldMapper.deleteByTableId(tableId);
    }

}
