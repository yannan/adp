package com.eisoo.metadatamanage.web.config;

import com.eisoo.metadatamanage.web.condition.KafkaEnableCondition;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 19:06
 * @Version:1.0
 */
@Configuration
@Conditional(KafkaEnableCondition.class)
@EnableKafka
public class KafkaConsumerConfig {
    @Value("${anydata.kafka.bootstrap-servers}")
    private String kafkaServerUrls;
    @Value("${anydata.kafka.consumer.group-id}")
    private String groupId;
    @Value("${anydata.kafka.consumer.enable-auto-commit}")
    private boolean enableAutoCommit;
    @Value("${anydata.kafka.consumer.max-poll-interval-ms}")
    private int pollTimeout;
    @Value("${anydata.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${anydata.kafka.consumer.session-timeout-ms}")
    private int sessionTimeoutMs;
    @Value("${anydata.kafka.consumer.request-timeout-ms}")
    private int requestTimeoutMs;
    @Value("${anydata.kafka.consumer.max-poll-records}")
    private int maxPollRecords;
    @Value("${anydata.kafka.consumer.isolation-level}")
    private String isolationLevel;
    @Value("${anydata.kafka.consumer.allow-auto-create-topics}")
    private String autoCreateTopics;
    @Value("${anydata.kafka.consumer.heartbeat-interval-ms}")
    private int heartbeatIntervalMs;
    //
    @Value("${anydata.kafka.consumer.listener.type}")
    private String type;
    @Value("${anydata.kafka.consumer.listener.concurrency}")
    private int concurrency;

    @Value("${anydata.kafka.consumer.properties.sasl.mechanism}")
    private String mechanism;
    @Value("${anydata.kafka.consumer.properties.sasl.jaas-config}")
    private String jaasConfig;
    @Value("${anydata.kafka.consumer.properties.security.protocol}")
    private String protocol;

    public Map<String, Object> getConsumerConfigs() {
        Map<String, Object> props = getDefaultConsumerConfigs();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServerUrls);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, autoCreateTopics);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        return props;
    }

    /**
     * 默认消费者配置
     */
    private Map<String, Object> getDefaultConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // 自动提交（按周期）已消费offset 批量消费下设置false
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        // 消费会话超时时间(超过这个时间consumer没有发送心跳,就会触发rebalance操作)
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        // 消费请求超时时间
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        // 序列化
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 如果Kafka中没有初始偏移量，或者服务器上不再存在当前偏移量（例如，因为该数据已被删除）自动将该偏移量重置成最新偏移量
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol);
        props.put(SaslConfigs.SASL_MECHANISM, mechanism);
        return props;
    }

    /**
     * 消费者工厂类
     */
    public ConsumerFactory<String, String> initConsumerFactory(Map<String, Object> consumerConfigs) {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs);
    }

    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> initKafkaListenerContainerFactory(Map<String, Object> consumerConfigs) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(initConsumerFactory(consumerConfigs));
        factory.setBatchListener(type.equals("batch"));  // 是否开启批量消费
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(pollTimeout);  // 消费的超时时间
        return factory;
    }

    @Bean(name = "LineageKafkaListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> lineageKafkaListenerContainerFactory() {
        Map<String, Object> consumerConfigs = this.getConsumerConfigs();
        return initKafkaListenerContainerFactory(consumerConfigs);
    }
}
