package com.eisoo.metadatamanage.web.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.metadatamanage.db.entity.TableEntity;
import com.eisoo.metadatamanage.db.mapper.DataSourceMapper;
import com.eisoo.metadatamanage.db.mapper.SchemaMapper;
import com.eisoo.metadatamanage.db.mapper.TableMapper;
import com.eisoo.metadatamanage.lib.vo.SchemaCatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.SchemaItemVo;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.web.service.IDataSourceService;
import com.eisoo.metadatamanage.web.service.ISchemaService;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

@Service
public class SchemaServiceImpl extends MPJBaseServiceImpl<SchemaMapper, SchemaEntity> implements ISchemaService {
    @Autowired(required = false)
    IDataSourceService dataSourceService;

    @Autowired(required = false)
    SchemaMapper schemaMapper;

    @Autowired(required = false)
    DataSourceMapper dataSourceMapper;

    @Autowired(required = false)
    TableMapper tableMapper;

    @Override
    public List<SchemaCatagoryItemVo> getListForCatagory() {
        MPJLambdaWrapper<SchemaEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(SchemaEntity::getDataSourceType, SchemaEntity::getDataSourceId, SchemaEntity::getId, SchemaEntity::getName);
        wrapper.orderByAsc(SchemaEntity::getDataSourceType, SchemaEntity::getDataSourceId, SchemaEntity::getId);
        return selectJoinList(SchemaCatagoryItemVo.class, wrapper);
    }

    @Override
    public Result<List<SchemaItemVo>> getList(Long dsId) {
        if (!dataSourceService.isExisted(dsId)) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ParentResourceNotExisted);
        }
        MPJLambdaWrapper<SchemaEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
            SchemaEntity::getDataSourceType, SchemaEntity::getDataSourceTypeName, 
            SchemaEntity::getDataSourceId, SchemaEntity::getDataSourceName, 
            SchemaEntity::getId, SchemaEntity::getName, 
            SchemaEntity::getCreateTime, SchemaEntity::getUpdateTime);
        wrapper.select(DataSourceEntity::getExtendProperty);
        wrapper.eq(SchemaEntity::getDataSourceId, dsId);
        wrapper.leftJoin(DataSourceEntity.class, on -> on.eq(DataSourceEntity::getId, SchemaEntity::getDataSourceId));
        List<SchemaItemVo> result = selectJoinList(SchemaItemVo.class, wrapper).stream().map(item -> {
            if(AiShuUtil.isNotEmpty(JSONUtils.props2Map(item.getExtendProperty()))) {
                String vCatalogName = JSONUtils.props2Map(item.getExtendProperty()).get(DataSourceConstants.VCATALOGNAME);
                item.setExtendProperty(vCatalogName);
            }
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long dsId, Long schemaId) {
        resourceCheck(dsId, schemaId);
        if (schemaMapper.deleteById(dsId, schemaId) <= 0) {
            // 资源被使用
            throw new AiShuException(ErrorCodeEnum.DeleteNotAllowed);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long dsId, Long schemaId, Long targetDSId, String schemaName) {
        try {
            LambdaUpdateWrapper<SchemaEntity> wrapper = resourceCheck(dsId, schemaId);
            DataSourceEntity ds = dataSourceMapper.selectById(targetDSId);
            if (ds == null) {
                // 资源不存在
                throw new AiShuException(ErrorCodeEnum.ParentResourceNotExisted);
            }

            SchemaEntity schema = new SchemaEntity();
            schema.setName(schemaName);
            schema.setDataSourceType(ds.getDataSourceType());
            schema.setDataSourceTypeName(ds.getDataSourceTypeName());
            schema.setDataSourceId(ds.getId());
            schema.setDataSourceName(ds.getName());
            schema.setUpdateTime(new Date());

            if (schemaMapper.update(schema, wrapper) <= 0) {
                // 资源不存在
                throw new AiShuException(ErrorCodeEnum.TargetParentResourceNotExisted);
            }

            LambdaUpdateWrapper<TableEntity> tw = new LambdaUpdateWrapper<>();
            TableEntity table = new TableEntity();
            table.setDataSourceType(ds.getDataSourceType());
            table.setDataSourceTypeName(ds.getDataSourceTypeName());
            table.setDataSourceId(ds.getId());
            table.setDataSourceName(ds.getName());
            table.setSchemaName(schemaName);
            tableMapper.update(table, tw.eq(TableEntity::getSchemaId, schemaId));
        } catch (Exception e) {
            // 判断是否为唯一键冲突，如果是则抛出schema名称冲突异常，否则直接抛出捕获的异常
            if (e instanceof DuplicateKeyException) {
                throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long dsId, String schemaName) {
        try {
            DataSourceEntity ds = dataSourceMapper.selectById(dsId);
            if (ds == null) {
                // 资源不存在
                throw new AiShuException(ErrorCodeEnum.ParentResourceNotExisted);
            }

            SchemaEntity schema = new SchemaEntity();
            Date currentDate = new Date();
            schema.setId(IdWorker.getId());
            schema.setName(schemaName);
            schema.setDataSourceType(ds.getDataSourceType());
            schema.setDataSourceTypeName(ds.getDataSourceTypeName());
            schema.setDataSourceId(ds.getId());
            schema.setDataSourceName(ds.getName());
            schema.setCreateTime(currentDate);
            schema.setUpdateTime(currentDate);

            schemaMapper.insert(schema);
        } catch (Exception e) {
            // 判断是否为唯一键冲突，如果是则抛出schema名称冲突异常，否则直接抛出捕获的异常
            if (e instanceof DuplicateKeyException) {
                throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
            } else {
                throw e;
            }
        }
    }

    private LambdaUpdateWrapper<SchemaEntity> resourceCheck(Long dsId, Long schemaId) {
        LambdaUpdateWrapper<SchemaEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SchemaEntity::getId, schemaId)
            .eq(SchemaEntity::getDataSourceId, dsId);
        if (!schemaMapper.exists(wrapper)) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        return wrapper;
    }

    @Override
    public List<Long> getUsedDataSource(Long... dsIds) {
        MPJLambdaWrapper<SchemaEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(SchemaEntity::getDataSourceId);
        wrapper.distinct();
        wrapper.in(SchemaEntity::getDataSourceId, Arrays.asList(dsIds));
        return selectJoinList(Long.class, wrapper);
    }

    @Override
    public Result<?> checkNameConflict(Long dsId, String schemaName) {
        LambdaQueryWrapper<SchemaEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchemaEntity::getDataSourceId, dsId);
        wrapper.eq(SchemaEntity::getName, schemaName);
        if (schemaMapper.exists(wrapper)) {
            return Result.success(true);
        }
        return Result.success(false);
    }
}
