package com.eisoo.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.eisoo.entity.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.mapper.ColumnLineageMapper;
import com.eisoo.mapper.IndicatorLineageMapper;
import com.eisoo.mapper.LineageOpLogMapper;
import com.eisoo.mapper.TableLineageMapper;
import com.eisoo.util.Constant;
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
 * @Date: 2024/12/26 14:32
 * @Version:1.0
 */
@Service
@Slf4j
public class LineageOpLogService extends ServiceImpl<LineageOpLogMapper, LineageOpLogEntity> {
    @Autowired
    private LineageOpLogMapper lineageOpLogMapper;
    @Autowired
    private TableLineageMapper tableLineageMapper;
    @Autowired
    private ColumnLineageMapper columnLineageMapper;
    @Autowired
    private IndicatorLineageMapper indicatorLineageMapper;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveLineageDataToLog(String type, String actionType, List<? extends BaseLineageEntity> lineageEntityList) {
        ArrayList<LineageOpLogEntity> result = new ArrayList<>(lineageEntityList.size());
        for (BaseLineageEntity baseLineageEntity : lineageEntityList) {
            LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity(type, actionType, JSONObject.toJSONString(baseLineageEntity));
            if (baseLineageEntity instanceof ColumnLineageEntity) {
                lineageOpLogEntity.setClassId(((ColumnLineageEntity) baseLineageEntity).getUniqueId());
            } else if (baseLineageEntity instanceof IndicatorLineageEntity) {
                lineageOpLogEntity.setClassId(((IndicatorLineageEntity) baseLineageEntity).getUuid());
            } else if (baseLineageEntity instanceof TableLineageEntity) {
                lineageOpLogEntity.setClassId(((TableLineageEntity) baseLineageEntity).getUniqueId());
            } else if (baseLineageEntity instanceof DolphinEntity) {
                lineageOpLogEntity.setClassId(((DolphinEntity) baseLineageEntity).getId());
            }
            result.add(lineageOpLogEntity);
        }
        lineageOpLogMapper.insertBatchSomeColumn(result);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveDolphinDataToLog(List<ColumnLineageEntity> entityList) {
        ArrayList<LineageOpLogEntity> result = new ArrayList<>(entityList.size());
        for (ColumnLineageEntity domain : entityList) {
            String uniqueId = domain.getUniqueId();
            LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity(Constant.DOLPHIN_ETL_COLUMN,
                                                                           "-",
                                                                           JSONObject.toJSONString(domain));
            lineageOpLogEntity.setClassId(uniqueId);
            result.add(lineageOpLogEntity);
        }
        if (!result.isEmpty()) {
            lineageOpLogMapper.insertBatchSomeColumn(result);
        }
    }

    /**
     * 初始化同步log数据
     *
     * @return 是否成功
     */
    public boolean saveInitDataToLog() {
        final boolean[] result = {true};
        try {
            lineageOpLogMapper.truncateTable();
            log.error("清空log表成功");
        } catch (Exception e) {
            result[0] = false;
            log.error("清空log表失败", e);
        }
        List<LineageOpLogEntity> ENTITY_LIST = new ArrayList<>(50);
        if (result[0]) {
            // 1,table
            tableLineageMapper.selectTableLineageBatch(new ResultHandler<TableLineageEntity>() {
                @Override
                public void handleResult(ResultContext<? extends TableLineageEntity> resultContext) {
                    try {
                        TableLineageEntity resultObject = resultContext.getResultObject();
                        LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity(Constant.TABLE,
                                                                                       Constant.INSERT,
                                                                                       JSONObject.toJSONString(resultObject)
                        );
                        lineageOpLogEntity.setClassId(resultObject.getUniqueId());
                        ENTITY_LIST.add(lineageOpLogEntity);
                        if (ENTITY_LIST.size() == 50) {
                            lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                            ENTITY_LIST.clear();
                        }
                    } catch (Exception e) {
                        result[0] = false;
                        log.error("初始同步table类型log数据出错:", e);
                        resultContext.stop();
                    }
                }
            });
            if (result[0]) {
                if (!ENTITY_LIST.isEmpty()) {
                    lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                    ENTITY_LIST.clear();
                }
            }
        }

        if (result[0]) {
            // 2,column
            columnLineageMapper.selectColumnLineageBatch(new ResultHandler<ColumnLineageEntity>() {
                @Override
                public void handleResult(ResultContext<? extends ColumnLineageEntity> resultContext) {
                    try {
                        ColumnLineageEntity resultObject = resultContext.getResultObject();
                        LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity(Constant.COLUMN,
                                                                                       Constant.INSERT,
                                                                                       JSONObject.toJSONString(resultObject)
                        );
                        lineageOpLogEntity.setClassId(resultObject.getUniqueId());
                        ENTITY_LIST.add(lineageOpLogEntity);
                        if (ENTITY_LIST.size() == 50) {
                            lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                            ENTITY_LIST.clear();
                        }
                    } catch (Exception e) {
                        result[0] = false;
                        log.error("初始同步column类型log数据出错:", e);
                        resultContext.stop();
                    }
                }
            });
            if (result[0]) {
                if (!ENTITY_LIST.isEmpty()) {
                    lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                    ENTITY_LIST.clear();
                }
            }
        }

        if (result[0]) {
            // 3,indicator
            indicatorLineageMapper.selectIndicatorLineageEntity(new ResultHandler<IndicatorLineageEntity>() {
                @Override
                public void handleResult(ResultContext<? extends IndicatorLineageEntity> resultContext) {
                    try {
                        IndicatorLineageEntity resultObject = resultContext.getResultObject();
                        LineageOpLogEntity lineageOpLogEntity = new LineageOpLogEntity(Constant.INDICATOR, Constant.INSERT,
                                                                                       JSONObject.toJSONString(resultObject));
                        lineageOpLogEntity.setClassId(resultObject.getUuid());
                        ENTITY_LIST.add(lineageOpLogEntity);
                        if (ENTITY_LIST.size() == 50) {
                            lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                            ENTITY_LIST.clear();
                        }
                    } catch (Exception e) {
                        result[0] = false;
                        log.error("初始同步indicator类型log数据出错:", e);
                        resultContext.stop();
                    }
                }
            });
            if (result[0]) {
                if (!ENTITY_LIST.isEmpty()) {
                    lineageOpLogMapper.insertBatchSomeColumn(ENTITY_LIST);
                    ENTITY_LIST.clear();
                }
            }
        }
        log.info("插入log表已完成");
        return result[0];
    }
}
