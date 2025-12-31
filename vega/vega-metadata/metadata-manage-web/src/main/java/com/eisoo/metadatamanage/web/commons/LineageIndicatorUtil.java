package com.eisoo.metadatamanage.web.commons;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.IndicatorLineageEntity;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.util.Constant;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import com.eisoo.util.LineageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/10 15:22
 * @Version:1.0
 */
public class LineageIndicatorUtil {
    public static String sendUpsertIndicatorToAD(List<BaseLineageEntity> lineageDomainList, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(), Constant.LINEAGE_INDICATOR, Constant.UPSERT, lineageDomainList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
    }

    public static String buildIndicator2ColumnAndSelfEdge(List<BaseLineageEntity> lineageDomainList, AnyDataGraphConfig anyDataGraphConfig) throws Exception {
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataListColumn = new ArrayList<>(lineageDomainList.size());
        ArrayList<HashMap<String, HashMap<String, String>>> graphDataListIndicator = new ArrayList<>(lineageDomainList.size());
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(), Constant.INDICATOR_2_COLUMN, Constant.UPSERT, Constant.EDGE);
        for (BaseLineageEntity domain : lineageDomainList) {
            IndicatorLineageEntity indicator = (IndicatorLineageEntity) domain;
            String columnUuids = indicator.getColumnUniqueIds();
            if (LineageUtil.isNotEmpty(columnUuids)) {
                String[] split = columnUuids.split(",");
                for (String columnUniqueId : split) {
                    HashMap<String, HashMap<String, String>> paraMap = new HashMap<>(2);
                    HashMap<String, String> start = new HashMap<>(2);
                    start.put("uuid", indicator.getUuid());
                    start.put("_start_entity", Constant.LINEAGE_INDICATOR);
                    HashMap<String, String> end = new HashMap<>(2);
                    end.put("unique_id", columnUniqueId);
                    end.put("_end_entity", Constant.LINEAGE_COLUMN);
                    paraMap.put("start", start);
                    paraMap.put("end", end);
                    paraMap.put("edge_pros", new HashMap<>());
                    graphDataListColumn.add(paraMap);
                }
            }
            String indicatorUuids = indicator.getIndicatorUuids();
            if (LineageUtil.isNotEmpty(indicatorUuids)) {
                String[] split = indicatorUuids.split(",");
                for (String indicatorEndUuid : split) {
                    HashMap<String, HashMap<String, String>> paraMap = new HashMap<>(2);
                    HashMap<String, String> start = new HashMap<>(2);
                    start.put("uuid", indicator.getUuid());
                    start.put("_start_entity", Constant.LINEAGE_INDICATOR);
                    HashMap<String, String> end = new HashMap<>(2);
                    end.put("uuid", indicatorEndUuid);
                    end.put("_end_entity", Constant.LINEAGE_INDICATOR);
                    paraMap.put("start", start);
                    paraMap.put("end", end);
                    paraMap.put("edge_pros", new HashMap<>());
                    graphDataListIndicator.add(paraMap);
                }
            }
        }
        String response = Constant.AD_RESPONSE_SUCCESS;
        if (!graphDataListColumn.isEmpty()) {
            anyDataBuilderParaDto.setGraphData(graphDataListColumn);
            String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
            response = HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
        }
        if (Constant.AD_RESPONSE_SUCCESS.equals(response) && !graphDataListIndicator.isEmpty()) {
            anyDataBuilderParaDto.setGraphData(graphDataListIndicator);
            anyDataBuilderParaDto.setName(Constant.INDICATOR_2_INDICATOR);
            String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
            response = HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
        }
        return response;
    }
}
