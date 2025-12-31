package com.eisoo.metadatamanage.web.mq;


import com.eisoo.metadatamanage.web.service.IDataSourceService;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.service.mq
 * @Date: 2023/6/15 9:49
 */
@Slf4j
@Component
public class KafkaConsumer {
    @Autowired
    IDataSourceService dataSourceService;

    @KafkaListener(topics = "af.configuration-center.datasource", groupId = "group_metadata_af.configuration-center.datasource", autoStartup = "${spring.kafka.enable}")
    public void onMessage(ConsumerRecords<String, String> data, Acknowledgment ack) {

        if (dataSourceService.MQHandle(data)) {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "af.metadata.actual-time.ddl", groupId = "group_metadata_af.actual-time.ddl", autoStartup = "${spring.kafka.enable}")
    public void onDDLMessage(ConsumerRecords<String, String> data, Acknowledgment ack) {
        if (dataSourceService.MQDDLHandle(data)) {
            ack.acknowledge();
        }
    }


}
