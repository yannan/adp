package com.eisoo.util;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.dto.AnyDataVidParaDto;
import com.eisoo.entity.DolphinLineageEntity;
import com.eisoo.lineage.presto.HandlerSelectItemUtil;
import com.eisoo.lineage.presto.LineageDolphinColumn;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/11 10:58
 * @Version:1.0
 */

@Slf4j
public class DolphinLineageUtil {

    /***
     * 解析加工类型的sql domain获取血缘domain
     * @param map
     * @return
     * @throws Exception
     */
//    public static ArrayList<DolphinLineageEntity.DolphinColumnLineage> getComposeColumnDomains(HashMap<String, DolphinLineageEntity.DolphinSqlDomain> map) throws Exception {
//        ArrayList<DolphinLineageEntity.DolphinColumnLineage> listResult = new ArrayList<>();
//        try {
//            Set<String> keysLineageDomain = map.keySet();
//            for (String key : keysLineageDomain) {
//                DolphinLineageEntity.DolphinSqlDomain dolphinSqlDomain = map.get(key);
//                String composeSQL = dolphinSqlDomain.getTargetTableInsert().replaceAll("\\\\", "");
//                String targetCatalog = dolphinSqlDomain.getTargetCatalog();
//                String targetSchema = dolphinSqlDomain.getTargetSchema();
//                String targetTableName = dolphinSqlDomain.getTargetTableName();
//                String targetTableCreateSQL = dolphinSqlDomain.getTargetTableCreate();
//                //1,获取目标table的column
//                List<String> fieldListTargetTable = HandlerSelectItemUtil.getFieldListFromCreteSQL(targetTableCreateSQL).get("column");
//                //2,获取insert后面的select的字段
//                List<String> selectItemList = HandlerSelectItemUtil.getSelectItemListFromSQL(composeSQL);
//                //3,根据insert sql 获取血缘
//                HashMap<String, ArrayList<LineageDolphinColumn>> columnLineageMap = com.eisoo.lineage.LineageUtil.getLineageBySql(composeSQL);
//
//                for (int i = 0; i < selectItemList.size(); i++) {
//                    // 存放依赖的列的uid
//                    StringBuilder columnUids = new StringBuilder();
//                    // 存放依赖的列的expression
//                    StringBuilder expressions = new StringBuilder();
//                    String columnNameSelect = selectItemList.get(i);
//                    String columnNameTarget = fieldListTargetTable.get(i);
//                    String uniqueIdTargetColumn = LineageUtil.makeMD5(targetCatalog, targetSchema, targetTableName, columnNameTarget);
//                    //获取每一个字段的血缘信息
//                    ArrayList<LineageDolphinColumn> lineageDolphinColumnsPerTargetItem = columnLineageMap.get(columnNameSelect);
//                    DolphinLineageEntity.DolphinColumnLineage dolphinColumnLineage = new DolphinLineageEntity.DolphinColumnLineage();
//                    HashSet<String> tabDepSet = dolphinColumnLineage.getTabDepSet();
//                    // 解析每一个依赖的列的uid和expression
//                    for (int j = 0; j < lineageDolphinColumnsPerTargetItem.size(); j++) {
//                        LineageDolphinColumn lineageDolphinColumnOne = lineageDolphinColumnsPerTargetItem.get(j);
//                        columnUids.append(getMD5FromLineageColumn(lineageDolphinColumnOne));
//                        expressions.append(lineageDolphinColumnOne.getExpression());
//
//                        String sourceCatAndDbName = lineageDolphinColumnOne.getSourceDbName();
//                        String sourceTableName = lineageDolphinColumnOne.getSourceTableName();
//                        // 将依赖的表的cat schema table 放入tabDepSet，后面用于请求元数据信息放入图谱中
//                        tabDepSet.add(sourceCatAndDbName + "." + sourceTableName);
//                        if (j != lineageDolphinColumnsPerTargetItem.size() - 1) {
//                            columnUids.append(",");
//                            expressions.append(",");
//                        }
//                    }
//                    dolphinColumnLineage.setUniqueId(uniqueIdTargetColumn);
//                    dolphinColumnLineage.setExpressionName(expressions.toString().trim());
//                    dolphinColumnLineage.setColumnUniqueIds(columnUids.toString().trim());
//                    listResult.add(dolphinColumnLineage);
//                }
//            }
//        } catch (Exception e) {
//            log.error("获取compose的dolphin血缘对象失败:map={}", JsonUtils.toJsonString(map));
//            throw new Exception(e);
//        }
//        return listResult;
//    }

    public static String getMD5FromLineageColumn(LineageDolphinColumn lineageDolphinColumnOne) {
        String[] sourceCatAndDbName = lineageDolphinColumnOne.getSourceDbName().split("\\.");
        return LineageUtil.makeMD5(sourceCatAndDbName[0], sourceCatAndDbName[1], lineageDolphinColumnOne.getSourceTableName(), lineageDolphinColumnOne.getTargetColumnName());
    }
    public static String getMD5FromLineageTable(LineageDolphinColumn lineageDolphinColumnOne) {
        String[] sourceCatAndDbName = lineageDolphinColumnOne.getSourceDbName().split("\\.");
        return LineageUtil.makeMD5(sourceCatAndDbName[0], sourceCatAndDbName[1], lineageDolphinColumnOne.getSourceTableName());
    }
    /***
     * 解析同步类型的sql domain获取血缘domain
     * @param map
     * @return
     */
//    public static ArrayList<DolphinLineageEntity.DolphinColumnLineage> getSyncColumnDomains(HashMap<String, DolphinLineageEntity.DolphinSqlDomain> map) {
//        Set<String> keys = map.keySet();
//        ArrayList<DolphinLineageEntity.DolphinColumnLineage> dolphinColumnLineages = new ArrayList<>();
//        for (String key : keys) {
//
//            DolphinLineageEntity.DolphinSqlDomain dolphinSqlDomain = map.get(key);
//            String sourceCatalog = dolphinSqlDomain.getSourceCatalog();
//            String sourceSchema = dolphinSqlDomain.getSourceSchema();
//            String sourceTableName = dolphinSqlDomain.getSourceTableName();
//            String uniqueIdSource = sourceCatalog + sourceSchema + sourceTableName;
//            String targetCatalog = dolphinSqlDomain.getTargetCatalog();
//            String targetSchema = dolphinSqlDomain.getTargetSchema();
//            String targetTableName = dolphinSqlDomain.getTargetTableName();
//            String targetIdSource = targetCatalog + targetSchema + targetTableName;
//
//            List<DolphinLineageEntity.DolphinSQLFieldDomain> targetFields = dolphinSqlDomain.getTargetFields();
//            List<DolphinLineageEntity.DolphinSQLFieldDomain> sourceFields = dolphinSqlDomain.getSourceFields();
//
//            for (int i = 0; i < targetFields.size(); i++) {
//                DolphinLineageEntity.DolphinColumnLineage insertDomain = new DolphinLineageEntity.DolphinColumnLineage();
//                String fieldNameTarget = targetFields.get(i).getFieldName();
//                String fieldNameSource = sourceFields.get(i).getFieldName();
//                insertDomain.setUniqueId(LineageUtil.makeMD5(targetIdSource + fieldNameTarget));
//                insertDomain.setColumnUniqueIds(LineageUtil.makeMD5(uniqueIdSource + fieldNameSource));
//                insertDomain.setExpressionName(Constant.SYNC);
//                insertDomain.getTabDepSet().add(sourceCatalog + "." + sourceSchema + "." + sourceTableName);
//                dolphinColumnLineages.add(insertDomain);
//            }
//        }
//        return dolphinColumnLineages;
//    }

    /***
     * 检测column的实体是否存在
     * @param vids
     * @param nodesInfoURL
     * @param graphId
     * @param headMap
     * @return
     * @throws Exception
     */
    public static HashMap<String, Boolean> checkColumnIsExists(ArrayList<String> vids,
                                                               String nodesInfoURL,
                                                               Integer graphId,
                                                               Map<String, Object> headMap) throws Exception {
        HashMap<String, Boolean> mapAll = new HashMap<>(vids.size());
        AnyDataVidParaDto anyDataVidParaDto = new AnyDataVidParaDto(vids);
        String jsonString = JsonUtils.toJsonString(anyDataVidParaDto);
        log.debug("checkColumnIsExists方法检测column的参数如下:{}", jsonString);
        nodesInfoURL = String.format(nodesInfoURL, graphId);
        String response = HttpRequestUtils.sendHttpsPostJsonColumnInfo(nodesInfoURL, headMap, jsonString);
        if (LineageUtil.isNotEmpty(response)) {
            JsonNode jsonNode = JsonUtils.toJsonNode(response);
            // {"res":{"nodes":[],"edges":[]}}
            if (null == jsonNode || !jsonNode.hasNonNull("res")) {
                log.error("checkColumnIsExists方法检测column是否存在失败，response={}", response);
                throw new Exception();
            }
            JsonNode nodes = jsonNode.get("res").get("nodes");
            if (null == nodes) {
                log.error("checkColumnIsExists方法检测column是否存在失败，response={}", response);
                throw new Exception();
            }
            HashSet<String> set = new HashSet<>();
            for (int i = 0; i < nodes.size(); i++) {
                JsonNode childNode = nodes.get(i);
                if (childNode.hasNonNull("id")) {
                    set.add(childNode.get("id").asText());
                }
            }
            for (String vid : vids) {
                if (set.contains(vid)) {
                    mapAll.put(vid, true);
                } else {
                    mapAll.put(vid, false);
                }
            }
        } else {
            log.error("checkColumnIsExists failed!,details is follow:{}", response);
            throw new Exception();
        }
        return mapAll;
    }


    /***
     * upsert图谱的column
     * @param upsertList
     * @param graphId
     * @param url
     * @param headMap
     * @throws Exception
     */
    public static boolean upsertDolphinColumnList(ArrayList<DolphinLineageEntity.DolphinColumnLineage> upsertList,
                                                  Integer graphId,
                                                  String url,
                                                  Map<String, Object> headMap) throws Exception {
        AnyDataBuilderParaDto anyDataParaDto = new AnyDataBuilderParaDto(graphId, Constant.LINEAGE_COLUMN);
        anyDataParaDto.setAction(Constant.UPSERT);
        anyDataParaDto.setGraphData(upsertList);
        try {
            String para = JsonUtils.toJsonString(anyDataParaDto);
            log.info("开始upsert来自dolphin的column:分成二步,para={}", para);
            String response = HttpRequestUtils.sendHttpsPostJson(url, headMap, para);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第一步：upsert dolphin sql 的column成功！现在开始构建column->column的edge......");
            } else {
                log.error("upsert dolphin sql 的column失败! 原因如下:{}", response);
                return false;
            }
            response = buildColumn2ColumnEdge(upsertList, graphId, url, headMap);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第二步：构建column->column的edge成功!完成upsert来自dolphin的column!");
                return true;
            } else {
                log.error("构建column->column的edge 失败! 原因如下:{}", response);
                return false;
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    /***
     * 构建column与column的边的关系
     * @param lineageDomainList
     * @param graphId
     * @param url
     * @param headMap
     * @return
     * @throws Exception
     */
    private static String buildColumn2ColumnEdge(ArrayList<DolphinLineageEntity.DolphinColumnLineage> lineageDomainList,
                                                 Integer graphId,
                                                 String url,
                                                 Map<String, Object> headMap) throws Exception {
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataList = new ArrayList<>(lineageDomainList.size());
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(graphId,
                                                                                Constant.COLUMN_2_COLUMN,
                                                                                Constant.UPSERT,
                                                                                Constant.EDGE);
        for (DolphinLineageEntity.DolphinColumnLineage domain : lineageDomainList) {
            graphDataList.addAll(getEdgeParaHashMap(domain));
        }
        anyDataBuilderParaDto.setGraphData(graphDataList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.info("开始构建dolphin的column->column的edge,para={}", jsonString);
        return HttpRequestUtils.sendHttpsPostJson(url, headMap, jsonString);
    }

    /***
     * 转换构edge所需要的参数
     * @param dolphinColumn
     * @return
     */
    private static ArrayList<HashMap<String, HashMap<String, String>>> getEdgeParaHashMap(DolphinLineageEntity.DolphinColumnLineage dolphinColumn) {
        ArrayList<HashMap<String, HashMap<String, String>>> listResult = new ArrayList<>();
        String uniqueId = dolphinColumn.getUniqueId();
        String[] columnUniqueIds = dolphinColumn.getColumnUniqueIds().split(",");
        HashMap<String, String> start = new HashMap<>(2);
        start.put("unique_id", uniqueId);
        start.put("_start_entity", Constant.LINEAGE_COLUMN);
        for (String columnUniqueId : columnUniqueIds) {
            HashMap<String, HashMap<String, String>> paraMap = new HashMap<>(2);
            paraMap.put("start", start);
            HashMap<String, String> end = new HashMap<>(2);
            end.put("_end_entity", Constant.LINEAGE_COLUMN);
            end.put("unique_id", columnUniqueId);
            paraMap.put("end", end);
            paraMap.put("edge_pros", new HashMap<>());
            listResult.add(paraMap);
        }
        return listResult;
    }


    /***
     * update图谱的column
     * @param updateList
     * @param graphId
     * @param url
     * @param headMap
     * @param urlDelete
     * @return 是否执行成功
     * @throws Exception
     */
    public static boolean updateDolphinColumnList(ArrayList<DolphinLineageEntity.DolphinColumnLineage> updateList,
                                                  Integer graphId,
                                                  String url,
                                                  Map<String, Object> headMap, String urlDelete) throws Exception {
        AnyDataBuilderParaDto anyDataParaDto = new AnyDataBuilderParaDto(graphId, Constant.LINEAGE_COLUMN);
        String response = "";
        log.info("开始update来源dolphin的column:分成三步：删除column的edge;更新column实体；构建column->column的edge");
        try {
            //1,首先删除之前的column->column的关系
            for (DolphinLineageEntity.DolphinColumnLineage lineage : updateList) {
                String responseDelEdge = batchDeleteEdgeColumn2Column(lineage.getUniqueId(),
                                                                      graphId,
                                                                      urlDelete,
                                                                      headMap);
                if (Constant.AD_RESPONSE_SUCCESS.equals(responseDelEdge)) {
                    log.info("第一步：删除column->column的edge成功！:lineage={}", lineage);
                } else {
                    log.error("第一步：删除column->column的edge失败！原因如下：{}", response);
                    throw new Exception();
                }
            }
            //2,更新dolphin的实体
            anyDataParaDto.setAction(Constant.UPDATE);
            anyDataParaDto.setGraphData(updateList);
            String paraJson = JsonUtils.toJsonString(anyDataParaDto);
            log.info("第二步：开始update column血缘实体:paraJson={}", paraJson);
            response = HttpRequestUtils.sendHttpsPostJson(url, headMap, paraJson);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第二步：update字段实体成功！开始构建字段与字段的edge");
            } else {
                log.info("第二步：update字段实体失败！details={}", response);
                throw new Exception();
            }
            //3,构建column->column的关系
            response = buildColumn2ColumnEdge(updateList, graphId, url, headMap);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第三步：构建column与column的edge成功!完成update字段！");
                return true;
            } else {
                log.error("第三步：构建column与column的edge失败！原因如下:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("update来源dolphin的column失败！updateList={}", JsonUtils.toJsonString(updateList));
            throw new Exception(e);
        }
    }

    /***
     * 批量删除column与column的关系
     */
    private static String batchDeleteEdgeColumn2Column(String uniqueId,
                                                       Integer graphId,
                                                       String urlDelete,
                                                       Map<String, Object> headMap) throws Exception {
        HashMap<String, Object> map = new HashMap<>();
        map.put("graph_id", graphId);
        HashMap<String, String> m1 = new HashMap<>();
        m1.put("name", Constant.COLUMN_2_COLUMN);
        m1.put("direction", "both");
        ArrayList<HashMap<String, String>> relationName = new ArrayList<>(2);
        relationName.add(m1);
        map.put("relation_name", relationName);
        HashMap<String, String> vertexData = new HashMap<>();
        vertexData.put("unique_id", uniqueId);
        map.put("vertex_data", vertexData);
        map.put("vertex_name", Constant.LINEAGE_COLUMN);
        String jsonString = JsonUtils.toJsonString(map);
        return HttpRequestUtils.sendHttpsPostJson(urlDelete, headMap, jsonString);
    }


    public static HashMap<String, Boolean> getTableNotExistsSet(HashSet<String> tabDepSet,
                                                                String nodeInfoCommonURL,
                                                                Integer graphId,
                                                                Map<String, Object> headMap) throws Exception {
        HashMap<String, Boolean> result = new HashMap<>();
        for (String tabDep : tabDepSet) {
            String[] split = tabDep.split("\\.");
            if (split.length != 3) {
                log.error("数据加工的sql血缘解析出来的catalog&schema&table不全，请检查！tableInfo={}", tabDep);
                throw new Exception();
            }
            String tableUniqueId = LineageUtil.makeMD5(split[0], split[1], split[2]);
            nodeInfoCommonURL = String.format(nodeInfoCommonURL, graphId);
            String queryTableInfo = String.format(Constant.QUERY_TABLE_INFO, tableUniqueId, graphId);
            String response = HttpRequestUtils.sendHttpsPostJsonColumnInfo(nodeInfoCommonURL, headMap, queryTableInfo);
            if (LineageUtil.isNotEmpty(response)) {
                JsonNode jsonNode = JsonUtils.toJsonNode(response);
                assert jsonNode != null;
                if (!jsonNode.hasNonNull("res") || null == jsonNode.get("res")) {
                    //{"res":{"nodes":[],"edges":[]}}
                    log.error("检测图谱table是否存在失败，queryTableInfo={}，response={}", queryTableInfo, response);
                    throw new Exception();
                }
                JsonNode nodes = jsonNode.get("res").get("nodes");
                if (nodes == null || nodes.isEmpty()) {
                    result.put(tabDep, false);
                } else {
                    result.put(tabDep, true);
                }
            } else {
                log.error("检测图谱table是否存在失败，response是空，请检查！queryTableInfo={}", queryTableInfo);
                throw new Exception();
            }
        }
        return result;
    }

    /***
     * 获取table到column构建edge所需要的参数
     * @param column
     * @return
     */
    public static HashMap<String, HashMap<String, String>> getEdgeParaCol2TabHashMap(DolphinLineageEntity.DolphinColumnLineageSuper column) {
        HashMap<String, HashMap<String, String>> paraMap = new HashMap<>(2);
        String tableUniqueId = column.getTableUniqueId();
        String uniqueId = column.getUniqueId();
        HashMap<String, String> start = new HashMap<>(2);
        start.put("unique_id", tableUniqueId);
        start.put("_start_entity", Constant.LINEAGE_TABLE);
        HashMap<String, String> end = new HashMap<>(2);
        end.put("unique_id", uniqueId);
        end.put("_end_entity", Constant.LINEAGE_COLUMN);
        paraMap.put("start", start);
        paraMap.put("end", end);
        // 必须添加
        paraMap.put("edge_pros", new HashMap<>());
        return paraMap;
    }

    /***
     * 构建column到其所属table的edge
     * @param upsertList
     * @param graphId
     * @param url
     * @param headMap
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String buildColumn2TableEdge(ArrayList<DolphinLineageEntity.DolphinColumnLineageSuper> upsertList,
                                               Integer graphId,
                                               String url,
                                               Map<String, Object> headMap) throws Exception {
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataList = new ArrayList<>(upsertList.size());
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(graphId, Constant.TABLE_2_COLUMN, Constant.UPSERT, Constant.EDGE);
        for (DolphinLineageEntity.DolphinColumnLineageSuper domain : upsertList) {
            graphDataList.add(DolphinLineageUtil.getEdgeParaCol2TabHashMap(domain));
        }
        anyDataBuilderParaDto.setGraphData(graphDataList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        return HttpRequestUtils.sendHttpsPostJson(url, headMap, jsonString);
    }


    /***
     * upsert来自dolphin的字段（元数据）:构建column->table的关系
     * @param upsertList
     * @param graphId
     * @param url
     * @param headMap
     * @return
     */
    public static boolean upsertColumnSuperList(ArrayList<DolphinLineageEntity.DolphinColumnLineageSuper> upsertList,
                                                Integer graphId,
                                                String url,
                                                Map<String, Object> headMap) {
        AnyDataBuilderParaDto anyDataParaDto = new AnyDataBuilderParaDto(graphId, Constant.LINEAGE_COLUMN);
        anyDataParaDto.setAction(Constant.UPSERT);
        anyDataParaDto.setGraphData(upsertList);
        String para = JsonUtils.toJsonString(anyDataParaDto);
        try {
            log.info("upsert的dolphin血缘paraJson={}", para);
            String response = HttpRequestUtils.sendHttpsPostJson(url, headMap, para);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第一步：upsert dolphin sql 成功！现在开始column与table的edge......");
            } else {
                log.error("第一步：upsert dolphin sql 失败！原因如下:{}", response);
                throw new Exception();
            }
            response = DolphinLineageUtil.buildColumn2TableEdge(upsertList, graphId, url, headMap);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第二步：构建column与table的edge成功!完成upsert字段(Super)!");
                return true;
            } else {
                log.error("第二步:构建字段与表的edge 失败! 原因如下:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
