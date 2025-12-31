package com.eisoo.metadatamanage.web.service.impl;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.eisoo.metadatamanage.db.entity.*;
import com.eisoo.metadatamanage.lib.dto.*;
import com.eisoo.metadatamanage.lib.enums.*;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.config.DolphinschedulerConfig;
import com.eisoo.metadatamanage.web.config.SchedulerConfig;
import com.eisoo.metadatamanage.web.configuration.VirtualizationConfiguration;
import com.eisoo.metadatamanage.web.extra.model.ConnectionParamObject;
import com.eisoo.metadatamanage.web.extra.model.DataSource;
import com.eisoo.metadatamanage.web.service.*;
import com.eisoo.metadatamanage.web.util.DataSourceUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.eisoo.standardization.common.threadpoolexecutor.MDCThreadPoolExecutor;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.eisoo.standardization.common.util.EnumUtil;
import com.eisoo.standardization.common.util.JsonUtil;
import com.eisoo.standardization.common.util.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.NullEventDataDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.quartz.CronExpression;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.druid.support.json.JSONUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eisoo.metadatamanage.db.mapper.DataSourceMapper;
import com.eisoo.metadatamanage.db.mapper.SchemaMapper;
import com.eisoo.metadatamanage.db.mapper.TableMapper;
import com.eisoo.metadatamanage.lib.vo.DataSourceVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceCatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceItemVo;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class DataSourceServiceImpl extends MPJBaseServiceImpl<DataSourceMapper, DataSourceEntity> implements IDataSourceService {
    @Autowired(required = false)
    DataSourceMapper dataSourceMapper;

    @Autowired(required = false)
    SchemaMapper schemaMapper;

    @Autowired(required = false)
    TableMapper tableMapper;

    @Autowired(required = false)
    IDictService dictService;

    @Autowired(required = false)
    @Lazy
    ISchemaService schemaService;

    @Autowired(required = false)
    ITableService tableService;

//    @Autowired(required = false)
//    ITableFieldService tableFieldService;

//    @Autowired(required = false)
//    ITableFieldHisService tableFieldHisService;

    @Autowired(required = false)
    @Lazy
    ITaskService taskService;

    @Autowired(required = false)
    ILiveDdlService liveDdlService;
    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DolphinschedulerConfig dolphinschedulerConfig;

    @Autowired
    VirtualizationConfiguration virtualizationConfiguration;

    @Autowired
    private SchedulerConfig schedulerConfig;

    private Map<String, BinaryLogClient> binaryLogClientMap = new HashMap<>();

    //元数据采集线程
    //ExecutorService fillMetaDataExecutorPool = Executors.newFixedThreadPool(10);
    ExecutorService fillMetaDataExecutorPool = MDCThreadPoolExecutor.newFixedThreadPool(10);

    @PostConstruct
    void checkBinlog() {
        LambdaQueryWrapper<DataSourceEntity> dataSourceEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDataSourceTypeName, "MySQL");
        dataSourceEntityLambdaQueryWrapper.ne(DataSourceEntity::getLiveUpdateStatus, DataSourceUpdateStatusEnum.IGNORE.getCode());
        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
        List<DataSourceEntity> liveUpdateMysqlDsList = list(dataSourceEntityLambdaQueryWrapper);
        if (AiShuUtil.isNotEmpty(liveUpdateMysqlDsList)) {
            liveUpdateMysqlDsList.forEach(dataSourceEntity -> {
                log.info("启动时检测是否开启binlog监听，当前数据源:{}, 已启动binlog监听器:{},检测结果:{}", dataSourceEntity, binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()), AiShuUtil.isEmpty(binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort())));
                if (AiShuUtil.isEmpty(binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()))) {
                    startMysqlBinlog(dataSourceEntity);
                } else if (!binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()).isConnected()) {
                    try {
                        Runnable listen = () -> {
                            try {
                                binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()).connect();
                                log.info("checkbinlog, binaryLogClientMap开启监听：{}", dataSourceEntity.getHost() + dataSourceEntity.getPort());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        };
                        Thread binaryLogListenThread = new Thread(listen);
                        binaryLogListenThread.start();
                    } catch (Exception e) {
                        log.info("binlog监听失败:{}", dataSourceEntity.getHost() + ":" + dataSourceEntity.getPort());
                    }
                } else {
                    log.info("binlog已监听，无需再启动:{}", dataSourceEntity.getHost() + ":" + dataSourceEntity.getPort());
                }
            });
        }
    }

    @Override
    public List<DataSourceCatagoryItemVo> getListForCatagory(Integer includeDeleted) {
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(!includeDeleted.equals(1), DataSourceEntity::getDeleteCode, 0);
        wrapper.select(DataSourceEntity::getDataSourceType, DataSourceEntity::getId, DataSourceEntity::getName, DataSourceEntity::getExtendProperty);
        wrapper.orderByAsc(DataSourceEntity::getDataSourceType, DataSourceEntity::getId);


        List<DataSourceCatagoryItemVo> result = selectJoinList(DataSourceCatagoryItemVo.class, wrapper).stream().map(item -> {
            if (AiShuUtil.isNotEmpty(com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(item.getExtendProperty()))) {
                String vCatalogName = com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(item.getExtendProperty()).get(DataSourceConstants.VCATALOGNAME);
                item.setExtendProperty(vCatalogName);
            }
            return item;
        }).collect(Collectors.toList());
        return result;
    }

    @Override
    public boolean isExisted(Long id) {
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(DataSourceEntity::getId, id);
        wrapper.eq(DataSourceEntity::getDeleteCode, 0);
        return dataSourceMapper.exists(wrapper);
    }

    @Override
    public Result<List<DataSourceItemVo>> getList(Integer enableStatus, Integer connectStatus, Integer includeDeleted, Integer dataSourceType, String keyword, Integer offset, Integer limit, String sort, String direction) {
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(DataSourceEntity.class);
        wrapper.eq(dataSourceType != null, DataSourceEntity::getDataSourceType, dataSourceType);
        wrapper.eq(enableStatus != null, DataSourceEntity::getEnableStatus, enableStatus);
        wrapper.eq(connectStatus != null, DataSourceEntity::getConnectStatus, connectStatus);
        wrapper.eq(!includeDeleted.equals(1), DataSourceEntity::getDeleteCode, 0);
        wrapper.like(keyword != null, DataSourceEntity::getName, keyword);
        boolean isAsc = direction.toLowerCase().equals("asc");
        if (sort.toLowerCase().equals("update_time")) {
            wrapper.orderBy(true, isAsc, DataSourceEntity::getUpdateTime);
        } else {
            wrapper.orderBy(true, isAsc, DataSourceEntity::getCreateTime);
        }

        Page<DataSourceEntity> pageObj = new Page<>(offset, limit);
        IPage<DataSourceEntity> retPage = page(pageObj, wrapper);
        IPage<DataSourceEntity> page = dataSourceMapper.selectJoinPage(retPage, DataSourceEntity.class, wrapper);
        List<DataSourceItemVo> data = new ArrayList<>();
        for (DataSourceEntity ds : page.getRecords()) {
            DataSourceItemVo dsi = new DataSourceItemVo();
            dsi.setDataSourceType(ds.getDataSourceType());
            dsi.setDataSourceTypeName(ds.getDataSourceTypeName());
            dsi.setId(ds.getId());
            dsi.setName(ds.getName());
            dsi.setDescription(ds.getDescription());
            dsi.setEnableStatus(ds.getEnableStatus());
            dsi.setConnectStatus(ds.getConnectStatus());
            dsi.setCreateTime(ds.getCreateTime());
            dsi.setUpdateTime(ds.getUpdateTime());
            dsi.setConfig("Host:" + ds.getHost() + ";端口:" + ds.getPort() + ";用户名:" + ds.getUserName() + ";扩展属性:" + ds.getExtendProperty());
            data.add(dsi);
        }
        return Result.success(data, page.getTotal());
    }

    @Override
    public Result<DataSourceVo> getDetail(Long id) {
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(DataSourceEntity.class);
        wrapper.eq(DataSourceEntity::getId, id);
        DataSourceVo ds = selectJoinOne(DataSourceVo.class, wrapper);
        if (ds == null) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        return Result.success(ds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DataSourceDTO params) {
        String dsTypeName = dictService.getDictValue((Integer) 1, params.getDataSourceType());
        if (dsTypeName == null) {
            // 未知的数据源类型
            throw new AiShuException(ErrorCodeEnum.InvalidParameter);
        }

        DataSourceEntity ds = new DataSourceEntity();
        ds.setDataSourceType(params.getDataSourceType());
        ds.setDataSourceTypeName(dsTypeName);
        ds.setId(String.valueOf(IdWorker.getId()));
        ds.setName(params.getName());
        ds.setDescription(params.getDescription() == null ? "" : params.getDescription());
        ds.setHost(params.getHost());
        ds.setPort(params.getPort());
        ds.setUserName(params.getUserName());
        ds.setPassword(params.getPassword());
        ds.setExtendProperty(params.getExtendProperty() == null ? "" : params.getExtendProperty());
        ds.setDatabaseName(params.getDatabaseName());
        try {
            dataSourceMapper.insert(ds);
        } catch (Exception e) {
            // 判断是否为唯一键冲突，如果是则抛出数据源名称冲突异常，否则直接抛出捕获的异常
            if (e instanceof DuplicateKeyException) {
                throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
            } else {
                throw e;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DataSourceDTO params) {
        String dsTypeName = dictService.getDictValue((Integer) 1, params.getDataSourceType());
        if (dsTypeName == null) {
            // 未知的数据源类型
            throw new AiShuException(ErrorCodeEnum.InvalidParameter);
        }

        try {
            LambdaUpdateWrapper<DataSourceEntity> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DataSourceEntity::getId, id);
            DataSourceEntity ds = new DataSourceEntity();
            ds.setDataSourceType(params.getDataSourceType());
            ds.setDataSourceTypeName(dsTypeName);
            ds.setName(params.getName());
            ds.setDescription(params.getDescription() == null ? "" : params.getDescription());
            ds.setHost(params.getHost());
            ds.setPort(params.getPort());
            ds.setUserName(params.getUserName());
            ds.setPassword(params.getPassword());
            ds.setExtendProperty(params.getExtendProperty() == null ? "" : params.getExtendProperty());
            ds.setDatabaseName(params.getDatabaseName());
            ds.setUpdateTime(new Date());

            if (dataSourceMapper.update(ds, wrapper) <= 0) {
                // 资源不存在
                throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
            }

            LambdaUpdateWrapper<SchemaEntity> sw = new LambdaUpdateWrapper<>();
            SchemaEntity schema = new SchemaEntity();
            schema.setDataSourceType(ds.getDataSourceType());
            schema.setDataSourceTypeName(ds.getDataSourceTypeName());
            schema.setDataSourceName(ds.getName());
            schemaMapper.update(schema, sw.eq(SchemaEntity::getDataSourceId, id));

            LambdaUpdateWrapper<TableEntity> tw = new LambdaUpdateWrapper<>();
            TableEntity table = new TableEntity();
            table.setDataSourceType(ds.getDataSourceType());
            table.setDataSourceTypeName(ds.getDataSourceTypeName());
            table.setDataSourceName(ds.getName());
            tableMapper.update(table, tw.eq(TableEntity::getDataSourceId, id));
        } catch (Exception e) {
            // 判断是否为唯一键冲突，如果是则抛出schema名称冲突异常，否则直接抛出捕获的异常
            if (e instanceof DuplicateKeyException) {
                throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
            } else {
                throw e;
            }
        }
    }

    private List<Long> getNotExistDataSourceList(List<Long> ids) {
        List<Long> notExistList = new ArrayList<>();
        notExistList.addAll(ids);
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(DataSourceEntity::getId);
        wrapper.in(DataSourceEntity::getId, ids);
        notExistList.removeAll(selectJoinList(Long.class, wrapper));
        return notExistList;
    }

    @Override
    public void updateEnableStatus(DataSourceStatusDTO params, List<Long> ids) {
        List<Long> notExistList = getNotExistDataSourceList(ids);

        // 获取代理对象，通过代理对象调拥有Transactional注解的方法，否则事务不会生效
        DataSourceServiceImpl proxyObj = (DataSourceServiceImpl) AopContext.currentProxy();
        proxyObj.updateEnableStatusProcess(params, ids);

        if (notExistList.size() > 0) {
            if (notExistList.size() < ids.size()) {
                // 部分成功部分失败
                throw new AiShuException(
                        ErrorCodeEnum.PartialFailure.getErrorCode(),
                        String.format(ErrorCodeEnum.PartialFailure.getErrorMsg(), ids.size() - notExistList.size(), notExistList.size()),
                        "不存在的数据源ID为" + JSONUtils.toJSONString(notExistList)
                );
            }
            // 所有删除失败，不可删除
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEnableStatusProcess(DataSourceStatusDTO params, List<Long> ids) {
        DataSourceEntity ds = new DataSourceEntity();
        ds.setEnableStatus(params.getEnableStatus());
        ds.setUpdateTime(new Date());

        LambdaUpdateWrapper<DataSourceEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(DataSourceEntity::getId, ids);
        dataSourceMapper.update(ds, wrapper);
    }

    @Override
    public Result<?> delete(List<Long> ids) {
        List<Long> notExistList = getNotExistDataSourceList(ids);
        List<Long> delFailedIDs = deleteProcess(ids);

        int failedCount = delFailedIDs.size() + notExistList.size();
        if (failedCount > 0) {
            String delFailedErr = delFailedIDs.size() > 0 ? "不可删除数据源ID为" + JSONUtils.toJSONString(delFailedIDs) : "";
            String notExistErr = notExistList.size() > 0 ? "不存在数据源ID为" + JSONUtils.toJSONString(notExistList) : "";

            if (failedCount < ids.size()) {
                // 部分成功部分失败
                throw new AiShuException(
                        ErrorCodeEnum.PartialFailure.getErrorCode(),
                        String.format(ErrorCodeEnum.PartialFailure.getErrorMsg(), ids.size() - failedCount, failedCount),
                        notExistErr + " " + delFailedErr
                );
            }

            if (failedCount == notExistList.size()) {
                throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
            } else if (failedCount == delFailedIDs.size()) {
                throw new AiShuException(ErrorCodeEnum.DeleteNotAllowed);
            } else {
                throw new AiShuException(ErrorCodeEnum.DeleteFailed.getErrorCode(), ErrorCodeEnum.DeleteFailed.getErrorMsg(), notExistErr + " " + delFailedErr);
            }
        }

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Long> deleteProcess(List<Long> ids) {
        List<Long> delFailedIDs;
        dataSourceMapper.deleteByIDs(ids);
        MPJLambdaWrapper<DataSourceEntity> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(DataSourceEntity::getId);
        wrapper.in(DataSourceEntity::getId, ids);
        wrapper.orderByAsc(DataSourceEntity::getId);
        delFailedIDs = selectJoinList(Long.class, wrapper);
        return delFailedIDs;
    }

    @Override
    public Result<?> checkNameConflict(Integer dataSourceType, String dataSourceName) {
        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceEntity::getDataSourceType, dataSourceType);
        wrapper.eq(DataSourceEntity::getName, dataSourceName);
        if (dataSourceMapper.exists(wrapper)) {
            return Result.success(true);
        }
        return Result.success(false);
    }

    @Override
    @Transactional
    public Result<?> fillMetaData(Long dsid) {
        DataSourceEntity dataSourceEntity = getById(dsid);
        if (AiShuUtil.isEmpty(dataSourceEntity)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        fillMetaDataExecutorPool.submit(() -> taskService.fillMetaDataExec(dataSourceEntity, null));
        return Result.success("采集任务已提交，请耐心等待后台运行完成");
    }

    @Override
    @Transactional
    public Result<?> fillMetaData(FillMetaDataDTO fillMetaDataDTO) {
        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(DataSourceEntity::getName, StringUtil.escapeSqlSpecialChars(fillMetaDataDTO.getName()));
        List<DataSourceEntity> dataSourceEntityList = list(wrapper);
        if (AiShuUtil.isEmpty(dataSourceEntityList)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        StringBuffer ds = new StringBuffer();
        dataSourceEntityList.forEach(dataSourceEntity -> {
            ds.append("[");
            ds.append(dataSourceEntity.getName());
            ds.append("]");
            fillMetaDataExecutorPool.submit(() -> taskService.fillMetaDataExec(dataSourceEntity, null));
        });
        return Result.success("采集任务已提交，请耐心等待后台运行完成");
    }


    @Override
    public DataSource getDataSource(DataSourceEntity dataSourceEntity) {
        DataSource dataSource = new DataSource();
        log.info("dataSourceEntity.getDataSourceTypeName().toLowerCase():" + dataSourceEntity.getDataSourceTypeName().toLowerCase());
        log.info("EnumUtil.getEnumObject(DbType.class , s -> s.getDescp().equals(dataSourceEntity.getDataSourceTypeName().toLowerCase())).get():" + EnumUtil.getEnumObject(DbType.class, s -> s.getDescp().equals(dataSourceEntity.getDataSourceTypeName().toLowerCase())).get());
        dataSource.setType(EnumUtil.getEnumObject(DbType.class, s -> s.getDescp().equals(dataSourceEntity.getDataSourceTypeName().toLowerCase())).get());
        ConnectionParamObject connectionParamObject = new ConnectionParamObject();
        connectionParamObject.setUser(dataSourceEntity.getUserName());

        String pwd = PasswordUtils.decodePasswordRSA(dataSourceEntity.getPassword());
        connectionParamObject.setPassword(pwd);

        LambdaQueryWrapper<DictEntity> dictEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictType, 16);
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictKey, dataSourceEntity.getDataSourceType());
        String jdbcPre = dictService.getOne(dictEntityLambdaQueryWrapper, false).getDictValue();
        connectionParamObject.setAddress(jdbcPre + dataSourceEntity.getHost() + ":" + dataSourceEntity.getPort());

        connectionParamObject.setDatabase(dataSourceEntity.getDatabaseName());
        dictEntityLambdaQueryWrapper.clear();
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictType, 15);
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictKey, dataSourceEntity.getDataSourceType());
        String driverClassName = dictService.getOne(dictEntityLambdaQueryWrapper, false).getDictValue();
        connectionParamObject.setDriverClassName(driverClassName);
        connectionParamObject.setJdbcUrl(getJdbc(dataSource.getType(), connectionParamObject));

        dictEntityLambdaQueryWrapper.clear();
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictType, 17);
        dictEntityLambdaQueryWrapper.eq(DictEntity::getDictKey, dataSourceEntity.getDataSourceType());
        String validationQuery = dictService.getOne(dictEntityLambdaQueryWrapper, false).getDictValue();
        connectionParamObject.setValidationQuery(validationQuery);

        if (AiShuUtil.isNotEmpty(dataSourceEntity.getExtendProperty())) {
            connectionParamObject.setOther(dataSourceEntity.getExtendProperty().substring(0, dataSourceEntity.getExtendProperty().length() - 1));
            connectionParamObject.setProps(com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(dataSourceEntity.getExtendProperty()));
        }

        String connectionParamStr = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(connectionParamObject);
        log.info("connectionParamStr:{}", connectionParamStr);

        dataSource.setConnectionParams(connectionParamStr);
        dataSource.setConnectionParamObject(connectionParamObject);
        return dataSource;
    }

    @Override
    public Boolean MQHandle(ConsumerRecords<String, String> data) {
        // 开启事务
        Boolean rlt = transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    LambdaQueryWrapper<DictEntity> dictWrapper = new LambdaQueryWrapper<>();
                    dictWrapper.eq(DictEntity::getDictType, 1);
//                    Map<Integer, DictEntity> dictMap = SimpleQuery.keyMap(dictWrapper, DictEntity::getDictKey);
                    List<DictEntity> dictList = dictService.list(dictWrapper);
                    List<DataSourceEntity> saveList = new ArrayList<>();
                    List<Long> deleteList = new ArrayList<>();
                    data.forEach(item -> {
                                if (AiShuUtil.isNotEmpty(item.key()) && item.key().length() > 20) {
                                    DataSourceKafkaDTO dto = new DataSourceKafkaDTO();
                                    dto = JsonUtil.json2Obj(item.value(), dto.getClass());
                                    log.info("kafka-dto:" + dto);
                                    String method = dto.getHeader().getMethod();
                                    if (AiShuUtil.isNotEmpty(method) && (method.equals("create") || method.equals("update"))) {
                                        try {
                                            DataSourceEntity dataSourceEntity = getById(dto.getPayload().getData_source_id());
                                            dataSourceEntity = AiShuUtil.isEmpty(dataSourceEntity) ? new DataSourceEntity() : dataSourceEntity;
                                            dataSourceEntity.setId(String.valueOf(dto.getPayload().getData_source_id()));
                                            dataSourceEntity.setName(dto.getPayload().getName());
                                            if (AiShuUtil.isEmpty(dataSourceEntity.getName()) || !dataSourceEntity.getName().matches(Constants.REGEX_ENGLISH_CHINESE_UNDERLINE_BAR_128)) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "数据源名称不能为空，不能超过128个字符，不能为中英文、下划线、中划线以外的字符");
                                            }
                                            dataSourceEntity.setInfoSystemId(dto.getPayload().getInfoSystemId());
                                            if (AiShuUtil.isNotEmpty(dataSourceEntity.getInfoSystemId()) && dataSourceEntity.getInfoSystemId().length() > 128) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "信息系统ID不能超过128个字符");
                                            }

                                            DictEntity dictEntity = convertDsType(dto.getPayload().getType_name(), dto.getPayload().getType(), dictList);
                                            if (AiShuUtil.isEmpty(dictEntity)) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "未能匹配数据源的类型枚举");
                                            }
                                            dataSourceEntity.setDataSourceType(dictEntity.getDictKey());
                                            dataSourceEntity.setDataSourceTypeName(dictEntity.getDictValue());

                                            if (StringUtils.isNotBlank(dto.getPayload().getGuardian_token())) {
                                                dataSourceEntity.setUserName("guardian_token");
                                            } else {
                                                dataSourceEntity.setUserName(dto.getPayload().getUsername());
                                                if (AiShuUtil.isEmpty(dataSourceEntity.getUserName()) || dataSourceEntity.getUserName().length() > 128) {
                                                    throw new AiShuException(ErrorCodeEnum.InvalidParameter, "用户的名字不能为空或长度超过128");
                                                }
                                            }
                                            if (StringUtils.isNotBlank(dto.getPayload().getGuardian_token())) {
                                                dataSourceEntity.setPassword(dto.getPayload().getGuardian_token());
                                            } else {
                                                dataSourceEntity.setPassword(dto.getPayload().getPassword());
                                                if (AiShuUtil.isEmpty(dataSourceEntity.getPassword())) {
                                                    //|| !dataSourceEntity.getPassword().matches(Constants.REGEX_BASE64)
                                                    PasswordUtils.decodePasswordRSA(dataSourceEntity.getPassword());
                                                    throw new AiShuException(ErrorCodeEnum.InvalidParameter, "密码长度不能为空");
                                                }
                                            }

                                            dataSourceEntity.setHost(dto.getPayload().getHost());
                                            if (AiShuUtil.isEmpty(dataSourceEntity.getHost()) || dataSourceEntity.getHost().length() > 128) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "host不能为空或长度超过128");
                                            }
                                            dataSourceEntity.setPort(dto.getPayload().getPort());
                                            if (dto.getPayload().getPort() < 1 || dto.getPayload().getPort() > 65535) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "port取值应该在1-65535之间");
                                            }
                                            if (method.equals("create")) {
                                                dataSourceEntity.setCreateTime(new Date());
                                            }
                                            dataSourceEntity.setUpdateTime(new Date());
                                            dataSourceEntity.setCreateUser(DataSourceConstants.AF_CREATEUSER);
                                            dataSourceEntity.setUpdateUser(DataSourceConstants.AF_CREATEUSER);
                                            dataSourceEntity.setDatabaseName(dto.getPayload().getDatabase_name());
                                            StringBuilder builder = new StringBuilder();
                                            builder.append(String.format("currentSchema=%s&", dto.getPayload().getSchema()));
                                            builder.append(String.format("vCatalogName=%s&", dto.getPayload().getCatalog_name()));
                                            if (AiShuUtil.isNotEmpty(dto.getPayload().getType_name())) {
                                                builder.append(String.format("vConnector=%s&", dto.getPayload().getType_name()));
                                            }
                                            dataSourceEntity.setExtendProperty(builder.toString());
                                            if (AiShuUtil.isEmpty(dataSourceEntity.getDatabaseName()) || dataSourceEntity.getDatabaseName().length() > 100) {
                                                throw new AiShuException(ErrorCodeEnum.InvalidParameter, "dbname");
                                            }
                                            //同步数据源到dolphin,为了通过重名校验加上信息系统ID
//                                            if (method.equals("create")) {
//                                                createOnDolphin(dataSourceEntity);
//                                            }
//                                            if (method.equals("update")) {
//                                                updateOnDolphin(dataSourceEntity);
//                                            }
                                            saveList.add(dataSourceEntity);
                                        } catch (Exception e) {
                                            log.info("出错的dto是：{}", dto);
                                            log.error(e.toString(), e);
                                        }
                                    }

                                    if (AiShuUtil.isNotEmpty(method) && (method.equals("delete"))) {
                                        deleteList.add(dto.getPayload().getData_source_id());
                                    }
                                }
                            }
                    );
                    if (AiShuUtil.isNotEmpty(saveList)) {
                        saveOrUpdateBatch(saveList);
                        //实时采集状态切换
//                        DataSourceLiveUpdateStatusDto dataSourceLiveUpdateStatusDto = new DataSourceLiveUpdateStatusDto();
//                        dataSourceLiveUpdateStatusDto.setDatasourceId(String.valueOf(saveList.get(0).getId()));
//                        setliveUpdateStatus(dataSourceLiveUpdateStatusDto);
//                        dataSourceLiveUpdateStatusDto.setCommand("stop");
                        //执行元数据采集
//                        saveList.forEach(ds -> {
//                            FillMetaDataDTO fillDto = new FillMetaDataDTO();
//                            fillDto.setName(ds.getName());
//                            fillDto.setCreateUser(ds.getCreateUser());
//                            fillDto.setInfoSystemId(ds.getInfoSystemId());
//                            if (virtualizationConfiguration.getIsAuto()) {
//                                taskService.fillMetaDataByVirtual(fillDto);
//                            } else {
//                                taskService.fillMetaData(fillDto);
//                            }
//                        });
                    }
                    //同步删除消息时，执行逻辑删除
                    if (AiShuUtil.isNotEmpty(deleteList)) {
                        logicDelete(deleteList);
                    }
                    return true;
                } catch (Exception e) {
                    // 事务回滚
                    status.setRollbackOnly();
                    log.error(e.toString(), e);
                    return false;
                }
            }

//            private void deleteDataSource(List<Long> deleteList) {
//                try {
//                    delete(deleteList);
//                } catch (AiShuException e) {
//                    if (!e.getErrorCode().equals(ErrorCodeEnum.ResourceNotExisted.getErrorCode())) {
//                        throw e;
//                    }
//                }
//            }
        });

        return rlt;
    }

    @Override
    public Boolean MQDDLHandle(ConsumerRecords<String, String> data) {
        List<DataSourceEntity> dsAll = dataSourceMapper.selectList(new LambdaQueryWrapper<>());
        if (AiShuUtil.isEmpty(dsAll)) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "系统尚未注册任何数据源");
        }
        List<LiveDdlDto> ddlDtoList = new ArrayList<>();
        List<DataSourceEntity> dataSourceEntityList = new ArrayList<>();
        try {
            List<SchemaEntity> schemaEntityList = schemaService.list();
            data.forEach(item -> {
                        FileBeatDto dto = new FileBeatDto();
                        dto = JsonUtil.json2Obj(item.value(), dto.getClass());
                        String sql = dto.getMessage();
                        log.info("收到的原始消息是：{}", sql);
                        //检测日志采集器配置的数据源

                        String datasourceNameStr = dto.getFields().getDatasource();
                        if (StringUtils.isBlank(datasourceNameStr)) {
                            throw new AiShuException(ErrorCodeEnum.DATA_NOT_EXIST, "实时采集器未配置数据源名称");
                        }
                        String[] datasourceNameList = StringUtils.split(datasourceNameStr, ",");
                        Arrays.stream(datasourceNameList).forEach(name -> {
                            Boolean exist = false;
                            for (DataSourceEntity dataSourceEntity : dsAll) {
                                if (dataSourceEntity.getName().equals(name)) {
                                    if (exist) {
                                        throw new AiShuException(ErrorCodeEnum.Duplicated, "系统注册了多个相同的数据源名称：" + name);
                                    } else {
                                        dataSourceEntityList.add(dataSourceEntity);

                                        //若schema不存在实时更新schema
                                        Boolean existSchema = schemaEntityList.stream().anyMatch(schemaEntity -> schemaEntity.getDataSourceId().equals(dataSourceEntity.getId()));
                                        if (!existSchema) {
                                            taskService.updateSchemaByVirtual(dataSourceEntity);
                                            LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                                            schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getDataSourceId, dataSourceEntity.getId());
                                            SchemaEntity schema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                                            schemaEntityList.add(schema);
                                        }
                                        exist = true;
                                    }
                                }
                            }
                            if (!exist) {
                                throw new AiShuException(ErrorCodeEnum.DATA_NOT_EXIST, "系统未注册此数据源名称：" + name);
                            }
                        });


                        String dbtypeName = StringUtils.lowerCase(dataSourceEntityList.get(0).getDataSourceTypeName());
                        com.alibaba.druid.DbType dbType = com.alibaba.druid.DbType.of(dbtypeName);
                        String[] dsTypeSupported = {com.alibaba.druid.DbType.postgresql.name()};
                        if (AiShuUtil.isEmpty(dbType) || !Arrays.stream(dsTypeSupported).anyMatch(s -> s.equals(dbType.name()))) {
                            throw new AiShuException(ErrorCodeEnum.InternalError, "不支持实时采集的数据源：" + dbtypeName);
                        }

                        //日志消息转ddl
                        sql = StringUtils.substringAfter(sql, "CST");
                        sql = StringUtils.substringAfter(sql, ":");
                        sql = StringUtils.substringAfter(sql, ":");
                        Date now = new Date();
                        DdlLogDto ddlLogDto = new DdlLogDto();
                        ddlLogDto.setStatement(sql);
                        ddlLogDto.setDdlTime(now);
                        ddlDtoList.addAll(DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList));
                    }
            );

            //实时更新表及字段元数据
            if (AiShuUtil.isNotEmpty(ddlDtoList)) {
                taskService.updateTableByVirtual(dataSourceEntityList, ddlDtoList);
            }

            return true;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return false;
        }
    }

    @Override
    public void logicDelete(List<Long> ids) {
        List<DataSourceEntity> entities = listByIds(ids);
        if (AiShuUtil.isNotEmpty(entities)) {
            List<DataSourceEntity> logicDeleteList = entities.stream().map(ds -> {
                ds.setDeleteCode(ds.getId());
                return ds;
            }).collect(Collectors.toList());
            updateBatchById(logicDeleteList);
        }
    }

    @Override
    public List<DataSourceEntity> queryByIdList(List<Long> dsIdList) {
        return dataSourceMapper.selectBatchIds(dsIdList);
    }

    @Override
    public DataSourceLiveUpdateStatusDto setliveUpdateStatus(DataSourceLiveUpdateStatusDto liveUpdateStatusDto) {
        Boolean schedulerTaskFlag = false;
        StringBuilder schedulerTaskResult = new StringBuilder();
        schedulerTaskResult.append("请检查调度服务返回信息：");
        if (StringUtils.isEmpty(liveUpdateStatusDto.getCommand())) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "参数command错误！", "请检查：command不能为空");
        } else if (!Constants.COMMAND_STOP.equals(liveUpdateStatusDto.getCommand()) && !Constants.COMMAND_START.equals(liveUpdateStatusDto.getCommand())) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "参数command错误！", "请检查：command只能为start或stop");
        }
        if (StringUtils.isEmpty(liveUpdateStatusDto.getDatasourceId())) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "参数datasourceId错误！", "请检查：datasourceId不能为空");
        }
//        Long datasourceId = ConvertUtil.toLong(liveUpdateStatusDto.getDatasourceId());
        String datasourceId = liveUpdateStatusDto.getDatasourceId();
//        if (datasourceId == null || datasourceId <= 0l) {
//            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "参数datasourceId错误！", "请检查：datasourceId只能为Long型正整数");
//        }
        if (datasourceId == null) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "参数datasourceId错误！", "请检查：datasourceId只能为String型");
        }
        LambdaQueryWrapper<DataSourceEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DataSourceEntity::getId, datasourceId);
        lambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
        DataSourceEntity dataSourceEntity = getOne(lambdaQueryWrapper);
        Map<String, String> props = com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
        String vConnector = props.get("vConnector");
        if (!"hologres".equals(vConnector) && !"postgresql".equals(vConnector) && !"mysql".equals(vConnector)) {
            throw new AiShuException(ErrorCodeEnum.InternalError, "启动实时采集失败！", "请检查：目前只限hologres、postgresql、mysql数据源支持实时采集");
        }
        if (AiShuUtil.isEmpty(dataSourceEntity)) {
            throw new AiShuException(ErrorCodeEnum.DATA_NOT_EXIST, "数据不存在或已删除！", "请检查：datasourceId对应的数据源是否存在");
        } else {
            if (Constants.COMMAND_START.equals(liveUpdateStatusDto.getCommand())) {
                //删除尚未执行的停止任务
                LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, dataSourceEntity.getId());
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.STOP_TASK.getCode());
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
                if (count() > 0l) {
                    log.info("尚未执行的停止任务，予以中止，中止结果：{}", taskService.remove(taskEntityLambdaQueryWrapper));
                }
                //注册实时采集任务结束
                if (dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.IGNORE.getCode())) {
                    //尝试向调度注册实时采集任务
                    SchedulerTaskDto taskDto = new SchedulerTaskDto();
                    taskDto.setName(schedulerConfig.getTaskName());
                    taskDto.setUuid(schedulerConfig.getTaskUuid());
                    taskDto.setBase_task_type("Http");

                    List<AdvancedDTO> advancedDTOList = new ArrayList<>();

                    AdvancedDTO modelTypeDto = new AdvancedDTO();
                    modelTypeDto.setKey(Constants.KEY_MODEL_TYPE);
                    modelTypeDto.setValue("Api");
                    advancedDTOList.add(modelTypeDto);

                    AdvancedDTO httpMethodDto = new AdvancedDTO();
                    httpMethodDto.setKey(Constants.KEY_HTTP_METHOD);
                    httpMethodDto.setValue("POST");
                    advancedDTOList.add(httpMethodDto);

                    AdvancedDTO urlDto = new AdvancedDTO();
                    urlDto.setKey(Constants.KEY_URL);
                    urlDto.setValue(schedulerConfig.getTaskUrl());
                    advancedDTOList.add(urlDto);

                    AdvancedDTO delayTimeDto = new AdvancedDTO();
                    delayTimeDto.setKey(Constants.KEY_DELAY_TIME);
                    delayTimeDto.setValue("0");
                    advancedDTOList.add(delayTimeDto);

                    AdvancedDTO failRetryIntervalDto = new AdvancedDTO();
                    failRetryIntervalDto.setKey(Constants.KEY_FAIL_RETRY_INTERVAL);
                    failRetryIntervalDto.setValue("1");
                    advancedDTOList.add(failRetryIntervalDto);

                    AdvancedDTO failRetryTimesDto = new AdvancedDTO();
                    failRetryTimesDto.setKey(Constants.KEY_FAIL_RETRY_TIMES);
                    failRetryTimesDto.setValue("0");
                    advancedDTOList.add(failRetryTimesDto);

                    AdvancedDTO connectTimeoutDto = new AdvancedDTO();
                    connectTimeoutDto.setKey(Constants.KEY_CONNECT_TIMEOUT);
                    connectTimeoutDto.setValue("3000");
                    advancedDTOList.add(connectTimeoutDto);

                    AdvancedDTO socketTimeoutDto = new AdvancedDTO();
                    socketTimeoutDto.setKey(Constants.KEY_SOCKET_TIMEOUT);
                    socketTimeoutDto.setValue("3000");
                    advancedDTOList.add(socketTimeoutDto);

                    AdvancedDTO httpCheckConditionDto = new AdvancedDTO();
                    httpCheckConditionDto.setKey(Constants.KEY_HTTP_CHECK_CONDITION);
                    httpCheckConditionDto.setValue("STATUS_CODE_DEFAULT");
                    advancedDTOList.add(httpCheckConditionDto);

                    AdvancedDTO conditionDto = new AdvancedDTO();
                    conditionDto.setKey(Constants.KEY_CONDITION);
                    conditionDto.setValue("STATUS_CODE_DEFAULT");
                    advancedDTOList.add(conditionDto);
                    List<HttpParamDto> httpParamDtoList = new ArrayList<>();

                    HttpParamDto contentTypeParamDto = new HttpParamDto();
                    contentTypeParamDto.setProp("Content-Type");
                    contentTypeParamDto.setHttpParametersType("HEADERS");
                    contentTypeParamDto.setValue("application/json");
                    httpParamDtoList.add(contentTypeParamDto);

                    HttpParamDto authorizationParamDto = new HttpParamDto();
                    authorizationParamDto.setProp("Authorization");
                    authorizationParamDto.setHttpParametersType("HEADERS");
                    authorizationParamDto.setValue(schedulerConfig.getToken());
                    httpParamDtoList.add(authorizationParamDto);

                    AdvancedDTO httpParamsDto = new AdvancedDTO();
                    httpParamsDto.setKey(Constants.KEY_HTTP_PARAMS);
                    httpParamsDto.setValue(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(httpParamDtoList));
                    advancedDTOList.add(httpParamsDto);

                    taskDto.setAdvanced_params(advancedDTOList);

                    String taskDtoStr = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(taskDto);

                    String resposeStr = HttpUtil.executePostWithJson(schedulerConfig.getSchedulerModelUrl(), taskDtoStr, null);
                    log.info("尝试注册元数据更新触发器，注册结果为{}", resposeStr);
                    Result result = com.eisoo.metadatamanage.web.util.JSONUtils.json2Obj(resposeStr, Result.class);
                    log.info("尝试启动元数据实时更新任务，启动结果为{}", resposeStr);
                    if (!result.getCode().equals("0")) {
                        schedulerTaskFlag = true;
                        schedulerTaskResult.append("尝试启动元数据实时更新任务失败,原因为");
                        schedulerTaskResult.append(resposeStr);
                    }
                    SchedulerProcessDto schedulerProcessDto = new SchedulerProcessDto();
                    schedulerProcessDto.setProcess_name(schedulerConfig.getProcessName());
                    schedulerProcessDto.setProcess_uuid(schedulerConfig.getProcessUuid());
                    schedulerProcessDto.setCrontab(schedulerConfig.getCron());
                    schedulerProcessDto.setCrontab_status(1);

                    List<SchedulerProcessRelationDto> relationDtoList = new ArrayList<>();
                    SchedulerProcessRelationDto relationDto = new SchedulerProcessRelationDto();
                    relationDto.setModel_uuid(schedulerConfig.getTaskUuid());
                    relationDto.setModel_type("3");
                    relationDtoList.add(relationDto);
                    schedulerProcessDto.setModels(relationDtoList);

                    schedulerProcessDto.setOnline_status(1);
                    schedulerProcessDto.setStart_time("2024-01-01 00:00:00");
                    schedulerProcessDto.setEnd_time("2034-01-01 00:00:00");
                    String processStr = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(schedulerProcessDto);

                    resposeStr = HttpUtil.executePostWithJson(schedulerConfig.getSchedulerProcessUrl(), processStr, null);
                    log.info("尝试注册元数据实时更新任务为{}", resposeStr);
                    result = com.eisoo.metadatamanage.web.util.JSONUtils.json2Obj(resposeStr, Result.class);
                    log.info("尝试启动元数据实时更新任务，启动结果为{}", resposeStr);
                    if (!result.getCode().equals("0")) {
                        schedulerTaskFlag = true;
                        schedulerTaskResult.append("尝试启动元数据实时更新任务失败,原因为");
                        schedulerTaskResult.append(resposeStr);
                    }


                    String stopProcessUrl = String.format("%s/cron/online", schedulerConfig.getSchedulerProcessUrl());
                    CronOnlineDto cronOnlineDto = new CronOnlineDto();
                    cronOnlineDto.setProcess_uuid(schedulerConfig.getProcessUuid());
                    cronOnlineDto.setCrontab_status(CronStatusEnum.Online.getCode());
                    String cronOnlineStr = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(cronOnlineDto);
                    resposeStr = HttpUtil.executePostWithJson(stopProcessUrl, cronOnlineStr, null);
                    result = com.eisoo.metadatamanage.web.util.JSONUtils.json2Obj(resposeStr, Result.class);
                    log.info("尝试启动元数据实时更新任务，启动结果为{}", resposeStr);
                    if (!result.getCode().equals("0")) {
                        schedulerTaskFlag = true;
                        schedulerTaskResult.append("尝试启动元数据实时更新任务失败,原因为");
                        schedulerTaskResult.append(resposeStr);
                    }

                    if (schedulerTaskFlag) {
                        throw new AiShuException(ErrorCodeEnum.InternalError, "启动实时采集失败！", schedulerTaskResult.toString());
                    }
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.WAITING.getCode());
                    // 如果是mysql数据源且相同ip端口没有其他数据源正在监听，增加一个binlog解析器
                    log.info("正在尝试启动binlog,dataSourceEntity:{}", dataSourceEntity);
                    log.info("正在尝试启动binlog,binaryLogClient:{}", binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()));
                    log.info("是否需要启动binlog:{}", "MySQL".equals(dataSourceEntity.getDataSourceTypeName()) && AiShuUtil.isEmpty(binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort())));
                    if ("MySQL".equals(dataSourceEntity.getDataSourceTypeName()) && AiShuUtil.isEmpty(binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()))) {
                        startMysqlBinlog(dataSourceEntity);
                    } else if ("MySQL".equals(dataSourceEntity.getDataSourceTypeName()) && !binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()).isConnected()) {
                        try {
                            Runnable listen = () -> {
                                try {
                                    binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()).connect();
                                    log.info("startcommand, binaryLogClientMap开启监听：{}", dataSourceEntity.getHost() + dataSourceEntity.getPort());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            };
                            Thread binaryLogListenThread = new Thread(listen);
                            binaryLogListenThread.start();
                        } catch (Exception e) {
                            log.info("binlog监听失败:{}", dataSourceEntity.getHost() + ":" + dataSourceEntity.getPort());
                        }
                    } else {
                        log.info("binlog已监听，无需再启动:{}", dataSourceEntity.getHost() + ":" + dataSourceEntity.getPort());
                    }
                    updateById(dataSourceEntity);
                    liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_SUCCESS);
                } else {
                    liveUpdateStatusDto.setHandleResult("数据源已处于实时采集状态");
                }
            } else if (Constants.COMMAND_STOP.equals(liveUpdateStatusDto.getCommand())) {
                if (!dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.IGNORE.getCode())) {
                    TaskEntity stopLiveUpdateTask = new TaskEntity();
                    stopLiveUpdateTask.setName(Constants.COMMAND_STOP_LIVE_UPDATE_TASK + "-" + dataSourceEntity.getName());
                    stopLiveUpdateTask.setObjectId(datasourceId);
                    stopLiveUpdateTask.setObjectType(TaskObjectTypeEnum.STOP_TASK.getCode());
                    stopLiveUpdateTask.setStatus(TaskStatusEnum.ONGOING.getCode());
                    LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, datasourceId);
                    taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.STOP_TASK.getCode());
                    taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
                    TaskEntity stopTask = taskService.getOne(taskEntityLambdaQueryWrapper, false);
                    if (AiShuUtil.isNotEmpty(stopTask)) {
                        Date now = new Date();
                        try {
                            String cronExpression = schedulerConfig.getCron();
                            CronExpression expression = new CronExpression(cronExpression);
                            // 计算下两个触发时间
                            Date firstValidTime = expression.getNextValidTimeAfter(stopTask.getStartTime());
                            Date secondValidTime = expression.getNextValidTimeAfter(firstValidTime);
                            if (now.compareTo(secondValidTime) > 0) {
                                stopTask.setStatus(TaskStatusEnum.FAIL.getCode());
                                taskService.updateById(stopTask);
                                liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_FAIL + ",数据源实时采集中止超时，请检查调度服务状态");
                            } else {
                                liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_WAITING + ",数据源实时采集正在中止，无须再发送停止指令");
                            }
                        } catch (Exception e) {
                            liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_FAIL + ",数据源实时采集中止异常，请检查数据源实时采集状态并上报管理员");
                            log.error(e.toString());
                        }
                    } else {
                        taskService.save(stopLiveUpdateTask);
                        liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_WAITING + ",数据源实时采集停止指令已发送，将在当前采集与广播周期完成后停止实时采集");
                    }
                } else {
                    liveUpdateStatusDto.setHandleResult(Constants.COMMAND_EXECUTE_SUCCESS + ",数据源已停止实时采集");
                }
            } else {
                throw new AiShuException(ErrorCodeEnum.OperationDenied, "无法识别的指令", "请输入start或者stop");
            }
        }
        return liveUpdateStatusDto;
    }

    @Override
    public void stopMysqlBinlog(DataSourceEntity dataSourceEntity) {
        //如果是mysql数据源，停止binlog解析
        if (DbType.MYSQL.getDescp().equals(StringUtils.lowerCase(dataSourceEntity.getDataSourceTypeName()))
                && AiShuUtil.isNotEmpty(binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()))) {
            try {
                binaryLogClientMap.get(dataSourceEntity.getHost() + dataSourceEntity.getPort()).disconnect();
                log.info("stop binaryLogClient of dataSource:{}", dataSourceEntity);
            } catch (Exception e) {
                log.error("stop binaryLogClient fail, dataSourceId:{},message:{}", dataSourceEntity.getId(), e.toString());
            }
        }
    }

    private void startMysqlBinlog(DataSourceEntity dataSourceEntity) {
        // 如果是mysql数据源，增加一个binlog解析器
        if (DbType.MYSQL.getDescp().equals(StringUtils.lowerCase(dataSourceEntity.getDataSourceTypeName()))) {
            EventDeserializer eventDeserializer = new EventDeserializer();
            eventDeserializer.setCompatibilityMode(
                    EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                    EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
            );
            for (EventType eventType : EventType.values()) {
                // 通过eventType过滤非query事件，不解析query以外的事件,如果排查问题就暂时去掉
                if (!eventType.equals(EventType.QUERY)) {
                    eventDeserializer.setEventDataDeserializer(eventType, new NullEventDataDeserializer());
                }
            }
            String password = PasswordUtils.decodePasswordRSA(dataSourceEntity.getPassword());
            BinaryLogClient client = new BinaryLogClient(dataSourceEntity.getHost(), dataSourceEntity.getPort(), dataSourceEntity.getDatabaseName(), dataSourceEntity.getUserName(), password);
            client.setEventDeserializer(eventDeserializer);
            client.registerEventListener(new BinaryLogClient.EventListener() {
                @Override
                public void onEvent(Event event) {
                    EventHeader eventHeader = event.getHeader();
                    EventType eventType = eventHeader.getEventType();
//                            log.info("全binlog事件打印,eventType:{},eventData:{}", eventType.name(),event.getData());
                    if (eventType.equals(EventType.QUERY)) {
                        QueryEventData queryEventData = event.getData();
                        if (!"BEGIN".equals(queryEventData.getSql())) {
                            log.info("QueryEvent is listening, ThreadId:{},Database:{},sql:{}", queryEventData.getThreadId(), queryEventData.getDatabase(), queryEventData.getSql());
                            List<LiveDdlEntity> ddlEntityList = new ArrayList<>();
                            LambdaUpdateWrapper<DataSourceEntity> allDatabaseWrapper = new LambdaUpdateWrapper<>();
                            allDatabaseWrapper.eq(DataSourceEntity::getPort, dataSourceEntity.getPort());
                            allDatabaseWrapper.eq(DataSourceEntity::getHost, dataSourceEntity.getHost());
                            allDatabaseWrapper.ne(DataSourceEntity::getLiveUpdateStatus, DataSourceUpdateStatusEnum.IGNORE.getCode());
                            allDatabaseWrapper.eq(DataSourceEntity::getDeleteCode, 0);
                            List<DataSourceEntity> allDatabaseList = list(allDatabaseWrapper);
                            if (AiShuUtil.isNotEmpty(allDatabaseList)) {
                                allDatabaseList.forEach(ds -> {
                                    LiveDdlEntity ddlEntity = new LiveDdlEntity();
                                    ddlEntity.setUpdateMessage("queryEventData.getDatabase():" + (StringUtils.isEmpty(queryEventData.getDatabase()) ? "empty" : queryEventData.getDatabase()));
                                    ddlEntity.setSqlText(queryEventData.getSql());
                                    ddlEntity.setDataSourceId(ds.getId());
                                    ddlEntity.setDataSourceName(ds.getName());
                                    if (StringUtils.isNotEmpty(ds.getDatabaseName())) {
                                        ddlEntity.setOriginCatalog(ds.getDatabaseName());
                                        Map<String, String> extendPropertyMap = com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(ds.getExtendProperty());
                                        ddlEntity.setVirtualCatalog(extendPropertyMap.get(DataSourceConstants.VCATALOGNAME));
                                        ddlEntity.setSchemaName(extendPropertyMap.get(DataSourceConstants.SCHEMAKEY));
                                    }
                                    ddlEntityList.add(ddlEntity);
                                });
                                liveDdlService.saveBatch(ddlEntityList);
                            }
                        }
                    }
                }
            });
            binaryLogClientMap.put(dataSourceEntity.getHost() + dataSourceEntity.getPort(), client);
            log.info("binaryLogClientMap增加监听器：{}", dataSourceEntity.getHost() + dataSourceEntity.getPort());
            Runnable listen = () -> {
                try {
                    client.connect();
                    log.info("startMysqlBinlog, binaryLogClientMap开启监听：{}", dataSourceEntity.getHost() + dataSourceEntity.getPort());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            Thread binaryLogListenThread = new Thread(listen);
            binaryLogListenThread.start();
        }
    }

    public DictEntity convertDsType(String type_name, Integer type, List<DictEntity> list) {
        if (AiShuUtil.isNotEmpty(type_name)) {
            switch (type_name) {
                case "hive-jdbc":
                case "hive-hadoop2":
                case "inceptor-jdbc":
                    type_name = "hive";
                    break;
                case "maria":
                    type_name = "mariadb";
                    break;
                case "hologres":
                case "opengauss":
                case "gaussdb":
                    type_name = "postgresql";
                    break;
                case "dameng":
                    type_name = "dm";
            }
            String finalType_name = type_name;
            return list.stream().filter(dict -> StringUtils.lowerCase(dict.getDictValue()).equals(finalType_name)).findAny().orElse(null);
        } else if (AiShuUtil.isNotEmpty(type)) {
            //转换配置中心的数据源类型枚举值为元数据字典表中的数据源枚举值
            Integer dsType;
            switch (type) {
                case 1:
                case 2:
                    dsType = 2;//mysql或mariadb
                    break;
                case 3:
                    dsType = 3;//postgresql
                    break;
                case 4:
                    dsType = 4;//sqlserver
                    break;
                case 5:
                    dsType = 1;//oracle
                    break;
                case 6:
                    dsType = 5;//hive
                    break;
                case 7:
                    dsType = 14;//clickhouse
                    break;
                default:
                    dsType = 0;
            }
            Integer finalDsType = dsType;
            return list.stream().filter(dict -> dict.getDictKey().equals(finalDsType)).findAny().orElse(null);
        } else {
            return null;
        }
    }

    public String getJdbc(DbType dbType, ConnectionParamObject connectionParamObject) {
        if (dbType == null) {
            return null;
        }
        String jdbc = null;
        switch (dbType) {
            case SQLSERVER:
                jdbc = connectionParamObject.getAddress() + ";databaseName=" + connectionParamObject.getDatabase() + ";trustServerCertificate=true;encrypt=false";
                break;
            default:
                jdbc = connectionParamObject.getAddress() + "/" + connectionParamObject.getDatabase();
        }
        return jdbc;
    }

    private List<DataSourceEntity> findBySchema(List<DataSourceEntity> dataSourceEntities, String schema) {
        List<DataSourceEntity> result = new ArrayList<>();
        if (AiShuUtil.isNotEmpty(dataSourceEntities) && StringUtils.isNotBlank(schema)) {
            dataSourceEntities.forEach(dataSourceEntity -> {
                Map<String, String> extendPropertyMap = com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
                if (schema.equals(extendPropertyMap.get(DataSourceConstants.SCHEMAKEY))) {
                    result.add(dataSourceEntity);
                }
            });
        }
        return result;
    }

    /**
     * 在dolphin上創建数据源
     *
     * @param dataSourceEntity
     */
    private void createOnDolphin(DataSourceEntity dataSourceEntity) {
        DataSourceDolphinDTO dto = getDataSourceDolphinDTO(dataSourceEntity);
        dto.setName(dataSourceEntity.getName() + "_" + IdWorker.getId());
        Map<String, String> headMap = new HashMap<>();
        headMap.put("token", dolphinschedulerConfig.getToken());
        String url = String.format("http://%s:%s/dolphinscheduler/datasources", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort());
        com.eisoo.metadatamanage.util.HttpUtil.executePostWithJson(url, com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(dto), headMap);
        //根据名称确定当前数据源对应的dolphinid,如果不统一数据源配置入口可能出现数据源名称重复导致的结果异常
        DataSourceSimpleDolphinDTO currentSimpleDs = getByDolphinName(dto.getName(), dataSourceEntity, headMap);
        dataSourceEntity.setDolphinId(currentSimpleDs.getValue());
    }

    /**
     * 在dolphin上修改数据源
     *
     * @param dataSourceEntity
     */
    private void updateOnDolphin(DataSourceEntity dataSourceEntity) {
        Map<String, String> headMap = new HashMap<>();
        headMap.put("token", dolphinschedulerConfig.getToken());
        DataSourceDolphinDTO dto = getDataSourceDolphinDTO(dataSourceEntity);
        dto.setName(null);
        //根据名称确定当前数据源对应的dolphinid,如果不统一数据源配置入口可能出现数据源名称重复导致的结果异常
        DataSourceSimpleDolphinDTO currentSimpleDs = getByDolphinId(dataSourceEntity, headMap);
        //若不为空向dolphin提交更改
        if (AiShuUtil.isNotEmpty(currentSimpleDs)) {
            String url = String.format("http://%s:%s/dolphinscheduler/datasources/%s", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort(), currentSimpleDs.getValue());
            String updateResult = com.eisoo.metadatamanage.util.HttpUtil.executePutHttpRequest(url, com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(dto), headMap);
            log.info(updateResult);
        }
    }

    /**
     * 通过dolphinId获取对应的数据源
     *
     * @param dataSourceEntity
     * @param headMap
     * @return
     */
    private DataSourceSimpleDolphinDTO getByDolphinId(DataSourceEntity dataSourceEntity, Map<String, String> headMap) {
        DataSourceDolphinDTO dto = getDataSourceDolphinDTO(dataSourceEntity);
        String url = String.format("http://%s:%s/dolphinscheduler/data-quality/getDatasourceOptionsById?datasourceId=%s", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort(), DbType.of(dto.getType()).getCode());
        String dsJsonStr = HttpUtil.executeGet(url, headMap);
        JsonNode resultJson = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonNode(dsJsonStr);
        List<DataSourceSimpleDolphinDTO> resultList = com.eisoo.metadatamanage.web.util.JSONUtils.toList(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(resultJson.findValue("data")), DataSourceSimpleDolphinDTO.class);
        //根据dolphinid确定当前数据源对应的dolphin数据源,
        DataSourceSimpleDolphinDTO currentSimpleDs = resultList.stream().filter(item -> item.getValue().equals(dataSourceEntity.getDolphinId())).findAny().orElse(null);
        return currentSimpleDs;
    }

    /**
     * 通过dolphinName获取对应的数据源
     *
     * @param dataSourceEntity
     * @param headMap
     * @return
     */
    private DataSourceSimpleDolphinDTO getByDolphinName(String dolphinName, DataSourceEntity dataSourceEntity, Map<String, String> headMap) {
        DataSourceDolphinDTO dto = getDataSourceDolphinDTO(dataSourceEntity);
        String url = String.format("http://%s:%s/dolphinscheduler/data-quality/getDatasourceOptionsById?datasourceId=%s", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort(), DbType.of(dto.getType()).getCode());
        String dsJsonStr = HttpUtil.executeGet(url, headMap);
        JsonNode resultJson = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonNode(dsJsonStr);
        if (AiShuUtil.isEmpty(resultJson) || AiShuUtil.isEmpty(resultJson.findValue("data"))) {
            return null;
        }
        List<DataSourceSimpleDolphinDTO> resultList = com.eisoo.metadatamanage.web.util.JSONUtils.toList(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(resultJson.findValue("data")), DataSourceSimpleDolphinDTO.class);
        //根据dolphinid确定当前数据源对应的dolphin数据源,
        DataSourceSimpleDolphinDTO currentSimpleDs = resultList.stream().filter(item -> item.getLabel().equals(dolphinName)).findAny().orElse(null);
        return currentSimpleDs;
    }


    /**
     * 元数据平台数据源转dolphin数据源
     *
     * @param dataSourceEntity
     * @return
     */
    private DataSourceDolphinDTO getDataSourceDolphinDTO(DataSourceEntity dataSourceEntity) {
        DataSource dataSource = getDataSource(dataSourceEntity);
        DataSourceDolphinDTO dto = new DataSourceDolphinDTO();
        dto.setType(dataSource.getType().getDescp());
        dto.setName(dataSourceEntity.getName());
        dto.setNote(String.format("{\"anyfabricId\": \"%s\"}", dataSourceEntity.getId()));
        dto.setHost(dataSourceEntity.getHost());
        dto.setPort(dataSourceEntity.getPort());
        dto.setPrincipal("");
        dto.setJavaSecurityKrb5Conf("");
        dto.setLoginUserKeytabUsername("");
        dto.setLoginUserKeytabPath("");
        dto.setUserName(dataSourceEntity.getUserName());
        dto.setPassword(dataSource.getConnectionParamObject().getPassword());
        dto.setDatabase(dataSourceEntity.getDatabaseName());
        dto.setConnectType("");
        dto.setOther(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonNode(com.eisoo.metadatamanage.web.util.JSONUtils.props2Map(dataSourceEntity.getExtendProperty())));
        return dto;
    }

    @Override
    public boolean clearColumnsByDsId(Long datasourceId) {
        if (!isExisted(datasourceId)) {
            throw new AiShuException(ErrorCodeEnum.DATA_NOT_EXIST, "参数id错误！", "请检查：参数id的数据源已删除或不存在");
        }
        return dataSourceMapper.clearColumnsByDsId(datasourceId);
    }
}
