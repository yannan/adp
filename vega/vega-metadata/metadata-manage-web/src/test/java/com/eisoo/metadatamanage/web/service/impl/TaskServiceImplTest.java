package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.LiveDdlEntity;
import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.metadatamanage.lib.dto.*;
import com.eisoo.metadatamanage.lib.enums.DataSourceUpdateStatusEnum;
import com.eisoo.metadatamanage.lib.enums.DdlPushStatusEnum;
import com.eisoo.metadatamanage.lib.enums.DdlTypeEnum;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.config.SchedulerConfig;
import com.eisoo.metadatamanage.web.util.DataSourceUtils;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

//@SpringBootTest
class TaskServiceImplTest {

    private static final String commentColumnPostgresql = "COMMENT ON COLUMN public.table100.field1_new IS 'ddd'";
    private static final String renameTablePostgresql = "ALTER TABLE public.example_table RENAME TO example_table1";
    private static final String commentTablePostgresql = "COMMENT ON TABLE public.example_table1 IS 'xxx'";
    private static final String addColumnPostgresql = "ALTER TABLE public.example_table1 ADD column1 varchar NULL";
    private static final String alterColumnPostgresql = "ALTER TABLE public.example_table1 ALTER COLUMN column1 TYPE real USING column1::real";
    private static final String dropColumnPostgresql = "ALTER TABLE public.example_table1 DROP COLUMN column1";
    private static final String copyTablePostgresql = "CREATE TABLE public.new_table AS TABLE public.example_table1 WITH NO DATA";
    private static final String dropTablePostgresql = "DROP TABLE public.new_table";
    private static final String createTablePostgresql = "CREATE TABLE public.aaa (id1 int4, id2 int4, id3 int4, id40 int4)";
    private SchedulerConfig schedulerConfig = new SchedulerConfig();

    @Test
    void registerUpdateMetadataTrigger() {
        schedulerConfig.setProcessUuid("ProcessUuid-test-vbc1");
        schedulerConfig.setProcessName("ProcessName-test-nvb1");
        schedulerConfig.setTaskName("TaskName-test-mvyt");
        schedulerConfig.setTaskUuid("TaskUuid-test-bdy1");
        schedulerConfig.setToken("token-test");
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
        urlDto.setValue("http://10.4.11.11:8888/api/metadata-manage/v1/task/updateMetaData");
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

        //启动任务自动分类分项与自动清理需求后再续写
//        AdvancedDTO projectCodeDto = new AdvancedDTO();
//        projectCodeDto.setKey(Constants.KEY_PROJECT_CODE);
//        projectCodeDto.setValue();
//        advancedDTOList.add(projectCodeDto);

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
        httpParamsDto.setValue(JSONUtils.toJsonString(httpParamDtoList));
        advancedDTOList.add(httpParamsDto);

        taskDto.setAdvanced_params(advancedDTOList);

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", schedulerConfig.getToken());
        String taskDtoStr = JSONUtils.toJsonString(taskDto);

        String schedulerModelUrl = "http://10.4.108.214:8787/api/data-sync/v2/model";
        String responseStr = HttpUtil.executePostWithJson(schedulerModelUrl, taskDtoStr, headerMap);
        Assert.assertEquals(true, StringUtils.contains(responseStr, "成功"));

        //创建新工作流

        SchedulerProcessDto schedulerProcessDto = new SchedulerProcessDto();
        schedulerProcessDto.setProcess_name(schedulerConfig.getProcessName());
        schedulerProcessDto.setProcess_uuid(schedulerConfig.getProcessUuid());
        schedulerProcessDto.setCrontab(schedulerConfig.getCron());
        schedulerProcessDto.setCrontab_status(0);

        List<SchedulerProcessRelationDto> relationDtoList = new ArrayList<>();
        SchedulerProcessRelationDto relationDto = new SchedulerProcessRelationDto();
        relationDto.setModel_uuid(schedulerConfig.getTaskUuid());
        relationDto.setModel_type("3");
        relationDtoList.add(relationDto);
        schedulerProcessDto.setModels(relationDtoList);

        schedulerProcessDto.setOnline_status(0);
        schedulerProcessDto.setStart_time("2024-01-01 00:00:00");
        schedulerProcessDto.setEnd_time("2034-01-01 00:00:00");
        String processStr = JSONUtils.toJsonString(schedulerProcessDto);

        String schedulerProcessUrl = "http://10.4.108.214:8787/api/data-sync/v2/process";
        responseStr = HttpUtil.executePostWithJson(schedulerProcessUrl, processStr, headerMap);
        Assert.assertEquals(true, StringUtils.contains(responseStr, "成功"));

        //删除旧工作流

        String deleteProcessUrl = "http://10.4.108.214:8787/api/data-sync/v2/process";
        deleteProcessUrl = String.format("%s/%s", deleteProcessUrl, schedulerConfig.getProcessUuid());
        responseStr = HttpUtil.executeDeleteHttpRequest(deleteProcessUrl, headerMap);
        Assert.assertEquals(true, StringUtils.contains(responseStr, "成功"));

        String deleteModelUrl = String.format("%s/%s", schedulerModelUrl, schedulerConfig.getTaskUuid());
        responseStr = HttpUtil.executeDeleteHttpRequest(deleteModelUrl, headerMap);
        Assert.assertEquals(true, StringUtils.contains(responseStr, "成功"));
    }

    @Test
    void updateSchemaByVirtual() {
        DataSourceEntity dataSourceEntity= new DataSourceEntity();
        dataSourceEntity.setId("11l");
        dataSourceEntity.setName("mariadb");
        dataSourceEntity.setDataSourceType(16);
        dataSourceEntity.setDataSourceTypeName("mariadb");
        dataSourceEntity.setUserName("root");
        dataSourceEntity.setPassword("MTIzNDU2");
        dataSourceEntity.setExtendProperty("currentSchema=testdb&vCatalogName=mymaria&vConnector=maria&");
        dataSourceEntity.setHost("10.4.36.90");
        dataSourceEntity.setPort(3306);
        dataSourceEntity.setDatabaseName("testdb");
        Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
        String schemaName = extendPropertyMap.get(DataSourceConstants.SCHEMAKEY);
        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setName(schemaName);
        Assert.assertEquals("testdb", schemaEntity.getName());
    }

    @Test
    void updateTableByVirtual() {
        Date now = new Date();
        DataSourceEntity dataSourceEntity= new DataSourceEntity();
        dataSourceEntity.setId("22l");
        dataSourceEntity.setName("holo_001");
        dataSourceEntity.setDataSourceType(3);
        dataSourceEntity.setDataSourceTypeName("PostgreSQL");
        dataSourceEntity.setUserName("root");
        dataSourceEntity.setPassword("MTIzNDU2");
        dataSourceEntity.setExtendProperty("currentSchema=public&vCatalogName=holo_001&vConnector=hologres&");
        dataSourceEntity.setHost("10.4.36.90");
        dataSourceEntity.setPort(80);
        dataSourceEntity.setDatabaseName("db_dev");
        List<DataSourceEntity> dataSourceEntityList = new ArrayList<>();
        dataSourceEntityList.add(dataSourceEntity);


        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setId(1l);
        schemaEntity.setName("public");
        schemaEntity.setDataSourceId("22l");
        List<SchemaEntity> schemaEntityList = new ArrayList<>();
        schemaEntityList.add(schemaEntity);

        DdlLogDto ddlLogDto = new DdlLogDto();
        ddlLogDto.setStatement(commentColumnPostgresql);
        ddlLogDto.setDdlTime(now);
        List<LiveDdlDto> ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        DdlTypeEnum expectType = DdlTypeEnum.CommentColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        ddlDtoList.clear();

        ddlLogDto.setStatement(renameTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.RenameTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        Assert.assertEquals("example_table1", ddlDtoList.get(0).getTargetTable());
        ddlDtoList.clear();

        ddlLogDto.setStatement(commentTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.CommentTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        Assert.assertEquals("xxx", ddlDtoList.get(0).getComment());
        ddlDtoList.clear();

        ddlLogDto.setStatement(addColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        ddlDtoList.clear();

        ddlLogDto.setStatement(alterColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        ddlDtoList.clear();

        ddlLogDto.setStatement(dropColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        ddlDtoList.clear();

        ddlLogDto.setStatement(copyTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.CreateTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        Assert.assertEquals("new_table", ddlDtoList.get(0).getTableName());
        Assert.assertEquals(schemaEntity.getName(), ddlDtoList.get(0).getSchemaName());
        Assert.assertEquals(dataSourceEntity.getId(), ddlDtoList.get(0).getDataSourceId());
        Assert.assertEquals(dataSourceEntity.getName(), ddlDtoList.get(0).getDataSourceName());
        Assert.assertEquals(dataSourceEntity.getDataSourceType(), ddlDtoList.get(0).getDatasourceType());
        Assert.assertEquals(dataSourceEntity.getDataSourceTypeName(), ddlDtoList.get(0).getDatasourceTypeName());
        ddlDtoList.clear();

        ddlLogDto.setStatement(dropTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.DropTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        Assert.assertEquals(dataSourceEntity.getId(), ddlDtoList.get(0).getDataSourceId());
        Assert.assertEquals("new_table", ddlDtoList.get(0).getTableName());
        ddlDtoList.clear();

        ddlLogDto.setStatement(createTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        expectType = DdlTypeEnum.CreateTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        Assert.assertEquals(schemaEntity.getId(), ddlDtoList.get(0).getSchemaId());
        Assert.assertEquals("aaa", ddlDtoList.get(0).getTableName());
        Assert.assertEquals(schemaEntity.getName(), ddlDtoList.get(0).getSchemaName());
        Assert.assertEquals(dataSourceEntity.getId(), ddlDtoList.get(0).getDataSourceId());
        Assert.assertEquals(dataSourceEntity.getName(), ddlDtoList.get(0).getDataSourceName());
        Assert.assertEquals(dataSourceEntity.getDataSourceType(), ddlDtoList.get(0).getDatasourceType());
        Assert.assertEquals(dataSourceEntity.getDataSourceTypeName(), ddlDtoList.get(0).getDatasourceTypeName());
    }

    @Test
    void updateMetaData() {
        List<DataSourceEntity> dataSourceEntityList = new ArrayList<>();
        DataSourceEntity entity = new DataSourceEntity();
        entity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.BROADCASTING.getCode());
        dataSourceEntityList.add(entity);
        entity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.WAITING.getCode());
        dataSourceEntityList.add(entity);
        entity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.UPDATING.getCode());
        dataSourceEntityList.add(entity);
        stateConvert(dataSourceEntityList);
    }
    private void stateConvert(List<DataSourceEntity> dataSourceEntityList) {
        if (AiShuUtil.isNotEmpty(dataSourceEntityList)) {
            dataSourceEntityList.forEach(dataSourceEntity -> {
                if (dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.BROADCASTING.getCode())) {
                    //通知流程
                    List<LiveDdlEntity> liveDdlEntityList = new ArrayList<>();
                    LiveDdlEntity liveDdlEntity =new LiveDdlEntity();
                    liveDdlEntity.setPushStatus(DdlPushStatusEnum.PUSH_WAITING.getCode());
                    liveDdlEntityList.add(liveDdlEntity);
                    if (AiShuUtil.isNotEmpty(liveDdlEntityList)) {
                        liveDdlEntityList.forEach(ddl -> ddl.setPushStatus(DdlPushStatusEnum.PUSH_FINISH.getCode()));
                    }
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.WAITING.getCode());
                } else if(dataSourceEntity.getLiveUpdateStatus().equals(DataSourceUpdateStatusEnum.WAITING.getCode())) {
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.UPDATING.getCode());
                } else {
                    dataSourceEntity.setLiveUpdateStatus(DataSourceUpdateStatusEnum.BROADCASTING.getCode());
                }
            });
        }
    }
}