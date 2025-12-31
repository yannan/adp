package com.eisoo.metadatamanage.web.commons;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/13 10:10
 * @Version:1.0
 */

import com.eisoo.lineage.CommonUtil;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.Constant;
import com.eisoo.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;

import java.util.Collections;
import java.util.Properties;

@Slf4j
public class KafkaUtil {
    public static void checkKafkaMessage(String message) {
        JsonNode jsonNode = null;
        ErrorCodeEnum invalidParameter = null;
        String detail = "";
        String solution = "";
        try {
            jsonNode = JsonUtils.strToJsonNode(message);
        } catch (JsonProcessingException e) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "message格式错误！无法完成反序列化", "请检查：message是否符合json格式！");
        }
        while (true) {
            if (!jsonNode.hasNonNull("payload")) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload】是空！";
                solution = "请检查:【payload】不应该是空!";
                break;
            }
            JsonNode payloadNode = jsonNode.get("payload");

            if (!payloadNode.hasNonNull("type") || payloadNode.isNull()) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.type】是空！";
                solution = "请检查:【payload.type】不应该是空!";
                break;
            }
            String typePayload = payloadNode.get("type").asText();
            if (!"lineage".equals(typePayload)) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.type】不合法！";
                solution = "请检查:【payload.type】应该是固定值：lineage";
                break;
            }
            if (!payloadNode.hasNonNull("content") || payloadNode.isNull()) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content】是空！";
                solution = "请检查:【payload.content】不应该是空!";
                break;
            }
            JsonNode contentNode = payloadNode.get("content");
            // type
            if (!contentNode.hasNonNull("type") || contentNode.isNull()) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.type】是空！";
                solution = "请检查:【payload.content.type】不应该是空!";
                break;
            }
            String type = contentNode.get("type").asText();
            if (!Constant.DELETE.equals(type) && !Constant.INSERT.equals(type) && !Constant.UPDATE.equals(type)) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.type】不合法！";
                solution = "请检查:【payload.content.type】应该是delete、insert、update三者之一";
                break;
            }
            // class_name
            if (!contentNode.hasNonNull("class_name") || contentNode.isNull()) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.class_name】是空！";
                solution = "请检查:【payload.content.class_name】不应该是空!";
                break;
            }
            String className = contentNode.get("class_name").asText();
            if (!Constant.TABLE.equals(className) && !Constant.COLUMN.equals(className) && !Constant.INDICATOR.equals(className) && !Constant.DOLPHIN.equals(className)) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.class_name】不合法！";
                solution = "请检查:【payload.content.class_name】应该是table、column、indicator、dolphin四者之一";
                break;
            }
            // entities
            if (!contentNode.hasNonNull("entities") || contentNode.isNull()) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.entities】不存在";
                solution = "请检查:【payload.content.entities】是重要信息，应该存在！";
                break;
            }
            JsonNode entitiesNode = contentNode.get("entities");
            if (null == entitiesNode) {
                invalidParameter = ErrorCodeEnum.InvalidParameter;
                detail = "【payload.content.entities】是空！";
                solution = "请检查:【payload.content.entities】不应该是空!";
                break;
            }
            break;
        }
        if (CommonUtil.isNotEmpty(detail)) {
            throw new AiShuException(invalidParameter, detail, solution);
        }
    }
    public static void createTopic(String servers, String topicName, String jaasConfig, String protocol, String mechanism) {
        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("security.protocol", protocol);
        props.put("sasl.mechanism", mechanism);
        props.put("sasl.jaas.config", jaasConfig);
        KafkaAdminClient adminClient = (KafkaAdminClient) AdminClient.create(props);
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singleton(topicName));
        try {
            TopicDescription topicDescription = describeTopicsResult.values().get(topicName).get();
        } catch (Exception e) {
            NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
            CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singleton(newTopic));
            try {
                createTopicsResult.values().get(topicName).get();
                log.info("++++++++++++++++++++++++++++创建topic:{}成功！+++++++++++++++++++++++++++++", topicName);
            } catch (Exception ex) {
                log.error("++++++++++++++++++++++++++++创建topic:{}失败！+++++++++++++++++++++++++++++", topicName);
                throw new RuntimeException(ex);
            }
        } finally {
            adminClient.close();
        }
    }
}

