package com.eisoo.metadatamanage.web.service.impl.lineage;

import com.eisoo.metadatamanage.db.entity.*;
import com.eisoo.metadatamanage.db.mapper.*;
import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.standardization.common.util.AiShuUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * 保存kafka中的血缘的数据库
 */
@Slf4j
@Service
public class Lineage2MysqlService {


    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    LineageTagTableMapper lineageTagTableMapper;

    @Autowired
    LineageTagColumnMapper lineageTagColumnMapper;

    @Autowired
    LineageEdgeColumnTableRelationMapper lineageEdgeColumnTableRelationMapper;

    @Autowired
    LineageEdgeTableMapper lineageEdgeTableMapper;

    @Autowired
    LineageEdgeColumnMapper lineageEdgeColumnMapper;

    public void consumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        if (records.count() == 0) {
            return;
        }
        List<Lineage> lineageList = new ArrayList<>(records.count());
        for (ConsumerRecord<String, String> record : records) {
            String value = record.value();
            List<Lineage> tempList = JSONUtils.json2List(value, Lineage.class);
            if (tempList != null) {
                lineageList.addAll(tempList);
            }
        }
        if (saveData(lineageList)) {
            // 手动提交
            ack.acknowledge();
        }

    }

    private Boolean saveData(List<Lineage> lineageList) {

        Set<LineageTagTableEntity> tagTbList = new HashSet<>();
        Set<LineageTagColumnEntity> tagColList = new HashSet<>();

        Set<LineageEdgeColumnTableRelationEntity> edgeTableColumnRelationList = new HashSet<>();
        Set<LineageEdgeTableEntity> edgeTbList = new HashSet<>();
        Set<LineageEdgeColumnEntity> edgeColList = new HashSet<>();

        for (Lineage row : lineageList) {
            LineageTagTableEntity srcTb = createLineageTagTableEntity(row.getSource());
            LineageTagColumnEntity srcCol = createLineageTagColumnEntity(srcTb.getId(), row.getSource());

            LineageTagTableEntity dstTb = createLineageTagTableEntity(row.getTarget());
            LineageTagColumnEntity dstCol = createLineageTagColumnEntity(dstTb.getId(), row.getTarget());

            tagTbList.add(srcTb);
            tagTbList.add(dstTb);

            LineageEdgeTableEntity edgeTable = createLineageEdgeTableEntity(row, srcTb, dstTb);
            edgeTbList.add(edgeTable);

            if (null != srcCol && dstCol != null) {
                tagColList.add(srcCol);
                tagColList.add(dstCol);

                LineageEdgeColumnTableRelationEntity srcTableColumnRelation = createLineageEdgeColumnTableRelationEntity(srcTb.getId(), srcCol.getId());
                LineageEdgeColumnTableRelationEntity dstTableColumnRelation = createLineageEdgeColumnTableRelationEntity(dstTb.getId(), dstCol.getId());
                edgeTableColumnRelationList.add(srcTableColumnRelation);
                edgeTableColumnRelationList.add(dstTableColumnRelation);

                LineageEdgeColumnEntity edgeColumn = createLineageEdgeColumnEntity(row, srcCol, dstCol);
                edgeColList.add(edgeColumn);
            }
        }

        Boolean rlt = saveData(tagTbList, tagColList, edgeTableColumnRelationList, edgeTbList, edgeColList);
        return rlt;
    }

    private Boolean saveData(Set<LineageTagTableEntity> tagTbList, Set<LineageTagColumnEntity> tagColList, Set<LineageEdgeColumnTableRelationEntity> edgeTableColumnRelationList, Set<LineageEdgeTableEntity> edgeTbList, Set<LineageEdgeColumnEntity> edgeColList) {
        // 开启事务
        Boolean rlt = transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    if (!tagTbList.isEmpty()) {
                        saveTableTag(tagTbList);
                        saveTableEdge(edgeTbList);
                    }
                    if (!tagColList.isEmpty()) {
                        saveColumnTag(tagColList);
                        saveColumnEdge(edgeColList);
                        saveColumnTableRelationEdge(edgeTableColumnRelationList);
                    }
                    return true;
                } catch (Exception e) {
                    // 事务回滚
                    status.setRollbackOnly();
                    log.error("", e);
                    return false;
                }
            }

            private void saveColumnTableRelationEdge(Set<LineageEdgeColumnTableRelationEntity> dataList) {
                List<String> ids = new ArrayList<>();
                for (LineageEdgeColumnTableRelationEntity row : dataList) {
                    ids.add(row.getId());
                }
                List<LineageEdgeColumnTableRelationEntity> tagExists = lineageEdgeColumnTableRelationMapper.selectBatchIds(ids);
                Set<String> existsIdSet = new HashSet<>();
                for (LineageEdgeColumnTableRelationEntity row : tagExists) {
                    existsIdSet.add(row.getId());
                }

                List<LineageEdgeColumnTableRelationEntity> insertList = new ArrayList<>();
                for (LineageEdgeColumnTableRelationEntity row : dataList) {
                    if (existsIdSet.contains(row.getId())) {
                        lineageEdgeColumnTableRelationMapper.updateById(row);
                    } else {
                        insertList.add(row);
                    }
                }
                if (!insertList.isEmpty()) {
                    lineageEdgeColumnTableRelationMapper.batchSave(insertList);
                }
            }

            private void saveColumnEdge(Set<LineageEdgeColumnEntity> dataList) {
                List<String> ids = new ArrayList<>();
                for (LineageEdgeColumnEntity row : dataList) {
                    ids.add(row.getId());
                }
                List<LineageEdgeColumnEntity> tagExists = lineageEdgeColumnMapper.selectBatchIds(ids);
                Set<String> existsIdSet = new HashSet<>();
                for (LineageEdgeColumnEntity row : tagExists) {
                    existsIdSet.add(row.getId());
                }

                List<LineageEdgeColumnEntity> insertList = new ArrayList<>();
                for (LineageEdgeColumnEntity row : dataList) {
                    if (existsIdSet.contains(row.getId())) {
                        row.setCreateTime(null);
                        lineageEdgeColumnMapper.updateById(row);
                    } else {
                        insertList.add(row);
                    }
                }
                if (!insertList.isEmpty()) {
                    lineageEdgeColumnMapper.batchSave(insertList);
                }
            }

            private void saveColumnTag(Set<LineageTagColumnEntity> tagColList) {
                List<String> ids = new ArrayList<>();
                for (LineageTagColumnEntity row : tagColList) {
                    ids.add(row.getId());
                }
                List<LineageTagColumnEntity> tagExists = lineageTagColumnMapper.selectBatchIds(ids);
                Set<String> existsIdSet = new HashSet<>();
                for (LineageTagColumnEntity row : tagExists) {
                    existsIdSet.add(row.getId());
                }

                List<LineageTagColumnEntity> insertList = new ArrayList<>();
                for (LineageTagColumnEntity row : tagColList) {
                    if (existsIdSet.contains(row.getId())) {
                        lineageTagColumnMapper.updateById(row);
                    } else {
                        insertList.add(row);
                    }
                }
                if (!insertList.isEmpty()) {
                    lineageTagColumnMapper.batchSave(insertList);
                }
            }

            private void saveTableEdge(Set<LineageEdgeTableEntity> edgeTbList) {
                List<String> ids = new ArrayList<>();
                for (LineageEdgeTableEntity row : edgeTbList) {
                    ids.add(row.getId());
                }
                List<LineageEdgeTableEntity> tagExists = lineageEdgeTableMapper.selectBatchIds(ids);
                Set<String> existsIdSet = new HashSet<>();
                for (LineageEdgeTableEntity row : tagExists) {
                    existsIdSet.add(row.getId());
                }

                List<LineageEdgeTableEntity> insertList = new ArrayList<>();
                for (LineageEdgeTableEntity row : edgeTbList) {
                    if (existsIdSet.contains(row.getId())) {
                        row.setCreateTime(null);
                        lineageEdgeTableMapper.updateById(row);
                    } else {
                        insertList.add(row);
                    }
                }
                if (!insertList.isEmpty()) {
                    lineageEdgeTableMapper.batchSave(insertList);
                }
            }

            private void saveTableTag(Set<LineageTagTableEntity> tagTbList) {
                List<String> tagIds = new ArrayList<>();
                for (LineageTagTableEntity row : tagTbList) {
                    tagIds.add(row.getId());
                }
                List<LineageTagTableEntity> tagExists = lineageTagTableMapper.selectBatchIds(tagIds);
                Set<String> tagExistsIdSet = new HashSet<>();
                for (LineageTagTableEntity row : tagExists) {
                    tagExistsIdSet.add(row.getId());
                }

                List<LineageTagTableEntity> tagInsertList = new ArrayList<>();
                for (LineageTagTableEntity row : tagTbList) {
                    if (tagExistsIdSet.contains(row.getId())) {
                        if (AiShuUtil.isEmpty(row.getDsId())) {
                            row.setDsId(null);
                        }
                        lineageTagTableMapper.updateById(row);
                    } else {
                        tagInsertList.add(row);
                    }
                }
                if (!tagInsertList.isEmpty()) {
                    lineageTagTableMapper.batchSave(tagInsertList);
                }

            }
        });


        return rlt;
    }

    private LineageEdgeColumnTableRelationEntity createLineageEdgeColumnTableRelationEntity(String tbId, String colId) {
        LineageEdgeColumnTableRelationEntity entity = new LineageEdgeColumnTableRelationEntity();
        entity.setTableId(tbId);
        entity.setColumnId(colId);

        // id
        entity.setId(md5(entity.toString()));
        return entity;
    }

    private LineageEdgeColumnEntity createLineageEdgeColumnEntity(Lineage lineage, LineageTagColumnEntity srcCol, LineageTagColumnEntity dstCol) {
        LineageEdgeColumnEntity entity = new LineageEdgeColumnEntity();
        entity.setParentId(srcCol.getId());
        entity.setChildId(dstCol.getId());
        entity.setCreateTime(new Date(lineage.getCreateTime() == null ? 0l : lineage.getCreateTime()));
        entity.setCreateType(lineage.getCreateType().name());
        entity.setQueryText(lineage.getQueryText());

        // id
        entity.setId(md5(entity.toString()));
        return entity;
    }

    private LineageEdgeTableEntity createLineageEdgeTableEntity(Lineage lineage, LineageTagTableEntity srcTb, LineageTagTableEntity dstTb) {
        LineageEdgeTableEntity entity = new LineageEdgeTableEntity();
        entity.setParentId(srcTb.getId());
        entity.setChildId(dstTb.getId());
        entity.setCreateTime(new Date(lineage.getCreateTime() == null ? 0l : lineage.getCreateTime()));
        entity.setCreateType(lineage.getCreateType().name());
        entity.setQueryText(lineage.getQueryText());

        // id
        entity.setId(md5(entity.toString()));
        return entity;
    }

    private LineageTagColumnEntity createLineageTagColumnEntity(String tbId, Lineage.Vertex vertex) {
        if (!AiShuUtil.isEmpty(vertex.getColumn())) {
            LineageTagColumnEntity column = new LineageTagColumnEntity();
            column.setTableId(tbId);
            column.setColumnName(vertex.getColumn());

            // id
            column.setId(md5(column.toString()));
            return column;
        }
        return null;
    }

    private LineageTagTableEntity createLineageTagTableEntity(Lineage.Vertex vertex) {
        LineageTagTableEntity table = new LineageTagTableEntity();
        table.setDbType(vertex.getDbType().getDescp());
        table.setDsId(vertex.getDataSource().getDsId().toString());
        table.setJdbcUrl(vertex.getDataSource().getJdbcUrl());
        table.setJdbcUser(vertex.getDataSource().getJdbcUser());
        table.setDbName(AiShuUtil.isEmpty(vertex.getDbName()) ? vertex.getDbSchema() : vertex.getDbName());
        table.setDbSchema(AiShuUtil.isEmpty(vertex.getDbSchema()) ? vertex.getDbName() : vertex.getDbSchema());
        table.setTbName(vertex.getTbName());

        // id
        table.setId(md5(table.toString()));
        return table;
    }


    private String md5(String str) {
        return DigestUtils.md5Hex(str);
    }


}
