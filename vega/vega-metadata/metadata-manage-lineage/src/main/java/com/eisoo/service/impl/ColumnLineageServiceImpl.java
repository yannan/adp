package com.eisoo.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.config.AnyFabricConfig;
import com.eisoo.config.SpringUtil;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.entity.TableLineageEntity;
import com.eisoo.mapper.*;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.service.ILineageService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/13 13:21
 * @Version:1.0
 */
@Service
@Slf4j
public class ColumnLineageServiceImpl extends ServiceImpl<ColumnLineageMapper, ColumnLineageEntity> implements ILineageService {
    @Autowired
    private ColumnLineageMapper columnLineageMapper;
    @Autowired
    private ColumnFromComposeMapper columnFromComposeMapper;
    @Autowired
    private ColumnFromViewMapper columnFromViewMapper;
    @Autowired
    private ColumnFromLogicMapper columnFromLogicMapper;
    @Autowired
    private TableLineageMapper tableLineageMapper;
    @Autowired
    private RelationService relationService;
    @Autowired
    private LineageOpLogService lineageOpLogService;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertBatchEntityAndLog(List<? extends BaseLineageEntity> entityList, String type) {
        insertBatchEntityOrUpdate(entityList);
        lineageOpLogService.saveLineageDataToLog(type, Constant.INSERT, entityList);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateBatchEntityAndLog(List<? extends BaseLineageEntity> entityList, String type) {
        updateBathColumnDeps((List<ColumnLineageEntity>) entityList);
        lineageOpLogService.saveLineageDataToLog(type, Constant.INSERT, entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer insertBatchEntityOrUpdate(List<? extends BaseLineageEntity> entityList) {
        return columnLineageMapper.insertBatchSomeColumn((List<ColumnLineageEntity>) entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer deleteBatchEntity(List<? extends BaseLineageEntity> entityList) {
        return columnLineageMapper.deleteBatchIds(entityList);
    }

    @Override
    public BaseLineageEntity selectById(String id) {
        return columnLineageMapper.selectById(id);
    }

    @Override
    public List<? extends BaseLineageEntity> selectBatchIds(List<String> ids) {
        return columnLineageMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // 1,删除实体
        deleteBatchEntity(entityList);
        // 2,删除relation
        relationService.removeBatchEntityList(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        insertBatchEntityOrUpdate(entityList);
        relationService.insertBatchEntityList(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        insertBatchEntityOrUpdate(entityList);
    }

    public String getColumnList(String tableUuid) {
        String result = "";
        QueryWrapper<TableLineageEntity> queryWrapperTab = new QueryWrapper<>();
        queryWrapperTab.eq("uuid", tableUuid);
        TableLineageEntity tableLineageEntity = tableLineageMapper.selectOne(queryWrapperTab);
        if (null == tableLineageEntity) {
            return result;
        }
        QueryWrapper<ColumnLineageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_unique_id", tableLineageEntity.getUniqueId());
        List<ColumnLineageEntity> columnLineageEntities = columnLineageMapper.selectList(queryWrapper);
        if (columnLineageEntities != null) {
            result = columnLineageEntities.stream().map(ColumnLineageEntity::getUniqueId).collect(Collectors.joining(","));
        }
        return result;
    }

    public void truncateTable() {
        columnLineageMapper.truncateTable();
    }

    public void initInsertComposeColumn() {
        columnFromComposeMapper.insertBatchSomeColumn();
    }

    public void initInsertBatchViewColumn(Integer BATCH_SIZE) throws Exception {
        List<ColumnLineageEntity> COLUMN_LINEAGE_ENTITY_LIST = new ArrayList<>();
        final boolean[] result = {true};
        columnFromViewMapper.selectColumnLineageBatchFromView(new ResultHandler<ColumnLineageEntity>() {
            @Override
            public void handleResult(ResultContext<? extends ColumnLineageEntity> resultContext) {
                try {
                    ColumnLineageEntity resultObject = resultContext.getResultObject();
                    COLUMN_LINEAGE_ENTITY_LIST.add(resultObject);
                    if (COLUMN_LINEAGE_ENTITY_LIST.size() == BATCH_SIZE) {
                        Integer insertCount = insertBatchEntityOrUpdate(COLUMN_LINEAGE_ENTITY_LIST);
                        log.info("插入了view类型{}条", insertCount);
                        COLUMN_LINEAGE_ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    log.error("同步view类型column数据失败", e);
                    result[0] = false;
                    resultContext.stop();
                }
            }
        });
        if (result[0]) {
            if (!COLUMN_LINEAGE_ENTITY_LIST.isEmpty()) {
                Integer insertCount = insertBatchEntityOrUpdate(COLUMN_LINEAGE_ENTITY_LIST);
                log.info("插入了view类型{}条", insertCount);
                COLUMN_LINEAGE_ENTITY_LIST.clear();
            }
        } else {
            throw new Exception();
        }
    }

    public List<String> selectColumnList(ArrayList<String> uniqueIdList) {
        return columnLineageMapper.selectColumnList(uniqueIdList);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer updateBathColumnDeps(List<ColumnLineageEntity> entityList) {
        return columnLineageMapper.updateColumnDeps(entityList);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer removeBatchColumnDeps(List<ColumnLineageEntity> entityList) {
        return columnLineageMapper.removeBatchColumnDeps(entityList);
    }

    public void initInsertLogicColumn(Integer batchSize) throws Exception {
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String dataLineageUrl = anyFabricConfig.getDataLineageUrl() + "?table_name=form_view_field&id=%s";
        List<ColumnLineageEntity> ENTITY_LIST = new ArrayList<>(batchSize);
        final boolean[] result = {true};

        columnFromLogicMapper.selectColumnIdBatchFromLogic(new ResultHandler<String>() {
            @Override
            public void handleResult(ResultContext<? extends String> resultContext) {
                try {
                    String id = resultContext.getResultObject();
                    String url = String.format(dataLineageUrl, id);
                    String response = HttpUtil.executeGet(url, null);
                    JSONObject jsonObject = JSONObject.parseObject(response);
                    if (!jsonObject.containsKey("type")) {
                        log.error("id={}的column不存在，无法解析！", id);
                        throw new Exception();
                    }
                    JSONArray entities = jsonObject.getJSONArray("entities");
                    JSONObject data = entities.getJSONObject(0);
                    data.put("type", "insert");
                    ColumnLineageEntity column = data.toJavaObject(ColumnLineageEntity.class);
                    ENTITY_LIST.add(column);
                    if (ENTITY_LIST.size() == batchSize) {
                        columnLineageMapper.insertBatchSomeColumn(ENTITY_LIST);
                        ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    log.error("同步logic类型column数据失败", e);
                    result[0] = false;
                    resultContext.stop();
                }
            }
        });
        if (result[0]) {
            if (!ENTITY_LIST.isEmpty()) {
                columnLineageMapper.insertBatchSomeColumn(ENTITY_LIST);
                ENTITY_LIST.clear();
            }
            log.info("完成column的逻辑&自定义视图-血缘数据向t_lineage_tag_column2表的同步");
        } else {
            throw new Exception();
        }
    }
}
