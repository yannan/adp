package com.eisoo.metadatamanage.web.mq;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.service.mq
 * @Date: 2023/6/15 9:49
 */
@Slf4j
@Component
public class LiveDdlProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }
}
