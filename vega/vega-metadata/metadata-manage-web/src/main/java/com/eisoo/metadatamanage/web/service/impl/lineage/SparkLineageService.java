package com.eisoo.metadatamanage.web.service.impl.lineage;

import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.lib.dto.lineage.SparkSplineLineage;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SparkLineageService extends LineageKafkaService {

    public void consumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            String value = record.value();
            List<Lineage> lineages = parseLineage(value);
            sendToLineageResultTopic(lineages, Lineage.Type.SPARK);
        }
        // 同步提交
        ack.acknowledge();
    }


    private List<Lineage> parseLineage(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            SparkSplineLineage lineage = JSONUtils.json2Obj(value, SparkSplineLineage.class);
            if (lineage == null
                    || lineage.getAttributes() == null
                    || lineage.getOperations() == null
                    || lineage.getOperations().getReads() == null
                    || lineage.getOperations().getWrite() == null
                    || lineage.getOperations().getOther() == null) {
                log.error("血缘内容字符串转对象失败，json:{}", value);
                return null;
            }
            return createColmunLineage(lineage);
        } catch (Exception e) {
            log.error("spark 血缘日志：{}", value);
            log.error("", e);
        }

        return null;
    }

    private static List<Lineage> createColmunLineage(SparkSplineLineage lineage) {
        Map<String, SparkSplineLineage.Attribute> arrtIdAttributeMap = createAttrIdMap(lineage);
        Map<String, SparkSplineLineage.Operations.Other> opIdOtherMap = createOpIdOtherMap(lineage.getOperations());
        Map<String, SparkSplineLineage.Expressions.Function> exprIdFunctionMap = createExprIdFunctionMap(lineage);
        Map<String, SparkSplineLineage.Operations.Params.Table.Identifier> readAttrIdTableMap = createReadAttrIdTableMap(lineage);
        Map<String, DbType> readAttrIdDbTypeMap = createReadAttrIdDbTypeMap(lineage);
        /**
         * 实现思路
         * 1、从write开始查找，找到write的childIds。
         * 2、根据步骤1的childIds去other查找id相同的节点，记录output数组字段，该字段为血缘目标表的字段列表。
         * 3、根据步骤2中的output，逐个从attributes查找逐个从attributes查找节点。
         * 4、查看attributes节点是否存在childRefs，不存在childRefs节点，记录源字段为当前attrId。存在childRefs节点，见步骤五。
         * 5、存在childRefs节点，取__exprId去expressions的functions里面查询，查找到function，确认存在childRefs节点，确认存在属性__attrId或__exprId。
         * 6、如果是__attrId属性重复步骤4后面的操作，如果是__exprId属性重复步骤5，直至到步骤4结束为止
         */

        SparkSplineLineage.Operations.Write write = lineage.getOperations().getWrite();
        List<String> writeOutput = new ArrayList<>();
        for (String writeExprId : write.getChildIds()) {
            SparkSplineLineage.Operations.Other wirteOther = opIdOtherMap.get(writeExprId);
            writeOutput.addAll(wirteOther.getOutput());
        }

        List<Lineage> sinkList = new ArrayList<>();
        for (String targetId : writeOutput) {
            SparkSplineLineage.Attribute attribute = arrtIdAttributeMap.get(targetId);
            List<SparkSplineLineage.Attribute.ChildRef> childRefs = attribute.getChildRefs();
            String sourceArrtId = null;
            if (null == childRefs || childRefs.isEmpty()) {
                sourceArrtId = attribute.getId();
            } else {
                String __exprId = childRefs.get(0).get__exprId();
                String __attrId = null;
                SparkSplineLineage.Expressions.Function function = exprIdFunctionMap.get(__exprId);
                if (function != null && function.getChildRefs() != null && !function.getChildRefs().isEmpty()) {
                    __exprId = getExprId(function);
                    __attrId = getAttrId(function);
                    while (true) {
                        if (StringUtils.isBlank(__exprId) && StringUtils.isBlank(__attrId)) {
                            break;
                        } else {
                            if (!StringUtils.isBlank(__exprId)) {
                                function = exprIdFunctionMap.get(__exprId);
                                __exprId = getExprId(function);
                                __attrId = getAttrId(function);
                            } else {
                                sourceArrtId = __attrId;
                                SparkSplineLineage.Attribute tempAttribute = arrtIdAttributeMap.get(__attrId);
                                __attrId = null;
                                __exprId = getExprId(tempAttribute);
                            }
                        }
                    }
                }
            }
            if (sourceArrtId != null) {
                Lineage sink = new Lineage();
                sink.setCreateType(Lineage.Type.SPARK);
                Lineage.Vertex target = sink.getTarget();
                target.setDbName(write.getParams().getTable().getIdentifier().getDatabase());
                target.setTbName(write.getParams().getTable().getIdentifier().getTable());
                target.setColumn(attribute.getName());
                target.setDbType(DbType.of(write.getExtra().getDestinationType()));

                Lineage.Vertex source = sink.getSource();
                SparkSplineLineage.Operations.Params.Table.Identifier table = readAttrIdTableMap.get(sourceArrtId);
                if (table != null) {
                    source.setDbType(readAttrIdDbTypeMap.get(sourceArrtId));
                    source.setDbName(table.getDatabase());
                    source.setTbName(table.getTable());
                    source.setColumn(arrtIdAttributeMap.get(sourceArrtId).getName());
                    sinkList.add(sink);
                }
            }
        }

        return sinkList;
    }

    private static String getAttrId(SparkSplineLineage.Expressions.Function function) {
        if (function == null) {
            return null;
        }
        List<SparkSplineLineage.Expressions.Function.ChildRef> childRefs = function.getChildRefs();
        if (childRefs == null || childRefs.isEmpty()) {
            return null;
        }
        return childRefs.get(0).get__attrId();
    }

    private static String getExprId(SparkSplineLineage.Expressions.Function function) {
        if (function == null) {
            return null;
        }
        List<SparkSplineLineage.Expressions.Function.ChildRef> childRefs = function.getChildRefs();
        if (childRefs == null || childRefs.isEmpty()) {
            return null;
        }
        return childRefs.get(0).get__exprId();
    }

    private static String getExprId(SparkSplineLineage.Attribute attribute) {
        if (attribute == null) {
            return null;
        }
        List<SparkSplineLineage.Attribute.ChildRef> childRefs = attribute.getChildRefs();
        if (childRefs == null || childRefs.isEmpty()) {
            return null;
        }
        return childRefs.get(0).get__exprId();

    }

    private static Map<String, SparkSplineLineage.Operations.Params.Table.Identifier> createReadAttrIdTableMap(SparkSplineLineage lineage) {
        Map<String, SparkSplineLineage.Operations.Params.Table.Identifier> readAttrIdTableMap = new HashMap<>();
        List<SparkSplineLineage.Operations.Read> readList = lineage.getOperations().getReads();
        for (SparkSplineLineage.Operations.Read row : readList) {
            for (String arrtId : row.getOutput()) {
                readAttrIdTableMap.put(arrtId, row.getParams().getTable().getIdentifier());
            }
        }
        return readAttrIdTableMap;
    }

    private static Map<String, DbType> createReadAttrIdDbTypeMap(SparkSplineLineage lineage) {
        Map<String, DbType> readAttrIdDbMap = new HashMap<>();
        List<SparkSplineLineage.Operations.Read> readList = lineage.getOperations().getReads();
        for (SparkSplineLineage.Operations.Read row : readList) {
            for (String arrtId : row.getOutput()) {
                readAttrIdDbMap.put(arrtId, DbType.of(row.getExtra().getSourceType()));
            }
        }
        return readAttrIdDbMap;
    }

    private static Map<String, SparkSplineLineage.Expressions.Function> createExprIdFunctionMap(SparkSplineLineage lineage) {
        Map<String, SparkSplineLineage.Expressions.Function> exprIdFunctionMap = new HashMap<>();
        if (lineage.getExpressions() == null) {
            return exprIdFunctionMap;
        }
        if (lineage.getExpressions().getFunctions() == null || lineage.getExpressions().getFunctions().isEmpty()) {
            return exprIdFunctionMap;
        }

        for (SparkSplineLineage.Expressions.Function row : lineage.getExpressions().getFunctions()) {
            exprIdFunctionMap.put(row.getId(), row);
        }
        return exprIdFunctionMap;
    }

    private static Map<String, SparkSplineLineage.Attribute> createAttrIdMap(SparkSplineLineage lineage) {
        Map<String, SparkSplineLineage.Attribute> arrtIdMap = new HashMap<>();
        if (lineage.getAttributes() == null || lineage.getAttributes().isEmpty()) {
            return arrtIdMap;
        }
        for (SparkSplineLineage.Attribute row : lineage.getAttributes()) {
            arrtIdMap.put(row.getId(), row);
        }
        return arrtIdMap;
    }

    private static Map<String, SparkSplineLineage.Operations.Other> createOpIdOtherMap(SparkSplineLineage.Operations operations) {
        Map<String, SparkSplineLineage.Operations.Other> opIdOtherMap = new HashMap<>();
        List<SparkSplineLineage.Operations.Other> others = operations.getOther();
        if (others == null || others.isEmpty()) {
            return opIdOtherMap;
        }
        for (SparkSplineLineage.Operations.Other row : others) {
            opIdOtherMap.put(row.getId(), row);
        }
        return opIdOtherMap;
    }


}
