package com.eisoo.metadatamanage.web.commons;

import com.eisoo.dto.build.NetWorkBuildDto;
import com.eisoo.dto.build.StartBuildDto;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.config.DataSourceBuildConfig;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import com.eisoo.util.LineageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/24 14:15
 * @Version:1.0
 */
@Slf4j
public class GraphUtil {
    public static void delAllServiceFromGraphId(Integer graphId, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        List<String> serviceListByGraphId = getServiceListByGraphId(graphId, anyDataGraphConfig.getServiceQueryURL(), anyDataGraphConfig.getHeadMap());
        for (String serviceId : serviceListByGraphId) {
            cancelAndDeleteService(serviceId, anyDataGraphConfig);
        }
    }
    public static void cancelAndDeleteService(String serviceId, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        String serviceCancelURL = anyDataGraphConfig.getServiceCancelURL();
        serviceCancelURL = String.format(serviceCancelURL, serviceId);
        try {
            String responseCancel = HttpRequestUtils.sendHttpsPosToAdGraphBuild(serviceCancelURL, anyDataGraphConfig.getHeadMap(), null);
            if ("null".equals(responseCancel)) {
                String serviceDeleteURL = anyDataGraphConfig.getServiceDeletelURL();
                serviceDeleteURL = String.format(serviceDeleteURL, serviceId);
                String responseDelete = HttpRequestUtils.sendHttpsPosToAdGraphBuild(serviceDeleteURL, anyDataGraphConfig.getHeadMap(), null);
                if ("null".equals(responseDelete)) {
                    log.info("graph service delete success ! service id :{} ", serviceId);
                } else {
                    log.error("graph service delete failed ! error detail is  follow:{}", responseDelete);
                    throw new Exception();
                }
            } else {
                log.error("graph service cancel failed ! error detail is  follow:{}", responseCancel);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("graph service delete failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    public static boolean deleteGraphById(Integer graphId, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        if (null == graphId) {
            return false;
        }
        String graphInfoURL = anyDataGraphConfig.getStartBuildProgressURL();
        Integer knwId = anyDataGraphConfig.getKnwId();
        HashMap<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("graph_id", graphId);
        try {
            String response = HttpRequestUtils.httpGet(graphInfoURL, anyDataGraphConfig.getHeadMap(), paramMap);
            JsonNode responseNode = JsonUtils.toJsonNode(response);
            if (responseNode != null && responseNode.hasNonNull("res")) {
                log.info("checkGraphIsExistAndDelById success ! graph:{} is exist !", graphId);
                // 查询这个图谱的关联的service并删除掉！
                List<String> serviceListByGraphId = getServiceListByGraphId(graphId, anyDataGraphConfig.getServiceQueryURL(), anyDataGraphConfig.getHeadMap());
                for (String serviceId : serviceListByGraphId) {
                    GraphUtil.cancelAndDeleteService(serviceId, anyDataGraphConfig);
                }
                // 开始删除图谱：{"graphids": [3],"knw_id": 3}
                String deleteJson = "{\"graphids\": [" + graphId + "],\"knw_id\":" + knwId + "}"; // {"48":54}
                String responseDelGraph = HttpRequestUtils.sendHttpsPosToAdGraphBuild(anyDataGraphConfig.getStartDeleteURL(), anyDataGraphConfig.getHeadMap(), deleteJson);
                JsonNode responseDelGraphNode = JsonUtils.toJsonNode(responseDelGraph);
                // {"graph_id": [3], "state": "success"}
                if (responseDelGraphNode != null && responseDelGraphNode.hasNonNull("state")) {
                    String state = responseDelGraphNode.get("state").asText();
                    if ("success".equals(state)) {
                        log.info("graph  delete success ! graph id :{} ", graphId);
                        return true;
                    }
                } else {
                    log.error("graph delete failed ! error detail is  follow:{}", responseDelGraph);
                    throw new Exception();
                }
            } else {
                log.error("graph delete failed ! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("graph delete failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
        return true;
    }

    public static HashMap<String, Integer> getAllGraphs(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        String startQueryAllGraphURL = anyDataGraphConfig.getStartQueryAllGraphURL();
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("knw_id", anyDataGraphConfig.getKnwId());
        paramMap.put("page", 1);
        paramMap.put("size", 100);
        paramMap.put("name", anyDataGraphConfig.getGraphName());
        paramMap.put("order", "desc");
        paramMap.put("rule", "create");
        try {
            String response = HttpRequestUtils.httpGet(startQueryAllGraphURL, anyDataGraphConfig.getHeadMap(), paramMap);
            JsonNode responseNode = JsonUtils.toJsonNode(response);
            if (responseNode != null && responseNode.hasNonNull("res")) {
                int count = responseNode.get("res").get("count").asInt();
                if (0 == count) {
                    return map;
                }
                JsonNode node = responseNode.get("res").get("df");
                for (int i = 0; i < node.size(); i++) {
                    int graphId = node.get(i).get("id").asInt();
                    String name = node.get(i).get("name").asText();
                    map.put(name, graphId);
                }
            }
        } catch (Exception e) {
            log.error("getAllGraphs fail ! error = {}", e.getMessage());
            throw new Exception(e);
        }
        return map;
    }

    public Integer getDsIdByName(int knwId, String dsName, String url, Map<String, Object> headMap) throws Exception {
        // ds_type=mysql&knw_id=3&order=ascend&page=1&size=10
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("knw_id", knwId);
        paramMap.put("page", 1);
        paramMap.put("size", 100);
        paramMap.put("order", "ascend");
        paramMap.put("ds_type", "mysql");
        String response = HttpRequestUtils.httpGet(url, headMap, paramMap);
        JsonNode responseNode = JsonUtils.toJsonNode(response);
        assert responseNode != null;
        if (responseNode.hasNonNull("res")) {
            int count = responseNode.get("res").get("count").asInt();
            if (0 == count) {
                return null;
            }
            JsonNode childDs = responseNode.get("res").get("df");
            for (int i = 0; i < childDs.size(); i++) {
                String dsNameChild = childDs.get(i).get("dsname").asText();
                if (dsName.equalsIgnoreCase(dsNameChild)) {
                    // id塞进去
                    return childDs.get(i).get("id").asInt();
                }
            }
        } else {
            log.error("getDsIdByName failed! error detail is  follow:{}", response);
            throw new Exception();
        }
        return null;
    }

    /***
     * 根据graphId查询serviceId
     */
    public static List<String> getServiceListByGraphId(int graphId, String url, Map<String, Object> headMap) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("kg_id", graphId);
        paramMap.put("page", 1);
        paramMap.put("size", 1000);
        paramMap.put("order_field", "create_time");
        paramMap.put("order_type", "desc");
        try {
            String response = HttpRequestUtils.httpGet(url, headMap, paramMap);
            JsonNode responseNode = JsonUtils.toJsonNode(response);
            if (responseNode != null && responseNode.hasNonNull("res")) {
                log.info("getServiceListByGraphId success! ");
                int count = responseNode.get("res").get("count").asInt();
                if (0 == count) {
                    return list;
                }
                JsonNode childDs = responseNode.get("res").get("results");
                for (int i = 0; i < childDs.size(); i++) {
                    String serviceId = childDs.get(i).get("id").asText();
                    if (LineageUtil.isNotEmpty(serviceId)) {
                        list.add(serviceId);
                    }
                }
            } else {
                log.error("getServiceListByGraphId failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("getServiceListByGraphId failed ! error is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
        return list;
    }

    /***
     * 2,构建DataSource:查询->存在就删除->build
     * @param anyDataGraphConfig
     * @param dataSourceBuildConfig
     * @throws Exception
     */
    public static void startBuildDataSource(AnyDataGraphConfig anyDataGraphConfig, DataSourceBuildConfig dataSourceBuildConfig) throws Exception {
        try {
            Integer knwId = anyDataGraphConfig.getKnwId();
            String dsName = dataSourceBuildConfig.getDsName();
            boolean isExist = checkDsIsExistByKnwId(knwId, dsName, anyDataGraphConfig);
            if (isExist) {
                Integer dsId = anyDataGraphConfig.getDsId();
                log.warn("dsId={};dsName={} already existed ! now start delete this datasource!", dsId, dsName);
                //  {"dsids": [4]}
                String requestDel = "{\"dsids\": [" + dsId + "]}";
                String deleteResponse = HttpRequestUtils.httpDelete(anyDataGraphConfig.getDsDeleteURL(), anyDataGraphConfig.getHeadMap(), requestDel);
                // {"ds_ids": [4],"res": "success delete dsids [4] ! success"}
                JsonNode jsonNode = JsonUtils.toJsonNode(deleteResponse);
                if (jsonNode != null && jsonNode.hasNonNull("res")) {
                    String res = jsonNode.get("res").asText();
                    log.info(res);
                } else {
                    log.error("delete data source  failed! error detail is  follow:{}", deleteResponse);
                    throw new Exception();
                }
            }
            dataSourceBuildConfig.setKnwId(knwId);
            String para = JsonUtils.toJsonString(dataSourceBuildConfig);
            // { "code": 200,"data": 44,"message": "success"}
            String response = HttpRequestUtils.sendHttpsPosToAdGraphBuild(anyDataGraphConfig.getDsURL(), anyDataGraphConfig.getHeadMap(), para);
            JsonNode jsonNode = JsonUtils.toJsonNode(response);
            if (jsonNode != null && jsonNode.hasNonNull("res")) {
                String res = jsonNode.get("res").asText();
                if ("insert success".equals(res)) {
                    log.info("build data source success !");
                    int dsIdNew = jsonNode.get("ds_id").asInt();
                    // 将新的datasource塞进config
                    anyDataGraphConfig.setDsId(dsIdNew);
                } else {
                    log.error("build data source  failed! error detail is  follow:{}", response);
                    throw new Exception();
                }
            } else {
                log.error("build data source  failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("build data source  failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }
    public static boolean checkDsIsExistByKnwId(int knwId, String dsName, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        // ds_type=mysql&knw_id=3&order=ascend&page=1&size=10
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("knw_id", knwId);
        paramMap.put("page", 1);
        paramMap.put("size", 1000);
        paramMap.put("order", "ascend");
        paramMap.put("ds_type", "mysql");
        try {
            String response = HttpRequestUtils.httpGet(anyDataGraphConfig.getDsGetInfoURL(), anyDataGraphConfig.getHeadMap(), paramMap);
            if ("null".equals(response)) {
                return false;
            }
            JsonNode responseNode = JsonUtils.toJsonNode(response);
            if (responseNode != null && responseNode.hasNonNull("res")) {
                log.info("getDsInfoByKnwId success! ");
                int count = responseNode.get("res").get("count").asInt();
                if (0 == count) {
                    return false;
                }
                JsonNode childDs = responseNode.get("res").get("df");
                for (int i = 0; i < childDs.size(); i++) {
                    String dsNameChild = childDs.get(i).get("dsname").asText();
                    if (dsName.equalsIgnoreCase(dsNameChild)) {
                        // id塞进去
                        int dsId = childDs.get(i).get("id").asInt();
                        anyDataGraphConfig.setDsId(dsId);
                        return true;
                    }
                }
            } else {
                log.error("getDsInfoByKnwId failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("getDsInfoByKnwId failed! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
        return false;
    }

    public static int getNetWorkKnwIdByName(String knwName, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("knw_name", knwName);
        paramMap.put("page", 1);
        paramMap.put("size", 10);
        paramMap.put("order", "desc");
        paramMap.put("rule", "create");
        try {
            String response = HttpRequestUtils.httpGet(anyDataGraphConfig.getNetworkGetIdByNameURL(), anyDataGraphConfig.getHeadMap(), paramMap);
            if ("null".equals(response)) {
                return -99;
            }
            JsonNode responseNode = JsonUtils.toJsonNode(response);
            if (null != responseNode && responseNode.hasNonNull("res")) {
                log.info("getNetWorkByName success! knwName={}", knwName);
                int count = responseNode.get("res").get("count").asInt();
                if (0 == count) {
                    return -99;
                }
                return responseNode.get("res").get("df").get(0).get("id").asInt();
            } else {
                log.error("getNetWorkByName failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("getNetWorkByName failed! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    private static String getBuildGraphStatus(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        String response = "";
        try {
            String startBuildProgressURL = anyDataGraphConfig.getStartBuildProgressURL();
            ArrayList<String> list = new ArrayList<>(1);
            list.add("\"task_status\"");
            HashMap<String, Object> paraMap = new HashMap<>();
            paraMap.put("graph_id", anyDataGraphConfig.getGraphId());
            paraMap.put("key", list);
            log.info("getBuildGraphStatus of URL:{};paraMap:{}", startBuildProgressURL, JsonUtils.toJsonString(paraMap));
            response = HttpRequestUtils.httpGet(startBuildProgressURL, anyDataGraphConfig.getHeadMap(), paraMap);
        } catch (Exception e) {
            log.error("get graph build status failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
        return response;
    }

    /***
     *  开始构建图谱
     */
    public static void startBuildGraph(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        try {
            startUpLoadBuild(anyDataGraphConfig);
            Integer graphId = anyDataGraphConfig.getGraphId();
            if (null == graphId) {
                log.error("build graph failed! graphId is null ! please why graphId is null !");
                throw new Exception();
            }
            String startBuildURL = anyDataGraphConfig.getStartBuildURL();
            startBuildURL = String.format(startBuildURL, graphId);
            StartBuildDto startBuildDto = new StartBuildDto();
            String para = JsonUtils.toJsonString(startBuildDto);
            // 发送请求:{"Cause":"graph not exist","Code":500021,"message":"graph not exist"}
            String response = HttpRequestUtils.sendHttpsPosToAdGraphBuild(startBuildURL, anyDataGraphConfig.getHeadMap(), para);
            //{"res":{"graph_task_id":1614}}
            JsonNode jsonNode = JsonUtils.toJsonNode(response);
            if (null != jsonNode && jsonNode.hasNonNull("res")) {
                int graphTaskId = jsonNode.get("res").get("graph_task_id").asInt();
                log.info("build graph start success! graphId={},graphTaskId={},now start monitor it progress!", graphId, graphTaskId);
                while (true) {
                    Thread.sleep(5000);
                    String buildGraphStatus = getBuildGraphStatus(anyDataGraphConfig);
                    //  { "res": {"id": 733,"name": "手搓血缘图谱-2_2", "task_status": "normal"}}
                    JsonNode buildGraphStatusJsonNode = JsonUtils.toJsonNode(buildGraphStatus);
                    if (buildGraphStatusJsonNode != null && buildGraphStatusJsonNode.hasNonNull("res")) {
                        String taskStatus = buildGraphStatusJsonNode.get("res").get("task_status").asText();
                        // edit：配置中,normal：正常,waiting：等待中,running：运行中,stop：终止,failed：失败
                        switch (taskStatus) {
                            case "normal":
                                log.info("build graph end success! ");
                                return;
                            case "failed":
                                log.error("build graph failed!");
                                throw new Exception();
                            case "stop":
                                log.error("build graph stop!");
                                throw new Exception();
                            case "edit":
                                log.warn("build graph is editing");
                                continue;
                            case "waiting":
                                log.warn("build graph is waiting");
                                continue;
                            case "running":
                                log.warn("build graph is running !!!");
                            default:
                                break;
                        }
                    } else {
                        log.error("build graph failed! error detail is  follow:{}", buildGraphStatus);
                        throw new Exception();
                    }
                }
            } else {
                log.error("build graph start failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("build up load source  failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    /***
     * 3,构建graph
     */
    private static void startUpLoadBuild(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        try {
            String knwId = String.valueOf(anyDataGraphConfig.getKnwId());
            Integer dsNewId = anyDataGraphConfig.getDsId();
            String dsOldId = anyDataGraphConfig.getDsOldId();
            // {"48":54}
            String idMapString = "{\"" + dsOldId + "\":" + dsNewId + "}";
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("ds_id_map", idMapString, ContentType.TEXT_PLAIN);
            builder.addTextBody("knw_id", knwId, ContentType.TEXT_PLAIN);
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(anyDataGraphConfig.getUpLoadGraphPath());
            Resource resource = resources[0];
            InputStream inputStream = resource.getInputStream();
            builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM, resource.getFilename());
            // {"graph_id":[733]}
            String response = HttpRequestUtils.sendHttpsPosFormDataToAdGraphBuild(anyDataGraphConfig.getUploadURL(), anyDataGraphConfig.getHeadMap(), builder);
            JsonNode jsonNode = JsonUtils.toJsonNode(response);
            if (jsonNode != null && jsonNode.hasNonNull("graph_id")) {
                int graphId = jsonNode.get("graph_id").get(0).asInt();
                // 将新的graph_id塞进config
                anyDataGraphConfig.setGraphId(graphId);
            } else {
                log.error("build up load source  failed ! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("build up load source  failed ! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    /***
     * 1,构建network
     */
    public static void buildNetWork(NetWorkBuildDto netWorkBuildDto, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        String para = JsonUtils.toJsonString(netWorkBuildDto);
        log.info("开始buildNetWork，para={}", para);
        try {
            String response = HttpRequestUtils.sendHttpsPosToAdGraphBuild(anyDataGraphConfig.getNetworkURL(), anyDataGraphConfig.getHeadMap(), para);
            // {"code": 200,"data": 44,"message": "success"}
            JsonNode jsonNode = JsonUtils.toJsonNode(response);
            if (jsonNode != null && jsonNode.hasNonNull("code") && null != jsonNode.get("code") && 200 == jsonNode.get("code").asInt()) {
                log.info("build network success ! netWorkBuildDto={}", netWorkBuildDto);
                int data = jsonNode.get("data").asInt();
                anyDataGraphConfig.setKnwId(data);
            } else {
                log.error("build network  failed! error detail is  follow:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("build network  failed! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    public static void startGraphService(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        try {
            GraphUtil.delAllServiceFromGraphId(anyDataGraphConfig.getGraphId(), anyDataGraphConfig);
            log.info("delete all service of graphId:{}", anyDataGraphConfig.getGraphId());
            String responseTab = startBuildGraphServiceInternal(anyDataGraphConfig);
            //  {"res": "9e67430ef75e4c108826e7b918517ae0"}
            JsonNode responseTabNode = JsonUtils.toJsonNode(responseTab);
            assert responseTabNode != null;
            if (responseTabNode.hasNonNull("res")) {
                log.info("build graph query service success! ");
            } else {
                log.error("build graph query service  failed! error detail is  follow:{}", responseTab);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("build graph query service  failed! error detail is  follow:{}", e.getMessage());
            throw new Exception(e);
        }
    }

    public static String startBuildGraphServiceInternal(AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        String response = "";
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(anyDataGraphConfig.getUpLoadGraphServicePath());
            Resource resource = resources[0];
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            String graphId = String.valueOf(anyDataGraphConfig.getGraphId());
            String knwId = String.valueOf(anyDataGraphConfig.getKnwId());
//        builder.addTextBody("name", serviceName);
            builder.addTextBody("kg_id", graphId);
            builder.addTextBody("knw_id", knwId);
            builder.addTextBody("publish", "true");
            builder.addBinaryBody("file", resource.getInputStream(), ContentType.APPLICATION_OCTET_STREAM, resource.getFilename());
            response = HttpRequestUtils.sendHttpsPosFormDataToAdGraphBuild(anyDataGraphConfig.getServiceBuildURL(), anyDataGraphConfig.getHeadMap(), builder);
        } catch (Exception e) {
            log.error("构建图谱service失败：{}", e.getMessage());
            throw new Exception(e);
        }
        return response;
    }
}
