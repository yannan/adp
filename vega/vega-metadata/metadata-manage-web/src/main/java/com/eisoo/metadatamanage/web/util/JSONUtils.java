package com.eisoo.metadatamanage.web.util;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static com.eisoo.metadatamanage.util.constant.DateConstants.YYYY_MM_DD_HH_MM_SS;

import com.fasterxml.jackson.databind.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.common.base.Strings;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.util
 * @Date: 2023/3/30 15:34
 */
public class JSONUtils {
    private static final Logger logger = LoggerFactory.getLogger(JSONUtils.class);

    static {
        logger.info("init timezone: {}", TimeZone.getDefault());
    }

    private static final SimpleModule LOCAL_DATE_TIME_MODULE = new SimpleModule()
            .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer())
            .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

    /**
     * can use static singleton, inject: just make sure to reuse!
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .configure(READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(REQUIRE_SETTERS_FOR_GETTERS, true)
            .registerModule(LOCAL_DATE_TIME_MODULE)
            .setTimeZone(TimeZone.getDefault())
            .setDateFormat(new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS));

    private JSONUtils() {
        throw new UnsupportedOperationException("Construct JSONUtils");
    }

    public static synchronized void setTimeZone(TimeZone timeZone) {
        objectMapper.setTimeZone(timeZone);
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    public static JsonNode toJsonNode(Object obj) {
        return objectMapper.valueToTree(obj);
    }

    public static JsonNode toJsonNode(String str) {
        try {
            return objectMapper.readTree(str);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * json representation of object
     *
     * @param object  object
     * @param feature feature
     * @return object to json string
     */
    public static String toJsonString(Object object, SerializationFeature feature) {
        try {
            ObjectWriter writer = objectMapper.writer(feature);
            return writer.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("object to json exception!", e);
        }

        return null;
    }

    /**
     * url参数解析为json字符串
     *
     * @param props
     * @return
     */
    public static Map<String, String> props2Map(String props) {
//        String[] propArray =  props.split("&");
//        StringBuffer propBuffer = new StringBuffer();
//        propBuffer.append("[");
//        Arrays.stream(propArray).forEach(a ->{
//            propBuffer.append("{");
//            propBuffer.append("\"");
//            String key = a.substring(0,a.indexOf("="));
//            propBuffer.append(key);
//            propBuffer.append("\"");
//            propBuffer.append(":\"");
//            String value = a.substring(a.indexOf("=")+1);
//            propBuffer.append(value);
//            propBuffer.append("\"");
//            propBuffer.append("}");
//            propBuffer.append(",");
//        });
//        propBuffer.deleteCharAt(propBuffer.length()-1);
//        propBuffer.append("]");
        try {
            String[] propArray = props.split("&");
            Map<String, String> resultMap = new HashMap<>();
            Arrays.stream(propArray).forEach(a -> {
                String key = a.substring(0, a.indexOf("="));
                String value = a.substring(a.indexOf("=") + 1);
                resultMap.put(key, value);
            });
            return resultMap;
        } catch (Exception e) {
            logger.error(e.toString());
            return null;
        }
    }

    /**
     * This method deserializes the specified Json into an object of the specified class. It is not
     * suitable to use if the specified class is a generic type since it will not have the generic
     * type information because of the Type Erasure feature of Java. Therefore, this method should not
     * be used if the desired type is a generic type. Note that this method works fine if the any of
     * the fields of the specified object are generics, just the object itself should not be a
     * generic type.
     *
     * @param json  the string from which the object is to be deserialized
     * @param clazz the class of T
     * @param <T>   T
     * @return an object of type T from the string
     * classOfT
     */
    public static @Nullable <T> T parseObject(String json, Class<T> clazz) {
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("Parse object exception, jsonStr: {}, class: {}", json, clazz, e);
        }
        return null;
    }

    /**
     * deserialize
     *
     * @param src   byte array
     * @param clazz class
     * @param <T>   deserialize type
     * @return deserialize type
     */
    public static <T> T parseObject(byte[] src, Class<T> clazz) {
        if (src == null) {
            return null;
        }
        String json = new String(src, UTF_8);
        return parseObject(json, clazz);
    }

    /**
     * json to list
     *
     * @param json  json string
     * @param clazz class
     * @param <T>   T
     * @return list
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyList();
        }

        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            logger.error("parse list exception!", e);
        }

        return Collections.emptyList();
    }

    /**
     * check json object valid
     *
     * @param json json
     * @return true if valid
     */
    public static boolean checkJsonValid(String json) {

        if (Strings.isNullOrEmpty(json)) {
            return false;
        }

        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            logger.error("check json object valid exception!", e);
        }

        return false;
    }

    /**
     * Method for finding a JSON Object field with specified name in this
     * node or its child nodes, and returning value it has.
     * If no matching field is found in this node or its descendants, returns null.
     *
     * @param jsonNode  json node
     * @param fieldName Name of field to look for
     * @return Value of first matching node found, if any; null if none
     */
    public static String findValue(JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.findValue(fieldName);

        if (node == null) {
            return null;
        }

        return node.asText();
    }

    /**
     * json to map
     * {@link #toMap(String, Class, Class)}
     *
     * @param json json
     * @return json to map
     */
    public static Map<String, String> toMap(String json) {
        return parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    /**
     * json to map
     *
     * @param json   json
     * @param classK classK
     * @param classV classV
     * @param <K>    K
     * @param <V>    V
     * @return to map
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> classK, Class<V> classV) {
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<K, V>>() {
            });
        } catch (Exception e) {
            logger.error("json to map exception!", e);
        }

        return Collections.emptyMap();
    }

    /**
     * from the key-value generated json  to get the str value no matter the real type of value
     *
     * @param json     the json str
     * @param nodeName key
     * @return the str value of key
     */
    public static String getNodeString(String json, String nodeName) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode jsonNode = rootNode.findValue(nodeName);
            if (Objects.isNull(jsonNode)) {
                return "";
            }
            return jsonNode.isTextual() ? jsonNode.asText() : jsonNode.toString();
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /**
     * json to object
     *
     * @param json json string
     * @param type type reference
     * @param <T>
     * @return return parse object
     */
    public static <T> T parseObject(String json, TypeReference<T> type) {
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            logger.error("json to map exception!", e);
        }

        return null;
    }

    /**
     * object to json string
     *
     * @param object object
     * @return json string
     */
    public static String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Object json deserialization exception.", e);
        }
    }

    /**
     * serialize to json byte
     *
     * @param obj object
     * @param <T> object type
     * @return byte array
     */
    public static <T> byte[] toJsonByteArray(T obj) {
        if (obj == null) {
            return null;
        }
        String json = "";
        try {
            json = toJsonString(obj);
        } catch (Exception e) {
            logger.error("json serialize exception.", e);
        }

        return json.getBytes(UTF_8);
    }

    public static ObjectNode parseObject(String text) {
        try {
            if (StringUtils.isEmpty(text)) {
                return parseObject(text, ObjectNode.class);
            } else {
                return (ObjectNode) objectMapper.readTree(text);
            }
        } catch (Exception e) {
            throw new RuntimeException("String json deserialization exception.", e);
        }
    }

    public static ArrayNode parseArray(String text) {
        try {
            return (ArrayNode) objectMapper.readTree(text);
        } catch (Exception e) {
            throw new RuntimeException("Json deserialization exception.", e);
        }
    }

    /**
     * json serializer
     */
    public static class JsonDataSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeRawValue(value);
        }

    }

    /**
     * json data deserializer
     */
    public static class JsonDataDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node instanceof TextNode) {
                return node.asText();
            } else {
                return node.toString();
            }
        }

    }

    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

        @Override
        public void serialize(LocalDateTime value,
                              JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(formatter));
        }
    }

    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return LocalDateTime.parse(p.getValueAsString(), formatter);
        }
    }

    public static Map<String, String> jsonToMap(String json) {
        return json2Obj(json, Map.class);
    }

    public static <T> String map2json(Map<String, T> map) {
        return obj2json(map);
    }

    public static String obj2json(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (IOException e) {
            logger.error("序列化错误：", e);
        }
        return null;
    }

    public static <T> T json2Obj(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (IOException e) {
            logger.error("反序列化错误：{}", json);
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> List<T> json2List(String jsonString, Class<T> valueType) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return objectMapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            logger.error("反序列化错误：{}", jsonString);
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
