package com.eisoo.metadatamanage.web.service.impl.lineage;

import com.eisoo.metadatamanage.lib.dto.lineage.HiveLogLineage;
import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class HiveLineageService extends LineageKafkaService {

    public void consumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            String value = record.value();
            List<Lineage> lineages = parseLineage(value);
            sendToLineageResultTopic(lineages, Lineage.Type.HIVE);
        }
        // 同步提交
        ack.acknowledge();
    }

    private List<Lineage> parseLineage(String value) {
        try {
            String jsonStr = value.substring(value.indexOf("{"));
            HiveLogLineage hiveLogLineage = JSONUtils.json2Obj(jsonStr, HiveLogLineage.class);
            if (hiveLogLineage == null) {
                log.error("血缘内容字符串转对象失败，json:{}", value);
                return null;
            }
            if (StringUtils.isBlank(hiveLogLineage.getQueryText())) {
                log.error("血缘内容字符串转对象后查询语句为空，json:{}", value);
                return null;
            }
            return createColmunLineage(hiveLogLineage);
        } catch (Exception e) {
            log.error("Hive 血缘日志：{}", value);
            log.error("", e);
        }
        return null;

    }

    private static List<Lineage> createColmunLineage(HiveLogLineage hiveLogLineage) {
        Map<Integer, HiveLogLineage.Vertex> idMap = new HashMap<>();
        for (HiveLogLineage.Vertex row : hiveLogLineage.getVertices()) {
            idMap.put(row.getId(), row);
        }

        List<Lineage> sinkList = new ArrayList<>();
        for (HiveLogLineage.Edge row : hiveLogLineage.getEdges()) {
            for (Integer sourcesId : row.getSources()) {
                Lineage.Vertex srcVertex = createVertex(idMap.get(sourcesId));
                if (srcVertex == null) {
                    continue;
                }
                for (Integer targetId : row.getTargets()) {
                    Lineage sink = new Lineage();
                    Lineage.Vertex targetVertex = createVertex(idMap.get(targetId));
                    if (targetVertex == null || srcVertex.equals(targetVertex)) {
                        continue;
                    }

                    sink.setSource(srcVertex);
                    sink.setTarget(targetVertex);
                    sink.setCreateTime(getSecTimestamp(hiveLogLineage.getTimestamp()));
                    sink.setCreateType(Lineage.Type.HIVE);
                    sink.setQueryText(hiveLogLineage.getQueryText());
                    sinkList.add(sink);
                }
            }
        }
        return sinkList;
    }

    private static Long getSecTimestamp(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        // 秒级时间戳
        if (timestamp.toString().length() == 10) {
            return timestamp * 1000;
        }
        return timestamp;
    }

    private static Lineage.Vertex createVertex(HiveLogLineage.Vertex vertex) {
        if (vertex == null || vertex.getVertexId() == null) {
            return null;
        }
        Lineage.Vertex result = new Lineage.Vertex();
        result.setDbType(DbType.HIVE);
        String[] strArray = vertex.getVertexId().split("\\.");
        // 三个参数：database.table.culumn,小于三说明没有库，一般是select语句。
        if (strArray.length < 3) {
            return null;
        }

        if (strArray.length == 3) {
            result.setDbName(strArray[0]);
        } else if (strArray.length == 4) {
            result.setDbName(strArray[1]);
        }
        result.setTbName(strArray[strArray.length - 2]);
        result.setColumn(strArray[strArray.length - 1]);
        return result;
    }


}
