package com.eisoo.util;

import com.eisoo.entity.BaseLineageEntity;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.eisoo.metadatamanage.util.constant.DateConstants.YYYY_MM_DD_HH_MM_SS;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.util
 * @Date: 2023/3/30 15:34
 */
public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    static {
        JsonUtils.logger.info("init timezone: {}", TimeZone.getDefault());
    }

    private static final SimpleModule LOCAL_DATE_TIME_MODULE = new SimpleModule().addSerializer(LocalDateTime.class, new LocalDateTimeSerializer()).addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

    /**
     * can use static singleton, inject: just make sure to reuse!
     */
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true).configure(READ_UNKNOWN_ENUM_VALUES_AS_NULL, true).configure(REQUIRE_SETTERS_FOR_GETTERS, true).registerModule(JsonUtils.LOCAL_DATE_TIME_MODULE).setTimeZone(TimeZone.getDefault()).setDateFormat(new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS));

    private JsonUtils() {
        throw new UnsupportedOperationException("Construct JSONUtils");
    }

    public static synchronized void setTimeZone(final TimeZone timeZone) {
        JsonUtils.objectMapper.setTimeZone(timeZone);
    }

    public static ArrayNode createArrayNode() {
        return JsonUtils.objectMapper.createArrayNode();
    }

    public static ObjectNode createObjectNode() {
        return JsonUtils.objectMapper.createObjectNode();
    }

    public static JsonNode toJsonNode(final Object obj) {
        return JsonUtils.objectMapper.valueToTree(obj);
    }

    public static JsonNode toJsonNode(final String str) {
        try {
            return JsonUtils.objectMapper.readTree(str);
        } catch (final JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static JsonNode strToJsonNode( String str) throws JsonProcessingException {
        return JsonUtils.objectMapper.readTree(str);
    }
    /**
     * json representation of object
     *
     * @param object  object
     * @param feature feature
     * @return object to json string
     */
    public static String toJsonString(final Object object, final SerializationFeature feature) {
        try {
            final ObjectWriter writer = JsonUtils.objectMapper.writer(feature);
            return writer.writeValueAsString(object);
        } catch (final Exception e) {
            JsonUtils.logger.error("object to json exception!", e);
        }

        return null;
    }

    /**
     * url参数解析为json字符串
     *
     * @param props
     * @return
     */
    public static Map<String, String> props2Map(final String props) {
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
            final String[] propArray = props.split("&");
            final Map<String, String> resultMap = new HashMap<>();
            Arrays.stream(propArray).forEach(a -> {
                final String key = a.substring(0, a.indexOf('='));
                final String value = a.substring(a.indexOf('=') + 1);
                resultMap.put(key, value);
            });
            return resultMap;
        } catch (final Exception e) {
            JsonUtils.logger.error(e.toString());
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
    @Nullable
    public static <T> T parseObject(final String json, final Class<T> clazz) {
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }

        try {
            return JsonUtils.objectMapper.readValue(json, clazz);
        } catch (final Exception e) {
            JsonUtils.logger.error("Parse object exception, jsonStr: {}, class: {}", json, clazz, e);
        }
        return null;
    }

    public static ArrayList<? extends BaseLineageEntity> parseList(final String json, final Class<? extends BaseLineageEntity> clazz) throws JsonProcessingException {
        final TypeFactory t = TypeFactory.defaultInstance();
        return JsonUtils.objectMapper.readValue(json, t.constructCollectionType(ArrayList.class, clazz));
    }


    /**
     * deserialize
     *
     * @param src   byte array
     * @param clazz class
     * @param <T>   deserialize type
     * @return deserialize type
     */
    public static <T> T parseObject(final byte[] src, final Class<T> clazz) {
        if (null == src) {
            return null;
        }
        final String json = new String(src, UTF_8);
        return JsonUtils.parseObject(json, clazz);
    }

    /**
     * json to list
     *
     * @param json  json string
     * @param clazz class
     * @param <T>   T
     * @return list
     */
    public static <T> List<T> toList(String json, final Class<T> clazz) {
        JsonUtils.objectMapper.configure(READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyList();
        }
        try {
            final CollectionType listType = JsonUtils.objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            json = json.replace("\\", "").replaceAll("\"\\[", "\\[").replaceAll("\\]\"", "\\]");
            return JsonUtils.objectMapper.readValue(json, listType);
        } catch (final Exception e) {
            JsonUtils.logger.error("parse list exception!", e);
        }
        return Collections.emptyList();
    }

    /**
     * check json object valid
     *
     * @param json json
     * @return true if valid
     */
    public static boolean checkJsonValid(final String json) {

        if (Strings.isNullOrEmpty(json)) {
            return false;
        }

        try {
            JsonUtils.objectMapper.readTree(json);
            return true;
        } catch (final IOException e) {
            JsonUtils.logger.error("check json object valid exception!", e);
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
    public static String findValue(final JsonNode jsonNode, final String fieldName) {
        final JsonNode node = jsonNode.findValue(fieldName);

        if (null == node) {
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
    public static Map<String, String> toMap(final String json) {
        return JsonUtils.parseObject(json, new TypeReference<Map<String, String>>() {
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
    public static <K, V> Map<K, V> toMap(final String json, final Class<K> classK, final Class<V> classV) {
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyMap();
        }

        try {
            return JsonUtils.objectMapper.readValue(json, new TypeReference<Map<K, V>>() {
            });
        } catch (final Exception e) {
            JsonUtils.logger.error("json to map exception!", e);
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
    public static String getNodeString(final String json, final String nodeName) {
        try {
            final JsonNode rootNode = JsonUtils.objectMapper.readTree(json);
            final JsonNode jsonNode = rootNode.findValue(nodeName);
            if (Objects.isNull(jsonNode)) {
                return "";
            }
            return jsonNode.isTextual() ? jsonNode.asText() : jsonNode.toString();
        } catch (final JsonProcessingException e) {
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
    public static <T> T parseObject(final String json, final TypeReference<T> type) {
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }

        try {
            return JsonUtils.objectMapper.readValue(json, type);
        } catch (final Exception e) {
            JsonUtils.logger.error("json to map exception!", e);
        }

        return null;
    }

    /**
     * object to json string
     *
     * @param object object
     * @return json string
     */
    public static String toJsonString(final Object object) {
        try {
            return JsonUtils.objectMapper.writeValueAsString(object);
        } catch (final Exception e) {
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
    public static <T> byte[] toJsonByteArray(final T obj) {
        if (null == obj) {
            return null;
        }
        String json = "";
        try {
            json = JsonUtils.toJsonString(obj);
        } catch (final Exception e) {
            JsonUtils.logger.error("json serialize exception.", e);
        }

        return json.getBytes(UTF_8);
    }

    public static ObjectNode parseObject(final String text) {
        try {
            if (StringUtils.isEmpty(text)) {
                return JsonUtils.parseObject(text, ObjectNode.class);
            } else {
                return (ObjectNode) JsonUtils.objectMapper.readTree(text);
            }
        } catch (final Exception e) {
            throw new RuntimeException("String json deserialization exception.", e);
        }
    }

    public static ArrayNode parseArray(final String text) {
        try {
            return (ArrayNode) JsonUtils.objectMapper.readTree(text);
        } catch (final Exception e) {
            throw new RuntimeException("Json deserialization exception.", e);
        }
    }

    /**
     * json serializer
     */
    public static class JsonDataSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(final String value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
            gen.writeRawValue(value);
        }

    }

    /**
     * json data deserializer
     */
    public static class JsonDataDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final JsonNode node = p.getCodec().readTree(p);
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
        public void serialize(final LocalDateTime value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(this.formatter));
        }
    }

    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

        @Override
        public LocalDateTime deserialize(final JsonParser p, final DeserializationContext context) throws IOException {
            return LocalDateTime.parse(p.getValueAsString(), this.formatter);
        }
    }

    public static Map<String, String> jsonToMap(final String json) {
        return JsonUtils.json2Obj(json, Map.class);
    }

    public static <T> String map2json(final Map<String, T> map) {
        return JsonUtils.obj2json(map);
    }

    public static String obj2json(final Object o) {
        try {
            return JsonUtils.objectMapper.writeValueAsString(o);
        } catch (final IOException e) {
            JsonUtils.logger.error("序列化错误：", e);
        }
        return null;
    }

    public static <T> T json2Obj(final String json, final Class<T> valueType) {
        try {
            return JsonUtils.objectMapper.readValue(json, valueType);
        } catch (final IOException e) {
            JsonUtils.logger.error("反序列化错误：{}", json);
            JsonUtils.logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> List<T> json2List(final String jsonString, final Class<T> valueType) {
        try {
            final JavaType javaType = JsonUtils.objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JsonUtils.objectMapper.readValue(jsonString, javaType);
        } catch (final IOException e) {
            JsonUtils.logger.error("反序列化错误：{}", jsonString);
            JsonUtils.logger.error(e.getMessage(), e);
        }
        return null;
    }
}
