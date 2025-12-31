package com.eisoo.metadatamanage.web.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eisoo.metadatamanage.db.entity.DictEntity;
import com.eisoo.metadatamanage.db.mapper.DictMapper;
import com.eisoo.metadatamanage.lib.vo.CatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceCatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.DictItemVo;
import com.eisoo.metadatamanage.lib.vo.SchemaCatagoryItemVo;
import com.eisoo.metadatamanage.web.service.ICatagoryService;
import com.eisoo.metadatamanage.web.service.IDataSourceService;
import com.eisoo.metadatamanage.web.service.IDictService;
import com.eisoo.metadatamanage.web.service.ISchemaService;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseServiceImpl;

@Service
public class CatagoryServiceImpl extends MPJBaseServiceImpl<DictMapper, DictEntity> implements ICatagoryService {
    // @Lazy
    @Autowired(required = false)
    IDictService dictService;

    // @Lazy
    @Autowired(required = false)
    IDataSourceService dataSourceService;

    // @Lazy
    @Autowired(required = false)
    ISchemaService schemaService;

    @Override
    public Result<List<CatagoryItemVo>> getList(Integer includeDeleted) {
        List<CatagoryItemVo> ret = new ArrayList<>();
        List<DictItemVo> dicts = dictService.getList(1);
        LambdaQueryWrapper<DataSourceEntity> dataSourceEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
        Map<Integer, List<DataSourceEntity>> dataSourceEntityMap = new HashMap<>();
        List<DataSourceEntity> dataSourceEntityList = dataSourceService.list(dataSourceEntityLambdaQueryWrapper);
        if (AiShuUtil.isNotEmpty(dataSourceEntityList)) {
            dataSourceEntityList.forEach(ds -> {
                if (AiShuUtil.isEmpty(dataSourceEntityMap.get(ds.getDataSourceType()))) {
                    List<DataSourceEntity> dataSourceEntityList1 = new ArrayList<>();
                    dataSourceEntityList1.add(ds);
                    dataSourceEntityMap.put(ds.getDataSourceType(), dataSourceEntityList1);
                } else {
                    dataSourceEntityMap.get(ds.getDataSourceType()).add(ds);
                }
            });
        }
        LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        Map<String, List<SchemaEntity>> schemaEntityMap = new HashMap<>();
        List<SchemaEntity> schemaEntityList = schemaService.list(schemaEntityLambdaQueryWrapper);
        if (AiShuUtil.isNotEmpty(schemaEntityList)) {
            schemaEntityList.forEach(schema -> {
                if (AiShuUtil.isEmpty(schemaEntityMap.get(schema.getDataSourceId()))) {
                    List<SchemaEntity> schemaEntityList1 = new ArrayList<>();
                    schemaEntityList1.add(schema);
                    schemaEntityMap.put(schema.getDataSourceId(), schemaEntityList1);
                } else {
                    schemaEntityMap.get(schema.getDataSourceId()).add(schema);
                }
            });
        }

        if (!dicts.isEmpty()) {
//            int dsIndex = 0;
//            int sIndex = 0;
//            List<DataSourceCatagoryItemVo> dataSources = dataSourceService.getListForCatagory(includeDeleted);
//            List<SchemaCatagoryItemVo> schemas = schemaService.getListForCatagory();
            for (DictItemVo dict : dicts) {
                CatagoryItemVo ci = new CatagoryItemVo();
                ci.setDataSourceType(dict.getDictKey());
                ci.setDataSourceTypeName(dict.getDictValue());
                List<DataSourceCatagoryItemVo> dataSourceCatagoryItemVos = new ArrayList<>();
                List<DataSourceEntity> tmpDataSources = dataSourceEntityMap.get(ci.getDataSourceType());
                if (AiShuUtil.isNotEmpty(tmpDataSources)) {
                   for (DataSourceEntity ds : tmpDataSources) {
                       DataSourceCatagoryItemVo dataSourceCatagoryItemVo = new DataSourceCatagoryItemVo();
                       dataSourceCatagoryItemVo.setId(ds.getId());
                       dataSourceCatagoryItemVo.setDataSourceType(ds.getDataSourceType());
                       dataSourceCatagoryItemVo.setName(ds.getName());
                       dataSourceCatagoryItemVo.setExtendProperty(ds.getExtendProperty());
                       List<SchemaCatagoryItemVo> schemaCatagoryItemVos = new ArrayList<>();
                       List<SchemaEntity> schemaEntities = schemaEntityMap.get(ds.getId());
                       if (AiShuUtil.isNotEmpty(schemaEntities)) {
                           for (SchemaEntity schema : schemaEntities) {
                               SchemaCatagoryItemVo schemaCatagoryItemVo = new SchemaCatagoryItemVo();
                               schemaCatagoryItemVo.setId(schema.getId());
                               schemaCatagoryItemVo.setDataSourceId(schema.getDataSourceId());
                               schemaCatagoryItemVo.setDataSourceType(schema.getDataSourceType());
                               schemaCatagoryItemVo.setName(schema.getName());
                               schemaCatagoryItemVos.add(schemaCatagoryItemVo);
                           }
                       }
                       dataSourceCatagoryItemVo.setSchemas(schemaCatagoryItemVos);
                       dataSourceCatagoryItemVos.add(dataSourceCatagoryItemVo);
                   }
                }
                ci.setDataSources(dataSourceCatagoryItemVos);
//                List<DataSourceCatagoryItemVo> tmpDataSources = new ArrayList<>();
//                genSubCatagoryForDataSourceTypeCatagory(dsIndex, sIndex, ci.getDataSourceType(), dataSources, schemas, tmpDataSources);
                // b1:
                // for (int i = dsIndex; i < dataSources.size(); i++) {
                //     switch (dataSources.get(i).getDataSourceType().compareTo(ci.getDataSourceType())) {
                //         case -1:
                //             continue b1;
                //         case 0:
                //             List<SchemaCatagoryItem> tmpSchemas = new ArrayList<>();
                //             b2:
                //             for (int j = sIndex; j < schemas.size(); j++) {
                //                 switch (schemas.get(j).getDataSourceType().compareTo(ci.getDataSourceType())) {
                //                     case -1:
                //                         continue b2;
                //                     case 0:
                //                         switch (schemas.get(j).getDataSourceId().compareTo(dataSources.get(i).getId())) {
                //                             case -1:
                //                                 continue b2;
                //                             case 0:
                //                                 tmpSchemas.add(schemas.get(j));
                //                                 continue b2;
                //                             case 1:
                //                                 sIndex = j;
                //                                 break b2;
                //                         }
                //                     case 1:
                //                         sIndex = j;
                //                         break b2;
                //                 }
                //             }
                //             dataSources.get(i).setSchemas(tmpSchemas);
                //             tmpDataSources.add(dataSources.get(i));
                //             continue b1;
                //         case 1:
                //             dsIndex = i;
                //             break b1;
                //     }
                // }
//                ci.setDataSources(tmpDataSources);
                ret.add(ci);
            }
        }

        return Result.success(ret);
    }

    private void genSubCatagoryForDataSourceTypeCatagory(
        Integer dsIndex, Integer sIndex, Integer dataSourceType, 
        List<DataSourceCatagoryItemVo> dataSources, List<SchemaCatagoryItemVo> schemas, List<DataSourceCatagoryItemVo> tmpDataSources) {
        // b1:
        for (int i = dsIndex; i < dataSources.size(); i++) {
            switch (dataSources.get(i).getDataSourceType().compareTo(dataSourceType)) {
                case -1:
                    // continue b1;
                    break;
                case 0:
                    List<SchemaCatagoryItemVo> tmpSchemas = new ArrayList<>();
                    genSubCatagoryForDataSourceCatagory(sIndex, dataSourceType, dataSources.get(i).getId(), schemas, tmpSchemas);
                    dataSources.get(i).setSchemas(tmpSchemas);
                    tmpDataSources.add(dataSources.get(i));
                    // continue b1;
                    break;
                case 1:
                    dsIndex = i;
                    // break b1;
                    return;
            }
        }
    }

    private void genSubCatagoryForDataSourceCatagory(
        Integer sIndex, Integer dataSourceType, String dataSourceId,
        List<SchemaCatagoryItemVo> schemas, List<SchemaCatagoryItemVo> tmpSchemas) {
        // b2:
        for (int j = sIndex; j < schemas.size(); j++) {
            switch (schemas.get(j).getDataSourceType().compareTo(dataSourceType)) {
                case -1:
                    // continue b2;
                    break;
                case 0:
                    switch (schemas.get(j).getDataSourceId().compareTo(dataSourceId /*dataSources.get(i).getId()*/)) {
                        case -1:
                            // continue b2;
                            break;
                        case 0:
                            tmpSchemas.add(schemas.get(j));
                            // continue b2;
                            break;
                        case 1:
                            sIndex = j;
                            // break b2;
                            return;
                    }
                    break;
                case 1:
                    sIndex = j;
                    // break b2;
                    return;
            }
        }
    }
}
