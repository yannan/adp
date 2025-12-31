package com.eisoo.metadatamanage.web.service;

import com.eisoo.standardization.common.api.Result;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 16:17
 * @Version:1.0
 */
public interface KafkaProduceWithTransaction {
    Result<?> sendMessage(String topic, String message);
}
