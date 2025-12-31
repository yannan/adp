package com.eisoo.metadatamanage.web.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.eisoo.metadatamanage.db.entity.*;
import com.eisoo.metadatamanage.db.mapper.DictMapper;
import com.eisoo.metadatamanage.db.mapper.TaskMapper;
import com.eisoo.metadatamanage.db.mapper.TaskScanMapper;
import com.eisoo.metadatamanage.lib.dto.*;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualColumnListDto;
import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualTableListDto;
import com.eisoo.metadatamanage.lib.enums.*;
import com.eisoo.metadatamanage.lib.vo.CheckErrorVo;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.config.DolphinschedulerConfig;
import com.eisoo.metadatamanage.web.config.SchedulerConfig;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.IDipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.impl.DipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.virtualService.VirtualService;
import com.eisoo.metadatamanage.web.mq.LiveDdlProducer;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.PasswordUtils;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.web.extra.model.BaseConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.DataSource;
import com.eisoo.metadatamanage.web.service.*;
import com.eisoo.metadatamanage.web.util.DataSourceUtils;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.threadpoolexecutor.MDCThreadPoolExecutor;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.eisoo.standardization.common.util.StringUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskEntity> implements ITaskService {
    private static final Logger log = LoggerFactory.getLogger("TaskLogger");
    //    @Autowired
//    IVegaDataSourceService vegaDataSourceService;
    @Autowired
    IDataConnectionDataSourceService dataConnectionDataSourceService;
    @Autowired
    DataSourceServiceImpl dataSourceService;
    @Autowired(required = false)
    IDictService dictService;
    @Autowired(required = false)
    DictMapper dictMapper;
    @Autowired(required = false)
    ISchemaService schemaService;
    @Autowired(required = false)
    ITableService tableService;
    @Autowired(required = false)
    ITableFieldService tableFieldService;
    @Autowired(required = false)
    ITaskLogService taskLogService;
    @Autowired
    IDipDataSourceService dipDataSourceService;
    @Autowired
    VirtualService virtualService;
    @Autowired
    ILiveDdlService liveDdlService;
    @Autowired
    javax.sql.DataSource dataSource;
    @Autowired
    private SchedulerConfig schedulerConfig;
    @Autowired
    LiveDdlProducer liveDdlProducer;
    @Autowired(required = false)
    private TaskScanMapper taskScanMapper;
    //元数据采集线程
    ExecutorService fillMetaDataExecutorPool = MDCThreadPoolExecutor.newFixedThreadPool(10);

    @PostConstruct
    void updatePassword() {
        LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.UPDATE_PASSWORD.getCode());
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.SUCCESS.getCode());
        if (count(taskEntityLambdaQueryWrapper) == 0L) {
            TaskEntity updatePasswordTask = new TaskEntity();
            updatePasswordTask.setStartTime(new Date());
            List<DataSourceEntity> dataSourceList = dataSourceService.list();
            List<DataSourceEntity> updatePasswordDsList = new ArrayList<>();
            try {
                dataSourceList.forEach(i -> {
//                    log.info("i.getPassword().length():{}",i.getPassword().length() );
                    if (i.getPassword().length() != 172) {
                        String password = PasswordUtils.decodePasswordNoSalt(i.getPassword());
                        i.setPassword(PasswordUtils.encodePasswordRSA(password));
                        updatePasswordDsList.add(i);
                    }
                });
                if (AiShuUtil.isNotEmpty(updatePasswordDsList)) {
                    dataSourceService.updateBatchById(updatePasswordDsList);
                }
            } catch (Exception e) {
                log.error(e.toString());
            }
//            updatePasswordTask.setObjectId(1l);
            updatePasswordTask.setObjectId("1");
            updatePasswordTask.setObjectType(TaskObjectTypeEnum.UPDATE_PASSWORD.getCode());
            updatePasswordTask.setName("datasourceUpdatePassword");
            updatePasswordTask.setStatus(TaskStatusEnum.SUCCESS.getCode());
            updatePasswordTask.setEndTime(new Date());
            saveOrUpdate(updatePasswordTask);
        }
    }

//    @PostConstruct
//    void registerUpdateMetadataTrigger() {
    //检测是否存在元数据项目 启动任务自动分类分项与自动清理需求后再续写
//        Map<String, String> headMap = new HashMap<>();
//        headMap.put("token", dolphinschedulerConfig.getToken());
//        String url = String.format("http://%s:%s/dolphinscheduler/projects/pageSize=10&pageNo=1&searchVal=%s", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort(), schedulerConfig.getProjectName());
//        String dsJsonStr = HttpUtil.executeGet(url, headMap);
//        JsonNode resultJson = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonNode(dsJsonStr);
//        if (AiShuUtil.isEmpty(resultJson) || AiShuUtil.isEmpty(resultJson.findValue("data"))) {
//            throw new AiShuException(ErrorCodeEnum.Invalid, Messages.MESSAGE_VALUE_NO_VALID_SCHEDULER_PROJECT);
//        }
//        List<SchedulerProjectDto> resultList = JSONUtils.toList(JSONUtils.toJsonString(resultJson.findValue("data")), SchedulerProjectDto.class);
//        SchedulerProjectDto schedulerProjectDto;
//        if (AiShuUtil.isEmpty(resultList) || !resultList.stream().anyMatch(i -> i.getName().equals(schedulerConfig.getProjectName()))) {
//            //如果不存在那么创建一个项目
//            String createProjectUrl = String.format("http://%s:%s/dolphinscheduler/projects", dolphinschedulerConfig.getHost(), dolphinschedulerConfig.getPort());
//            Map<String, String> paramsMap = new HashMap<>();
//            paramsMap.put("projectName", schedulerConfig.getProjectName());
//            paramsMap.put("description", "");
//            paramsMap.put("userName", dolphinschedulerConfig.getUser());
//            HttpResponseVo responseVo = HttpUtil.httpPostFromData(createProjectUrl, paramsMap, headMap);
//            if (!responseVo.isSucesss()) {
//                throw new AiShuException(ErrorCodeEnum.Invalid, Messages.MESSAGE_VALUE_CREATE_SCHEDULER_PROJECT_FAIL);
//            } else {
//                dsJsonStr = responseVo.getResult();
//                resultJson = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonNode(dsJsonStr);
//                if (AiShuUtil.isEmpty(resultJson) || AiShuUtil.isEmpty(resultJson.findValue("data"))) {
//                    throw new AiShuException(ErrorCodeEnum.Invalid, Messages.MESSAGE_VALUE_NO_VALID_SCHEDULER_PROJECT);
//                } else {
//                    schedulerProjectDto = JSONUtils.json2Obj(JSONUtils.toJsonString(resultJson.findValue("data")), SchedulerProjectDto.class);
//                }
//            }
//        } else {
//            schedulerProjectDto = resultList.stream().filter(i -> i.getName().equals(schedulerConfig.getProjectName())).findAny().orElse(null);
//        }
    //尝试注册元数据更新触发器
//        SchedulerTaskDto taskDto = new SchedulerTaskDto();
//        taskDto.setName(schedulerConfig.getTaskName());
//        taskDto.setUuid(schedulerConfig.getTaskUuid());
//        taskDto.setBase_task_type("Http");
//
//        List<AdvancedDTO> advancedDTOList = new ArrayList<>();
//
//        AdvancedDTO modelTypeDto = new AdvancedDTO();
//        modelTypeDto.setKey(Constants.KEY_MODEL_TYPE);
//        modelTypeDto.setValue("Api");
//        advancedDTOList.add(modelTypeDto);
//
//        AdvancedDTO httpMethodDto = new AdvancedDTO();
//        httpMethodDto.setKey(Constants.KEY_HTTP_METHOD);
//        httpMethodDto.setValue("POST");
//        advancedDTOList.add(httpMethodDto);
//
//        AdvancedDTO urlDto = new AdvancedDTO();
//        urlDto.setKey(Constants.KEY_URL);
//        urlDto.setValue(schedulerConfig.getTaskUrl());
//        advancedDTOList.add(urlDto);
//
//        AdvancedDTO delayTimeDto = new AdvancedDTO();
//        delayTimeDto.setKey(Constants.KEY_DELAY_TIME);
//        delayTimeDto.setValue("0");
//        advancedDTOList.add(delayTimeDto);
//
//        AdvancedDTO failRetryIntervalDto = new AdvancedDTO();
//        failRetryIntervalDto.setKey(Constants.KEY_FAIL_RETRY_INTERVAL);
//        failRetryIntervalDto.setValue("1");
//        advancedDTOList.add(failRetryIntervalDto);
//
//        AdvancedDTO failRetryTimesDto = new AdvancedDTO();
//        failRetryTimesDto.setKey(Constants.KEY_FAIL_RETRY_TIMES);
//        failRetryTimesDto.setValue("0");
//        advancedDTOList.add(failRetryTimesDto);
//
//        AdvancedDTO connectTimeoutDto = new AdvancedDTO();
//        connectTimeoutDto.setKey(Constants.KEY_CONNECT_TIMEOUT);
//        connectTimeoutDto.setValue("3000");
//        advancedDTOList.add(connectTimeoutDto);
//
//        AdvancedDTO socketTimeoutDto = new AdvancedDTO();
//        socketTimeoutDto.setKey(Constants.KEY_SOCKET_TIMEOUT);
//        socketTimeoutDto.setValue("3000");
//        advancedDTOList.add(socketTimeoutDto);
//
//        AdvancedDTO httpCheckConditionDto = new AdvancedDTO();
//        httpCheckConditionDto.setKey(Constants.KEY_HTTP_CHECK_CONDITION);
//        httpCheckConditionDto.setValue("STATUS_CODE_DEFAULT");
//        advancedDTOList.add(httpCheckConditionDto);
//
//        AdvancedDTO conditionDto = new AdvancedDTO();
//        conditionDto.setKey(Constants.KEY_CONDITION);
//        conditionDto.setValue("STATUS_CODE_DEFAULT");
//        advancedDTOList.add(conditionDto);
//
//        //启动任务自动分类分项与自动清理需求后再续写
////        AdvancedDTO projectCodeDto = new AdvancedDTO();
////        projectCodeDto.setKey(Constants.KEY_PROJECT_CODE);
////        projectCodeDto.setValue();
////        advancedDTOList.add(projectCodeDto);
//
//        List<HttpParamDto> httpParamDtoList = new ArrayList<>();
//
//        HttpParamDto contentTypeParamDto = new HttpParamDto();
//        contentTypeParamDto.setProp("Content-Type");
//        contentTypeParamDto.setHttpParametersType("HEADERS");
//        contentTypeParamDto.setValue("application/json");
//        httpParamDtoList.add(contentTypeParamDto);
//
//        HttpParamDto authorizationParamDto = new HttpParamDto();
//        authorizationParamDto.setProp("Authorization");
//        authorizationParamDto.setHttpParametersType("HEADERS");
//        authorizationParamDto.setValue(schedulerConfig.getToken());
//        httpParamDtoList.add(authorizationParamDto);
//
//        AdvancedDTO httpParamsDto = new AdvancedDTO();
//        httpParamsDto.setKey(Constants.KEY_HTTP_PARAMS);
//        httpParamsDto.setValue(JSONUtils.toJsonString(httpParamDtoList));
//        advancedDTOList.add(httpParamsDto);
//
//        taskDto.setAdvanced_params(advancedDTOList);
//
//        String taskDtoStr = JSONUtils.toJsonString(taskDto);
//        try {
//            String resposeStr = HttpUtil.executePostWithJson(schedulerConfig.getSchedulerModelUrl(), taskDtoStr, null);
//            log.info("启动时尝试注册元数据更新触发器，注册结果为{}", resposeStr);
//        } catch (Exception e) {
//            log.error("启动时尝试注册元数据更新触发器失败，原因为{}", e.toString());
//        }


//        //创建新工作流
//        if (schedulerConfig.getTrigger()) {
//            SchedulerProcessDto schedulerProcessDto = new SchedulerProcessDto();
//            schedulerProcessDto.setProcess_name(schedulerConfig.getProcessName());
//            schedulerProcessDto.setProcess_uuid(schedulerConfig.getProcessUuid());
//            schedulerProcessDto.setCrontab(schedulerConfig.getCron());
//            schedulerProcessDto.setCrontab_status(1);
//
//            List<SchedulerProcessRelationDto> relationDtoList = new ArrayList<>();
//            SchedulerProcessRelationDto relationDto = new SchedulerProcessRelationDto();
//            relationDto.setModel_uuid(schedulerConfig.getTaskUuid());
//            relationDto.setModel_type("3");
//            relationDtoList.add(relationDto);
//            schedulerProcessDto.setModels(relationDtoList);
//
//            schedulerProcessDto.setOnline_status(1);
//            schedulerProcessDto.setStart_time("2024-01-01 00:00:00");
//            schedulerProcessDto.setEnd_time("2034-01-01 00:00:00");
//            String processStr = JSONUtils.toJsonString(schedulerProcessDto);
//            try {
//                String resposeStr = HttpUtil.executePostWithJson(schedulerConfig.getSchedulerProcessUrl(), processStr, null);
//                log.info("启动时尝试注册元数据实时更新任务，注册结果为{}", resposeStr);
//            } catch (Exception e) {
//                log.error("启动时尝试注册元数据实时更新任务失败，原因为{}", e.toString());
//            }
//        } else {
//            //删除旧工作流
//            try {
//                String deleteProcessUrl = String.format("%s/%s", schedulerConfig.getSchedulerProcessUrl(), schedulerConfig.getProcessUuid());
//                String resposeStr = HttpUtil.executeDeleteHttpRequest(deleteProcessUrl, null);
//                log.info("启动时尝试删除元数据更新触发器定时服务，删除结果为{}", resposeStr);
//            } catch (Exception e) {
//                log.error("启动时尝试删除元数据更新触发器定时服务失败，原因为{}", e.toString());
//            }
//        }
//    }

    @Override
    @Transactional
    public Result<?> fillMetaData(String dsid) {
        DataSourceEntity dataSourceEntity = dataSourceService.getById(dsid);
        if (AiShuUtil.isEmpty(dataSourceEntity) || dataSourceEntity.getIsDeleted()) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, dsid);
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.DATASOURCE.getCode());
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
        TaskEntity ongoing = getOne(taskEntityLambdaQueryWrapper, false);
        if (AiShuUtil.isNotEmpty(ongoing)) {
            Result.success(String.format("所选的数据源采集任务正在进行中，请勿重复提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", ongoing.getName(), ongoing.getId()));
        }
        TaskDto taskDto = getTaskDto(dataSourceEntity);
        TaskEntity taskEntity = new TaskEntity();
        AiShuUtil.copyProperties(taskDto, taskEntity);
        save(taskEntity);
        fillMetaDataExecutorPool.submit(() -> fillMetaDataExec(dataSourceEntity, taskEntity));

        return Result.success(String.format("采集任务已提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", taskDto.getTaskName(), taskDto.getTaskId()));
    }

    @Override
    public Result<?> fillMetaDataByVirtual(String taskId) {
        TaskScanEntity taskScanEntity = taskScanMapper.selectById(taskId);
        String dsId = taskScanEntity.getDsId();
//      DipDataSourceEntity dataSourceEntityVega = vegaDataSourceService.getByDataSourceId(dsid);
        DataSourceEntityDataConnection dsDataSourceEntity = dataConnectionDataSourceService.getByDataSourceId(dsId);
        if (AiShuUtil.isEmpty(dsDataSourceEntity)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        //增加对采集中的数据源重复采集的限制
//        LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.DATASOURCE.getCode());
//        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, dsid);
//        taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
        int runningDs = taskScanMapper.getRunningDs(dsId);
        if (runningDs != 0) {
            throw new AiShuException(ErrorCodeEnum.OperationDenied, "数据源采集进行中，请勿重复提交");
        }
//        if (count(taskEntityLambdaQueryWrapper) > 0) {
//            throw new AiShuException(ErrorCodeEnum.OperationDenied, "数据源采集进行中，请勿重复提交");
//        }
        //增加对历史任务记录数量的限制
//        if (count() > 200) {
//            taskEntityLambdaQueryWrapper.clear();
//            taskEntityLambdaQueryWrapper.orderByAsc(TaskEntity::getId);
//            taskEntityLambdaQueryWrapper.last("limit 100");
//            List<TaskEntity> removeTaskList = list(taskEntityLambdaQueryWrapper);
//            removeBatchByIds(removeTaskList);
//            //同时删除日志
//            if (AiShuUtil.isNotEmpty(removeTaskList)) {
//                List<Long> removeTaskIdList = removeTaskList.stream().map(i -> i.getId()).collect(Collectors.toList());
//                LambdaQueryWrapper<TaskLogEntity> taskLogEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
//                taskLogEntityLambdaQueryWrapper.in(TaskLogEntity::getTaskId, removeTaskIdList);
//                taskLogService.remove(taskLogEntityLambdaQueryWrapper);
//            }
//        }
//        //bug630066要求每次采集后重新排序，故物理删除字段以保证每次采集后顺序与数据库一致
//        dataConnectionDataSourceService.clearColumnsByDsId(dsId);
//        //记录采集任务
//        TaskDto taskDto = getTaskDto(dataSourceEntityVega);
//        TaskEntity taskEntity = new TaskEntity();
//        AiShuUtil.copyProperties(taskDto, taskEntity);
//        save(taskEntity);
        // 更新状态:0 wait 1 running 2 success 3 fail
        taskScanMapper.updateScanStatusById(taskId, 1);
        fillMetaDataExecutorPool.submit(() -> fillMetaDataExecByVirtual(dsDataSourceEntity, taskScanEntity));
        return Result.success(String.format("采集任务已提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", taskScanEntity.getName(), taskScanEntity.getId()));
    }

    private TaskDto getTaskDto(DipDataSourceEntity dataSourceEntity) {
        StringBuffer taskNameBuffer = new StringBuffer();
        StringBuffer taskIdBuffer = new StringBuffer();

        TaskDto taskDto = new TaskDto();
        taskDto.setId(IdWorker.getId());
        taskDto.setObjectId(dataSourceEntity.getId());
        //Todo 字典化
        taskDto.setObjectType(1);
        taskDto.setStartTime(new Date());
        taskDto.setStatus(2);
        String taskName = dataSourceEntity.getName() + "-task-" + taskDto.getId();
        taskDto.setName(taskName);
        taskNameBuffer.append(taskDto.getName()).append(',');
        taskIdBuffer.append(taskDto.getId()).append(',');
        String taskNameStr = "";
        if (taskNameBuffer.length() > 0) {
            taskNameStr = taskNameBuffer.deleteCharAt(taskNameBuffer.length() - 1).toString();
        }
        taskDto.setTaskName(taskNameStr);
        String taskIdStr = "";
        if (taskIdBuffer.length() > 0) {
            taskIdStr = taskIdBuffer.deleteCharAt(taskIdBuffer.length() - 1).toString();
        }
        taskDto.setTaskId(taskIdStr);
        return taskDto;
    }


    private TaskDto getTaskDto(DataSourceEntity dataSourceEntity) {
        StringBuffer taskNameBuffer = new StringBuffer();
        StringBuffer taskIdBuffer = new StringBuffer();

        TaskDto taskDto = new TaskDto();
        taskDto.setId(IdWorker.getId());
        taskDto.setObjectId(dataSourceEntity.getId());
        //Todo 字典化
        taskDto.setObjectType(1);
        taskDto.setStartTime(new Date());
        taskDto.setStatus(2);
        String taskName = dataSourceEntity.getName() + "-task-" + taskDto.getId();
        taskDto.setName(taskName);
        taskNameBuffer.append(taskDto.getName()).append(',');
        taskIdBuffer.append(taskDto.getId()).append(',');
        String taskNameStr = "";
        if (taskNameBuffer.length() > 0) {
            taskNameStr = taskNameBuffer.deleteCharAt(taskNameBuffer.length() - 1).toString();
        }
        taskDto.setTaskName(taskNameStr);
        String taskIdStr = "";
        if (taskIdBuffer.length() > 0) {
            taskIdStr = taskIdBuffer.deleteCharAt(taskIdBuffer.length() - 1).toString();
        }
        taskDto.setTaskId(taskIdStr);
        return taskDto;
    }


    @Override
    @Transactional
    public Result<?> fillMetaData(FillMetaDataDTO fillMetaDataDTO) {
        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceEntity::getName, fillMetaDataDTO.getName());
        wrapper.eq(DataSourceEntity::getCreateUser, fillMetaDataDTO.getCreateUser());
        wrapper.eq(DataSourceEntity::getInfoSystemId, fillMetaDataDTO.getInfoSystemId());
        wrapper.eq(DataSourceEntity::getDeleteCode, 0);
        DataSourceEntity dataSourceEntity;
        try {
            dataSourceEntity = dataSourceService.getOne(wrapper);
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
        }
        if (AiShuUtil.isEmpty(dataSourceEntity)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted, "数据源不存在或已禁用");
        }
        DataSourceEntity finalDataSourceEntity = dataSourceEntity;
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(IdWorker.getId());
        taskEntity.setObjectId(dataSourceEntity.getId());
        //Todo 字典化
        taskEntity.setObjectType(1);
        taskEntity.setStartTime(new Date());
        taskEntity.setStatus(2);
        String taskName = dataSourceEntity.getName() + "-task-" + taskEntity.getId();
        taskEntity.setName(taskName);
        save(taskEntity);
        fillMetaDataExecutorPool.submit(() -> fillMetaDataExec(finalDataSourceEntity, taskEntity));
        return Result.success(String.format("采集任务已提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", taskEntity.getName(), taskEntity.getId()));
    }

    @Override
    public Result<?> fillMetaDataByVirtual(FillMetaDataDTO fillMetaDataDTO) {
        // TODO:这里为了适配VegaDataSourceEntity更改做了修改，后面如果有需要需要把下面的参数进行修改！
//        LambdaQueryWrapper<DipDataSourceEntity> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DipDataSourceEntity::getName, fillMetaDataDTO.getName());
//        wrapper.eq(DipDataSourceEntity::getCreatedByUid, fillMetaDataDTO.getCreateUser());
//        wrapper.eq(VegaDataSourceEntity::getInfoSystemId, fillMetaDataDTO.getInfoSystemId());
//        wrapper.eq(VegaDataSourceEntity::getDeleteCode, 0);
//        DipDataSourceEntity dataSourceEntity;

        LambdaQueryWrapper<DataSourceEntityDataConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceEntityDataConnection::getFName, fillMetaDataDTO.getName());
        wrapper.eq(DataSourceEntityDataConnection::getFCreatedByUid, fillMetaDataDTO.getCreateUser());
        DataSourceEntityDataConnection dataSourceEntity;
        try {
//            dataSourceEntity = vegaDataSourceService.getOne(wrapper);
            dataSourceEntity = dataConnectionDataSourceService.getOne(wrapper);
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated);
        }
        if (AiShuUtil.isEmpty(dataSourceEntity)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted, "数据源不存在或已禁用");
        }
//        DipDataSourceEntity finalDataSourceEntity = dataSourceEntity;
        DataSourceEntityDataConnection finalDataSourceEntity = dataSourceEntity;
//        TaskEntity taskEntity = new TaskEntity();
//        taskEntity.setId(IdWorker.getId());
//        taskEntity.setObjectId(dataSourceEntity.getFId());
//        //Todo 字典化
//        taskEntity.setObjectType(1);
//        taskEntity.setStartTime(new Date());
//        taskEntity.setStatus(2);
        TaskScanEntity taskEntity = new TaskScanEntity();
        taskEntity.setId(IdWorker.getId() + "");
        taskEntity.setDsId(dataSourceEntity.getFId());
        //Todo 字典化
//        taskEntity.setObjectType(1);
        taskEntity.setStartTime(new Date());
        taskEntity.setScanStatus(2);
        String taskName = dataSourceEntity.getFName() + "-task-" + taskEntity.getId();
        taskEntity.setName(taskName);
//        save(taskEntity);
        fillMetaDataExecutorPool.submit(() -> fillMetaDataExecByVirtual(finalDataSourceEntity, taskEntity));
        return Result.success(String.format("采集任务已提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", taskEntity.getName(), taskEntity.getId()));
    }

    /**
     * 采集元数据
     *
     * @param dataSourceEntity
     * @param taskEntity
     */
    @Override
    public void fillMetaDataExec(DataSourceEntity dataSourceEntity, TaskEntity taskEntity) {
//        DataSourceEntity dataSourceEntity = getById(dsid);
        MDC.put("taskId", String.valueOf(taskEntity.getId()));
        ResultSet tables = null;
        Connection connection = null;
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            try {
                log.info("采集任务开始，任务id：{}", taskEntity.getId());
                DataSource dataSource = dataSourceService.getDataSource(dataSourceEntity);
                BaseConnectionParam connectionParam =
                        (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                                dataSource.getType(),
                                dataSource.getConnectionParams());
                log.info("BaseConnectionParam connectionParam ={}", connectionParam);
                connection =
                        DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
                log.info("connection={}", connection);
                DatabaseMetaData metaData = connection.getMetaData();
                LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                String schema = null;
                try {
                    schema = AiShuUtil.isEmpty(connection.getSchema()) ? connection.getCatalog() : connection.getSchema();
                    if (AiShuUtil.isNotEmpty(dataSource.getConnectionParamObject().getProps()) && AiShuUtil.isNotEmpty(dataSource.getConnectionParamObject().getProps().get(DataSourceConstants.SCHEMAKEY))) {
                        schema = dataSource.getConnectionParamObject().getProps().get(DataSourceConstants.SCHEMAKEY);
                    }
                    log.info("开始采集schema={}", schema);
                    if (AiShuUtil.isEmpty(schema)) {
                        throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
                    }
                    //schema保存逻辑
                    //查询结果转换schema实体类
                    SchemaEntity schemaEntity = new SchemaEntity();
                    schemaEntity.setName(schema);
                    schemaEntity.setDataSourceName(dataSourceEntity.getName());
                    schemaEntity.setDataSourceType(dataSourceEntity.getDataSourceType());
                    schemaEntity.setDataSourceTypeName(dataSourceEntity.getDataSourceTypeName());
                    schemaEntity.setDataSourceId(dataSourceEntity.getId());
                    //查询是否现有schema
                    schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getDataSourceId, schemaEntity.getDataSourceId());
                    schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getName, schemaEntity.getName());
                    SchemaEntity oldSchema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                    Date now = new Date();
                    if (AiShuUtil.isNotEmpty(oldSchema)) {
                        schemaEntity.setId(oldSchema.getId());
                    } else {
//                        schemaEntity.setCreateUser(AiShuUtil.getUser().getUserName());
                        schemaEntity.setCreateTime(now);
                    }
                    schemaEntity.setUpdateTime(now);
//                    schemaEntity.setUpdateUser(AiShuUtil.getUser().getUserName());
//                    schemaEntity.setAuthorityId(AiShuUtil.getUser().getUserId());
                    //有则改，无则增
                    schemaService.saveOrUpdate(schemaEntity);
                } catch (SQLException e) {
                    log.error("cant not get the schema : {}", e.getMessage(), e);
                }
                SchemaEntity newSchema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                //表级元数据处理逻辑
                Map<String, Long> countMap = getCountMap(connection, schema, dataSource.getType());

                tables = metaData.getTables(
                        connectionParam.getDatabase(),
                        getDbSchemaPattern(dataSource.getType(), schema, connectionParam),
                        "%", DataSourceConstants.TABLE_TYPES);
                log.info("开始采集tables...");
                Map<String, TableEntity> currentTables = new HashMap<>();
                while (tables.next()) {

                    TableEntity tableEntity = new TableEntity();
                    String name = tables.getString(DataSourceConstants.TABLE_NAME);
                    log.info("当前采集的table:{}", name);
                    tableEntity.setName(name);
                    tableEntity.setDataSourceName(dataSourceEntity.getName());
                    tableEntity.setDataSourceType(dataSourceEntity.getDataSourceType());
                    tableEntity.setDataSourceTypeName(dataSourceEntity.getDataSourceTypeName());
                    tableEntity.setDataSourceId(dataSourceEntity.getId());
                    tableEntity.setSchemaName(schema);
                    String description = tables.getString(DataSourceConstants.REMARKS);
                    tableEntity.setDescription(description);
                    tableEntity.setSchemaId(newSchema.getId());
                    tableEntity.setTableRows(countMap.get(name));
                    currentTables.put(tableEntity.getName(), tableEntity);
                }
                log.info("table采集完成:{}", currentTables);
                //表级元数据保存逻辑
                //查询现有表级元数据
                Map<String, TableEntity> oldTables = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableEntity.class).eq(TableEntity::getSchemaId, newSchema.getId()), TableEntity::getName);
                //获取待删除列表并删除
                oldTables.keySet().stream().filter(tableName -> !currentTables.containsKey(tableName)).map(tableName -> oldTables.get(tableName))
                        .forEach(table -> {
                            tableService.delete(dataSourceEntity.getId(), newSchema.getId(), table.getId());
                        });
                //获取当前表级元数据并判断修改/新增
//                String finalSchema1 = schema;
                String finalSchema2 = schema;
                List<TableEntity> currentTableList = currentTables.keySet().stream().map(tableName -> {
                            TableEntity currentTable = currentTables.get(tableName);
                            TableEntity oldTable = oldTables.get(tableName);
                            Date now = new Date();
                            if (AiShuUtil.isNotEmpty(oldTable)) {
                                currentTable.setId(oldTable.getId());
                                currentTable.setAdvancedParams(oldTable.getAdvancedParams());
                            } else {
//                                currentTable.setCreateUser(AiShuUtil.getUser().getUserName());
                                currentTable.setCreateTime(now);
                            }
                            currentTable.setUpdateTime(now);
//                            currentTable.setUpdateUser(AiShuUtil.getUser().getUserName());
//                            currentTable.setAuthorityId(AiShuUtil.getUser().getUserId());
                            //高级参数处理逻辑
                            ResultSet primaryKeys = null;
                            try {
                                primaryKeys = metaData.getPrimaryKeys(connectionParam.getDatabase(), getDbSchemaPattern(dataSource.getType(), finalSchema2, connectionParam), currentTable.getName());
                                StringBuffer primaryKeysBuffer = new StringBuffer();
                                //查询现有高级参数
                                List<AdvancedDTO> advancedParams = new ArrayList<>(JSONUtils.toList(currentTable.getAdvancedParams(), AdvancedDTO.class));
                                //主键处理
                                //取出现有主键
                                AdvancedDTO oldPrimaryKeys = tableService.getAdvancedParamByKey(currentTable, DataSourceConstants.PrimaryKeys);
                                while (primaryKeys.next()) {
                                    primaryKeysBuffer.append(primaryKeys.getString(DataSourceConstants.COLUMN_NAME)).append(",");
                                }
                                if (primaryKeysBuffer.length() > 0) {
                                    primaryKeysBuffer.deleteCharAt(primaryKeysBuffer.length() - 1);
                                    String currentPrimaryKeysStr = primaryKeysBuffer.toString();
                                    //原来没有主键参数就新增，有则改
                                    if (AiShuUtil.isEmpty(oldPrimaryKeys)) {
                                        AdvancedDTO primaryKeysDto = new AdvancedDTO();
                                        primaryKeysDto.setKey(DataSourceConstants.PrimaryKeys);
                                        primaryKeysDto.setValue(currentPrimaryKeysStr);
                                        advancedParams.add(primaryKeysDto);
                                    } else if (!currentPrimaryKeysStr.equals(oldPrimaryKeys.getValue())) {
                                        advancedParams = advancedParams.stream().map(param -> {
                                            if (DataSourceConstants.PrimaryKeys.equals(param.getKey())) {
                                                param.setValue(currentPrimaryKeysStr);
                                            }
                                            return param;
                                        }).collect(Collectors.toList());
                                    }
                                } else {//现在没有主键就清除元数据的主键参数
                                    if (AiShuUtil.isNotEmpty(oldPrimaryKeys)) {
                                        advancedParams.remove(oldPrimaryKeys);
                                    }
                                }
                                //如果是配置中心数据源，高级参数增加虚拟化目录
                                if (dataSourceEntity.getCreateUser().equals(DataSourceConstants.AF_CREATEUSER)) {
                                    //从数据源扩展参数取出虚拟化目录名
                                    Map<String, String> props = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
                                    String vCatalogName = props.get("vCatalogName");
                                    //取出现有目录名
                                    AdvancedDTO oldVCatalogName = tableService.getAdvancedParamByKey(currentTable, DataSourceConstants.VCATALOGNAME);
                                    if (StringUtils.isNotEmpty(vCatalogName)) {
                                        //原来没有虚拟目录参数就新增，有则改
                                        if (AiShuUtil.isEmpty(oldVCatalogName)) {
                                            AdvancedDTO advancedDTO = new AdvancedDTO();
                                            advancedDTO.setKey(DataSourceConstants.VCATALOGNAME);
                                            advancedDTO.setValue(vCatalogName);
                                            advancedParams.add(advancedDTO);
                                        } else if (!vCatalogName.equals(oldVCatalogName.getValue())) {
                                            advancedParams = advancedParams.stream().map(param -> {
                                                if (DataSourceConstants.VCATALOGNAME.equals(param.getKey())) {
                                                    param.setValue(vCatalogName);
                                                }
                                                return param;
                                            }).collect(Collectors.toList());
                                        }
                                    } else if (AiShuUtil.isNotEmpty(oldVCatalogName)) {//现在没有虚拟目录就清除元数据的虚拟目录参数
                                        advancedParams.remove(oldVCatalogName);
                                    }
                                }
                                currentTable.setAdvancedParams(JSONUtils.toJsonString(advancedParams));
                            } catch (Exception e) {
                                log.error("cant not get the primaryKeys in table {}: {}", currentTable.getName(), e.getMessage(), e);
                                throw new AiShuException(ErrorCodeEnum.InternalError);
                            } finally {
                                closeResult(primaryKeys);
                            }

                            return currentTable;
                        }
                ).collect(Collectors.toList());
                tableService.saveOrUpdateBatch(currentTableList);
                log.info("table更新完成:{}", currentTableList);
                //字段级元数据保存逻辑
//                log.info("开始采集columns...");
//                LambdaQueryWrapper<TableEntity> tableEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
//                tableEntityLambdaQueryWrapper.eq(TableEntity::getSchemaId, newSchema.getId());
//                currentTableList = tableService.list(tableEntityLambdaQueryWrapper);
//                if (AiShuUtil.isNotEmpty(currentTableList)) {
//                    //取此类型数据源的字段类型集合
//                    LambdaQueryWrapper<DictEntity> dictEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
//                    dictEntityLambdaQueryWrapper.eq(DictEntity::getDictType, dataSourceEntity.getDataSourceType() + 1);
////                    List<DictEntity> dictItemList = dictService.list(dictEntityLambdaQueryWrapper);
//                    //逐表处理字段元数据
//                    String finalSchema = schema;
//                    currentTableList.forEach(table -> {
//                        log.info("table:{},开始采集字段", table.getName());
//                        ResultSet fields = null;
//                        Map<String, TableFieldEntity> currentFields = new LinkedHashMap<>();
//                        Map<String, String> defaultValueMap = new HashMap<>();
//                        Map<String, String> isNullMap = new HashMap<>();
//                        //保存字段元数据
//                        try {
//                            fields = metaData.getColumns(connectionParam.getDatabase(), getDbSchemaPattern(dataSource.getType(), finalSchema, connectionParam), table.getName(), "%");
//                            while (fields.next()) {
//                                TableFieldEntity tableFieldEntity = new TableFieldEntity();
//                                tableFieldEntity.setFieldName(fields.getString(DataSourceConstants.COLUMN_NAME));
//                                log.info("当前采集的字段:{}", tableFieldEntity.getFieldName());
//                                tableFieldEntity.setFieldComment(StringUtils.isEmpty(fields.getString(DataSourceConstants.REMARKS)) ? "" : fields.getString(DataSourceConstants.REMARKS));
//                                tableFieldEntity.setFieldLength(fields.getInt(DataSourceConstants.COLUMN_SIZE));
//                                tableFieldEntity.setFieldPrecision(fields.getInt(DataSourceConstants.DECIMAL_DIGITS));
//
//                                //通过jdbctype匹配字段类型，因为jdbctype并非一一映射而废弃
////                                Integer jdbcDataType = fields.getInt(DataSourceConstants.DATA_TYPE);
////                                DictEntity typeDict = dictService.getByJdbcType(jdbcDataType, dictItemList);
////                                if (typeDict == null) {
////                                    log.info("typeDict is null");
////                                }
////                                if (AiShuUtil.isEmpty(typeDict)) {
////                                    log.error("{} have not the jdbcDataType :{}", dataSourceEntity.getDataSourceTypeName(), jdbcDataType);
////                                }
//                                //通过typename匹配字段类型
//                                String jdbcTypeName = fields.getString(DataSourceConstants.TYPE_NAME);
////                                DictEntity typeDict = dictItemList.stream().filter(dictItem ->
////                                        StringUtils.lowerCase(dictItem.getDictValue()).equals(StringUtils.lowerCase(jdbcTypeName))
////                                ).findAny().orElse(null);
////                                if (AiShuUtil.isEmpty(typeDict)) {
////                                    log.error("{} have not the jdbcTypeName :{}", dataSourceEntity.getDataSourceTypeName(), jdbcTypeName);
////                                    typeDict = dictItemList.stream().filter(dictItem ->
////                                            dictItem.getDictValue().equals("UNKNOWN")
////                                    ).findAny().orElse(null);
////                                }
////                                tableFieldEntity.setFieldType(AiShuUtil.isEmpty(typeDict)?null:typeDict.getDictKey());
//                                tableFieldEntity.setFieldType(AiShuUtil.isEmpty(jdbcTypeName) ? "UNKNOWN" : jdbcTypeName);
//                                tableFieldEntity.setTableId(table.getId());
//                                currentFields.put(tableFieldEntity.getFieldName(), tableFieldEntity);
//                                defaultValueMap.put(tableFieldEntity.getFieldName(), fields.getString(DataSourceConstants.COLUMN_DEF));
//                                isNullMap.put(tableFieldEntity.getFieldName(), fields.getString(DataSourceConstants.IS_NULLABLE));
//                            }
//                            //查询现有字段级元数据
//                            Map<String, TableFieldEntity> oldFields = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableFieldEntity.class).eq(TableFieldEntity::getTableId, table.getId()), TableFieldEntity::getFieldName);
//                            //声明版本变更标识
//                            AtomicReference<Boolean> versionFlag = new AtomicReference<>(false);
//                            //获取待删除列表并删除
//                            List<Long> deleteList = oldFields.keySet().stream().filter(fieldName -> !currentFields.containsKey(fieldName)).map(fieldName -> oldFields.get(fieldName).getId()).collect(Collectors.toList());
//                            if (AiShuUtil.isNotEmpty(deleteList)) {
//                                versionFlag.set(true);
//                                tableFieldService.removeBatchByIds(deleteList);
//                            }
//                            //获取当前字段级元数据并判断修改/新增
//
//                            List<TableFieldEntity> currentTableFieldList = currentFields.keySet().stream().map(fieldName -> {
//                                        TableFieldEntity currentField = currentFields.get(fieldName);
//                                        TableFieldEntity oldField = oldFields.get(fieldName);
//                                        //旧表发现新字段版本变更
//                                        if (AiShuUtil.isNotEmpty(oldField)) {
//                                            currentField.setId(oldField.getId());
//                                            currentField.setVersion(oldField.getVersion());
//                                        } else if (AiShuUtil.isNotEmpty(oldFields)) {
//                                            versionFlag.set(true);
//                                        }
//
//                                        //高级参数处理逻辑
//                                        try {
//                                            List<AdvancedDTO> advancedParams = new ArrayList<>(com.eisoo.metadatamanage.web.util.JSONUtils.toList(currentField.getAdvancedParams(), AdvancedDTO.class));
//                                            //取出是否主键参数有则改，无则增
//                                            AdvancedDTO oldCheckPrimaryKey = tableFieldService.getAdvancedParamByKey(currentField, DataSourceConstants.CHECKPRIMARYKEY);
//                                            String isPrimaryKeyStr = tableService.isPrimaryKey(currentField.getFieldName(), table);
//                                            AdvancedDTO checkPrimaryKey = new AdvancedDTO();
//                                            if (AiShuUtil.isEmpty(oldCheckPrimaryKey)) {
//                                                checkPrimaryKey.setKey(DataSourceConstants.CHECKPRIMARYKEY);
//                                                checkPrimaryKey.setValue(isPrimaryKeyStr);
//                                                advancedParams.add(checkPrimaryKey);
//                                            } else if (!isPrimaryKeyStr.equals(oldCheckPrimaryKey.getValue())) {
//                                                advancedParams = advancedParams.stream().map(param -> {
//                                                    if (DataSourceConstants.PrimaryKeys.equals(param.getKey())) {
//                                                        param.setValue(isPrimaryKeyStr);
//                                                    }
//                                                    return param;
//                                                }).collect(Collectors.toList());
//                                            }
//                                            //取出默认值参数有则改，无则增
//                                            AdvancedDTO oldColumnDef = tableFieldService.getAdvancedParamByKey(currentField, DataSourceConstants.COLUMN_DEF);
//                                            String defaultValueStr = defaultValueMap.get(currentField.getFieldName());
//                                            AdvancedDTO columnDef = new AdvancedDTO();
//                                            if (AiShuUtil.isEmpty(oldColumnDef)) {
//                                                columnDef.setKey(DataSourceConstants.COLUMN_DEF);
//                                                columnDef.setValue(String.valueOf(defaultValueStr));
//                                                advancedParams.add(columnDef);
//                                            } else if (!String.valueOf(defaultValueStr).equals(String.valueOf(oldCheckPrimaryKey.getValue()))) {
//                                                advancedParams = advancedParams.stream().map(param -> {
//                                                    if (DataSourceConstants.COLUMN_DEF.equals(param.getKey())) {
//                                                        param.setValue(String.valueOf(defaultValueStr));
//                                                    }
//                                                    return param;
//                                                }).collect(Collectors.toList());
//                                            }
//                                            //取出是否为空参数有则改，无则增
//                                            AdvancedDTO oldIsNullAble = tableFieldService.getAdvancedParamByKey(currentField, DataSourceConstants.IS_NULLABLE);
//                                            String isNullAbleStr = isNullMap.get(currentField.getFieldName());
//                                            AdvancedDTO isNullAble = new AdvancedDTO();
//                                            if (AiShuUtil.isEmpty(oldIsNullAble)) {
//                                                isNullAble.setKey(DataSourceConstants.IS_NULLABLE);
//                                                isNullAble.setValue(String.valueOf(isNullAbleStr));
//                                                advancedParams.add(isNullAble);
//                                            } else if (!String.valueOf(isNullAbleStr).equals(String.valueOf(oldIsNullAble.getValue()))) {
//                                                advancedParams = advancedParams.stream().map(param -> {
//                                                    if (DataSourceConstants.IS_NULLABLE.equals(param.getKey())) {
//                                                        param.setValue(String.valueOf(isNullAbleStr));
//                                                    }
//                                                    return param;
//                                                }).collect(Collectors.toList());
//                                            }
//
//                                            //保存高级参数
//                                            currentField.setAdvancedParams(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(advancedParams));
//                                        } catch (Exception e) {
//                                            log.error("cant not get the advancedParams in table {}: {}", currentField.getFieldName(), e.getMessage(), e);
//                                            throw new AiShuException(ErrorCodeEnum.InternalError);
//                                        } finally {
////                                            closeResult(primaryKeys);
//                                        }
//                                        versionFlag.set(isChangeVersion(versionFlag.get(), oldField, currentField));
//                                        return currentField;
//                                    }
//                            ).collect(Collectors.toList());
//                            //如果版本变更，保存历史字段
//                            if (versionFlag.get() == true) {
//                                tableFieldHisService.saveBatch(oldFields.keySet().stream().map(fieldName -> {
//                                    TableFieldHisEntity tableFieldHisEntity = new TableFieldHisEntity();
//                                    AiShuUtil.copyProperties(oldFields.get(fieldName), tableFieldHisEntity);
//                                    return tableFieldHisEntity;
//                                }).collect(Collectors.toList()));
//                                //字段版本号+1
//                                tableFieldService.saveOrUpdateBatch(currentTableFieldList.stream().map(item -> {
//                                    item.setVersion(table.getVersion() + 1);
//                                    return item;
//                                }).collect(Collectors.toList()));
//                                //表版本号+1
//                                table.setVersion(table.getVersion() + 1);
//                                tableService.updateById(table);
//                            } else {
//                                tableFieldService.saveOrUpdateBatch(currentTableFieldList);
//                            }
//                            log.info("当前table:{}字段保存完成", table.getName());
//                        } catch (SQLException e) {
//                            log.error("cant not get the fields : {}", e.getMessage(), e);
//                            throw new AiShuException(ErrorCodeEnum.InternalError);
//                        } finally {
//                            closeResult(fields);
//                        }
//                    });
//                    //保存主键
////                    if(AiShuUtil.isNotEmpty(primaryKeysList)) {
////                        tableService.saveOrUpdateBatch(primaryKeysList);
////                    }
//                }
                //记录任务状态
                taskEntity.setEndTime(new Date());
                taskEntity.setStatus(0);
                updateById(taskEntity);
            } catch (Exception e) {
                //记录任务状态
                taskEntity.setEndTime(new Date());
                taskEntity.setStatus(1);
                updateById(taskEntity);
                log.error(e.toString(), e);
            } finally {
                closeResult(tables);
                releaseConnection(connection);
            }
            log.info("采集任务结束，任务id：{}", taskEntity.getId());
            writeLog(taskEntity.getId());
        }
    }

    /**
     * 采集元数据
     *
     * @param dataSourceEntity
     * @param taskEntity
     */
    public void fillMetaDataExecByVirtual(DataSourceEntityDataConnection dataSourceEntity, TaskScanEntity taskEntity) {
        MDC.put("taskId", String.valueOf(taskEntity.getId()));
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            SchemaEntity schemaEntity = new SchemaEntity();
            try {
                log.info("采集任务开始，任务id：{}", taskEntity.getId());
//                //连接器处理逻辑
                dipDataSourceService.createConnector();
                //schema处理逻辑
                //查询现有高级参数
//                JSONObject jsonProperty = null;
//                try {
//                    jsonProperty = JSONObject.parse(new String(dataSourceEntity.getBinData()));
//                } catch (Exception e) {
//                    log.error("获取数据源的binData失败，dataSourceEntity={}", dataSourceEntity, e);
//                    throw new AiShuException(ErrorCodeEnum.UnKnowException);
//                }
//                String schemaName = jsonProperty.getString("database_name").toLowerCase();

//                if (jsonProperty.containsKey("schema")) {
//                    schemaName = jsonProperty.getString("schema").toLowerCase();
//                }
                String schemaName = dataSourceEntity.getFDatabase();
                if (schemaName != null) {
                    schemaName = schemaName.toLowerCase();
                }
                String fSchema = dataSourceEntity.getFSchema();
                if (AiShuUtil.isNotEmpty(fSchema)) {
                    schemaName = fSchema.toLowerCase();
                }
//                String catalogName = jsonProperty.getString("catalog_name").toLowerCase();
                String catalogName = dataSourceEntity.getFCatalog().toLowerCase();
//                String typeName = dataSourceEntity.getTypeName();
                String typeName = dataSourceEntity.getFType();
                // currentSchema=vega&vCatalogName=mysql_7a5ch77y&vConnector=mysql&
                String extendProperty = "currentSchema=" + schemaName + "&vCatalogName=" + catalogName + "&vConnector=" + typeName.toLowerCase() + "&";
                Map<String, String> extendPropertyMap = JSONUtils.props2Map(extendProperty);
                String catalog = extendPropertyMap.get(DataSourceConstants.VCATALOGNAME);
                String schema = extendPropertyMap.get(DataSourceConstants.SCHEMAKEY);//currentSchema
                log.info("开始采集schema={}", schema);
                if (AiShuUtil.isEmpty(schema)) {
                    throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
                }
                //schema保存逻辑
                //查询结果转换schema实体类
                schemaEntity.setName(schema);
                schemaEntity.setDataSourceName(dataSourceEntity.getFName());
                LambdaQueryWrapper<DictEntity> dictWrapper = new LambdaQueryWrapper<>();
                dictWrapper.eq(DictEntity::getDictType, 1);
                List<DictEntity> dictList = dictService.list(dictWrapper);
                DictEntity dictEntity = dataSourceService.convertDsType(typeName,
                        null,
                        dictList);
//                Integer dictKey = dictMapper.getDictKey(dataSourceEntity.getTypName().toLowerCase());
                schemaEntity.setDataSourceType(dictEntity.getDictKey());
                schemaEntity.setDataSourceTypeName(typeName);
                schemaEntity.setDataSourceId(dataSourceEntity.getFId());
                //查询是否现有schema
                LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getDataSourceId, schemaEntity.getDataSourceId());
                schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getName, schemaEntity.getName());
                SchemaEntity oldSchema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                Date now = new Date();
                if (AiShuUtil.isNotEmpty(oldSchema)) {
                    schemaEntity.setId(oldSchema.getId());
                } else {
                    schemaEntity.setId(IdWorker.getId());
                    schemaEntity.setCreateTime(now);
                }
                schemaEntity.setUpdateTime(now);
                //有则改，无则增
                schemaService.saveOrUpdate(schemaEntity);
                SchemaEntity newSchema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                //表级元数据处理逻辑
                log.info("开始采集tables...");
                long start = System.currentTimeMillis();
                Map<String, TableEntity> currentTables = new HashMap<>();
                VirtualTableListDto tableListDto = virtualService.getTable(catalog, schema);
                List<VirtualTableListDto.VirtualTableDto> virtualTableDtoList = tableListDto.getData();
                log.info("读取表级元数据耗时ms:" + (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                List<TableEntity> saveList = new ArrayList<>();
                List<TableEntity> updateList = new ArrayList<>();
                if (AiShuUtil.isNotEmpty(virtualTableDtoList)) {
                    virtualTableDtoList.forEach(virtualTableDto -> {
                        TableEntity tableEntity = new TableEntity();
                        String name = virtualTableDto.getTable();
                        tableEntity.setName(name);
                        tableEntity.setDataSourceName(dataSourceEntity.getFName());
                        tableEntity.setDataSourceType(dictEntity.getDictKey());
                        tableEntity.setDataSourceTypeName(typeName);
                        tableEntity.setDataSourceId(dataSourceEntity.getFId());
                        tableEntity.setSchemaName(schema);
                        //表级注释
                        tableEntity.setDescription(virtualTableDto.getComment());
                        tableEntity.setSchemaId(newSchema.getId());
                        String tableType = virtualTableDto.getTableType();
                        Integer type = null;
                        if (AiShuUtil.isNotEmpty(tableType)) {
                            if ("table".equalsIgnoreCase(tableType)) {
                                type = 0;
                            } else if ("view".equalsIgnoreCase(tableType)) {
                                type = 1;
                                String dsType = tableEntity.getDataSourceTypeName();
                                if (("mysql".equalsIgnoreCase(dsType) || "maria".equalsIgnoreCase(dsType)) && "VIEW".equals(tableEntity.getDescription())) {
                                    tableEntity.setDescription("");
                                }
                            }
                        }
                        tableEntity.setFScanSource(type);
                        //Todo 数据量虚拟化未提供
                        List<AdvancedDTO> advancedParams = new ArrayList<>();
                        AdvancedDTO advancedDTO = new AdvancedDTO();
                        advancedDTO.setKey(DataSourceConstants.VCATALOGNAME);//vCatalogName
                        advancedDTO.setValue(catalog);
                        advancedParams.add(advancedDTO);
                        Object params = virtualTableDto.getParams();
                        if (AiShuUtil.isNotEmpty(params)) {
                            Map<String, Object> paramsMap = (Map<String, Object>) params;
                            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                                AdvancedDTO tmp = new AdvancedDTO();
                                tmp.setKey(entry.getKey());
                                Object value = entry.getValue();
                                if (value != null) {
                                    tmp.setValue(String.valueOf(value));
                                } else {
                                    tmp.setValue(null);
                                }
                                advancedParams.add(tmp);
                            }
                        }
                        String jsonString = JSONUtils.toJsonString(advancedParams);
                        tableEntity.setAdvancedParams(jsonString);
                        currentTables.put(tableEntity.getName(), tableEntity);
                    });
                }
                log.info("构建表级元数据Map耗时ms:" + (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                //表级元数据保存逻辑
                //查询现有表级元数据
//                Map<String, TableEntity> oldTables = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableEntity.class).eq(TableEntity::getSchemaId, newSchema.getId()), TableEntity::getName);
                Map<String, TableEntity> oldTables = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableEntity.class)
                        .eq(TableEntity::getDataSourceId, dataSourceEntity.getFId()), TableEntity::getName);
                //获取待删除列表并删除
                List<TableEntity> deleteTableList = oldTables.keySet().stream().filter(tableName -> !currentTables.containsKey(tableName)).map(tableName -> {
                    TableEntity tableEntity = oldTables.get(tableName);
                    if (tableEntity.getDeleted().equals(0)) {
                        tableEntity.setDeleted(1);
                        tableEntity.setDeleteTime(now);
                    }
                    return tableEntity;
                }).collect(Collectors.toList());
                if (AiShuUtil.isNotEmpty(deleteTableList)) {
                    tableService.updateBatchById(deleteTableList);
                    log.info("删除表级元数据耗时ms:" + (System.currentTimeMillis() - start));
                }
                start = System.currentTimeMillis();
                //获取当前表级元数据并判断修改/新增
                currentTables.keySet().stream().forEach(tableName -> {
                            TableEntity currentTable = currentTables.get(tableName);
                            TableEntity oldTable = oldTables.get(tableName);
                            if (AiShuUtil.isNotEmpty(oldTable)) {
                                currentTable.setId(oldTable.getId());
                            }
                            if (AiShuUtil.isEmpty(currentTable.getId())) {
                                currentTable.setDeleted(0);
                                currentTable.setVersion(0);
                                currentTable.setTableRows(AiShuUtil.isEmpty(currentTable.getTableRows()) ? 0 : currentTable.getTableRows());
                                currentTable.setAuthorityId("");
                                currentTable.setCreateTime(now);
                                currentTable.setCreateUser("");
                                currentTable.setUpdateTime(now);
                                currentTable.setUpdateUser("");
                                saveList.add(currentTable);
                            } else {
                                currentTable.setDeleted(0);
                                currentTable.setUpdateTime(now);
                                updateList.add(currentTable);
                            }
                        }
                );
                log.info("更新表级元数据map耗时ms:{}", (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                tableService.saveBatch(saveList, 1000);
                tableService.updateBatchById(updateList, 1000);
                LambdaUpdateWrapper<TableEntity> updateWrapper = Wrappers.lambdaUpdate();
                updateWrapper.eq(TableEntity::getDeleted, 0);
                updateWrapper.set(TableEntity::getDeleteTime, null);
                tableService.update(updateWrapper);
                log.info("表级元数据更新入库耗时ms:{}", (System.currentTimeMillis() - start));
                log.info("提交字段元数据采集接口");
                start = System.currentTimeMillis();
//                String collectResult = virtualService.collectMetadata(catalog, schema, dataSourceEntity.getFId(), schemaEntity.getId());
                String collectResult = virtualService.collectMetadata(catalog, schema, dataSourceEntity.getFId(), schemaEntity.getId(), taskEntity.getId());
                log.info("采集字段元数据结果【异步】：{}，耗时{}ms", collectResult, (System.currentTimeMillis() - start));
                if (!"success".equals(collectResult)) {
                    //记录任务状态:// 更新状态:0 wait 1 running 2 success 3 fail
                    taskEntity.setEndTime(new Date());
                    taskEntity.setScanStatus(3);
//                updateById(taskEntity);
                    taskScanMapper.updateById(taskEntity);
                }

            } catch (Exception e) {
                //记录任务状态// 更新状态:0 wait 1 running 2 success 3 fail
                taskEntity.setEndTime(new Date());
                taskEntity.setScanStatus(3);
//                updateById(taskEntity);
                taskScanMapper.updateById(taskEntity);
                log.error(e.toString(), e);
            }
            log.info("采集任务结束，任务id：{}", taskEntity.getId());
//            writeLog(taskEntity.getId());
        }
    }

    /**
     * 通过虚拟化接口更新schema
     *
     * @param dataSourceEntity
     */
    @Override
    public void updateSchemaByVirtual(DataSourceEntity dataSourceEntity) {
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            log.info("开始更新数据源-{}的schema", dataSourceEntity.getName());
            long start = System.currentTimeMillis();
            try {
                //连接器处理逻辑
                dipDataSourceService.createConnector();
                Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
//                String catalog = extendPropertyMap.get(DataSourceConstants.VCATALOGNAME);
                String schemaName = extendPropertyMap.get(DataSourceConstants.SCHEMAKEY);
                SchemaEntity schemaEntity = new SchemaEntity();
                schemaEntity.setName(schemaName);
                schemaEntity.setDataSourceName(dataSourceEntity.getName());
                schemaEntity.setDataSourceType(dataSourceEntity.getDataSourceType());
                schemaEntity.setDataSourceTypeName(dataSourceEntity.getDataSourceTypeName());
                schemaEntity.setDataSourceId(dataSourceEntity.getId());
                //查询是否现有schema
                LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getDataSourceId, schemaEntity.getDataSourceId());
                schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getName, schemaEntity.getName());
                SchemaEntity oldSchema = schemaService.getOne(schemaEntityLambdaQueryWrapper, false);
                Date now = new Date();
                if (AiShuUtil.isNotEmpty(oldSchema)) {
                    schemaEntity.setId(oldSchema.getId());
                } else {
                    schemaEntity.setId(IdWorker.getId());
                    schemaEntity.setCreateTime(now);
                }
                schemaEntity.setUpdateTime(now);
                //有则改，无则增
                schemaService.saveOrUpdate(schemaEntity);
            } catch (Exception e) {
                //记录任务状态
                log.error(e.toString(), e);
            }
            log.info("更新数据源-{}的schema结束，耗时：{}", dataSourceEntity.getName(), (System.currentTimeMillis() - start));
        }
    }

    /**
     * 通过虚拟化接口更新表级元数据
     *
     * @param dataSourceEntityList
     * @param ddlDtoList
     */
    @Override
    public void updateTableByVirtual(List<DataSourceEntity> dataSourceEntityList, List<LiveDdlDto> ddlDtoList) {
        if (AiShuUtil.isNotEmpty(dataSourceEntityList) && AiShuUtil.isNotEmpty(ddlDtoList)) {
            long start = System.currentTimeMillis();
            //表级更新
            //判断是否需要全表更新
            if (ddlDtoList.stream().anyMatch(ddl -> !ddl.getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE) &&
                    (ddl.getSchemaId() == null || StringUtils.contains(StringUtils.lowerCase(ddl.getStatement()), "exists")))) {
                LiveDdlDto dto = ddlDtoList.stream().filter(ddl -> !ddl.getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE) &&
                        (ddl.getSchemaId() == null || StringUtils.contains(StringUtils.lowerCase(ddl.getStatement()), "exists"))).findAny().orElse(null);
                log.info("待更新的dto:{}", dto);
                dataSourceEntityList.forEach(i -> {
                    updateTableByVirtual(i);
                });
                log.info("存在无法确认schema或无法确认执行效果的ddl，进行全表更新，耗时{}ms", System.currentTimeMillis() - start);
            } else {
                for (LiveDdlDto ddlDto : ddlDtoList) {
                    log.info("待更新的ddlDto:{}", ddlDto);
                    if (!ddlDto.getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE)) {
                        try {
                            switch (ddlDto.getType()) {
                                case CreateTable:
                                    TableEntity table = tableService.getOne(ddlDto.getDataSourceId(), ddlDto.getSchemaId(), ddlDto.getTableName());
                                    if (AiShuUtil.isEmpty(table)) {
                                        table = new TableEntity();
                                    }
                                    table.setName(ddlDto.getTableName());
                                    DataSourceEntity dataSourceEntity = dataSourceEntityList.stream().filter(ds -> ds.getId().equals(ddlDto.getDataSourceId())).findAny().orElse(null);
                                    if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
                                        List<AdvancedDTO> advancedParams = new ArrayList<>();
                                        AdvancedDTO advancedDTO = new AdvancedDTO();
                                        advancedDTO.setKey(DataSourceConstants.VCATALOGNAME);
                                        Map<String, String> props = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
                                        String vCatalogName = AiShuUtil.isEmpty(props) ? null : props.get("vCatalogName");
                                        advancedDTO.setValue(vCatalogName);
                                        advancedParams.add(advancedDTO);
                                        table.setAdvancedParams(JSONUtils.toJsonString(advancedParams));
                                    }
                                    table.setDescription(ddlDto.getComment());
                                    table.setSchemaId(ddlDto.getSchemaId());
                                    table.setSchemaName(ddlDto.getSchemaName());
                                    table.setDataSourceId(ddlDto.getDataSourceId());
                                    table.setDataSourceName(ddlDto.getDataSourceName());
                                    table.setDataSourceType(ddlDto.getDatasourceType());
                                    table.setDataSourceTypeName(ddlDto.getDatasourceTypeName());
                                    table.setVersion(0);
                                    table.setCreateTime(ddlDto.getMonitorTime());
                                    table.setUpdateTime(ddlDto.getMonitorTime());
                                    table.setDeleted(0);
                                    LambdaUpdateWrapper<TableEntity> updateWrapper = Wrappers.lambdaUpdate();
                                    updateWrapper.eq(TableEntity::getId, table.getId());
                                    updateWrapper.set(TableEntity::getDeleteTime, null);
                                    tableService.saveOrUpdate(table, updateWrapper);
                                    ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_INC);
                                    break;
                                case CommentTable:
                                    table = tableService.getOne(ddlDto.getDataSourceId(), ddlDto.getSchemaId(), ddlDto.getTableName());
                                    if (AiShuUtil.isNotEmpty(table)) {
                                        table.setDescription(ddlDto.getComment());
                                        tableService.updateById(table);
                                        ddlDto.setTableId(table.getId());
                                    }
                                    ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_INC);
                                    break;
                                case RenameTable:
                                    table = tableService.getOne(ddlDto.getDataSourceId(), ddlDto.getSchemaId(), ddlDto.getTableName());
                                    if (AiShuUtil.isNotEmpty(table)) {
                                        table.setName(ddlDto.getTargetTable());
                                        tableService.updateById(table);
                                    }
                                    ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_INC);
                                    break;
                                case DropTable:
                                    table = tableService.getOne(ddlDto.getDataSourceId(), ddlDto.getSchemaId(), ddlDto.getTableName());
                                    if (AiShuUtil.isNotEmpty(table)) {
                                        table.setDeleted(1);
                                        table.setDeleteTime(ddlDto.getMonitorTime());
                                        tableService.updateById(table);
                                        ddlDto.setTableId(table.getId());
                                    }
                                    ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_INC);
                                    break;
                            }
                        } catch (Exception e) {
                            ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_FAIL);
                            ddlDto.setUpdateMessage(e.toString());
                        }
                    }
                }
                log.info("ddl表更新，耗时{}ms", System.currentTimeMillis() - start);
            }

            //字段级更新
            start = System.currentTimeMillis();
            List<TableEntity> tableEntityList = new ArrayList<>();
            Map<TableEntity, List<TableFieldEntity>> tableEntityListMap = new HashMap<>();
            //判断是否需要通过全表比对更新字段
            if (ddlDtoList.stream().anyMatch(ddl -> !ddl.getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE) && ddl.getSchemaId() == null)) {
                List<String> dsIdList = dataSourceEntityList.stream().map(ds -> ds.getId()).collect(Collectors.toList());
                LambdaQueryWrapper<TableEntity> tableEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                tableEntityLambdaQueryWrapper.in(TableEntity::getDataSourceId, dsIdList);
                tableEntityList = tableService.list(tableEntityLambdaQueryWrapper);
                log.info("存在无法确定schema的ddl,加载待更新数据源全部表级元数据，耗时{}ms", System.currentTimeMillis() - start);
            }
            start = System.currentTimeMillis();
            for (LiveDdlDto ddlDto : ddlDtoList) {
                if (!ddlDto.getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE) && !ddlDto.getUpdateStatus().equals(DdlUpdateStatusEnum.PARSE_FAIL)) {
                    try {
                        switch (ddlDto.getType()) {
                            case AlterColumn:
                            case CreateTable:
                            case CommentColumn:
                            case DropTable:
                                List<TableEntity> tableList = new ArrayList<>();
                                if (AiShuUtil.isNotEmpty(ddlDto.getSchemaId())) {
                                    TableEntity table = tableService.getOne(ddlDto.getDataSourceId(), ddlDto.getSchemaId(), ddlDto.getTableName());
                                    ddlDto.setTableId(table.getId());
                                    tableList.add(table);
                                } else {
                                    for (TableEntity table : tableEntityList) {
                                        Boolean containFlag = table.getName().equals(ddlDto.getTableName());
                                        if (containFlag) {
                                            tableList.add(table);
                                        }
                                    }
                                }
                                if (AiShuUtil.isNotEmpty(tableList)) {
                                    ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_INC);
                                    tableList.forEach(table -> {
                                        DataSourceEntity ds = dataSourceEntityList.stream().filter(i -> i.getId().equals(table.getDataSourceId())).findAny().orElse(null);
                                        if (AiShuUtil.isNotEmpty(ds)) {
                                            getColumnByVirtual(ds, table, tableEntityListMap);
                                        }
                                    });
                                }
                                break;
                        }
                    } catch (Exception e) {
                        ddlDto.setUpdateMessage(e.toString());
                        ddlDto.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_FAIL);
                    }
                }
            }
            log.info("生成实时待更新字段级元数据map，耗时{}ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            saveOrUpdateColumn(tableEntityListMap);
            log.info("实时更新字段级元数据，耗时{}ms", System.currentTimeMillis() - start);
        }
    }

    @Override
    public Result<?> updateMetaData() {
        if (!schedulerConfig.getTrigger()) {
            return Result.success();
        }

        LambdaQueryWrapper<DataSourceEntity> dataSourceEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
        List<Integer> statusIgnoreList = new ArrayList<>();
        statusIgnoreList.add(DataSourceUpdateStatusEnum.IGNORE.getCode());
        statusIgnoreList.add(DataSourceUpdateStatusEnum.UPDATING.getCode());
        dataSourceEntityLambdaQueryWrapper.notIn(DataSourceEntity::getLiveUpdateStatus, statusIgnoreList);
        List<DataSourceEntity> dataSourceEntityList = dataSourceService.list(dataSourceEntityLambdaQueryWrapper);
        if (AiShuUtil.isNotEmpty(dataSourceEntityList)) {
            dataSourceEntityList.forEach(dataSourceEntity -> {
                //中止机制
                LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, dataSourceEntity.getId());
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.STOP_TASK.getCode());
                taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
                TaskEntity stopTask = getOne(taskEntityLambdaQueryWrapper, false);
                if (AiShuUtil.isNotEmpty(stopTask) && !dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.BROADCASTING.getCode())) {
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.IGNORE.getCode());
                    dataSourceService.updateById(dataSourceEntity);
                    stopTask.setStatus(TaskStatusEnum.SUCCESS.getCode());
                    Date now = new Date();
                    stopTask.setEndTime(now);
                    updateById(stopTask);
                    //如果全部数据源都中止，则关闭调度任务
                    dataSourceEntityLambdaQueryWrapper.clear();
                    dataSourceEntityLambdaQueryWrapper.ne(DataSourceEntity::getLiveUpdateStatus, DataSourceUpdateStatusEnum.IGNORE.getCode());
                    dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
                    if (dataSourceService.count(dataSourceEntityLambdaQueryWrapper) == 0L) {
                        try {
                            String stopProcessUrl = String.format("%s/cron/online", schedulerConfig.getSchedulerProcessUrl());
                            CronOnlineDto cronOnlineDto = new CronOnlineDto();
                            cronOnlineDto.setProcess_uuid(schedulerConfig.getProcessUuid());
                            cronOnlineDto.setCrontab_status(CronStatusEnum.OffLine.getCode());
                            String cronOnlineStr = com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(cronOnlineDto);
                            String resposeStr = HttpUtil.executePostWithJson(stopProcessUrl, cronOnlineStr, null);
                            log.info("尝试停止元数据更新触发器定时服务，删除结果为{}", resposeStr);
                        } catch (Exception e) {
                            log.error("尝试停止元数据更新触发器定时服务失败，原因为{}", e.toString());
                        }
                    }
                    //如果同地址端口全部mysql数据源中止，则关闭binlog
                    if ("MySQL".equals(dataSourceEntity.getDataSourceTypeName())) {
                        dataSourceEntityLambdaQueryWrapper.clear();
                        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDataSourceTypeName, "MySQL");
                        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getHost, dataSourceEntity.getHost());
                        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getPort, dataSourceEntity.getPort());
                        dataSourceEntityLambdaQueryWrapper.ne(DataSourceEntity::getLiveUpdateStatus, DataSourceUpdateStatusEnum.IGNORE.getCode());
                        dataSourceEntityLambdaQueryWrapper.eq(DataSourceEntity::getDeleteCode, 0);
                        if (dataSourceService.count(dataSourceEntityLambdaQueryWrapper) == 0L) {
                            dataSourceService.stopMysqlBinlog(dataSourceEntity);
                        }
                    }
                    return;
                }
                if (dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.BROADCASTING.getCode())) {
                    //通知流程
                    LambdaQueryWrapper<LiveDdlEntity> liveDdlEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    liveDdlEntityLambdaQueryWrapper.eq(LiveDdlEntity::getDataSourceId, dataSourceEntity.getId());
                    liveDdlEntityLambdaQueryWrapper.eq(LiveDdlEntity::getPushStatus, DdlPushStatusEnum.PUSH_WAITING.getCode());
                    List<LiveDdlEntity> liveDdlEntityList = liveDdlService.list(liveDdlEntityLambdaQueryWrapper);
                    if (AiShuUtil.isNotEmpty(liveDdlEntityList)) {
                        liveDdlEntityList.forEach(ddl -> {
                            String message = JSONUtils.toJsonString(ddl);
                            liveDdlProducer.sendMessage(Constants.MQ_LIVEDDL_PRODUCER_TOPIC, message);
                            ddl.setPushStatus(DdlPushStatusEnum.PUSH_FINISH.getCode());
                        });
                    }
                    liveDdlService.updateBatchById(liveDdlEntityList);
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.WAITING.getCode());
                    dataSourceService.updateById(dataSourceEntity);
                } else {
                    //更新流程
                    updateMetaData(dataSourceEntity);
                }
            });
        }
        return Result.success();
    }

    private void updateMetaData(DataSourceEntity dataSourceEntity) {
        Boolean updateFlag = true;
        if (!DataSourceUpdateStatusEnum.WAITING.getCode().equals(dataSourceEntity.getLiveUpdateStatus())
                && !DataSourceUpdateStatusEnum.UNAVAILABLE.getCode().equals(dataSourceEntity.getLiveUpdateStatus())
                && !DataSourceUpdateStatusEnum.UNAUTHORIZED.getCode().equals(dataSourceEntity.getLiveUpdateStatus())) {
            updateFlag = false;
        }
        LambdaQueryWrapper<TaskEntity> taskEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectId, dataSourceEntity.getId());
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getObjectType, TaskObjectTypeEnum.DATASOURCE.getCode());
        taskEntityLambdaQueryWrapper.eq(TaskEntity::getStatus, TaskStatusEnum.ONGOING.getCode());
        if (baseMapper.exists(taskEntityLambdaQueryWrapper)) {
            updateFlag = false;
        }
        if (updateFlag) {
            dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.UPDATING.getCode());
            dataSourceService.updateById(dataSourceEntity);
            fillMetaDataExecutorPool.submit(() -> updateMetaDataExec(dataSourceEntity));
        }
    }

    private void updateMetaDataExec(DataSourceEntity dataSourceEntity) {
        long start = System.currentTimeMillis();
        List<DataSourceEntity> relativeList = new ArrayList<>();
        List<LiveDdlDto> liveDdlDtoList = new ArrayList<>();
        Connection connection = null;
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            if (DbType.POSTGRESQL.getDescp().equals(StringUtils.lowerCase(dataSourceEntity.getDataSourceTypeName()))) {
                //创建存放ddl数据源连接
                connection = getDdlConnection(dataSourceEntity);
                if (AiShuUtil.isEmpty(connection)) {
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.UNAVAILABLE.getCode());
                    dataSourceService.updateById(dataSourceEntity);
                    throw new AiShuException(ErrorCodeEnum.Unavailable, "存放ddl数据源连接不可用");
                }
                log.info("创建数据源连接成功,耗时{}ms", System.currentTimeMillis() - start);
                //分类注册ddl监听器
                commitDdlMonitor(dataSourceEntity, connection);
            }
            relativeList.add(dataSourceEntity);
            LambdaQueryWrapper<SchemaEntity> schemaEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
            schemaEntityLambdaQueryWrapper.eq(SchemaEntity::getDataSourceId, dataSourceEntity.getId());
            List<SchemaEntity> schemaEntityList = schemaService.list(schemaEntityLambdaQueryWrapper);
            //若schema不存在实时更新schema
            if (AiShuUtil.isEmpty(schemaEntityList)) {
                updateSchemaByVirtual(dataSourceEntity);
                schemaEntityList = schemaService.list(schemaEntityLambdaQueryWrapper);
            }

            addLiveDdlDtoList(dataSourceEntity, liveDdlDtoList, connection, schemaEntityList, relativeList);
            if (AiShuUtil.isNotEmpty(liveDdlDtoList)) {
                //优化合并更新操作
                try {
                    liveDdlDtoList = liveDdlDtoList.stream().sorted(Comparator.comparing(LiveDdlDto::getMonitorTime).reversed()).collect(Collectors.toList());
                    for (int i = 0; i < liveDdlDtoList.size(); i++) {
                        if (AiShuUtil.isEmpty(liveDdlDtoList.get(i).getSchemaId())
                                && !DdlUpdateStatusEnum.PARSE_FAIL.equals(liveDdlDtoList.get(i).getUpdateStatus())
                                && !liveDdlDtoList.get(i).getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE)) {
                            liveDdlDtoList.get(i).setUpdateStatus(DdlUpdateStatusEnum.UPDATE_ALL);
                        }
                        if (!liveDdlDtoList.get(i).getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE) && AiShuUtil.isNotEmpty(liveDdlDtoList.get(i).getSchemaId())) {
                            if (liveDdlDtoList.get(i).getType().equals(DdlTypeEnum.CreateTable)) {
                                Long currentSchemaId = liveDdlDtoList.get(i).getSchemaId();
                                String currentTableName = liveDdlDtoList.get(i).getTableName();
                                liveDdlDtoList.forEach(s -> {
                                    if (s.getSchemaId().equals(currentSchemaId) && s.getTableName().equals(currentTableName) && s.getAffect().equals(DdlAffectEnum.Column)) {
                                        s.setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
                                    }
                                });
                            }
                            for (int j = i + 1; j < liveDdlDtoList.size(); j++) {
                                if (!liveDdlDtoList.get(j).getUpdateStatus().equals(DdlUpdateStatusEnum.UPDATE_IGNORE)
                                        && liveDdlDtoList.get(j).getTableName().equals(liveDdlDtoList.get(i).getTableName())
                                        && liveDdlDtoList.get(i).getSchemaId().equals(liveDdlDtoList.get(j).getSchemaId())) {
                                    switch (liveDdlDtoList.get(i).getType()) {
                                        case DropTable:
                                            liveDdlDtoList.get(j).setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
                                            break;
                                        case CommentTable:
                                            if (liveDdlDtoList.get(j).getType().equals(DdlTypeEnum.CommentTable)) {
                                                liveDdlDtoList.get(j).setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
                                            }
                                            break;
                                        case RenameTable:
                                            if (liveDdlDtoList.get(j).getType().equals(DdlTypeEnum.RenameTable)) {
                                                liveDdlDtoList.get(j).setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
                                            }
                                            break;
                                        case AlterColumn:
                                        case CommentColumn:
                                            if (liveDdlDtoList.get(j).getAffect().equals(DdlAffectEnum.Column)) {
                                                liveDdlDtoList.get(j).setUpdateStatus(DdlUpdateStatusEnum.UPDATE_IGNORE);
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(e.toString());
                }

                //实时更新表及字段元数据
                updateTableByVirtual(relativeList, liveDdlDtoList);
                //记录ddl更新结果
                if (AiShuUtil.isNotEmpty(liveDdlDtoList)) {
                    List<LiveDdlEntity> liveDdlEntityList = new ArrayList<>();
                    liveDdlDtoList.forEach(i -> {
                        log.info("当前记录的解析结果：{}", i);
                        LiveDdlEntity entity = new LiveDdlEntity();
                        AiShuUtil.copyProperties(i, entity);
                        entity.setSqlType(i.getType().name());
                        entity.setSqlText(i.getStatement());
                        entity.setMonitorTime(i.getMonitorTime());
                        entity.setUpdateStatus(i.getUpdateStatus().getCode());
                        switch (i.getUpdateStatus()) {
                            case UPDATE_ALL:
                            case UPDATE_INC:
                            case PARSE_FAIL:
                            case UPDATE_FAIL:
                                entity.setPushStatus(DdlPushStatusEnum.PUSH_WAITING.getCode());
                                break;
                            case UPDATE_IGNORE:
                                entity.setPushStatus(DdlPushStatusEnum.PUSH_IGNORE.getCode());
                                break;
                        }
                        liveDdlEntityList.add(entity);
                    });
                    liveDdlService.saveOrUpdateBatch(liveDdlEntityList);
                }
            }
        }
        Date now = new Date();
        if (AiShuUtil.isNotEmpty(liveDdlDtoList)) {
            Date maxMonitorTime = liveDdlDtoList.get(0).getMonitorTime();
            dataSourceEntity.setLiveUpdateTime(now);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String liveUpdateBenchmark = sdf.format(maxMonitorTime);
            dataSourceEntity.setLiveUpdateBenchmark(liveUpdateBenchmark);
            dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.BROADCASTING.getCode());
            dataSourceService.updateById(dataSourceEntity);
        } else {
            dataSourceEntity.setLiveUpdateTime(now);
            dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.WAITING.getCode());
            dataSourceService.updateById(dataSourceEntity);
        }
    }

    private void commitDdlMonitor(DataSourceEntity dataSourceEntity, Connection connection) {
        if (!schedulerConfig.getAutoMonitorTrigger()) {
            return;
        }
        Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
        String ddlMonitorTrigger = extendPropertyMap.get(DataSourceConstants.DDL_MONITOR_TRIGGER);
        String ddlLogTable = extendPropertyMap.get(DataSourceConstants.DDL_LOG_TABLE);
        String vConnector = extendPropertyMap.get(DataSourceConstants.VCONNECTOR);
        PreparedStatement checkTablePst = null;
        ResultSet checkTableSet = null;
        Statement stmt = null;
        ResultSet checkTriggerSet = null;
        try {
            if ("postgresql".equals(vConnector)) {
                if (StringUtils.isEmpty(ddlLogTable)) {
                    String createDdlLogTableSql = String.format("CREATE TABLE IF NOT EXISTS public.%s (\n" +
                                    "    f_id bigserial NOT NULL,\n" +
                                    "    f_catalog_name varchar(255) NULL,\n" +
                                    "    f_schema_name varchar(255) NULL,\n" +
                                    "    f_event_name varchar(255) NOT NULL,\n" +
                                    "    f_command_tag varchar(255) NOT  NULL,\n" +
                                    "    f_command_query text NOT  NULL,\n" +
                                    "    f_username varchar(255) NULL,\n" +
                                    "    f_client varchar(255) NULL,\n" +
                                    "    f_created_time timestamptz(0) DEFAULT now() NOT  NULL,\n" +
                                    "    CONSTRAINT aishu_log_ddl_pkey PRIMARY KEY (f_id)\n" +
                                    ");" +
                                    "CREATE INDEX IF NOT EXISTS %s ON public.%s(f_created_time);",
                            Constants.DDL_LOG_TABLE_POSTGRESQL,
                            Constants.DDL_LOG_IDX_POSTGRESQL,
                            Constants.DDL_LOG_TABLE_POSTGRESQL);
                    stmt = connection.createStatement();
                    stmt.execute(createDdlLogTableSql);
                    String extengProperty = String.format("%s%s=%s&", dataSourceEntity.getExtendProperty(), DataSourceConstants.DDL_LOG_TABLE, Constants.DDL_LOG_TABLE_POSTGRESQL);
                    dataSourceEntity.setExtendProperty(extengProperty);
                }
                if (StringUtils.isEmpty(ddlMonitorTrigger)) {
                    String checkTriggerSQL = String.format("SELECT 1 FROM pg_event_trigger WHERE evtname = '%s'", Constants.DDL_LOG_TRIGGER_POSTGRESQL);
                    String createFunctionAndTriggerSQL = String.format("CREATE OR REPLACE FUNCTION public.%s\n" +
                                    " RETURNS event_trigger\n" +
                                    " LANGUAGE plpgsql\n" +
                                    "AS $function$\n" +
                                    "DECLARE\n" +
                                    "    command_query text ;\n" +
                                    "BEGIN\n" +
                                    "    select current_query()\n" +
                                    "    into command_query\n" +
                                    "    ;\n" +
                                    "    insert into %s(\n" +
                                    "        f_catalog_name,\n" +
                                    "        f_schema_name,\n" +
                                    "        f_event_name,\n" +
                                    "        f_command_tag,\n" +
                                    "        f_command_query,\n" +
                                    "        f_username,\n" +
                                    "        f_client\n" +
                                    "    )\n" +
                                    "    select current_catalog,\n" +
                                    "           current_schema,\n" +
                                    "           tg_event ,\n" +
                                    "           tg_tag,\n" +
                                    "           command_query,\n" +
                                    "           current_user,\n" +
                                    "           inet_client_addr()::text\n" +
                                    "    ;\n" +
                                    "    RETURN ;\n" +
                                    "EXCEPTION WHEN OTHERS THEN\n" +
                                    "    RAISE NOTICE 'error [%%]%%', SQLSTATE, SQLERRM;\n" +
                                    "    return ;\n" +
                                    "END;\n" +
                                    "$function$\n" +
                                    ";\n" +
                                    "CREATE EVENT TRIGGER %s ON ddl_command_end EXECUTE PROCEDURE %s;",
                            Constants.DDL_LOG_FUNCTION_POSTGRESQL,
                            Constants.DDL_LOG_TABLE_POSTGRESQL,
                            Constants.DDL_LOG_TRIGGER_POSTGRESQL,
                            Constants.DDL_LOG_FUNCTION_POSTGRESQL);
                    checkTriggerSet = stmt.executeQuery(checkTriggerSQL);
                    if (!checkTriggerSet.next()) {
                        stmt.execute(createFunctionAndTriggerSQL);
                        String extengProperty = String.format("%s%s=%s&", dataSourceEntity.getExtendProperty(), DataSourceConstants.DDL_MONITOR_TRIGGER, Constants.DDL_LOG_TRIGGER_POSTGRESQL);
                        dataSourceEntity.setExtendProperty(extengProperty);
                    }
                }

            }
            if (StringUtils.isEmpty(ddlMonitorTrigger) || StringUtils.isEmpty(ddlLogTable)) {
                dataSourceService.updateById(dataSourceEntity);
            }
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            try {
                if (checkTableSet != null) {
                    checkTableSet.close();
                }
                if (checkTriggerSet != null) {
                    checkTriggerSet.close();
                }
                if (checkTablePst != null) {
                    checkTablePst.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
    }


    private Connection getDdlConnection(DataSourceEntity dataSourceEntity) {
        Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
        String vConnector = extendPropertyMap.get(DataSourceConstants.VCONNECTOR);
        // 待采集数据源自身存有ddl时访问待采集数据源， 否则访问自己的ddl表
        if ("hologres".equals(vConnector) || "postgresql".equals(vConnector)) {
            DataSource dataSource = dataSourceService.getDataSource(dataSourceEntity);
            log.info("数据源名称：{}", dataSource.getName());
            BaseConnectionParam connectionParam =
                    (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                            dataSource.getType(),
                            dataSource.getConnectionParams());
            try {
                return DataSourceUtils.getConnection(dataSource.getType(), connectionParam, true);
            } catch (Exception e) {
                log.error("创建ddl存放数据源{}失败，错误信息：{}", dataSourceEntity.getName(), e.toString());
                return null;
            }
        } else {
            try {
                return dataSource.getConnection();
            } catch (Exception e) {
                log.error("创建ddl存放数据源{}失败，错误信息：{}", dataSourceEntity.getName(), e.toString());
                return null;
            }
        }
    }

    private void addLiveDdlDtoList(DataSourceEntity dataSourceEntity, List<LiveDdlDto> liveDdlDtoList, Connection connection, List<SchemaEntity> schemaEntityList, List<DataSourceEntity> relativeList) {
        Long start = System.currentTimeMillis();
        Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
        String vConnector = extendPropertyMap.get(DataSourceConstants.VCONNECTOR);
        if (AiShuUtil.isEmpty(dataSourceEntity.getLiveUpdateBenchmark())) {
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = sdf.format(now);
            dataSourceEntity.setLiveUpdateBenchmark(dateStr);
        }
        String getDdlSql;
        PreparedStatement pst = null;
        ResultSet ddlSet = null;
        try {
            // 数据源自身存有ddl
            if ("hologres".equals(vConnector)) {
                getDdlSql = String.format("select status, command_tag, query_end, query from hologres.hg_query_log where query_end > TIMESTAMPTZ '%s'", dataSourceEntity.getLiveUpdateBenchmark());
                pst = connection.prepareStatement(getDdlSql);
                ddlSet = pst.executeQuery();
                while (ddlSet.next()) {
                    String status = ddlSet.getString("status");
                    if ("SUCCESS".equals(status)) {
                        String commandTag = ddlSet.getString("command_tag");
                        if (AiShuUtil.isNotEmpty(HoloDdlTagEnum.of(commandTag))) {
                            Timestamp query_end = ddlSet.getTimestamp("query_end");
                            String query = ddlSet.getString("query");
                            DdlLogDto ddlLogDto = new DdlLogDto();
                            ddlLogDto.setStatement(query);
                            ddlLogDto.setDdlTime(query_end);
                            liveDdlDtoList.addAll(DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, relativeList));
                        }
                    }
                }
            } else if ("postgresql".equals(vConnector)) {
                getDdlSql = String.format("select f_catalog_name, f_schema_name, f_command_tag, f_command_query, f_created_time from public.aishu_log_ddl where f_created_time > TIMESTAMPTZ '%s'", dataSourceEntity.getLiveUpdateBenchmark());
                pst = connection.prepareStatement(getDdlSql);
                ddlSet = pst.executeQuery();
                while (ddlSet.next()) {
                    String commandTag = ddlSet.getString("f_command_tag");
                    if (AiShuUtil.isNotEmpty(HoloDdlTagEnum.of(commandTag))) {
                        Timestamp query_end = ddlSet.getTimestamp("f_created_time");
                        String query = ddlSet.getString("f_command_query");
                        DdlLogDto ddlLogDto = new DdlLogDto();
                        ddlLogDto.setStatement(query);
                        ddlLogDto.setDdlTime(query_end);
                        ddlLogDto.setCatalogName(ddlSet.getString("f_catalog_name"));
                        ddlLogDto.setSchemaName(ddlSet.getString("f_schema_name"));
                        liveDdlDtoList.addAll(DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, relativeList));
                    }
                }
            } else if ("mysql".equals(vConnector)) {
                LambdaQueryWrapper<LiveDdlEntity> ddlEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
                ddlEntityLambdaQueryWrapper.eq(LiveDdlEntity::getDataSourceId, dataSourceEntity.getId());
                ddlEntityLambdaQueryWrapper.gt(LiveDdlEntity::getMonitorTime, ConvertUtil.toDate(dataSourceEntity.getLiveUpdateBenchmark()));
                List<LiveDdlEntity> ddlEntityList = liveDdlService.list(ddlEntityLambdaQueryWrapper);
                if (AiShuUtil.isNotEmpty(ddlEntityList)) {
                    for (LiveDdlEntity item : ddlEntityList) {
                        DdlLogDto ddlLogDto = new DdlLogDto();
                        ddlLogDto.setId(item.getId());
                        ddlLogDto.setSchemaName(item.getSchemaName());
                        ddlLogDto.setCatalogName(item.getOriginCatalog());
                        ddlLogDto.setStatement(item.getSqlText());
                        ddlLogDto.setDdlTime(item.getMonitorTime());
                        ddlLogDto.setUpdateMessage(item.getUpdateMessage());
                        liveDdlDtoList.addAll(DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, relativeList));
                    }
                }
            } else {
                //当前支持hologres、postgresql、mysql,后续补充其他数据源的ddl采集
            }

        } catch (Exception e) {
            if (AiShuUtil.isNotEmpty(relativeList)) {
                relativeList = relativeList.stream().map(i -> {
                    i.setLiveUpdateStatus(DataSourceUpdateStatusEnum.UNAUTHORIZED.getCode());
                    return i;
                }).collect(Collectors.toList());
                dataSourceService.updateBatchById(relativeList);
            }
            log.error(e.toString());
        } finally {
            try {
                if (ddlSet != null) {
                    ddlSet.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (connection != null) {
                    connection.close();
                }
                if (AiShuUtil.isEmpty(liveDdlDtoList)) {
                    log.info("本次检查无需更新ddl,耗时{}ms", System.currentTimeMillis() - start);
                } else {
                    log.info("本次检查提取ddl{}条,耗时{}ms", liveDdlDtoList.size(), System.currentTimeMillis() - start);
                }
            } catch (Exception e) {
                log.error(e.toString());
                log.error("提取ddl失败,耗时{}ms", System.currentTimeMillis() - start);
            }
        }
    }

    private void updateTableByVirtual(DataSourceEntity dataSourceEntity) {
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            long start = System.currentTimeMillis();
            try {
                Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
                String catalog = extendPropertyMap.get(DataSourceConstants.VCATALOGNAME);
                String schemaName = extendPropertyMap.get(DataSourceConstants.SCHEMAKEY);

                //查询是否现有schema
                LambdaQueryWrapper<SchemaEntity> schemaWrapper = new LambdaQueryWrapper<>();
                schemaWrapper.eq(SchemaEntity::getDataSourceId, dataSourceEntity.getId());
                schemaWrapper.eq(SchemaEntity::getName, schemaName);
                SchemaEntity schema = schemaService.getOne(schemaWrapper, false);
                if (AiShuUtil.isEmpty(schema)) {
                    throw new AiShuException(ErrorCodeEnum.ResourceNotExisted, "数据源：" + dataSourceEntity.getName() + "下的schema：" + schemaName + "不存在!", "请检查数据源是否已扫描");
                }
                Date now = new Date();

                //表级元数据处理逻辑
                log.info("开始查询tables...");
                long current = System.currentTimeMillis();

                Map<String, TableEntity> currentTables = new HashMap<>();
                VirtualTableListDto tableListDto = virtualService.getTable(catalog, schemaName);
                List<VirtualTableListDto.VirtualTableDto> virtualTableDtoList = tableListDto.getData();
                log.info("读取表级元数据耗时ms:" + (System.currentTimeMillis() - current));
                List<TableEntity> saveList = new ArrayList<>();
                List<TableEntity> updateList = new ArrayList<>();
                if (AiShuUtil.isNotEmpty(virtualTableDtoList)) {
                    virtualTableDtoList.forEach(virtualTableDto -> {
                        TableEntity tableEntity = new TableEntity();
                        String name = virtualTableDto.getTable();
                        tableEntity.setName(name);
                        tableEntity.setDataSourceName(dataSourceEntity.getName());
                        tableEntity.setDataSourceType(dataSourceEntity.getDataSourceType());
                        tableEntity.setDataSourceTypeName(dataSourceEntity.getDataSourceTypeName());
                        tableEntity.setDataSourceId(dataSourceEntity.getId());
                        tableEntity.setSchemaName(schemaName);
                        //表级注释
                        tableEntity.setDescription(virtualTableDto.getComment());
                        tableEntity.setSchemaId(schema.getId());
                        //Todo 数据量虚拟化未提供:[{"key":"vCatalogName","value":"postgresql_8ekmfxdo"}]
                        List<AdvancedDTO> advancedParams = new ArrayList<>();
                        Map<String, String> props = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
                        String vCatalogName = AiShuUtil.isEmpty(props) ? null : props.get("vCatalogName");
                        AdvancedDTO advancedDTO = new AdvancedDTO();
                        advancedDTO.setKey(DataSourceConstants.VCATALOGNAME);//vCatalogName
                        advancedDTO.setValue(vCatalogName);
                        advancedParams.add(advancedDTO);

                        Object params = virtualTableDto.getParams();
                        if (AiShuUtil.isNotEmpty(params)) {
                            Map<String, Object> paramsMap = (Map<String, Object>) params;
                            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                                AdvancedDTO tmp = new AdvancedDTO();
                                tmp.setKey(entry.getKey());
                                Object value = entry.getValue();
                                if (value != null) {
                                    tmp.setValue(String.valueOf(value));
                                } else {
                                    tmp.setValue(null);
                                }
                                advancedParams.add(tmp);
                            }
                        }
                        tableEntity.setAdvancedParams(JSONUtils.toJsonString(advancedParams));
                        currentTables.put(name, tableEntity);
                    });
                }
                log.info("构建表级元数据Map耗时ms:" + (System.currentTimeMillis() - current));
                current = System.currentTimeMillis();
                //表级元数据保存逻辑
                //查询现有表级元数据
                Map<String, TableEntity> oldTables = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableEntity.class).eq(TableEntity::getSchemaId, schema.getId()), TableEntity::getName);
                //获取待删除列表并删除
                List<TableEntity> deleteTableList = oldTables.keySet().stream().filter(tableName -> !currentTables.containsKey(tableName)).map(tableName -> {
                    TableEntity tableEntity = oldTables.get(tableName);
                    if (tableEntity.getDeleted().equals(0)) {
                        tableEntity.setDeleted(1);
                        tableEntity.setDeleteTime(now);
                    }
                    return tableEntity;
                }).collect(Collectors.toList());
                if (AiShuUtil.isNotEmpty(deleteTableList)) {
                    tableService.updateBatchById(deleteTableList);
                    log.info("删除表级元数据耗时ms:" + (System.currentTimeMillis() - current));
                }
                current = System.currentTimeMillis();
                //获取当前表级元数据并判断修改/新增
                currentTables.keySet().stream().forEach(tableName -> {
                            TableEntity currentTable = currentTables.get(tableName);
                            TableEntity oldTable = oldTables.get(tableName);
                            if (AiShuUtil.isNotEmpty(oldTable)) {
                                currentTable.setId(oldTable.getId());
                                currentTable.setAdvancedParams(oldTable.getAdvancedParams());
                            }
                            if (AiShuUtil.isEmpty(currentTable.getId())) {
                                currentTable.setDeleted(0);
                                currentTable.setVersion(0);
                                currentTable.setTableRows(AiShuUtil.isEmpty(currentTable.getTableRows()) ? 0 : currentTable.getTableRows());
                                currentTable.setAuthorityId("");
                                currentTable.setCreateTime(now);
                                currentTable.setCreateUser("");
                                currentTable.setUpdateTime(now);
                                currentTable.setUpdateUser("");
                                saveList.add(currentTable);
                            } else {
                                currentTable.setDeleted(0);
                                currentTable.setUpdateTime(now);
                                updateList.add(currentTable);
                            }
                        }
                );
                log.info("更新表级元数据map耗时ms:{}", (System.currentTimeMillis() - current));
                current = System.currentTimeMillis();
                tableService.saveBatch(saveList, 1000);
                tableService.updateBatchById(updateList, 1000);
                LambdaUpdateWrapper<TableEntity> updateWrapper = Wrappers.lambdaUpdate();
                updateWrapper.eq(TableEntity::getDeleted, 0);
                updateWrapper.set(TableEntity::getDeleteTime, null);
                tableService.update(updateWrapper);
                log.info("表级元数据更新入库耗时ms:{}", (System.currentTimeMillis() - current));
                //todo 从kafka中获得一批ddl，解析并按数据源聚合后（ddl不一定含schema信息所以更新schema和表信息后，可能需要根据表名反查schema），每个数据源执行一次更新，逐表更新字段
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
            log.info("更新表级元数据结束，h耗时：{}", (System.currentTimeMillis() - start));
        }
    }

    /**
     * 通过虚拟化接口更新字段级元数据
     *
     * @param dataSource
     * @param table
     */
    private void getColumnByVirtual(DataSourceEntity dataSource, TableEntity table, Map<TableEntity, List<TableFieldEntity>> tableEntityListMap) {
        if (AiShuUtil.isNotEmpty(table) && table.getDeleted() > 0) {
            log.info("table:{}已删除，跳过采集字段并删除存量字段", table.getName());
//            tableFieldService.logicalDeleteByTableId(table.getId());
            //bug630066改成物理删除字段
            LambdaQueryWrapper<TableFieldEntity> deleteFieldWrapper = new LambdaQueryWrapper<>();
            deleteFieldWrapper.eq(TableFieldEntity::getTableId, table.getId());
            tableFieldService.remove(deleteFieldWrapper);
            return;
        }
        Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSource.getExtendProperty());
        String catalog = extendPropertyMap.get(DataSourceConstants.VCATALOGNAME);
        try {
            //取此类型数据源的字段类型集合
            LambdaQueryWrapper<DictEntity> dictEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dictEntityLambdaQueryWrapper.eq(DictEntity::getDictType, dataSource.getDataSourceType() + 1);
            log.info("table:{},开始查询字段", table.getName());
            Map<String, TableFieldEntity> currentFields = new LinkedHashMap<>();
            Map<String, String> defaultValueMap = new HashMap<>();
            Map<String, Boolean> isNullMap = new HashMap<>();
            Map<String, Boolean> isPrimaryKeyMap = new HashMap<>();
            Map<String, String> originTypeMap = new HashMap<>();
            Map<String, String> virtualTypeMap = new HashMap<>();
            VirtualColumnListDto columnListDto = virtualService.getColumn(catalog, table.getSchemaName(), table.getName());
            List<VirtualColumnListDto.VirtualColumnDto> virtualColumnDtoList = columnListDto.getData();
            //保存字段元数据
            try {
                if (AiShuUtil.isNotEmpty(virtualColumnDtoList)) {
                    virtualColumnDtoList.forEach(virtualColumnDto -> {
                        TableFieldEntity tableFieldEntity = new TableFieldEntity();
                        tableFieldEntity.setFieldName(virtualColumnDto.getName());
                        log.info("当前采集的字段:{}", tableFieldEntity.getFieldName());
                        tableFieldEntity.setFieldComment(virtualColumnDto.getComment());
                        List<VirtualColumnListDto.VirtualColumnDto.TypeSignature.Arguments> arguments = virtualColumnDto.getTypeSignature().getArguments();
                        if (AiShuUtil.isNotEmpty(arguments) && AiShuUtil.isNotEmpty(arguments.get(0))) {
                            //字段长度,虚拟化提供
                            tableFieldEntity.setFieldLength(arguments.get(0).getValue());
                            if (arguments.size() > 1 && AiShuUtil.isNotEmpty(arguments.get(1))) {
                                //字段精度,虚拟化提供
                                tableFieldEntity.setFieldPrecision(arguments.get(1).getValue());
                            }
                        }
                        //字段类型,虚拟化提供的是带括号的，做字符串处理
                        String[] strings = StringUtils.split(virtualColumnDto.getOrigType(), '(');
                        tableFieldEntity.setFieldType(AiShuUtil.isEmpty(strings) ? "UNKNOWN" : strings[0]);
                        tableFieldEntity.setTableId(table.getId());
                        currentFields.put(tableFieldEntity.getFieldName(), tableFieldEntity);
                        defaultValueMap.put(tableFieldEntity.getFieldName(), virtualColumnDto.getColumnDef());
                        isNullMap.put(tableFieldEntity.getFieldName(), virtualColumnDto.getNullAble());
                        isPrimaryKeyMap.put(tableFieldEntity.getFieldName(), virtualColumnDto.getPrimaryKey());
                        originTypeMap.put(tableFieldEntity.getFieldName(), virtualColumnDto.getOrigType());
                        virtualTypeMap.put(tableFieldEntity.getFieldName(), virtualColumnDto.getType());
                    });
                }

                //获取当前字段级元数据并判断修改/新增

                List<TableFieldEntity> currentTableFieldList = currentFields.keySet().stream().map(fieldName -> {
                            TableFieldEntity currentField = currentFields.get(fieldName);
                            //高级参数处理逻辑
                            try {
                                List<AdvancedDTO> advancedParams = new ArrayList<>(com.eisoo.metadatamanage.web.util.JSONUtils.toList(currentField.getAdvancedParams(), AdvancedDTO.class));
                                //取出是否主键参数有则改，无则增
                                String isPrimaryKeyStr;
                                if (AiShuUtil.isNotEmpty(isPrimaryKeyMap.get(fieldName)) && isPrimaryKeyMap.get(fieldName)) {
                                    isPrimaryKeyStr = DataSourceConstants.YES;
                                } else {
                                    isPrimaryKeyStr = DataSourceConstants.NO;
                                }
                                AdvancedDTO checkPrimaryKey = new AdvancedDTO();
                                checkPrimaryKey.setKey(DataSourceConstants.CHECKPRIMARYKEY);
                                checkPrimaryKey.setValue(isPrimaryKeyStr);
                                advancedParams.add(checkPrimaryKey);

                                //取出默认值参数有则改，无则增
                                String defaultValueStr = defaultValueMap.get(currentField.getFieldName());
                                AdvancedDTO columnDef = new AdvancedDTO();
                                columnDef.setKey(DataSourceConstants.COLUMN_DEF);
                                columnDef.setValue(String.valueOf(defaultValueStr));
                                advancedParams.add(columnDef);

                                //取出是否为空参数有则改，无则增
                                String isNullAbleStr;
                                if (AiShuUtil.isNotEmpty(isNullMap.get(fieldName)) && isNullMap.get(fieldName)) {
                                    isNullAbleStr = DataSourceConstants.YES;
                                } else {
                                    isNullAbleStr = DataSourceConstants.NO;
                                }
                                AdvancedDTO isNullAble = new AdvancedDTO();
                                isNullAble.setKey(DataSourceConstants.IS_NULLABLE);
                                isNullAble.setValue(isNullAbleStr);
                                advancedParams.add(isNullAble);


                                //取出虚拟化字段类型参数 有则改，无则增
                                //临时方案，字符串处理去虚拟化字段类型括号
                                String virtualFieldTypeStrOrigin = virtualTypeMap.get(currentField.getFieldName());
                                String[] strings = StringUtils.split(virtualFieldTypeStrOrigin, '(');
                                String virtualFieldTypeStr = AiShuUtil.isEmpty(strings) ? "UNKNOWN" : strings[0];

                                AdvancedDTO virtualFieldTypeDto = new AdvancedDTO();
                                virtualFieldTypeDto.setKey(DataSourceConstants.VIRTUAL_FIELD_TYPE);
                                virtualFieldTypeDto.setValue(String.valueOf(virtualFieldTypeStr));
                                advancedParams.add(virtualFieldTypeDto);


                                //取出原始字段类型参数 有则改，无则增
                                String originFieldTypeStr = originTypeMap.get(currentField.getFieldName());
                                AdvancedDTO originFieldTypeDto = new AdvancedDTO();
                                originFieldTypeDto.setKey(DataSourceConstants.ORIGIN_FIELD_TYPE);
                                originFieldTypeDto.setValue(String.valueOf(originFieldTypeStr));
                                advancedParams.add(originFieldTypeDto);

                                //保存高级参数
                                currentField.setAdvancedParams(com.eisoo.metadatamanage.web.util.JSONUtils.toJsonString(advancedParams));
                            } catch (Exception e) {
                                log.error("cant not get the advancedParams in table {}: {}", currentField.getFieldName(), e.toString());
                                throw new AiShuException(ErrorCodeEnum.InternalError, e.toString(), "请检查字段元数据高级参数处理逻辑");
                            }
                            return currentField;
                        }
                ).collect(Collectors.toList());
                tableEntityListMap.put(table, currentTableFieldList);
            } catch (Exception e) {
                log.error("cant not save the fields : {}", e);
                throw new AiShuException(ErrorCodeEnum.InternalError, e.toString(), "请检查字段元数据保存逻辑");
            }
        } catch (Exception e) {
            log.error("cant not get the fields : {}", e);
            throw new AiShuException(ErrorCodeEnum.InternalError, e.toString(), "请检查字段元数据采集逻辑");
        }
    }


    /**
     * 查询数据元ID校验
     *
     * @param id
     * @return
     */

    @Override
    public CheckVo<TaskEntity> checkID(Long id) {
        String errorCode = "";
        List<CheckErrorVo> checkErrors = Lists.newLinkedList();
        //校验集合-数据元ID参数不能为空
        if (id == null) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.MissingParameter.getErrorCode(), "任务ID参数不能为空"));
        }
        //校验集合-目录ID指向的数据元是否存在
        TaskEntity taskEntity = getById(id);
        if (taskEntity == null) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), String.format("任务不存在或已删除", id)));
        }
        if (checkErrors.size() > 0) {
            errorCode = ErrorCodeEnum.InvalidParameter.getErrorCode();
        }
        return new CheckVo<>(errorCode, checkErrors, taskEntity);
    }

    @Override
    public CheckVo<String> checkID(String ids) {
        String errorCode = "";
        List<CheckErrorVo> checkErrors = Lists.newLinkedList();
        //校验集合-ID集合参数不能为空
        if (StringUtils.isBlank(ids)) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.MissingParameter.getErrorCode(), "任务ID集合ids参数不能为空"));
        }
        //校验集合-ID集合形式为 1,2,3 等等,长度在1-2000
        if (!ids.matches(Constants.getRegexNumVarL(1, 2000))) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), "任务ID集合ids形式应为 {1,2,3},长度在1-2000"));
        }
        //校验集合-单个id
        List<Long> idList = StringUtil.splitNumByRegex(ids, ",");
        if (AiShuUtil.isNotEmpty(idList)) {
            idList.forEach(id -> {
                String errRode = checkID(id).getCheckCode();
                if (!StringUtils.isEmpty(errRode)) {
                    checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), String.format("任务不存在或已删除", id)));
                }
            });
        }
        if (checkErrors.size() > 0) {
            errorCode = ErrorCodeEnum.InvalidParameter.getErrorCode();
        }
        return new CheckVo<>(errorCode, checkErrors, ids);
    }

    @Override
    public Result<List<TaskEntity>> getList(Date start_time, Date end_time, String keyword, Integer task_status, Integer offset, Integer limit, String sort, String direction) {
        LambdaQueryWrapper<TaskEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(keyword != null, TaskEntity::getName, keyword);
        lambdaQueryWrapper.ge(start_time != null, TaskEntity::getStartTime, start_time);
        lambdaQueryWrapper.le(end_time != null, TaskEntity::getEndTime, end_time);
        lambdaQueryWrapper.eq(task_status != null, TaskEntity::getStatus, task_status);
        boolean isAsc = direction.toLowerCase().equals("asc");
        if (sort.toLowerCase().equals("end_time")) {
            lambdaQueryWrapper.orderBy(true, isAsc, TaskEntity::getEndTime);
        } else {
            lambdaQueryWrapper.orderBy(true, isAsc, TaskEntity::getStartTime);
        }
        Page<TaskEntity> p = new Page<>(offset, limit);
        IPage<TaskEntity> page = page(p, lambdaQueryWrapper);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @Override
    public Result<?> updateRowNum(Long tableId) {
        TableEntity tableEntity = tableService.getById(tableId);
        if (AiShuUtil.isEmpty(tableEntity)) {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(IdWorker.getId());
        taskEntity.setObjectId(String.valueOf(tableId));
        //Todo 字典化
        taskEntity.setObjectType(2);
        taskEntity.setStartTime(new Date());
        taskEntity.setStatus(2);
        String taskName = tableEntity.getName() + "-task-" + taskEntity.getId();
        taskEntity.setName(taskName);
        save(taskEntity);
        fillMetaDataExecutorPool.submit(() -> updateRowNumExec(tableEntity, taskEntity));
        return Result.success(String.format("数据量更新任务已提交{任务名称:%s,任务ID:%s}，请耐心等待后台运行完成", taskEntity.getName(), taskEntity.getId()));
    }

    //从文件改成从数据库读取
    @Override
    public Result<?> getLog(Long taskId) {
//        String filename = "logs/task-" + taskId + ".log";
//        StringBuffer result = new StringBuffer();
//        try {
//            FileReader fr = new FileReader(filename);
//            BufferedReader br = new BufferedReader(fr);
//            String line;
//            while ((line = br.readLine()) != null) {
//                result.append(line);
//            }
//        } catch (Exception e) {
//            log.error(e.toString(), e);
//        }
        LambdaQueryWrapper<TaskLogEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskLogEntity::getTaskId, taskId);
        TaskLogEntity taskLogEntity = taskLogService.getOne(queryWrapper, false);
        if (AiShuUtil.isEmpty(taskLogEntity) || StringUtils.isEmpty(taskLogEntity.getLog())) {
            return Result.success("日志已删除或不存在");
        } else {
            return Result.success(taskLogEntity.getLog());
        }
    }

    private void writeLog(Long taskId) {
        RandomAccessFile rf = null;
        List<String> logList = new ArrayList<>();
        //从主日志中摘取单个任务内容
        try {
            String filename = "logs/task.log";
            rf = new RandomAccessFile(filename, "r");
            long len = rf.length();
            long start = rf.getFilePointer();
            long nextend = start + len - 1;
            String line;
            rf.seek(nextend);
            int c = -1;
            boolean flag = false;
            while (nextend > start) {
                c = rf.read();
                if (c == '\n' || c == '\r') {
                    line = rf.readLine();
                    if (line != null) {
                        line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                    }
                    if (line != null && (line.contains("TaskId-" + taskId))) {
                        logList.add(line);
                        flag = true;
                    }
                    if (flag == true && !line.contains("TaskLogger")) {
                        logList.add(line);
                    }
                    if (line != null && line.contains("采集任务开始，任务id：" + taskId)) {
                        break;
                    }
                    nextend--;
                }
                nextend--;
                rf.seek(nextend);
//                if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
//                    System.out.println(new String(rf.readLine().getBytes("ISO-8859-1"), "UTF-8"));
//                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //输出单个任务日志,从文件改成数据库以便持久化
        String filename = "logs/task-" + taskId + ".log";
        try {
//            FileWriter fw = new FileWriter(filename);
//            BufferedWriter bw = new BufferedWriter(fw);
            StringBuilder stringBuilder = new StringBuilder();
            if (AiShuUtil.isNotEmpty(logList)) {
                int max = logList.size();
                for (int i = 0; i < max; i++) {
//                    bw.write(logList.get(max - i - 1));
//                    bw.append('\n');
                    stringBuilder.append(logList.get(max - i - 1));
                    stringBuilder.append('\n');
                }

            }
//            bw.flush();
//            bw.close();
            TaskLogEntity taskLogEntity = new TaskLogEntity();
            taskLogEntity.setTaskId(taskId);
            taskLogEntity.setLog(stringBuilder.toString());
            taskLogService.save(taskLogEntity);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * 数据量更新任务
     *
     * @param tableEntity
     * @param taskEntity
     */
    private void updateRowNumExec(TableEntity tableEntity, TaskEntity taskEntity) {
        MDC.put("taskId", String.valueOf(taskEntity.getId()));
        log.info("采集任务开始，任务id：{}", taskEntity.getId());
        DataSourceEntity dataSourceEntity = dataSourceService.getById(tableEntity.getDataSourceId());
        if (AiShuUtil.isNotEmpty(dataSourceEntity)) {
            DataSource dataSource = dataSourceService.getDataSource(dataSourceEntity);
            log.info("数据源名称：{}", dataSource.getName());
            BaseConnectionParam connectionParam =
                    (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                            dataSource.getType(),
                            dataSource.getConnectionParams());
            Connection connection =
                    DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
            log.info("创建数据源连接成功...");
            String getCountSql;
            PreparedStatement pst = null;
            ResultSet rowCounts = null;
            Long count = 0L;
            try {
                switch (dataSource.getType()) {
                    case POSTGRESQL:
                        getCountSql = "SELECT count(*) as table_rows FROM %s.%s";
                        getCountSql = String.format(getCountSql, tableEntity.getSchemaName(), tableEntity.getName());
                        log.info("执行计算语句：{}", getCountSql);
                        pst = connection.prepareStatement(getCountSql);
                        rowCounts = pst.executeQuery();
                        while (rowCounts.next()) {
                            count = rowCounts.getLong("table_rows");
                        }
                        break;
                    default:
                        getCountSql = "SELECT count(*) as table_rows FROM %s";
                        getCountSql = String.format(getCountSql, tableEntity.getName());
                        log.info("执行计算语句：{}", getCountSql);
                        pst = connection.prepareStatement(getCountSql);
                        rowCounts = pst.executeQuery();
                        while (rowCounts.next()) {
                            count = rowCounts.getLong("table_rows");
                        }

                        break;
                }
                tableEntity.setTableRows(count);
                log.info("数据量统计结果：{}", count);
                tableService.updateById(tableEntity);
                log.info("表名：{}，数据量已更新", tableEntity.getName());
                //记录任务状态
                taskEntity.setEndTime(new Date());
                taskEntity.setStatus(0);
                updateById(taskEntity);
            } catch (SQLException e) {
                //记录任务状态
                taskEntity.setEndTime(new Date());
                taskEntity.setStatus(1);
                updateById(taskEntity);
                log.error(e.toString(), e);
            } finally {
                closePreparedStatement(pst);
                closeResult(rowCounts);
                releaseConnection(connection);
            }
            log.info("采集任务结束，任务id：{}", taskEntity.getId());
            writeLog(taskEntity.getId());
        }
    }

    /**
     * 判断版本是否需要变更
     *
     * @param result
     * @param oldField
     * @param currentField
     * @return
     */
    private Boolean isChangeVersion(Boolean result, TableFieldEntity oldField, TableFieldEntity currentField) {

        String[] ignoreFields = {"id", "fieldName", "fieldType", "tableId", "advancedParams", "version"};
        if (AiShuUtil.isNotEmpty(oldField) && AiShuUtil.isNotEmpty(AiShuUtil.compareFields(oldField, currentField, ignoreFields))) {
            Map<String, List<Object>> map = AiShuUtil.compareFields(oldField, currentField, ignoreFields);
            result = true;
        }
        return result;
    }

    /**
     * 释放数据库连接
     *
     * @param connection
     */
    private void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Connection release error", e);
            }
        }
    }

    /**
     * 关闭查询结果集
     *
     * @param rs
     */
    private void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error("ResultSet close error", e);
            }
        }
    }

    private void closePreparedStatement(PreparedStatement pst) {
        if (pst != null) {
            try {
                pst.close();
            } catch (Exception e) {
                log.error("PreparedStatement close error", e);
            }
        }
    }

    /**
     * 获取数据源对应的schema
     *
     * @param dbType
     * @param schema
     * @param connectionParam
     * @return
     */
    private String getDbSchemaPattern(DbType dbType, String schema, BaseConnectionParam connectionParam) {
        if (dbType == null) {
            return null;
        }
        String schemaPattern = null;
        switch (dbType) {
            case HIVE:
                schemaPattern = StringUtils.isEmpty(schema) ? connectionParam.getDatabase() : schema;
                break;
            case ORACLE:
                schemaPattern = StringUtils.isEmpty(schema) ? connectionParam.getUser() : schema;
                if (null != schemaPattern) {
                    schemaPattern = schemaPattern.toUpperCase();
                }
                break;
            case SQLSERVER:
                schemaPattern = StringUtils.isEmpty(schema) ? "dbo" : schema;
                break;
            case CLICKHOUSE:
            case PRESTO:
                if (!StringUtils.isEmpty(schema)) {
                    schemaPattern = schema;
                }
                break;
            case POSTGRESQL:
                if (AiShuUtil.isNotEmpty(connectionParam.getProps()) && AiShuUtil.isNotEmpty(connectionParam.getProps().get(DataSourceConstants.SCHEMAKEY))) {
                    schemaPattern = schema;
                }
                break;
            default:
                break;
        }
        return schemaPattern;
    }

    /**
     * 通过系统表获取数据量
     *
     * @param connection
     * @param schema
     * @param dbType
     * @return
     * @throws SQLException
     */
    private Map<String, Long> getCountMap(Connection connection, String schema, DbType dbType) throws SQLException {
        Map<String, Long> countMap = new HashMap<>();
        String getCountSql;
        PreparedStatement pst;
        ResultSet rowCounts = null;
        try {
            switch (dbType) {
                case HIVE:
                    getCountSql = "select d.NAME,t.TBL_NAME,t.TBL_ID,p.PART_ID,p.PART_NAME,a.PARAM_VALUE " +
                            "from sys.tbls t " +
                            "left join sys.dbs d " +
                            "on t.DB_ID = d.DB_ID " +
                            "left join sys.partitions p " +
                            "on t.TBL_ID = p.TBL_ID  " +
                            "left join sys.PARTITION_PARAMS a " +
                            "on p.PART_ID=a.PART_ID " +
                            "where d.NAME='%s' and a.PARAM_KEY='numRows'";
                    getCountSql = String.format(getCountSql, schema);
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("t.tbl_name");
                        Long count = rowCounts.getLong("a.param_value");
                        if (AiShuUtil.isEmpty(countMap.get(tableName))) {
                            countMap.put(tableName, count);
                        } else {
                            Long totalCount = countMap.get(tableName) + count;
                            countMap.put(tableName, totalCount);
                        }
                    }
                    pst.close();
                    break;
                case MYSQL:
                    getCountSql = "select table_name,table_rows from information_schema.tables where TABLE_SCHEMA='%s'";
                    getCountSql = String.format(getCountSql, schema);
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("table_name");
                        Long count = rowCounts.getLong("table_rows");
                        countMap.put(tableName, count);
                    }
                    pst.close();
                    break;
                case SQLSERVER:
                    getCountSql = "SELECT db_name() as DbName, t.NAME AS table_name, s.Name AS SchemaName, p.rows AS table_rows, SUM(a.total_pages) * 8 AS TotalSpaceKB, CAST(ROUND(((SUM(a.total_pages) * 8) / 1024.00), 2) AS NUMERIC(36, 2)) AS TotalSpaceMB,SUM(a.used_pages) * 8 AS UsedSpaceKB, CAST(ROUND(((SUM(a.used_pages) * 8) / 1024.00), 2) AS NUMERIC(36, 2)) AS UsedSpaceMB, (SUM(a.total_pages) - SUM(a.used_pages)) * 8 AS UnusedSpaceKB,CAST(ROUND(((SUM(a.total_pages) - SUM(a.used_pages)) * 8) / 1024.00, 2) AS NUMERIC(36, 2)) AS UnusedSpaceMB " +
                            "FROM sys.tables t " +
                            "INNER JOIN sys.indexes i ON t.OBJECT_ID = i.object_id " +
                            "INNER JOIN sys.partitions p ON i.object_id = p.OBJECT_ID AND i.index_id = p.index_id " +
                            "INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id " +
                            "LEFT OUTER JOIN sys.schemas s ON t.schema_id = s.schema_id " +
                            "WHERE t.NAME NOT LIKE 'dt%' AND t.is_ms_shipped = 0 AND i.OBJECT_ID > 0 " +
                            "GROUP BY t.Name, s.Name, p.Rows " +
                            "ORDER BY TotalSpaceMB desc";
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("table_name");
                        Long count = rowCounts.getLong("table_rows");
                        countMap.put(tableName, count);
                    }
                    pst.close();
                    break;
                case CLICKHOUSE:
                    getCountSql = "select table, sum(rows) as rows from system.parts where active and database = '%s' group by table";
                    getCountSql = String.format(getCountSql, schema);
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("table");
                        Long count = rowCounts.getLong("rows");
                        countMap.put(tableName, count);
                    }
                    pst.close();
                    break;
                case ORACLE:
                    getCountSql = "select table_name, num_rows from user_tables";
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("table_name");
                        Long count = rowCounts.getLong("num_rows");
                        countMap.put(tableName, count);
                    }
                    pst.close();
                    break;
                case POSTGRESQL:
                    getCountSql = "SELECT schemaname,relname as table_name ,n_live_tup as table_rows FROM pg_stat_user_tables where schemaname='%s'";
                    getCountSql = String.format(getCountSql, schema);
                    pst = connection.prepareStatement(getCountSql);
                    rowCounts = pst.executeQuery();
                    while (rowCounts.next()) {
                        String tableName = rowCounts.getString("table_name");
                        Long count = rowCounts.getLong("table_rows");
                        countMap.put(tableName, count);
                    }
                    pst.close();
                    break;
                default:
                    break;
            }
        } catch (SQLException e) {
            log.error(e.toString(), e);
        } finally {
            closeResult(rowCounts);
        }
        return countMap;
    }

    /**
     * @param fieldEntityMap
     */
    private Boolean saveOrUpdateColumn(Map<TableEntity, List<TableFieldEntity>> fieldEntityMap) {
        if (AiShuUtil.isEmpty(fieldEntityMap)) {
            return false;
        }
        Connection metadataConnection = null;
        PreparedStatement ps;
        long tempTableId = IdWorker.getId();
        String sqlDropTempTable = "DROP TABLE if exists adp.t_table_field_" + tempTableId;
        try {
            metadataConnection = dataSource.getConnection();
            ps = metadataConnection.prepareStatement(sqlDropTempTable);
            ps.execute();
            log.info("准备插入元数据，检测是否存在临时表，存在则删除：{}", sqlDropTempTable);
            long start = System.currentTimeMillis();
            String tempTableCreateSql = "CREATE TABLE if not exists adp.t_table_field_" + tempTableId + " (\n" +
                    "`f_table_id` BIGINT(20) NOT NULL DEFAULT '0',\n" +
                    "`f_field_name` VARCHAR(128) NOT NULL COMMENT '字段名' COLLATE 'utf8mb4_general_ci',\n" +
                    "`f_field_type` VARCHAR(128) NULL DEFAULT NULL COMMENT '字段类型' COLLATE 'utf8mb4_unicode_ci',\n" +
                    "`f_field_length` INT(11) NULL DEFAULT NULL COMMENT '字段长度',\n" +
                    "`f_field_precision` INT(11) NULL DEFAULT NULL COMMENT '字段精度',\n" +
                    "`f_field_comment` VARCHAR(2048) NULL DEFAULT NULL COMMENT '字段注释' COLLATE 'utf8mb4_unicode_ci',\n" +
                    "`f_advanced_params` VARCHAR(2048) NOT NULL DEFAULT '[]' COMMENT '字段高级参数' COLLATE 'utf8mb4_unicode_ci',\n" +
                    "`f_update_flag` TINYINT(1) NOT NULL DEFAULT '0',\n" +
                    "`f_update_time` DATETIME NOT NULL DEFAULT current_timestamp()\n" +
                    ")\n" +
                    "COMMENT='字段元数据'";
            ps = metadataConnection.prepareStatement(tempTableCreateSql);
            ps.execute();
            log.info("创建临时表：{}", "adp.t_table_field_" + tempTableId);
            String sql = "INSERT INTO adp.t_table_field_" + tempTableId + "(f_table_id, f_field_name,f_field_type,f_field_length,f_field_precision,f_field_comment,f_advanced_params,f_update_flag,f_update_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = metadataConnection.prepareStatement(sql);
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = sdf.format(now);
            AtomicInteger i = new AtomicInteger();
            AtomicInteger k = new AtomicInteger();
            Set<SchemaEntity> schemaEntities = new HashSet<>();
            Iterator<Map.Entry<TableEntity, List<TableFieldEntity>>> iterator = fieldEntityMap.entrySet().iterator();
            StringBuilder tableIds = new StringBuilder();
            tableIds.append('(');
            List<TableEntity> updateTableList = new ArrayList<>();
            while (iterator.hasNext()) {
                Map.Entry<TableEntity, List<TableFieldEntity>> fieldEntityEntry = iterator.next();
                TableEntity tableEntity = fieldEntityEntry.getKey();
                SchemaEntity schemaEntity = new SchemaEntity();
                schemaEntity.setId(tableEntity.getSchemaId());
                schemaEntity.setDataSourceId(tableEntity.getDataSourceId());
                schemaEntities.add(schemaEntity);
                List<TableFieldEntity> fieldEntityList = fieldEntityEntry.getValue();

                if (AiShuUtil.isNotEmpty(fieldEntityList)) {
                    updateTableList.add(tableEntity);
                    PreparedStatement finalPs = ps;
                    fieldEntityEntry.getValue().forEach(field -> {
                        i.getAndIncrement();
                        String tableName = tableEntity.getName();
                        Long tableId = tableEntity.getId();
                        String columnName = field.getFieldName();
                        String type = field.getFieldType();
                        Integer length = field.getFieldLength();
                        Integer precision = field.getFieldPrecision();
                        String advancedParams = field.getAdvancedParams();
                        String comment = field.getFieldComment();
                        try {
                            finalPs.setLong(1, tableId);
                            finalPs.setString(2, columnName);
                            finalPs.setString(3, type);
                            if (length == null) {
                                finalPs.setNull(4, Types.INTEGER);
                            } else {
                                finalPs.setInt(4, length);
                            }
                            if (precision == null) {
                                finalPs.setNull(5, Types.INTEGER);
                            } else {
                                finalPs.setInt(5, precision);
                            }
                            finalPs.setString(6, comment);
                            finalPs.setString(7, advancedParams);
                            finalPs.setInt(8, 0);
                            finalPs.setString(9, dateStr);
                            finalPs.addBatch();
                        } catch (Exception e) {
                            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "表：" + tableName + "拼接插入元数据字段sql错误", e.toString());
                        }
                        try {
                            if (i.get() == 1000) {
                                long start1 = System.currentTimeMillis();
                                finalPs.executeBatch();
                                finalPs.clearBatch();
                                i.set(0);
                                k.addAndGet(1000);
                                log.info("单次插入时间:{}ms" + "已插入数据量：{}s", (System.currentTimeMillis() - start1), k);
                            }
                        } catch (Exception e) {
                            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "表：" + tableName + "插入元数据字段错误", e.toString());
                        }
                    });
                    finalPs.executeBatch();
                    finalPs.clearBatch();
                    tableIds.append(tableEntity.getId()).append(',');
                }
            }
            tableIds.deleteCharAt(tableIds.length() - 1);
            tableIds.append(')');
            log.info("总插入时间{}ms", (System.currentTimeMillis() - start));
            //630066要求先物理删除字段元数据以实时更新字段排序
            if (AiShuUtil.isNotEmpty(updateTableList)) {
                log.info("开始删除字段元数据...");
                start = System.currentTimeMillis();
                LambdaQueryWrapper<TableFieldEntity> deleteFieldWrapper = new LambdaQueryWrapper<>();
                List<Long> updateTableIdList = updateTableList.stream().map(s -> s.getId()).collect(Collectors.toList());
                deleteFieldWrapper.in(TableFieldEntity::getTableId, updateTableIdList);
                tableFieldService.remove(deleteFieldWrapper);
                log.info("删除字段元数据时间{}ms", (System.currentTimeMillis() - start));
            }
            log.info("开始更新字段元数据...");
            start = System.currentTimeMillis();
            String updateSql = "INSERT INTO adp.t_table_field (f_table_id, f_field_name,f_field_type,f_field_length,f_field_precision,f_field_comment,f_advanced_params,f_update_flag,f_update_time)\n" +
                    "SELECT f_table_id, f_field_name,f_field_type,f_field_length,f_field_precision,f_field_comment,f_advanced_params,f_update_flag,f_update_time\n" +
                    "FROM adp.t_table_field_" + tempTableId + " \n" +
                    "ON DUPLICATE KEY UPDATE\n" +
                    "f_update_flag = (\n" +
                    "CASE WHEN !(t_table_field.f_field_name <=> t_table_field_" + tempTableId + ".f_field_name) THEN 1\n" +
                    "WHEN !(t_table_field.f_field_type <=> t_table_field_" + tempTableId + ".f_field_type) THEN 1\n" +
                    "WHEN !(t_table_field.f_field_length <=> t_table_field_" + tempTableId + ".f_field_length) THEN 1\n" +
                    "WHEN !(t_table_field.f_field_precision <=> t_table_field_" + tempTableId + ".f_field_precision) THEN 1\n" +
                    "WHEN !(t_table_field.f_field_comment <=> t_table_field_" + tempTableId + ".f_field_comment) THEN 1\n" +
                    "WHEN !(t_table_field.f_advanced_params <=> t_table_field_" + tempTableId + ".f_advanced_params) THEN 1\n" +
                    "ELSE 0 END),\n" +
                    "f_field_type = VALUES(f_field_type),\n" +
                    "f_field_length = VALUES(f_field_length),\n" +
                    "f_field_precision = VALUES(f_field_precision),\n" +
                    "f_field_comment = VALUES(f_field_comment),\n" +
                    "f_advanced_params = VALUES(f_advanced_params),\n" +
                    "f_update_time = VALUES(f_update_time),\n" +
                    "f_delete_time = null,\n" +
                    "f_delete_flag = 0;";
            ps = metadataConnection.prepareStatement(updateSql);
            ps.execute();
            log.info("更新时间:{}ms", (System.currentTimeMillis() - start));
            //bug630066 改成物理删除字段元数据, 注释逻辑删除模块
//            log.info("开始删除数据...");
//            start = System.currentTimeMillis();
//            for (SchemaEntity ignored : schemaEntities) {
//                String deleteSql = "UPDATE adp.t_table_field SET f_delete_flag = 1, f_delete_time = '%s' WHERE  f_table_id IN  %s AND f_update_time != '%s'";
//                deleteSql = String.format(deleteSql, dateStr, tableIds, dateStr);
//                ps = metadataConnection.prepareStatement(deleteSql);
//                ps.execute();
//            }
//            log.info("删除时间:{}ms", (System.currentTimeMillis() - start));
        } catch (SQLException e) {
            log.error("更新字段元数据失败:{}", e.toString());
        } finally {
            try {
                ps = metadataConnection.prepareStatement(sqlDropTempTable);
                ps.execute();
                log.info("执行完成删除临时表：{}", sqlDropTempTable);
                if (ps != null) {
                    ps.close();
                }
                if (metadataConnection != null) {
                    metadataConnection.close();
                }
                return Boolean.TRUE;
            } catch (SQLException e) {
                log.error("关闭元数据库连接异常:{}", e.toString());
                return Boolean.FALSE;
            }
        }
    }

    private String blobToString(byte[] blob) {
        return Base64.getEncoder().encodeToString(blob);
    }

}
