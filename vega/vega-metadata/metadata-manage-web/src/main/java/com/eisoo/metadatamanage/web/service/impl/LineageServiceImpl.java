package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.lib.dto.LineageReportDto;
import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.web.commons.Constants;
import com.eisoo.metadatamanage.web.extra.model.DataSource;
import com.eisoo.metadatamanage.web.service.IDataSourceService;
import com.eisoo.metadatamanage.web.service.ILineageService;
import com.eisoo.metadatamanage.web.service.impl.lineage.*;
import com.eisoo.metadatamanage.web.util.CheckErrorUtil;
import com.eisoo.metadatamanage.web.util.CheckErrorVo;
import com.eisoo.standardization.common.exception.AiShuException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class LineageServiceImpl extends LineageKafkaService implements ILineageService {

    @Autowired
    DataxLineageService dataxLineageService;

    @Autowired
    HiveLineageService hiveLineageService;

    @Autowired
    SparkLineageService sparkLineageService;


    @Autowired
    Lineage2MysqlService lineage2MysqlService;

    @Autowired
    IDataSourceService iDataSourceService;


    @Override
    public void report(LineageReportDto reportDto) {
        List<Lineage> lineageList = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        for (LineageReportDto.Lineage row : reportDto.getData()) {
            Lineage temp = new Lineage();
            temp.setCreateTime(timestamp);
            temp.setQueryText(row.getQueryText());

            Lineage.Vertex source = temp.getSource();
            source.setDbType(DbType.of(row.getSource().getDbType()));
            source.setDbName(row.getSource().getDbName());
            source.setDbSchema(row.getSource().getDbSchema());
            source.setTbName(row.getSource().getTbName());
            source.setColumn(row.getSource().getColumn());
            source.setDataSource(new Lineage.Vertex.DataSource());
            source.getDataSource().setDsId(row.getSource().getDataSource().getDsId());

            Lineage.Vertex target = temp.getTarget();
            target.setDbType(DbType.of(row.getTarget().getDbType()));
            target.setDbName(row.getTarget().getDbName());
            target.setDbSchema(row.getTarget().getDbSchema());
            target.setTbName(row.getTarget().getTbName());
            target.setColumn(row.getTarget().getColumn());
            target.setDataSource(new Lineage.Vertex.DataSource());
            target.getDataSource().setDsId(row.getTarget().getDataSource().getDsId());

            lineageList.add(temp);
        }
        setDataSource(lineageList);
        try {
            sendToLineageResultTopic(lineageList, Lineage.Type.USER_REPORT);
        } catch (Exception e) {
            log.error("发送到kafka失败：", e);
            throw new AiShuException(ErrorCodeEnum.InternalError);
        }

    }

    private void setDataSource(List<Lineage> lineageList) {
        Set<Long> dsIdSet = new HashSet<>();
        for (Lineage row : lineageList) {
            dsIdSet.add(row.getSource().getDataSource().getDsId());
            dsIdSet.add(row.getTarget().getDataSource().getDsId());
        }

        List<Long> dsIdList = new ArrayList<>();
        for (Long id : dsIdSet) {
            dsIdList.add(id);
        }

        List<DataSourceEntity> dataSourceEntityList = iDataSourceService.queryByIdList(dsIdList);
        Map<String, DataSourceEntity> dsIdEntityMap = new HashMap<>();
        for (DataSourceEntity row : dataSourceEntityList) {
            dsIdEntityMap.put(row.getId(), row);
        }

        List<CheckErrorVo> errorVos = new ArrayList<>();
        int idx = 0;
        String filedTemp = "data[%s].%s.data_source.ds_id";
        String msgTemp = "数据源id[%s]对应的数据源不存在";
        for (Lineage row : lineageList) {
            Long srcDsId = row.getSource().getDataSource().getDsId();
            Long dstDsId = row.getTarget().getDataSource().getDsId();

            DataSourceEntity srcDs = dsIdEntityMap.get(srcDsId);
            DataSourceEntity dstDs = dsIdEntityMap.get(dstDsId);

            if (null == srcDs) {
                CheckErrorUtil.createError(String.format(filedTemp, idx, "source"), String.format(msgTemp, srcDsId), errorVos);
            } else {
                if (!DbType.of(srcDs.getDataSourceTypeName()).equals(row.getSource().getDbType())) {
                    CheckErrorUtil.createError(
                            String.format("data[%s].source.db_tpye", idx),
                            String.format("数据库类型[%s]和数据源id查询结果[%s]不一致", row.getSource().getDbType().name(), srcDs.getDataSourceTypeName()),
                            errorVos);
                } else {
                    Lineage.Vertex.DataSource ds = row.getSource().getDataSource();
                    ds.setJdbcUrl(creatJdbcUrl(srcDs));
                    ds.setJdbcUser(srcDs.getUserName());
                }

            }

            if (null == dstDs) {
                CheckErrorUtil.createError(String.format(filedTemp, idx, "target"), String.format(msgTemp, dstDsId), errorVos);
            } else {
                if (!DbType.of(dstDs.getDataSourceTypeName()).equals(row.getTarget().getDbType())) {
                    CheckErrorUtil.createError(
                            String.format("data[%s].target.db_tpye", idx),
                            String.format("数据库类型[%s]和数据源id查询结果[%s]不一致", row.getTarget().getDbType().name(), dstDs.getDataSourceTypeName()),
                            errorVos);
                } else {
                    Lineage.Vertex.DataSource ds = row.getTarget().getDataSource();
                    ds.setJdbcUrl(creatJdbcUrl(dstDs));
                    ds.setJdbcUser(dstDs.getUserName());
                }
            }
        }

        if (errorVos.size() > 0) {
            throw new AiShuException(com.eisoo.standardization.common.enums.ErrorCodeEnum.DATA_NOT_EXIST, errorVos);
        }
    }

    private String creatJdbcUrl(DataSourceEntity srcDs) {
        DataSource ds = iDataSourceService.getDataSource(srcDs);
        String jdbcUrl = ds.getConnectionParamObject().getJdbcUrl();
        return jdbcUrl.split("\\?")[0];
    }

//    @KafkaListener(topics = Constants.DATAX_LINEAGE_TOPIC, groupId = Constants.KAFKA_GROUP_ID_DATAX_LINEAGE)
    public void dataxLineageConsumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        dataxLineageService.consumer(records, ack);
    }

//    @KafkaListener(topics = Constants.SPARK_LINEAGE_TOPIC, groupId = Constants.KAFKA_GROUP_ID_SPARK_LINEAGE)
    public void sparkLineageConsumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        sparkLineageService.consumer(records, ack);
    }

//    @KafkaListener(topics = Constants.HIVE_LINEAGE_TOPIC, groupId = Constants.KAFKA_GROUP_ID_HIVE_LINEAGE)
    public void hiveLineageConsumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        hiveLineageService.consumer(records, ack);
    }

//    @KafkaListener(topics = Constants.METADATA_LINEAGE_TOPIC, groupId = Constants.KAFKA_GROUP_ID_METADATA_LINEAGE)
    public void metadataLineageConsumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        lineage2MysqlService.consumer(records, ack);
    }


}
