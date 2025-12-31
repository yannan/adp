package com.eisoo.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.config.AnyFabricConfig;
import com.eisoo.config.SpringUtil;
import com.eisoo.entity.*;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.lineage.LineageUtil;
import com.eisoo.lineage.presto.HandlerSelectItemUtil;
import com.eisoo.lineage.presto.LineageDolphinColumn;
import com.eisoo.mapper.DolphinLineageMapper;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.service.ILineageService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.Constant;
import com.eisoo.util.DolphinLineageUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2025/1/7 15:45
 * @Version:1.0
 */
@Service
@Slf4j
public class DolphinLineageServiceImpl extends ServiceImpl<DolphinLineageMapper, ColumnLineageEntity> implements ILineageService {
    @Autowired
    private DolphinLineageMapper dolphinLineageMapper;
    @Autowired
    private TableLineageServiceImpl tableLineageServiceImpl;
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;
    @Autowired
    private RelationService relationService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer insertBatchEntityOrUpdate(List<? extends BaseLineageEntity> entityList) {
        return dolphinLineageMapper.insertBatchColumn((List<ColumnLineageEntity>) entityList);
    }

    @Override
    public Integer deleteBatchEntity(List<? extends BaseLineageEntity> entityList) {
        return null;
    }

    @Override
    public BaseLineageEntity selectById(String id) {
        return null;
    }

    @Override
    public List<? extends BaseLineageEntity> selectBatchIds(List<String> ids) {
        return null;
    }

    @Override
    public void deleteBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        log.warn("dolphin类型血缘实体没有delete事件......");
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) throws Exception {
        for (BaseLineageEntity entity : entityList) {
            DolphinEntity dolphinEntity = (DolphinEntity) entity;
            String targetTableId = dolphinEntity.getTargetTableId();
            String insertSql = dolphinEntity.getInsertSql();
            if (CommonUtil.isNotEmpty(targetTableId)) {
                syncDolphinService(dolphinEntity, true);
            }
            if (CommonUtil.isNotEmpty(insertSql)) {
                composeDolphinService(dolphinEntity, true);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) throws Exception {
        for (BaseLineageEntity entity : entityList) {
            DolphinEntity dolphinEntity = (DolphinEntity) entity;
            String targetTableId = dolphinEntity.getTargetTableId();
            String insertSql = dolphinEntity.getInsertSql();
            if (CommonUtil.isNotEmpty(targetTableId)) {
                syncDolphinService(dolphinEntity, true);
            }
            if (CommonUtil.isNotEmpty(insertSql)) {
                composeDolphinService(dolphinEntity, true);
            }
        }
    }
    /***
     * 离线初始化数据同步的血缘数据
     */
    public boolean initSyncDolphin() {
        final boolean[] result = {true};
        dolphinLineageMapper.selectDolphinSyncEntity(new ResultHandler<DolphinEntity>() {
            @Override
            public void handleResult(ResultContext<? extends DolphinEntity> resultContext) {
                DolphinEntity dolphinSyncEntity = resultContext.getResultObject();
                // sync类型不需要校验sql是否正确，因此下面注释掉
//                String id = dolphinSyncEntity.getId();
//                if (!checkSqlTask(id)) {
//                    return;
//                }
                try {
                    syncDolphinService(dolphinSyncEntity, false);
                } catch (Exception e) {
                    log.error("dolphin-数据同步的血缘数据向t_lineage_tag_column2表的同步失败", e);
                    result[0] = false;
                    resultContext.stop();
                }
            }
        });
        return result[0];
    }

    /***
     * 插入sync类型的血缘实体
     * @param dolphinSyncEntity：sync类型实体
     * @param needUpdateRelation：是否同时更新relation
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void syncDolphinService(DolphinEntity dolphinSyncEntity, boolean needUpdateRelation) throws Exception {
        String sourceTableId = dolphinSyncEntity.getSourceTableId();
        String targetTableId = dolphinSyncEntity.getTargetTableId();
        HashMap<String, Object> tableAndColumnsSource = getTableAndColumns(sourceTableId);
        HashMap<String, Object> tableAndColumnsTarget = getTableAndColumns(targetTableId);
        List<ColumnLineageEntity> fieldsTarget = (List<ColumnLineageEntity>) tableAndColumnsTarget.get("fields");
        List<ColumnLineageEntity> fieldsSource = (List<ColumnLineageEntity>) tableAndColumnsSource.get("fields");
        for (int i = 0; i < fieldsTarget.size(); i++) {
            ColumnLineageEntity target = fieldsTarget.get(i);
            ColumnLineageEntity source = fieldsSource.get(i);
            target.setExpressionName(Constant.SYNC);
            target.setColumnUniqueIds(source.getUniqueId());
        }
        insertBatchEntityOrUpdate(fieldsTarget);
        if (needUpdateRelation) {
            relationService.saveOrUpdateDolphinRelation(fieldsTarget);
        }
    }

    /***
     * 插入compose类型的血缘实体
     * @param dolphinComposeEntity：compose类型实体
     * @param needUpdateRelation：是否同时更新relation
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void composeDolphinService(DolphinEntity dolphinComposeEntity, boolean needUpdateRelation) throws Exception {
        String targetTableCreateSQL = dolphinComposeEntity.getCreateSql();
        String insertSql = dolphinComposeEntity.getInsertSql();
        //2,获取insert后面的select的字段
        HashMap<String, List<String>> map;
        try {
            //1,获取目标table的column:
            map = HandlerSelectItemUtil.getFieldListFromCreteSQL(targetTableCreateSQL);
        } catch (Exception e) {
            log.error("解析create table sql 失败！sql={}", targetTableCreateSQL, e);
            throw new Exception(e);
        }
        List<String> tableTargetInfo = map.get("table");
        List<String> fieldListTargetTable = map.get("column");
        if (tableTargetInfo == null || tableTargetInfo.size() != 3) {
            log.error("获取table信息失败！create sql:{}", targetTableCreateSQL);
            throw new Exception();
        }
        String targetCatalogName = tableTargetInfo.get(0);
        String targetSchemaName = tableTargetInfo.get(1);
        String targetTableName = tableTargetInfo.get(2);
        String composeSQL = insertSql.replaceAll("\\\\", "");
        //2,获取insert后面的select的字段:[name, id]
        List<String> selectItemList = null;
        try {
            selectItemList = HandlerSelectItemUtil.getSelectItemListFromSQL(composeSQL);
        } catch (URISyntaxException e) {
            log.error("获取insert后面的select的字段失败！sql:{}", composeSQL, e);
            throw new Exception(e);
        }
        //3,根据insert sql 获取血缘
        HashMap<String, ArrayList<LineageDolphinColumn>> columnLineageMap = null;
        try {
            columnLineageMap = LineageUtil.getLineageBySql(composeSQL);
        } catch (URISyntaxException e) {
            log.error("获取insert sql 获取血缘失败！sql:{}", composeSQL, e);
            throw new Exception(e);
        }
        for (int i = 0; i < selectItemList.size(); i++) {
            String targetColumnName = fieldListTargetTable.get(i);
            // 存放依赖的列的uid
            HashSet<String> columnUniqueIds = new HashSet();
            // 存放依赖的列的expression
            HashSet<String> expressions = new HashSet<>();
            String columnNameSelect = selectItemList.get(i);
            //获取每一个字段的血缘信息
            ArrayList<LineageDolphinColumn> lineagesPerTargetItem = columnLineageMap.get(columnNameSelect);
            // 解析每一个依赖的列的uid和expression
            List<TableLineageEntity> tableLineageEntities = new LinkedList<>();
            LinkedList<ColumnLineageEntity> columnLineageEntities = new LinkedList<>();
            for (int j = 0; j < lineagesPerTargetItem.size(); j++) {
                LineageDolphinColumn lineageDolphinColumnOne = lineagesPerTargetItem.get(j);
                String uniqueIdColumn = DolphinLineageUtil.getMD5FromLineageColumn(lineageDolphinColumnOne);
                columnUniqueIds.add(uniqueIdColumn);
                expressions.add(lineageDolphinColumnOne.getExpression());
                // 依赖的列的table信息
                String sourceCatAndDbName = lineageDolphinColumnOne.getSourceDbName();
                String sourceTableName = lineageDolphinColumnOne.getSourceTableName();
                String sourceColumnName = lineageDolphinColumnOne.getTargetColumnName();
                // 这里补充信息
                String md5FromLineageTable = DolphinLineageUtil.getMD5FromLineageTable(lineageDolphinColumnOne);
                TableLineageEntity tableEntity = (TableLineageEntity) tableLineageServiceImpl.selectById(md5FromLineageTable);
                if (tableEntity == null) {
                    tableEntity = addTableLineageEntity(sourceCatAndDbName, sourceTableName);
                    tableLineageEntities.add(tableEntity);
                }
                ColumnLineageEntity columnEntity = (ColumnLineageEntity) columnLineageServiceImpl.selectById(uniqueIdColumn);
                if (columnEntity == null) {
                    columnEntity = addColumnLineageEntity(sourceCatAndDbName, sourceTableName, sourceColumnName);
                    columnLineageEntities.add(columnEntity);
                }
            }
            // 需要补充的信息插入到db
            if (!tableLineageEntities.isEmpty()) {
                // 说明是实时的任务
                if (needUpdateRelation) {
                    tableLineageServiceImpl.insertBatchEntityAndLog(tableLineageEntities, Constant.TABLE);
                } else {
                    tableLineageServiceImpl.insertBatchEntityOrUpdate(tableLineageEntities);
                }
            }
            if (!columnLineageEntities.isEmpty()) {
                if (needUpdateRelation) {
                    columnLineageServiceImpl.insertBatchEntityAndLog(columnLineageEntities, Constant.COLUMN);
                } else {
                    columnLineageServiceImpl.insertBatchEntityOrUpdate(columnLineageEntities);
                }
            }
            // 2,处理结果
            String uniqueIdTable = com.eisoo.util.LineageUtil.makeMD5(targetCatalogName, targetSchemaName, targetTableName);
            TableLineageEntity table = (TableLineageEntity) tableLineageServiceImpl.selectById(uniqueIdTable);
            // 2.1,处理依赖的table
            if (table == null) {
                table = addTableLineageEntity(targetCatalogName + "." + targetSchemaName, targetTableName);
                List<TableLineageEntity> list = new LinkedList<>();
                list.add(table);
                if (needUpdateRelation) {
                    tableLineageServiceImpl.insertBatchEntityAndLog(list, Constant.TABLE);
                } else {
                    tableLineageServiceImpl.insertBatchEntityOrUpdate(list);
                }
                log.info("成功补充table血缘数据：table={}", table);
            }
            // 2.2,处理column
            String uniqueId = com.eisoo.util.LineageUtil.makeMD5(targetCatalogName,
                                                                 targetSchemaName,
                                                                 targetTableName,
                                                                 targetColumnName);
            ColumnLineageEntity column = (ColumnLineageEntity) columnLineageServiceImpl.selectById(uniqueId);
            if (column == null) {
                column = addColumnLineageEntity(targetCatalogName + "." + targetSchemaName,
                                                targetTableName,
                                                targetColumnName);
            }
            String ids = column.getColumnUniqueIds();
            String expression = column.getColumnUniqueIds();
            if (CommonUtil.isEmpty(ids)) {
                column.setColumnUniqueIds(StringUtils.join(columnUniqueIds, Constant.GLOBAL_SPLIT_COMMA));
                column.setExpressionName(StringUtils.join(expressions, Constant.GLOBAL_SPLIT_COMMA));
            } else {
                Set<String> set = Arrays.stream(ids.split(Constant.GLOBAL_SPLIT_COMMA)).collect(Collectors.toSet());
                set.addAll(columnUniqueIds);
                column.setColumnUniqueIds(StringUtils.join(set, Constant.GLOBAL_SPLIT_COMMA));
                Set<String> set2 = Arrays.stream(expression.split(Constant.GLOBAL_SPLIT_COMMA)).collect(Collectors.toSet());
                set2.addAll(expressions);
                column.setExpressionName(StringUtils.join(set2, Constant.GLOBAL_SPLIT_COMMA));
            }
            // 2.3,处理column
            List<ColumnLineageEntity> result = new LinkedList<>();
            result.add(column);
            insertBatchEntityOrUpdate(result);
            if (needUpdateRelation) {
                relationService.saveOrUpdateDolphinRelation(result);
            }
        }
    }

    /***
     * 离线初始化数据加工的血缘数据
     */
    public boolean initComposeDolphin() {
        final boolean[] result = {true};
        dolphinLineageMapper.selectDolphinComposeEntity(new ResultHandler<DolphinEntity>() {
            @Override
            public void handleResult(ResultContext<? extends DolphinEntity> resultContext) {
                DolphinEntity dolphinComposeEntity = resultContext.getResultObject();
                String id = dolphinComposeEntity.getId();
                if (!checkSqlTask(id)) {
                    result[0] = false;
                    resultContext.stop();
                }
                try {
                    composeDolphinService(dolphinComposeEntity, false);
                } catch (Exception e) {
                    log.error("dolphin-数据加工的血缘数据向t_lineage_tag_column2表的同步失败", e);
                    result[0] = false;
                    resultContext.stop();
                }
            }
        });
        return result[0];
    }

    /***
     * 获取table和column的血缘信息
     * @param tableId：table的id
     * @return：table和column的血缘信息
     */
    public HashMap<String, Object> getTableAndColumns(String tableId) throws Exception {
        HashMap<String, Object> result = new HashMap<>();
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String tableColumnInfoLineageUrl = anyFabricConfig.getTableColumnInfoLineageUrl();
        String url = String.format(tableColumnInfoLineageUrl, tableId);
        String response = HttpUtil.executeGet(url, null);
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (!jsonObject.containsKey("table") || !jsonObject.containsKey("fields")) {
                log.error("获取table&column失败！url={}", url);
                throw new Exception();
            }
            TableLineageEntity tableEntity = jsonObject.getJSONObject("table").toJavaObject(TableLineageEntity.class);
            List<ColumnLineageEntity> fields = jsonObject.getJSONArray("fields").toJavaList(ColumnLineageEntity.class);
            result.put("table", tableEntity);
            result.put("fields", fields);
        } catch (Exception e) {
            log.error("获取table&column失败！url={}", url, e);
            throw new Exception(e);
        }
        return result;
    }

    /***
     *  校验执行sql是否正确
     * @param id：任务id
     * @return：是否正确
     */
    private boolean checkSqlTask(String id) {
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String tableColumnInfoLineageUrl = anyFabricConfig.getCheckTaskLineageUrl();
        String url = String.format(tableColumnInfoLineageUrl, id);
        String response = HttpUtil.executeGet(url, null);
        boolean result = false;
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            String code = jsonObject.getString("code");
            if ("0".equals(code)) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data != null && data.getInteger("total") > 0) {
                    String status = data.getJSONArray("total_list").getJSONObject(0).getString("status");
                    if ("SUCCESS".equals(status)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("check任务失败，返回false", e);
        }
        return result;
    }

    /**
     * 从af获取table信息
     *
     * @param sourceCatAndDbName：catalog和schema信息
     * @param sourceTableName：                    table信息
     * @return TableLineageEntity
     */
    private TableLineageEntity addTableLineageEntity(String sourceCatAndDbName, String sourceTableName) {
        TableLineageEntity tableEntity;
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String tableColumnInfoLineageUrl = anyFabricConfig.getTableColumnInfoLineageUrl();
        String url = String.format(tableColumnInfoLineageUrl, sourceCatAndDbName + "." + sourceTableName);
        String response = HttpUtil.executeGet(url, null);
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (!jsonObject.containsKey("table")) {
                log.error("获取table失败！url={}", url);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
            tableEntity = jsonObject.getJSONObject("table").toJavaObject(TableLineageEntity.class);
        } catch (Exception e) {
            log.error("获取table失败！url={}", url, e);
            throw new AiShuException(ErrorCodeEnum.UnKnowException);
        }
        return tableEntity;
    }

    /***
     * 从af获取column信息
     * @param sourceCatAndDbName:catalog和schema信息
     * @param sourceTableName:table信息
     * @param sourceColumnName：column信息
     * @return ColumnLineageEntity
     */
    private ColumnLineageEntity addColumnLineageEntity(String sourceCatAndDbName, String sourceTableName, String sourceColumnName) {
        ColumnLineageEntity columnEntity;
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String tableColumnInfoLineageUrl = anyFabricConfig.getTableColumnInfoLineageUrl();
        String url = String.format(tableColumnInfoLineageUrl, sourceCatAndDbName + "." + sourceTableName + "." + sourceColumnName);
        String response = HttpUtil.executeGet(url, null);
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (!jsonObject.containsKey("fields")) {
                log.error("获取column失败！url={}", url);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
            JSONArray entities = jsonObject.getJSONArray("fields");
            columnEntity = entities.getJSONObject(0).toJavaObject(ColumnLineageEntity.class);
        } catch (Exception e) {
            log.error("获取 column失败！url={}", url, e);
            throw new AiShuException(ErrorCodeEnum.UnKnowException);
        }
        return columnEntity;
    }
}
