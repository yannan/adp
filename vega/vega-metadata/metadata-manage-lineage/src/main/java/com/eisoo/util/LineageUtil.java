package com.eisoo.util;

import com.eisoo.entity.DolphinLineageEntity;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.fasterxml.jackson.databind.JsonNode;
import sun.misc.BASE64Encoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/8 10:45
 * @Version:1.0
 */
public class LineageUtil {
    public static final Integer CHUNK_DEFAULT_SIZE = 100;
    public static final BASE64Encoder BASE64Encoder = new BASE64Encoder();
    public static List<ArrayList<DolphinLineageEntity.DolphinColumnLineage>> groupList(ArrayList<DolphinLineageEntity.DolphinColumnLineage> originalList) {
        int listSize = originalList.size();
        int groupCount = (int) Math.ceil((double) listSize / CHUNK_DEFAULT_SIZE);
        List<ArrayList<DolphinLineageEntity.DolphinColumnLineage>> groupedLists = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groupedLists.add(new ArrayList<>());
        }
        for (int i = 0; i < listSize; i++) {
            int groupIndex = i / CHUNK_DEFAULT_SIZE;
            groupedLists.get(groupIndex).add(originalList.get(i));
        }
        return groupedLists;
    }

    public static boolean isEmpty(String s) {
        if (s == null || s.isEmpty() || "''".equals(s) || "null".equals(s)) {
            return true;
        }
        return false;
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static String makeVid(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(data.getBytes(StandardCharsets.UTF_8));
            // 将处理后的字节转成 16 进制，得到最终 32 个字符
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String makeMD5(String... data) {
        StringBuilder s = new StringBuilder();
        for (String d : data) {
            s.append(d);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(s.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static HashSet<String> extractUuidFromJson(String str) {
//        String ss = "[{\"member\":[{\"field_id\":[\"22cd8e3b-1b94-4300-84d5-64c9e80e9e68\",\"4fa27efb-71fd-44db-9721-330bee683a1d\"],\"field_name\":\"项目代号\",\"field_type\":\"1\",\"operator\":\"not null\",\"value\":\"\"}],\"relation\":\"and\"}]";
        HashSet<String> list = new HashSet<>();
        JsonNode jsonNode = JsonUtils.toJsonNode(str);
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode node = jsonNode.get(i);
            if (node.hasNonNull("member")) {
                JsonNode node1 = node.get("member");
                for (int k = 0; k < node1.size(); k++) {
                    JsonNode uuidNode = node1.get(k);
                    JsonNode node2 = uuidNode.get("field_id");
                    for (int j = 0; j < node2.size(); j++) {
                        String uuid = node2.get(j).asText();
                        list.add(uuid);
                    }
                }
            }
        }
        return list;
    }

    public static Map<String, HashSet<String>> extractUuid(String strAll) {
        HashMap<String, HashSet<String>> map = new HashMap<>();
        String startIndexFlag = "{{";
        String endIndexFlag = "}}";
        int beginIndex = -1;
        int endIndex = -1;
        beginIndex = strAll.indexOf(startIndexFlag);
        endIndex = strAll.indexOf(endIndexFlag);
        HashSet<String> columnUuidList = new HashSet<>();
        HashSet<String> indicatorUuidList = new HashSet<>();
        while (beginIndex != -1 && endIndex != -1) {
            String uuid = strAll.substring(beginIndex + 2, endIndex);
            if (uuid.contains("-")) {
                columnUuidList.add(uuid);
            } else {
                indicatorUuidList.add(uuid);
            }
            strAll = strAll.substring(endIndex + 1, strAll.length());
            beginIndex = strAll.indexOf(startIndexFlag);
            endIndex = strAll.indexOf(endIndexFlag);
        }
        if (!columnUuidList.isEmpty()) {
            map.put("column", columnUuidList);
        }
        if (!indicatorUuidList.isEmpty()) {
            map.put("indicator", indicatorUuidList);
        }
        return map;
    }

    public static String separateWithComma(HashSet<String> strings) {
        return strings.stream().collect(Collectors.joining(","));
    }

    public static String getBase64(String base64) {
        return BASE64Encoder.encode(base64.getBytes());
    }

    /***
     *  检测AdLineageQueryController的请求参数
     * @param type
     * @param direction
     * @param step
     */
    public static void checkGetAdLineageParams(String id, String type, String direction, String step) {
        ErrorCodeEnum invalidParameter = null;
        String detail = "";
        String solution = "";
        if (CommonUtil.isEmpty(id)) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【请求参数id】是空!";
            solution = "请检查:【请求参数id】不能是空！";
            throw new AiShuException(invalidParameter, detail, solution);
        } else if (CommonUtil.isEmpty(type)) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【请求参数type】是空!";
            solution = "请检查:【请求参数type】不能是空！";
            throw new AiShuException(invalidParameter, detail, solution);
        } else if (CommonUtil.isEmpty(direction)) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【请求参数direction】是空!";
            solution = "请检查:【请求参数direction】不能是空！";
            throw new AiShuException(invalidParameter, detail, solution);
        }
        if (!Constant.TABLE.equals(type) && !Constant.COLUMN.equals(type) && !Constant.INDICATOR.equals(type)) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【type】不合法！";
            solution = "请检查:【type】table、column、indicator三者之一";
        } else if (!Constant.FORWARD.equals(direction) && !Constant.REVERSELY.equals(direction) && !Constant.BIDIRECT.equals(direction)) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【direction】不合法！";
            solution = "请检查:【direction】forward、reversely、bidirect三者之一";
        } else if (CommonUtil.isNotEmpty(step) && !step.matches("[1-9]\\d*")) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【step】不合法！";
            solution = "请检查:【step】必须是正整数";
        } else if (CommonUtil.isNotEmpty(step) && Integer.parseInt(step) > 1000) {
            invalidParameter = ErrorCodeEnum.InvalidParameter;
            detail = "【step】不合法！";
            solution = "请检查:【step】最大的条数不能超过1000";
        }
        if (CommonUtil.isNotEmpty(detail)) {
            assert invalidParameter != null;
            throw new AiShuException(invalidParameter, detail, solution);
        }
    }
}
