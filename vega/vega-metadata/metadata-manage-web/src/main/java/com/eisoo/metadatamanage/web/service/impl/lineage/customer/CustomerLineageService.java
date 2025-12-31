package com.eisoo.metadatamanage.web.service.impl.lineage.customer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.eisoo.entity.*;
import com.eisoo.metadatamanage.db.mapper.DataSourceMapper;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualConnectorListDto;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.impl.DipDataSourceService;
import com.eisoo.metadatamanage.web.service.KafkaProduceWithTransaction;
import com.eisoo.service.impl.ColumnLineageServiceImpl;
import com.eisoo.service.impl.TableLineageServiceImpl;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.Constant;
import com.eisoo.util.LineageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/20 20:40
 * @Version:1.0
 */
@Service
@Slf4j
public class CustomerLineageService {
    @Autowired
    private TableLineageServiceImpl tableLineageServiceImpl;
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;
    @Autowired(required = false)
    DataSourceMapper dataSourceMapper;
    @Autowired
    private KafkaProduceWithTransaction kafkaProduceWithTransaction;
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Autowired
    private DipDataSourceService dipDataSourceService;

    /***
     * 根据数据源信息获取catalog schema信息
     * @param typeDb 数据源类型,例如 mysql clickhouse
     * @param host IP地址
     * @param port 端口
     * @param databaseName 数据库
     * @return
     */
    private List<HashMap<String, String>> getAFInfo(String typeDb, String host, int port, String databaseName) {
        List<String> dataSource = dataSourceMapper.getDataSourceList(typeDb.toUpperCase(), host, port, databaseName);
        if (dataSource == null || dataSource.size() == 0) {
            return null;
        }
        List<HashMap<String, String>> result = new ArrayList<>(dataSource.size());
        for (String extendProperty : dataSource) {
            HashMap<String, String> catalogMap = new HashMap<>();
            if (extendProperty != null) {
                if (LineageUtil.isNotEmpty(extendProperty)) {
                    // currentSchema=dmw&vCatalogName=mysql_letb2t2n&vConnector=mysql&
                    String[] split = extendProperty.split("&");
                    for (String info : split) {
                        if (info.contains("=")) {
                            String[] keyVaule = info.split("=");
                            catalogMap.put(keyVaule[0], keyVaule[1]);
                        } else {
                            return null;
                        }
                    }
                }
                result.add(catalogMap);
            }
        }
        return result;
    }

    /***
     * 获取table类型的血缘数据
     * @param list 三方数据
     * @param isDataWareHouse 是否是etl来源
     * @return
     */
    private List<BaseLineageEntity> getEtlTabLineageFromCustomer(List<CustomerTableETL> list, boolean isDataWareHouse, String actionType) {
        List<BaseLineageEntity> result = new ArrayList<>(list.size());
        for (CustomerTableETL customer : list) {
            String typeDb = customer.getType();
            String host = customer.getHost();
            Integer port = customer.getPort();
            String databaseName = customer.getDatabaseName();
            String tableName = customer.getTableName();
            String comment = customer.getComment();
            String schemaNameOrigin = customer.getSchemaName();
            // 新增字段：表加工任务的相关名称
            String taskExecutionInfo = customer.getTaskExecutionInfo();
            String catalogName = "";
            String schemaName = databaseName;
            // 获取AF的数据源信息
            List<HashMap<String, String>> afInfoList = getAFInfo(typeDb, host, port, databaseName);
            // table的主键
            String tableUniqueId = LineageUtil.makeMD5(typeDb, host, port + "", schemaName, tableName);
            // 传过来的schemaName 是否是正确的
            boolean schemaNameOriginIsOk = false;
            if (afInfoList == null || afInfoList.isEmpty()) {
                if (isDataWareHouse) {
                    String errorMsg = "数据源:Dbtype:%s;host:%s;port:%s;databaseName:%s,在AF中不存在！";
                    errorMsg = String.format(errorMsg, typeDb, host, port, databaseName);
                    throw new AiShuException(ErrorCodeEnum.UnKnowException, errorMsg, "请检查数据格式是否符合规范");
                }
            } else {
                for (HashMap<String, String> m : afInfoList) {
                    catalogName = m.get("vCatalogName");
                    schemaName = m.get("currentSchema");
                    if (schemaName.equals(schemaNameOrigin)) {
                        schemaNameOriginIsOk = true;
                        break;
                    }
                }
            }
            if (isDataWareHouse) {
                // schemaName 在AF中不存在，报错！
                if (!schemaNameOriginIsOk) {
                    String data = JSON.toJSONString(customer);
                    log.error("schema:" + schemaNameOrigin + "在AF中不存在！data:" + data);
                    throw new AiShuException(ErrorCodeEnum.UnKnowException, "schema:" + schemaNameOrigin + "在AF中不存在！", "请检查数据格式是否符合规范:" + data);
                } else {
                    tableUniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName);
                }
            } else {
                if (schemaNameOriginIsOk) {
                    tableUniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName);
                }
            }
            TableLineageEntity tableLineageEntity = null;
            if (isDataWareHouse) {
                BaseLineageEntity baseLineageEntity = tableLineageServiceImpl.selectById(tableUniqueId);
                if (null == baseLineageEntity) {
                    tableLineageEntity = new TableLineageEntity(tableUniqueId,
                                                                Constant.EXTERNAL_TABLE,
                                                                tableUniqueId,
                                                                catalogName,
                                                                schemaName,
                                                                tableName);
                    // postgres@10.4.71.29:5432
                } else {
                    tableLineageEntity = (TableLineageEntity) baseLineageEntity;
                    tableLineageEntity.setTableType(Constant.EXTERNAL_TABLE);
                }
                tableLineageEntity.setCatalogType(typeDb);
                tableLineageEntity.setBusinessName(tableName);
                tableLineageEntity.setComment(comment);
                tableLineageEntity.setCatalogAddr(host + ":" + port);
                tableLineageEntity.setTaskExecutionInfo(taskExecutionInfo);
                tableLineageEntity.setActionType(actionType);
                tableLineageEntity.setCreatedAt(customer.getCreatedAt());
                tableLineageEntity.setUpdatedAt(customer.getUpdatedAt());
                result.add(tableLineageEntity);
                continue;
            }
            tableLineageEntity = new TableLineageEntity(tableUniqueId,
                                                        Constant.EXTERNAL_TABLE,
                                                        tableUniqueId,
                                                        "",
                                                        schemaName,
                                                        tableName);
            tableLineageEntity.setCatalogType(typeDb);
            tableLineageEntity.setBusinessName(tableName);
            tableLineageEntity.setComment(comment);
            // postgres@10.4.71.29:5432
            tableLineageEntity.setCatalogAddr(host + ":" + port);
            tableLineageEntity.setTaskExecutionInfo(taskExecutionInfo);
            tableLineageEntity.setActionType(actionType);
            tableLineageEntity.setCreatedAt(customer.getCreatedAt());
            tableLineageEntity.setUpdatedAt(customer.getUpdatedAt());
            result.add(tableLineageEntity);
        }
        return result;
    }

    /***
     *  获取血缘实体
     * @param customer 三方血缘
     * @param isEtlType 是否是etl类型
     * @param throwException 是否是数仓类型
     * @return 血缘实体
     */
    private ColumnLineageEntity getEtlColumnLineageFromCustomer(CustomerColumnETL customer,
                                                                boolean isEtlType,
                                                                boolean throwException,
                                                                String actionType) {
        String typeDb = customer.getType();
        String host = customer.getHost();
        Integer port = customer.getPort();
        String columnEtlDeps = customer.getColumnEtlDeps();
        // 新增schema这个字段
        String schemaNameOrigin = customer.getSchemaName();
        String databaseName = customer.getDatabaseName();
        String tableName = customer.getTableName();
        String columnName = customer.getColumnName();
        Integer primaryKey = customer.getPrimaryKey();
        String comment = customer.getComment();
        String dataType = customer.getDataType();
        String expressionName = customer.getExpressionName();
        String catalogName = "";
        String schemaName = databaseName;
        List<HashMap<String, String>> afInfoList = getAFInfo(typeDb, host, port, databaseName);
        // 找不到schema的话使用下面的规则
        String tableUniqueId = LineageUtil.makeMD5(typeDb, host, port + "", schemaName, tableName);
        String uniqueId = LineageUtil.makeMD5(typeDb, host, port + "", schemaName, tableName, columnName);
        // 传过来的schemaName 是否是正确的
        boolean schemaNameOriginIsOk = false;
        if (afInfoList == null || afInfoList.isEmpty()) {
            // af数据源不存在抛异常
            if (throwException) {
                String errorMsg = "数据源:Dbtype:%s;host:%s;port:%s;databaseName:%s,在AF中不存在！";
                errorMsg = String.format(errorMsg, typeDb, host, port, databaseName);
                throw new AiShuException(ErrorCodeEnum.UnKnowException, errorMsg, "请检查数据格式是否符合规范");
            }
        } else {
            for (HashMap<String, String> m : afInfoList) {
                catalogName = m.get("vCatalogName");
                schemaName = m.get("currentSchema");
                if (schemaName.equals(schemaNameOrigin)) {
                    schemaNameOriginIsOk = true;
                    break;
                }
            }
        }
        if (throwException) {
            // schemaName 在AF中不存在，报错！
            if (!schemaNameOriginIsOk) {
                String data = JSON.toJSONString(customer);
                log.error("schema:" + schemaNameOrigin + "在AF中不存在！data:" + data);
                throw new AiShuException(ErrorCodeEnum.UnKnowException, "schema:" + schemaNameOrigin + "在AF中不存在！", "请检查数据格式是否符合规范:" + data);
            } else {
                tableUniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName);
                uniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName, columnName);
            }
        } else {
            if (schemaNameOriginIsOk) {
                tableUniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName);
                uniqueId = LineageUtil.makeMD5(catalogName, schemaName, tableName, columnName);
            }
        }
        ColumnLineageEntity columnLineageEntity = new ColumnLineageEntity();
        columnLineageEntity.setUniqueId(uniqueId);
        columnLineageEntity.setUuid(uniqueId);
        // 元数据类型
        columnLineageEntity.setTechnicalName(columnName);
        columnLineageEntity.setBusinessName(columnName);
        columnLineageEntity.setComment(comment);
        columnLineageEntity.setTableUniqueId(tableUniqueId);
        columnLineageEntity.setPrimaryKey(primaryKey + "");
        columnLineageEntity.setDataType(dataType);
        columnLineageEntity.setActionType(actionType);
        columnLineageEntity.setCreatedAt(customer.getCreatedAt());
        columnLineageEntity.setUpdatedAt(customer.getUpdatedAt());
        if (isEtlType) {
            // [{url，库名，表名，列名},{url，库名2，表名2，列名2}]
            //  String tableUniqueId = LineageUtil.makeMD5(typeDb, host, databaseName, tableName);
            List<CustomerDepETLColumn> deps = JSONArray.parse(columnEtlDeps).toList(CustomerDepETLColumn.class);
            HashSet<String> depIds = new HashSet<>();
            for (CustomerDepETLColumn colInfo : deps) {
                String dbTypeDep = colInfo.getType();
                String hostAddrDep = colInfo.getHost();
                Integer portDep = colInfo.getPort();
                String userDep = colInfo.getUser();
                String dbDep = colInfo.getDatabaseName();
                String schemaDepOrigin = colInfo.getSchemaName();
                String tabDep = colInfo.getTableName();
                String colDep = colInfo.getColumnName();
                String catalogNameDep = "";
                String schemaNameDep = dbDep;
                String uniqueIdDep = LineageUtil.makeMD5(dbTypeDep, hostAddrDep, portDep + "", dbDep, tabDep, colDep);
                // 传过来的schemaName 是否是正确的
                boolean schemaDepOriginIsOk = false;
                List<HashMap<String, String>> afInfoDep = getAFInfo(dbTypeDep, hostAddrDep, portDep, dbDep);
                if (afInfoDep == null || afInfoDep.isEmpty()) {
                    if (throwException) {
                        String error = dbTypeDep + ":" + hostAddrDep + ":" + portDep + ":" + userDep + ":" + dbDep;
                        throw new AiShuException(ErrorCodeEnum.UnKnowException, "数据源：" + error + "在AF中不存在！", "请检查数据格式是否符合规范");
                    }
                } else {
                    for (HashMap<String, String> dep : afInfoDep) {
                        catalogNameDep = dep.get("vCatalogName");
                        schemaNameDep = dep.get("currentSchema");
                        if (schemaNameDep.equals(schemaDepOrigin)) {
                            schemaDepOriginIsOk = true;
                            break;
                        }
                    }
                }
                if (throwException) {
                    if (!schemaDepOriginIsOk) {
                        String data = JSON.toJSONString(colInfo);
                        log.error("schema:" + schemaDepOrigin + "在AF中不存在！data:" + data);
                        throw new AiShuException(ErrorCodeEnum.UnKnowException, "schema:" + schemaDepOrigin + "在AF中不存在！", "请检查数据格式是否符合规范:data:" + data);
                    } else {
                        uniqueIdDep = LineageUtil.makeMD5(catalogNameDep, schemaNameDep, tabDep, colDep);
                    }
                } else {
                    if (schemaDepOriginIsOk) {
                        uniqueIdDep = LineageUtil.makeMD5(catalogNameDep, schemaNameDep, tabDep, colDep);
                    }
                }
                depIds.add(uniqueIdDep);
            }
            columnLineageEntity.setColumnUniqueIds(String.join(Constant.GLOBAL_SPLIT_COMMA, depIds));
            columnLineageEntity.setExpressionName(expressionName);
        }
        // 检测字段依赖的table是否存在
        BaseLineageEntity baseLineageEntity = tableLineageServiceImpl.selectById(columnLineageEntity.getTableUniqueId());
        if (null == baseLineageEntity) {
            String columnLineageString = JSON.toJSONString(columnLineageEntity);
            throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                     "columnLineageEntity:" + columnLineageString + "依赖的table在不存在！",
                                     "请检查为何column依赖的table不存在");
        }
        return columnLineageEntity;
    }

    /***
     * 处理meta类型的table血缘数据
     * @param message：json数据
     */
    public void dealCustomerMetaTable(String message) {
        String type;
        String actionType;
        Integer dbType = 0;
        List<CustomerTableETL> tableList = null;
        JSONObject root;
        try {
            root = JSONObject.parse(message);
            type = root.getString("type");
            dbType = root.getInteger("db_type");
            actionType = root.getString("action_type");
            JSONArray entities = root.getJSONArray("entities");
            tableList = entities.toJavaList(CustomerTableETL.class);
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException, "发送数据格式错误：" + e, "请检查json数据格式是否满足规范");
        }
        Set<String> connectors = dipDataSourceService.getConnectors().getConnectors().stream()
                                               .map(VirtualConnectorListDto.VirtualConnectorDto::getOlkConnectorName)
                                               .collect(Collectors.toSet());
        for (CustomerTableETL customer : tableList) {
            String typeDb = customer.getType();
            if (LineageUtil.isEmpty(typeDb)) {
                throw new AiShuException(ErrorCodeEnum.UnKnowException, "发送数据格式错误：type不能是空", "请检查json数据格式是否满足规范");
            }
            if (!connectors.contains(typeDb)) {
                String s = connectors.stream().collect(Collectors.joining(","));
                throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                         "发送数据格式错误：typeDb=" + typeDb + "不在AF支持的connector之内：目前支持的connector如下：" + s,
                                         "请检查json数据格式是否满足规范");
            }
        }
        boolean isDataWareHouse = dbType == 0;
        List<BaseLineageEntity> result = getEtlTabLineageFromCustomer(tableList, isDataWareHouse, actionType);
        if (!result.isEmpty()) {
            switch (type) {
                case Constant.BATCH:
                    tableLineageServiceImpl.insertBatchEntityAndLog(result, Constant.EXTERNAL_TABLE);
                    break;
                case Constant.STREAM:

                    JSONObject content = new JSONObject();
                    content.put("class_name", Constant.EXTERNAL_TABLE);
                    content.put("entities", result);
                    content.put("type", actionType);
                    JSONObject payload = new JSONObject();
                    payload.put("content", content);
                    JSONObject kafkaMessage = new JSONObject();
                    kafkaMessage.put("payload", payload);
                    String messageKafka = JSON.toJSONString(kafkaMessage, JSONWriter.Feature.WriteMapNullValue);
                    log.info("发送kafka的table消息：{}", messageKafka);
                    kafkaProduceWithTransaction.sendMessage(anyDataGraphConfig.getKafkaTopic(), messageKafka);
                    break;
            }
        }
    }

    /***
     * 处理来自三方的column类型的血缘数据
     * @param message:处理meta类型的column血缘数据
     * @param isTaskType:是否是血缘依赖类型的标识
     */
    public void dealCustomerColumn(String message, boolean isTaskType) {
        String type;
        String actionType;
        Integer dbType;
        List<CustomerColumnETL> columnList;
        JSONObject root;
        try {
            root = JSONObject.parse(message);
            type = root.getString("type");
            dbType = root.getInteger("db_type");
            actionType = root.getString("action_type");
            JSONArray entities = root.getJSONArray("entities");
            columnList = entities.toJavaList(CustomerColumnETL.class);
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException, "发送数据格式错误：" + e, "请检查json数据格式是否满足规范");
        }
        // 校验数据源类型是否正确
        Set<String> connectors = dipDataSourceService.getConnectors().getConnectors().stream()
                                               .map(VirtualConnectorListDto.VirtualConnectorDto::getOlkConnectorName)
                                               .collect(Collectors.toSet());
        for (CustomerColumnETL customer : columnList) {
            String typeDb = customer.getType();
            if (LineageUtil.isEmpty(typeDb)) {
                throw new AiShuException(ErrorCodeEnum.UnKnowException, "发送数据格式错误：type不能是空", "请检查json数据格式是否满足规范");
            }
            if (!connectors.contains(typeDb)) {
                String s = String.join(",", connectors);
                throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                         "发送数据格式错误：typeDb=" + typeDb + "不在AF支持的connector之内：目前支持的connector如下：" + s,
                                         "请检查json数据格式是否满足规范");
            }
        }
        // 来自数仓&其它
        boolean throwException = dbType == 0;
        // 记录元数据类型
        List<ColumnLineageEntity> resultMeta = new ArrayList<>(columnList.size());
        // 记录etl类型
        List<ColumnLineageEntity> resultETL = new ArrayList<>(columnList.size());

        for (CustomerColumnETL customer : columnList) {
            ColumnLineageEntity col = getEtlColumnLineageFromCustomer(customer, isTaskType, throwException, actionType);
            if (isTaskType) {
                resultETL.add(col);
            } else {
                resultMeta.add(col);
            }
        }
        // batch&stream
        if (!resultMeta.isEmpty()) {
            switch (type) {
                case Constant.BATCH:
                    columnLineageServiceImpl.insertBatchEntityAndLog(resultMeta, Constant.EXTERNAL_COLUMN);
                    break;
                case Constant.STREAM:

                    JSONObject content = new JSONObject();
                    content.put("class_name", Constant.EXTERNAL_COLUMN);
                    content.put("entities", resultMeta);
                    content.put("type", actionType);

                    JSONObject payload = new JSONObject();
                    payload.put("content", content);
                    JSONObject kafkaMessage = new JSONObject();
                    kafkaMessage.put("payload", payload);
                    String messageKafka = JSON.toJSONString(kafkaMessage, JSONWriter.Feature.WriteMapNullValue);
                    log.info("发送kafka的table消息：{}", messageKafka);
                    kafkaProduceWithTransaction.sendMessage(anyDataGraphConfig.getKafkaTopic(), messageKafka);
                    break;
            }
        }
        if (!resultETL.isEmpty()) {
            switch (type) {
                case Constant.BATCH:
                    columnLineageServiceImpl.insertBatchEntityAndLog(resultETL, Constant.EXTERNAL_COLUMN);
                    break;
                case Constant.STREAM:
                    JSONObject content = new JSONObject();
                    content.put("class_name", Constant.EXTERNAL_RELATION_COLUMN);
                    content.put("entities", resultETL);
                    content.put("type", actionType);

                    JSONObject payload = new JSONObject();
                    payload.put("content", content);
                    JSONObject kafkaMessage = new JSONObject();
                    kafkaMessage.put("payload", payload);
                    String messageKafka = JSON.toJSONString(kafkaMessage, JSONWriter.Feature.WriteMapNullValue);
                    log.info("发送kafka的column消息：{}", messageKafka);
                    kafkaProduceWithTransaction.sendMessage(anyDataGraphConfig.getKafkaTopic(), messageKafka);
                    break;
            }
        }
    }
}