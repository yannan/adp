package com.eisoo.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.config.AnyFabricConfig;
import com.eisoo.config.SpringUtil;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.TableLineageEntity;
import com.eisoo.mapper.TableFromComposeMapper;
import com.eisoo.mapper.TableFromLogicMapper;
import com.eisoo.mapper.TableFromViewMapper;
import com.eisoo.mapper.TableLineageMapper;
import com.eisoo.metadatamanage.util.HttpUtil;
import com.eisoo.service.ILineageService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
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
 * @Date: 2024/12/13 13:21
 * @Version:1.0
 */
@Service
@Slf4j
public class TableLineageServiceImpl extends ServiceImpl<TableLineageMapper, TableLineageEntity> implements ILineageService {
    @Autowired
    private TableLineageMapper tableLineageMapper;
    @Autowired
    private TableFromViewMapper tableFromViewMapper;
    @Autowired
    private TableFromComposeMapper tableFromComposeMapper;
    @Autowired
    private TableFromLogicMapper tableFromLogicMapper;
    @Autowired
    private LineageOpLogService lineageOpLogService;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertBatchEntityAndLog(List<? extends BaseLineageEntity> entityList, String type) {
        insertBatchEntityOrUpdate(entityList);
        // 记录日志
        lineageOpLogService.saveLineageDataToLog(type, Constant.INSERT, entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer insertBatchEntityOrUpdate(List<? extends BaseLineageEntity> entityList) {
        return tableLineageMapper.insertBatchSomeColumn((List<TableLineageEntity>) entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Integer deleteBatchEntity(List<? extends BaseLineageEntity> entityList) {
        return tableLineageMapper.deleteBatchIds(entityList);
    }

    @Override
    public BaseLineageEntity selectById(String id) {
        return tableLineageMapper.selectById(id);
    }

    @Override
    public List<? extends BaseLineageEntity> selectBatchIds(List<String> ids) {
        return tableLineageMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // 1,删除实体
        deleteBatchEntity(entityList);
        // 2,table类型不需要删除relation
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // 1,插入实体
        insertBatchEntityOrUpdate(entityList);
        // 2,table类型不需要插入relation
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateBatchEntityAndRelation(List<? extends BaseLineageEntity> entityList) {
        // 1,更新实体
        insertBatchEntityOrUpdate(entityList);
        // 2,table类型不需要更新relation
    }

    public void initInsertViewTable() {
        tableFromViewMapper.insertBatchSomeColumn();
    }

    public void initInsertComposeTable() {
        tableFromComposeMapper.insertBatchSomeColumn();
        log.info("完成table的业务血缘数据向t_lineage_tag_table2表的同步");
    }

    public void initInsertLogicTable() throws Exception {
        AnyFabricConfig anyFabricConfig = SpringUtil.getBean(AnyFabricConfig.class);
        String dataLineageUrl = anyFabricConfig.getDataLineageUrl() + "?table_name=form_view&id=%s";
        List<TableLineageEntity> ENTITY_LIST = new ArrayList<>(50);
        final boolean[] result = {true};
        tableFromLogicMapper.selectTableIdBatch(new ResultHandler<String>() {
            @Override
            public void handleResult(ResultContext<? extends String> resultContext) {
                String id = resultContext.getResultObject();
                String url = String.format(dataLineageUrl, id);
                String response = HttpUtil.executeGet(url, null);
                try {
                    JSONObject jsonObject = JSONObject.parseObject(response);
                    if (!jsonObject.containsKey("type")) {
                        log.error("id={}无法表不存在，无法解析！", id);
                        throw new Exception();
                    }
                    JSONArray entities = jsonObject.getJSONArray("entities");
                    JSONObject data = entities.getJSONObject(0);
                    data.put("type", "insert");
                    TableLineageEntity table = data.toJavaObject(TableLineageEntity.class);
                    ENTITY_LIST.add(table);
                    if (ENTITY_LIST.size() == 50) {
                        tableLineageMapper.insertBatchSomeColumn(ENTITY_LIST);
                        ENTITY_LIST.clear();
                    }
                } catch (Exception e) {
                    log.error("同步logic类型table数据失败", e);
                    result[0] = false;
                    resultContext.stop();
                }
            }
        });
        if (result[0]) {
            if (!ENTITY_LIST.isEmpty()) {
                tableLineageMapper.insertBatchSomeColumn(ENTITY_LIST);
                ENTITY_LIST.clear();
            }
            log.info("完成table的逻辑&自定义视图-血缘数据向t_lineage_tag_table2表的同步");
        } else {
            throw new Exception();
        }
    }
    public void truncateTable() {
        tableLineageMapper.truncateTable();
    }

    public List<String> selectTableUniqueIdList(ArrayList<String> tableUniqueIds) {
        return tableLineageMapper.selectTableUniqueIdList(tableUniqueIds);
    }
}
