package com.eisoo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.dto.RelationForwardDto;
import com.eisoo.dto.RelationReversedDto;
import com.eisoo.entity.*;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.mapper.IndicatorLineageMapper;
import com.eisoo.mapper.RelationMapper;
import com.eisoo.util.Constant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/13 17:57
 * @Version:1.0
 */
@Service
@Slf4j
public class RelationService extends ServiceImpl<RelationMapper, RelationEntity> {
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private IndicatorLineageMapper indicatorLineageMapper;

    public List<Object> makeRelationData(List<Object> list, String id, int step, String direction) {
        RelationEntity relationEntity = getById(id);
        if (null == relationEntity) {
            return list;
        }
        if (2 == step) {
            switch (direction) {
                case Constant.FORWARD:
                    Set<String> child = relationEntity.getChild();
                    RelationForwardDto relationForwardDto = new RelationForwardDto(id, relationEntity.getClassType(), StringUtils.join(child, Constant.GLOBAL_SPLIT_COMMA));
                    list.add(relationForwardDto);
                    break;
                case Constant.REVERSELY:
                    Set<String> parent = relationEntity.getParent();
                    RelationReversedDto relationReversedDto = new RelationReversedDto(id, relationEntity.getClassType(), StringUtils.join(parent, Constant.GLOBAL_SPLIT_COMMA));
                    list.add(relationReversedDto);
                    break;
            }
        } else {
            switch (direction) {
                case Constant.FORWARD:
                    Set<String> child = relationEntity.getChild();
                    RelationForwardDto relationForwardDto = new RelationForwardDto(id, relationEntity.getClassType(), StringUtils.join(child, Constant.GLOBAL_SPLIT_COMMA));
                    list.add(relationForwardDto);
                    --step;
                    for (String chidID : child) {
                        list = makeRelationData(list, chidID, step, Constant.FORWARD);
                    }
                    break;
                case Constant.REVERSELY:
                    Set<String> parent = relationEntity.getParent();
                    RelationReversedDto relationReversedDto = new RelationReversedDto(id, relationEntity.getClassType(), StringUtils.join(parent, Constant.GLOBAL_SPLIT_COMMA));
                    list.add(relationReversedDto);
                    --step;
                    for (String parentId : parent) {
                        list = makeRelationData(list, parentId, step, Constant.REVERSELY);
                    }
                    break;
            }
        }
        return list;
    }

    /***
     * 清空指定实体和依赖
     * @param entityList 实体数据
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void removeOnlyRelationsNotDelete(List<? extends BaseLineageEntity> entityList) {
        for (BaseLineageEntity baseLineageEntity : entityList) {
            String id = ((ColumnLineageEntity) baseLineageEntity).getUniqueId();
            RelationEntity relation = selectById(id);
            if (relation != null) {
                List<RelationEntity> list = getAllDeleteRelationEntities(relation);
                // 批量更新，这里不是直接删除
                if (list != null && list.size() != 0) {
                    saveOrUpdateRelationBatch(list);
                }
                // 删除自己
                relation.getParent().clear();
                relation.getChild().clear();
                saveOrUpdateRelation(relation);
            }
        }
    }

    /***
     * 删除指定实体+remove其关系
     * @param entityList 实体数据
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void removeBatchEntityList(List<? extends BaseLineageEntity> entityList) {
        for (BaseLineageEntity baseLineageEntity : entityList) {
            String id = "";
            if (baseLineageEntity instanceof ColumnLineageEntity) {
                id = ((ColumnLineageEntity) baseLineageEntity).getUniqueId();
            } else if (baseLineageEntity instanceof IndicatorLineageEntity) {
                id = ((IndicatorLineageEntity) baseLineageEntity).getUuid();
            }
            RelationEntity relation = selectById(id);
            if (relation != null) {
                List<RelationEntity> list = getAllDeleteRelationEntities(relation);
                // 删除自己
                deleteById(relation);
                // 批量更新，这里不是直接删除
                if (list != null && list.size() != 0) {
                    saveOrUpdateRelationBatch(list);
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertColumnAndParentOnly(RelationEntity relationEntity, String columnUniqueIds) {
        String uniqueId = relationEntity.getUniqueId();
        String[] columnsParent = columnUniqueIds.split(Constant.GLOBAL_SPLIT_COMMA);
        for (String parentId : columnsParent) {
            // 添加parentId
            relationEntity.getParent().add(parentId);
            RelationEntity parent = selectById(parentId);
            if (parent != null) {
                parent.getChild().add(uniqueId);
            } else {
                parent = new RelationEntity(parentId, 1);
                HashSet<String> set = new HashSet<>();
                set.add(uniqueId);
                parent.setChild(set);
            }
            // 更新
            saveOrUpdateRelation(parent);
        }
        saveOrUpdateRelation(relationEntity);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertIndicatorAndParentOnly(RelationEntity relationEntity, String depsIndicators) {
        String currentUuid = relationEntity.getUniqueId();
        String[] indicatorsParent = depsIndicators.split(Constant.GLOBAL_SPLIT_COMMA);
        for (String parentId : indicatorsParent) {
            relationEntity.getParent().add(parentId);
            RelationEntity parent = selectById(parentId);
            if (parent != null) {
                parent.getChild().add(currentUuid);
            } else {
                parent = new RelationEntity(parentId, 2);
                HashSet<String> set = new HashSet<>();
                set.add(currentUuid);
                parent.setChild(set);
            }
            saveOrUpdateRelation(parent);
        }
        saveOrUpdateRelation(relationEntity);
    }

    /***
     * 插入指定实体和关系
     * @param entityList 血缘实体数据
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertBatchEntityList(List<? extends BaseLineageEntity> entityList) {
        for (BaseLineageEntity baseLineageEntity : entityList) {
            String id = "";
            if (baseLineageEntity instanceof ColumnLineageEntity) {
                ColumnLineageEntity column = (ColumnLineageEntity) baseLineageEntity;
                id = column.getUniqueId();
                String columnUniqueIds = column.getColumnUniqueIds();
                RelationEntity indicatorNew = new RelationEntity(id, 1);
                indicatorNew.setParent(new HashSet<>(1));
                if (CommonUtil.isEmpty(columnUniqueIds)) {
                    saveOrUpdateRelation(indicatorNew);
                } else {
                    insertColumnAndParentOnly(indicatorNew, columnUniqueIds);
                }
            } else if (baseLineageEntity instanceof IndicatorLineageEntity) {
                IndicatorLineageEntity indicator = (IndicatorLineageEntity) baseLineageEntity;
                String currentUuid = indicator.getUuid();
                RelationEntity indicatorNew = new RelationEntity(currentUuid, 2);
                indicatorNew.setParent(new HashSet<>());
                // 更新依赖
                String depsIndicators = indicator.getIndicatorUuids();
                if (CommonUtil.isNotEmpty(depsIndicators)) {
                    insertIndicatorAndParentOnly(indicatorNew, depsIndicators);
                }
                String columnUuids = indicator.getColumnUniqueIds();
                if (CommonUtil.isNotEmpty(columnUuids)) {
                    insertColumnAndParentOnly(indicatorNew, columnUuids);
                }
            }
        }
    }

    /***
     * 更新指定实体和关系
     * @param entityList 血缘实体数据
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateBatchEntityList(List<? extends BaseLineageEntity> entityList) {
        // 只有indicator更新的时候才会更新relation！！！！
        removeBatchEntityList(entityList);
        insertBatchEntityList(entityList);
    }

    /***
     *  操作指定实体的相关relation实体，并返回待更新的
     * @param relation 实体
     * @return 待更新的实体
     */
    public List<RelationEntity> getAllDeleteRelationEntities(RelationEntity relation) {
        String id = relation.getUniqueId();
        List<RelationEntity> list = new ArrayList<>();
        // 查出parent,删除掉其child的id
        Set<String> parents = relation.getParent();
        for (String parentId : parents) {
            RelationEntity parent = relationMapper.selectById(parentId);
            if (null != parent) {
                parent.getChild().remove(id);
                list.add(parent);
            }
        }
        // 查出child,删除掉其parent的id
        Set<String> children = relation.getChild();
        for (String childId : children) {
            RelationEntity child = relationMapper.selectById(childId);
            if (null != child) {
                child.getChild().remove(id);
                list.add(child);
            }
        }
        return list;
    }

    /***
     * 批量更新指定的relation对象
     * @param list 血缘数据实体
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveOrUpdateRelationBatch(List<RelationEntity> list) {
        relationMapper.insertBatchSomeColumn(list);
    }

    public void truncateTable() {
        relationMapper.truncateTable();
    }

    public boolean initInsertBatchParentRelationColumn() {
        boolean result = true;
        try {
            relationMapper.insertBatchColumnRelation();
            log.info("同步column类型parent成功");
        } catch (Exception e) {
            log.error("同步column类型parent失败", e);
            result = false;
        }
        return result;
    }

    public boolean initInsertBatchChildRelationColumn(Integer batchSize) {
        List<RelationEntity> RELATION_LINEAGE_ENTITY_LIST = new ArrayList<>(batchSize);
        final boolean[] result = {true};
        relationMapper.selectRelationEntity(new ResultHandler<RelationEntity>() {
            @Override
            public void handleResult(ResultContext<? extends RelationEntity> resultContext) {
                try {
                    RelationEntity relationEntity = resultContext.getResultObject();
                    String childId = relationEntity.getUniqueId();
                    Set<String> parent = relationEntity.getParent();
                    for (String parentId : parent) {
                        RelationEntity relationEntityParent = relationMapper.selectById(parentId);
                        if (null != relationEntityParent) {
                            Set<String> child = relationEntityParent.getChild();
                            child.add(childId);
                        } else {
                            relationEntityParent = new RelationEntity();
                            relationEntityParent.setUniqueId(parentId);
                            relationEntityParent.setClassType(1);
                            HashSet<String> set = new HashSet<>(1);
                            set.add(childId);
                            relationEntityParent.setChild(set);
                        }
                        RELATION_LINEAGE_ENTITY_LIST.add(relationEntityParent);
                    }
                    if (RELATION_LINEAGE_ENTITY_LIST.size() == batchSize) {
                        Integer insertCount = relationMapper.updateChildBatchChildField(RELATION_LINEAGE_ENTITY_LIST);
                        log.info("向t_lineage_relation更新了column类型child字段{}条", insertCount);
                        RELATION_LINEAGE_ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    result[0] = false;
                    log.error("向t_lineage_relation更新了column类型child字段出错:", e);
                    resultContext.stop();
                }
            }
        });
        if (result[0]) {
            if (!RELATION_LINEAGE_ENTITY_LIST.isEmpty()) {
                Integer insertCount = relationMapper.updateChildBatchChildField(RELATION_LINEAGE_ENTITY_LIST);
                log.info("向t_lineage_relation更新了column类型child字段{}条", insertCount);
                RELATION_LINEAGE_ENTITY_LIST.clear();
            }
        }
        return result[0];
    }

    public boolean initInsertBatchParentRelationIndicator() {
        boolean result = true;
        try {
            relationMapper.insertBatchIndicatorRelation();
            log.info("同步indicator类型parent数据成功");
        } catch (Exception e) {
            log.error("同步indicator类型parent数据失败", e);
            result = false;
        }
        return result;
    }
    public boolean initInsertBatchChildRelationIndicator(Integer batchSize) {
        List<RelationEntity> RELATION_LINEAGE_ENTITY_LIST = new ArrayList<>(batchSize);
        final boolean[] result = {true};
        indicatorLineageMapper.selectDepsInfo(new ResultHandler<IndicatorLineageEntity>() {
            @Override
            public void handleResult(ResultContext<? extends IndicatorLineageEntity> resultContext) {
                try {
                    IndicatorLineageEntity indicatorLineageEntity = resultContext.getResultObject();
                    String childId = indicatorLineageEntity.getUuid();
                    String columnUuids = indicatorLineageEntity.getColumnUniqueIds();
                    String indicatorUuids = indicatorLineageEntity.getIndicatorUuids();
                    if (CommonUtil.isNotEmpty(columnUuids)) {
                        String[] columnUniqueIds = columnUuids.split(Constant.GLOBAL_SPLIT_COMMA);
                        for (String columnParentId : columnUniqueIds) {
                            // 查询这个column是否存在
                            RelationEntity relationEntityParent = relationMapper.selectById(columnParentId);
                            if (null != relationEntityParent) {
                                Set<String> child = relationEntityParent.getChild();
                                child.add(childId);
                            } else {
                                relationEntityParent = new RelationEntity();
                                relationEntityParent.setUniqueId(columnParentId);
                                relationEntityParent.setClassType(1);
                                HashSet<String> set = new HashSet<>(1);
                                set.add(childId);
                                relationEntityParent.setChild(set);
                            }
                            RELATION_LINEAGE_ENTITY_LIST.add(relationEntityParent);
                        }
                    }
                    if (CommonUtil.isNotEmpty(indicatorUuids)) {
                        String[] indicatorIds = indicatorUuids.split(Constant.GLOBAL_SPLIT_COMMA);
                        for (String indicatorParentId : indicatorIds) {
                            // 查询这个column是否存在
                            RelationEntity indicatorParent = relationMapper.selectById(indicatorParentId);
                            if (null != indicatorParent) {
                                Set<String> child = indicatorParent.getChild();
                                child.add(childId);
                            } else {
                                indicatorParent = new RelationEntity();
                                indicatorParent.setUniqueId(indicatorParentId);
                                indicatorParent.setClassType(2);
                                HashSet<String> set = new HashSet<>(1);
                                set.add(childId);
                                indicatorParent.setChild(set);
                            }
                            RELATION_LINEAGE_ENTITY_LIST.add(indicatorParent);
                        }
                    }
                    if (RELATION_LINEAGE_ENTITY_LIST.size() == batchSize) {
                        Integer insertCount = relationMapper.updateChildBatchChildField(RELATION_LINEAGE_ENTITY_LIST);
                        log.info("向t_lineage_relation插入了indicator类型entity{}条", insertCount);
                        RELATION_LINEAGE_ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    result[0] = false;
                    log.error("向t_lineage_relation插入了indicator类型出错:", e);
                    resultContext.stop();
                }
            }
        });
        if (result[0]) {
            if (!RELATION_LINEAGE_ENTITY_LIST.isEmpty()) {
                Integer insertCount = relationMapper.updateChildBatchChildField(RELATION_LINEAGE_ENTITY_LIST);
                log.info("向t_lineage_relation插入了indicator类型entity{}条", insertCount);
                RELATION_LINEAGE_ENTITY_LIST.clear();
            }
        }
        return result[0];
    }

    public RelationEntity selectById(String currentUuid) {
        return relationMapper.selectById(currentUuid);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveOrUpdateRelation(RelationEntity parent) {
        relationMapper.saveOrUpdateRelation(parent);
    }

    public Integer insertBatchSomeColumnNotDep(List<RelationEntity> entityList) {
        return relationMapper.insertBatchSomeColumnNotDep(entityList);
    }

    /***
     * 更新来自dolphin的relation
     * @param dolphinColumnList:dolphin血缘实体list
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveOrUpdateDolphinRelation(List<ColumnLineageEntity> dolphinColumnList) {
        for (ColumnLineageEntity dolphinColumn : dolphinColumnList) {
            String currentUuid = dolphinColumn.getUniqueId();
            // 下面要做的：1，把relation表里存在的之前的parent 2，处理新的relation的parent
            RelationEntity relationEntity = selectById(currentUuid);
            // 理论上不应该是空
            if (null == relationEntity) {
                relationEntity = new RelationEntity(currentUuid, 1);
                relationEntity.setParent(new HashSet<>());
                relationEntity.setChild(new HashSet<>());
            } else {
                // 第一步：删除旧的parent
                Set<String> parentOld = relationEntity.getParent();
                for (String parentKey : parentOld) {
                    // 删除这里面的childId
                    RelationEntity parent = selectById(parentKey);
                    if (null != parent) {
                        parent.getChild().remove(currentUuid);
                        // 更新child
                        saveOrUpdateRelation(parent);
                    }
                }
            }
            // 清空自己的之前的parent
            relationEntity.getParent().clear();
            // 第二步：把依赖的column的更新到parent
            String depsColumnUniqueIds = dolphinColumn.getColumnUniqueIds();
            if (CommonUtil.isNotEmpty(depsColumnUniqueIds)) {
                String[] columnsParent = depsColumnUniqueIds.split(Constant.GLOBAL_SPLIT_COMMA);
                for (String id : columnsParent) {
                    // 更新自己的id
                    relationEntity.getParent().add(id);
                    // 查询是否存在这个parent,理论上存在
                    RelationEntity parent = selectById(id);
                    if (parent != null) {
                        Set<String> set = parent.getChild();
                        set.add(currentUuid);
                    } else {
                        parent = new RelationEntity(id, 1);
                        HashSet<String> set = new HashSet<>();
                        set.add(currentUuid);
                        parent.setChild(set);
                    }
                    saveOrUpdateRelation(parent);
                }
            }
            // 更新自己
            saveOrUpdateRelation(relationEntity);
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteById(RelationEntity relation) {
        relationMapper.deleteById(relation);
    }
}
