package com.eisoo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.entity.RelationEntity;
import com.eisoo.lineage.CommonUtil;
import com.eisoo.mapper.ColumnLineageMapper;
import com.eisoo.service.ILineageService;
import com.eisoo.util.Constant;
import lombok.extern.slf4j.Slf4j;
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
 * @Date: 2024/12/24 17:48
 * @Version:1.0
 */
@Service
@Slf4j
public class CustomerEtlColumnLineageServiceImpl extends ServiceImpl<ColumnLineageMapper, ColumnLineageEntity> implements ILineageService {
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;
    @Autowired
    private RelationService relationService;

    @Override
    public Integer insertBatchEntityOrUpdate(List<? extends BaseLineageEntity> entityList) {
        return null;
    }

    @Override
    public Integer deleteBatchEntity(List<? extends BaseLineageEntity> entityList) {
        return null;
    }

    @Override
    public BaseLineageEntity selectById(String id) {
        return columnLineageServiceImpl.selectById(id);
    }

    @Override
    public List<? extends BaseLineageEntity> selectBatchIds(List<String> ids) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // delete:不会删除column信息，只会删除别的字段的relation + deps置空
        columnLineageServiceImpl.removeBatchColumnDeps((List<ColumnLineageEntity>) entityList);
        relationService.removeOnlyRelationsNotDelete(entityList);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // insert:不会插入column信息，只会新建这个字段与别的字段的relation + 更新column的column_unique_ids,但是这里也要改成使用insert
        columnLineageServiceImpl.insertBatchEntityOrUpdate(entityList);
        // 更新relation
        for (BaseLineageEntity baseLineageEntity : entityList) {
            ColumnLineageEntity column = (ColumnLineageEntity) baseLineageEntity;
            String currentUuid = column.getUniqueId();
            String depsColumnUniqueIds = column.getColumnUniqueIds();
            RelationEntity relationEntity = relationService.selectById(currentUuid);
            if (relationEntity == null) {
                relationEntity = new RelationEntity(currentUuid, 1);
                relationEntity.setParent(new HashSet<>());
            }
            if (CommonUtil.isNotEmpty(depsColumnUniqueIds)) {
                String[] columnsParent = depsColumnUniqueIds.split(Constant.GLOBAL_SPLIT_COMMA);
                List<RelationEntity> parents = new ArrayList<>(columnsParent.length);
                for (String id : columnsParent) {
                    // insert自己的id
                    relationEntity.getParent().add(id);
                    // 查询是否存在这个parent,理论上存在
                    RelationEntity parent = relationService.selectById(id);
                    if (parent != null) {
                        Set<String> set = parent.getChild();
                        set.add(currentUuid);
                    } else {
                        parent = new RelationEntity(id, 1);
                        HashSet<String> set = new HashSet<>();
                        set.add(currentUuid);
                        parent.setChild(set);
                    }
                    parents.add(parent);
                }
                relationService.saveOrUpdateRelationBatch(parents);
                relationService.saveOrUpdateRelation(relationEntity);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        deleteBatchEntityAndRelation(entityList);
        // 这里要新建relation实体
        insertBatchEntityAndRelation(entityList);
    }
}
