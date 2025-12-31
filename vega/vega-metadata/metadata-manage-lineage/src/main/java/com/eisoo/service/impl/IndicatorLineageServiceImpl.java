package com.eisoo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.config.AnyFabricConfig;
import com.eisoo.config.SpringUtil;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.IndicatorLineageEntity;
import com.eisoo.mapper.IndicatorLineageMapper;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.service.ILineageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/13 13:21
 * @Version:1.0
 */
@Service
@Slf4j
public class IndicatorLineageServiceImpl extends ServiceImpl<IndicatorLineageMapper, IndicatorLineageEntity> implements ILineageService {
    @Autowired
    private IndicatorLineageMapper indicatorLineageMapper;
    @Autowired
    private RelationService relationService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer insertBatchEntityOrUpdate(List<? extends BaseLineageEntity> entityList) {
        return indicatorLineageMapper.insertBatchSomeColumn((List<IndicatorLineageEntity>) entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer deleteBatchEntity(List<? extends BaseLineageEntity> entityList) {
        return indicatorLineageMapper.deleteBatchIds(entityList);
    }

    @Override
    public BaseLineageEntity selectById(String id) {
        return indicatorLineageMapper.selectById(id);
    }

    @Override
    public List<? extends BaseLineageEntity> selectBatchIds(List<String> ids) {
        return indicatorLineageMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // 1,删除实体
        deleteBatchEntity(entityList);
        // 2,删除relation
        relationService.removeBatchEntityList(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        insertBatchEntityOrUpdate(entityList);
        relationService.insertBatchEntityList(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        insertBatchEntityOrUpdate(entityList);
        relationService.updateBatchEntityList(entityList);
    }

    public void truncateTable() {
        indicatorLineageMapper.truncateTable();
    }

    public void initInsertBatchIndicator(Integer batchSize) throws Exception {
        List<IndicatorLineageEntity> INDICATOR_LINEAGE_ENTITY_LIST = new ArrayList<>(batchSize);
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String dataLineageUrl = anyFabricConfig.getIndicatorLineageUrl();
        final boolean[] result = {true};

        indicatorLineageMapper.selectIndicatorId(new ResultHandler<String>() {
            @Override
            public void handleResult(ResultContext<? extends String> resultContext) {
                try {
                    String id = resultContext.getResultObject();
                    String url = String.format(dataLineageUrl, id);
                    log.debug("url:{}", url);
                    String response = HttpUtil.executeGet(url, null);
                    JSONObject data = JSONObject.parseObject(response);
                    data.put("type", "insert");
                    IndicatorLineageEntity indicator = data.toJavaObject(IndicatorLineageEntity.class);
                    INDICATOR_LINEAGE_ENTITY_LIST.add(indicator);
                    if (INDICATOR_LINEAGE_ENTITY_LIST.size() == batchSize) {
                        indicatorLineageMapper.insertBatchSomeColumn(INDICATOR_LINEAGE_ENTITY_LIST);
                        INDICATOR_LINEAGE_ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    log.error("indicator血缘数据向t_lineage_tag_indicator2表的同步失败", e);
                    result[0] = false;
                    resultContext.stop();
                }

            }
        });
        if (result[0]) {
            if (!INDICATOR_LINEAGE_ENTITY_LIST.isEmpty()) {
                indicatorLineageMapper.insertBatchSomeColumn(INDICATOR_LINEAGE_ENTITY_LIST);
                INDICATOR_LINEAGE_ENTITY_LIST.clear();
            }
            log.info("完成indicator血缘数据向t_lineage_tag_indicator2表的同步");
        } else {
            throw new Exception();
        }

    }
}
