package com.eisoo.metadatamanage.web.service.impl.lineage.platform;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.metadatamanage.web.service.impl.lineage.AnyDataLineageServiceManager;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.Constant;
import com.eisoo.util.LineageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 10:29
 * @Version:1.0
 */
@Component
@Slf4j
public class PlatFormLineageRealTimeService {
    @Autowired
    private AnyDataLineageServiceManager anyDataLineageServiceManager;
    @KafkaListener(containerFactory = "LineageKafkaListenerContainerFactory", topics = "${anydata.kafka.group-topic}", autoStartup = "${anydata.kafka.enable}")
    public void setCommitType(ConsumerRecords<String, String> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            String data = "";
            String entities = "";
            String actionType = "";
            String classType = "";
            try {
                data = record.value();
                JSONObject root = JSONObject.parse(data);
                JSONObject jsonObject = root.getJSONObject("payload");
                JSONObject contentJsonObject = jsonObject.getJSONObject("content");
                entities = contentJsonObject.getString("entities");
                actionType = contentJsonObject.getString("type");
                classType = contentJsonObject.getString("class_name");
            } catch (Exception e) {
                log.error("解析kafka消息失败！data={}", data, e);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
            if (LineageUtil.isEmpty(classType) || LineageUtil.isEmpty(actionType)) {
                continue;
            }
            Class aClass = Constant.TYPE_REFERENCE_MAP.get(classType);
            if (null == aClass) {
                log.error("解析kafka消息失败！反序列化所需Class无法找到！classType={}", classType);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
            try {
                List<BaseLineageEntity> list = JSON.parseArray(entities, aClass);
                anyDataLineageServiceManager.dispatcherDataBase(classType, actionType, list);
                log.info("kafka消费{}类型实体成功：{}相关db&relation成功,更新数据如下：\n{}", classType, actionType, entities);
            }catch (Exception e) {
                log.error("消费kafka消息失败！data={}", data, e);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
/*             response = anyDataLineageServiceManager.dispatcher(classType,actionType,arrayList);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("kafka consumer 消费成功！发送adf请求成功，图谱更新成功！");
            } else {
                //{"Description":"Nebula has some error.","ErrorCode":"Builder.GraphController.AlterGraphData.UnknownError","ErrorDetails":"Nebula has some error.","ErrorLink":"","Solution":"Please contact the developers."}
                log.error("kafka consumer 消费成功!发送adf请求成功，但是图谱更新失败，原因如下：{}", response);
                throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                         "kafka consumer 消费成功!发送adf请求成功，但是图谱更新失败",
                                         response);
            }*/
        }
        ack.acknowledge();
    }
}
