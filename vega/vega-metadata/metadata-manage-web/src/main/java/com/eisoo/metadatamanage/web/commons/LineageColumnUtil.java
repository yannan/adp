package com.eisoo.metadatamanage.web.commons;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.util.Constant;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/10 14:57
 * @Version:1.0
 */
@Slf4j
public class LineageColumnUtil {
    public static String sendUpsertColumnListToAd(List<BaseLineageEntity> lineageDomainList, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(),
                                                                                Constant.LINEAGE_COLUMN,
                                                                                Constant.UPSERT, lineageDomainList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.debug("sendUpsertColumnListToAd的参数：{}",jsonString);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(),
                                                  anyDataGraphConfig.getHeadMap(),
                                                  jsonString
        );
    }

    public static String buildTable2ColumnEdge(List<BaseLineageEntity> lineageDomainList,AnyDataGraphConfig anyDataGraphConfig) throws UnsupportedEncodingException {
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataList = new ArrayList<>(lineageDomainList.size());
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(), Constant.TABLE_2_COLUMN, Constant.UPSERT, Constant.EDGE);
        for (BaseLineageEntity domain : lineageDomainList) {
            graphDataList.add(getEdgeParaHashMap((ColumnLineageEntity) domain));
        }
        anyDataBuilderParaDto.setGraphData(graphDataList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
    }
    private static HashMap<String, HashMap<String, String>> getEdgeParaHashMap(ColumnLineageEntity column) {
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
     * 构建边与边的关系
     * @param lineageDomainList
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String buildColumn2ColumnEdge(List<BaseLineageEntity> lineageDomainList,AnyDataGraphConfig anyDataGraphConfig) throws UnsupportedEncodingException {
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataList = new ArrayList<>(lineageDomainList.size());
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(),
                                                                                Constant.COLUMN_2_COLUMN,
                                                                                Constant.UPSERT,
                                                                                Constant.EDGE);

        for (BaseLineageEntity domain : lineageDomainList) {
            graphDataList.addAll(getEdgeCol2ColParaHashMap((ColumnLineageEntity) domain));
        }
        anyDataBuilderParaDto.setGraphData(graphDataList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
    }
    private static ArrayList<HashMap<String, HashMap<String, String>>> getEdgeCol2ColParaHashMap(ColumnLineageEntity columnLineageEntity) {
        ArrayList<HashMap<String, HashMap<String, String>>> listResult = new ArrayList<>();
        String uniqueId = columnLineageEntity.getUniqueId();
        String[] columnUniqueIds = columnLineageEntity.getColumnUniqueIds().split(",");
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
}
