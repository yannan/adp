package com.eisoo.metadatamanage.web.config;

import com.eisoo.metadatamanage.web.commons.KafkaUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 16:37
 * @Version:1.0
 */
@Configuration
public class KafkaProviderConfig {
    @Value("${anydata.kafka.enable}")
    private Boolean enable;
    @Value("${anydata.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${anydata.kafka.group-topic}")
    private String groupTopic;
    @Value("${anydata.kafka.producer.transaction-id-prefix}")
    private String transactionIdPrefix;
    @Value("${anydata.kafka.producer.acks}")
    private String acks;
    @Value("${anydata.kafka.producer.retries}")
    private String retries;
    @Value("${anydata.kafka.producer.batch-size}")
    private String batchSize;
    @Value("${anydata.kafka.producer.buffer-memory}")
    private String bufferMemory;
    @Value("${anydata.kafka.producer.key-serializer}")
    private String keySerializer;
    @Value("${anydata.kafka.producer.value-serializer}")
    private String valueSerializer;
    @Value("${anydata.kafka.producer.transaction-timeout}")
    private String transactionTimeout;
    @Value("${anydata.kafka.producer.max-block-ms}")
    private String maxBlockMs;
    @Value("${anydata.kafka.producer.properties.sasl.mechanism}")
    private String mechanism;
    @Value("${anydata.kafka.producer.properties.sasl.jaas-config}")
    private String jaasConfig;
    @Value("${anydata.kafka.producer.properties.security.protocol}")
    private String protocol;

    @Bean
    public LinkedBlockingQueue<KafkaProducer<String, String>> kafkaProducer() {
        Map<String, Object> props = new HashMap<>(16);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        //发生错误后，消息重发的次数，开启事务必须大于0
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        //当多个消息发送到相同分区时,生产者会将消息打包到一起,以减少请求交互. 而不是一条条发送
        //批次的大小可以通过batch.size 参数设置.默认是16KB
        //较小的批次大小有可能降低吞吐量（批次大小为0则完全禁用批处理）。比如说，kafka里的消息5秒钟Batch才凑满了16KB，才能发送出去。那这些消息的延迟就是5秒钟,实测batchSize这个参数没有用
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        //有的时刻消息比较少,过了很久,比如5min也没有凑够16KB,这样延时就很大,所以需要一个参数. 再设置一个时间,到了这个时间,即使数据没达到16KB,也将这个批次发送出去
        props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        //生产者内存缓冲区的大小
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        //事务相关配置
        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeout);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        //反序列化，和生产者的序列化方式对应
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol);
        props.put(SaslConfigs.SASL_MECHANISM, mechanism);
        LinkedBlockingQueue<KafkaProducer<String, String>> queue = new LinkedBlockingQueue<>(50);
        // 创建topic
        if (enable){
            KafkaUtil.createTopic(bootstrapServers, groupTopic, jaasConfig, protocol, mechanism);
            for (int i = 0; i < 50; i++) {
                props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix + "-" + i);
                KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
                kafkaProducer.initTransactions();
                queue.add(kafkaProducer);
            }
        }
        return queue;
    }
}
