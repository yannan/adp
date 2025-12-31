package com.eisoo.metadatamanage.web.service.impl.lineage.platform;

import com.eisoo.metadatamanage.web.service.KafkaProduceWithTransaction;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.AiShuUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 16:18
 * @Version:1.0
 */
@Slf4j
@Service
public class KafkaProduceWithTransactionImpl implements KafkaProduceWithTransaction {
    @Autowired(required = false)
    private LinkedBlockingQueue<KafkaProducer<String, String>> linkedBlockingQueue;
    @Override
    public Result<?> sendMessage(String topic, String message) {
        KafkaProducer<String, String> kafkaProducer = null;
        if (AiShuUtil.isEmpty(linkedBlockingQueue)) {
            return null;
        }
        try {
            kafkaProducer = linkedBlockingQueue.take();
        } catch (InterruptedException e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                     "linkedBlockingQueue获取kafka producer失败",
                                     e.getMessage(),
                                     "");
        }
        try {
            kafkaProducer.beginTransaction();
            ProducerRecord<String, String> resultRecord = new ProducerRecord<>(topic, "key", message);
            kafkaProducer.send(resultRecord).get();
        } catch (Exception e) {
            kafkaProducer.abortTransaction();
            Result<String> result = new Result<>();
            result.setCode("-1");
            result.setDetail("发送kafka消息失败，发送的message=" + message);
            result.setDescription(e.getMessage());
            return result;
        }
        kafkaProducer.commitTransaction();
        try {
            linkedBlockingQueue.put(kafkaProducer);
        } catch (InterruptedException e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException,
                                     "linkedBlockingQueue放还kafka producer失败",
                                     e.getMessage(),
                                     "");
        }
        return Result.success();
    }
}
