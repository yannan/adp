package com.eisoo.metadatamanage.web.service.impl.lineage.platform;

import com.eisoo.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 10:29
 * @Version:1.0
 */
@Service
@Slf4j
public class PlatFormLineageInitService {
    @Autowired
    private TableLineageServiceImpl tableLineageServiceImpl;
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;
    @Autowired
    private IndicatorLineageServiceImpl indicatorLineageServiceImpl;
    @Autowired
    private DolphinLineageServiceImpl dolphinLineageServiceImpl;
    @Resource
    private RelationService relationService;


    private final Integer BATCH_SIZE = 100;

    public boolean insertBatchColumn() {
        try {
            // 首先清空表
            columnLineageServiceImpl.truncateTable();
            log.info("清空t_lineage_tag_column2成功！开始同步column的业务血缘数据");
            // 同步view类型的血缘数据
            columnLineageServiceImpl.initInsertBatchViewColumn(BATCH_SIZE);
            // 同步compose类型的血缘数据
            columnLineageServiceImpl.initInsertComposeColumn();
            // 同步logic类型的血缘数据
            columnLineageServiceImpl.initInsertLogicColumn(BATCH_SIZE);
            log.info("完成column的业务血缘数据向t_lineage_tag_column2表的同步");
        } catch (Exception e) {
            log.error("t_lineage_tag_column2初始化同步失败", e);
            return false;
        }
        return true;
    }

    /**
     * 同步table的血缘数据到db
     */
    public boolean insertBatchTable() {
        try {
            tableLineageServiceImpl.truncateTable();
            log.info("清空t_lineage_tag_table2成功！开始同步table的业务血缘数据");
            tableLineageServiceImpl.initInsertViewTable();
            tableLineageServiceImpl.initInsertComposeTable();
            tableLineageServiceImpl.initInsertLogicTable();
        } catch (Exception e) {
            log.error("t_lineage_tag_table2初始化同步失败", e);
            return false;
        }
        return true;
    }
    public boolean insertBatchIndicator() {
        try {
            indicatorLineageServiceImpl.truncateTable();
            log.info("清空t_lineage_tag_indicator2成功！开始同步indicator的业务血缘数据");
            indicatorLineageServiceImpl.initInsertBatchIndicator(BATCH_SIZE);
        } catch (Exception e) {
            log.error("t_lineage_tag_indicator2初始化同步失败", e);
            return false;
        }
        return true;
    }

    /**
     * 同步AF的关于table和column的关系数据
     */
    public boolean insertBatchRelation() {
        boolean result = true;
        try {
            // 首先清空表
            relationService.truncateTable();
            log.info("清空t_lineage_relation成功！开始同步relation的业务血缘数据");
        } catch (Exception e) {
            log.error("relationService 清空表失败", e);
            result = false;
        }
        if (result) {
            // 1.1 同步column类型parent
            result = relationService.initInsertBatchParentRelationColumn();
        }
        if (result) {
            // 1.2 同步column类型child
            result = relationService.initInsertBatchChildRelationColumn(BATCH_SIZE);
        }
        if (result) {
            // 2.1 同步indicator类型parent
            result = relationService.initInsertBatchParentRelationIndicator();
        }
        if (result) {
            // 2.2 同步indicator类型child
            result = relationService.initInsertBatchChildRelationIndicator(BATCH_SIZE);
        }
        return result;
    }

    /***
     * 同步AF的关于同步sql的数据
     */
    public boolean insertBatchDolphin() {
        boolean result;
        result = dolphinLineageServiceImpl.initSyncDolphin();
        if (result) {
            result = dolphinLineageServiceImpl.initComposeDolphin();
        }
        return result;
    }
}
