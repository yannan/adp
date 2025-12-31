package com.eisoo.metadatamanage.web.service.impl.lineage;

import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.metadatamanage.web.service.IAnyDataBaseLineageService;
import com.eisoo.metadatamanage.web.service.impl.AnyDataColumnLineageServiceImpl;
import com.eisoo.metadatamanage.web.service.impl.AnyDataIndicatorLineageServiceImpl;
import com.eisoo.metadatamanage.web.service.impl.AnyDataTableLineageServiceImpl;
import com.eisoo.service.ILineageService;
import com.eisoo.service.impl.*;
import com.eisoo.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/13 10:11
 * @Version:1.0
 */
@Service
@Slf4j
public class AnyDataLineageServiceManager {
    @Autowired
    private AnyDataTableLineageServiceImpl anyDataTableLineageServiceImpl;
    @Autowired
    private AnyDataColumnLineageServiceImpl anyDataColumnLineageServiceImpl;
    @Autowired
    private AnyDataIndicatorLineageServiceImpl anyDataIndicatorLineageServiceImpl;
    @Autowired
    private ColumnLineageServiceImpl columnLineageServiceImpl;
    @Autowired
    private CustomerEtlColumnLineageServiceImpl customerEtlColumnLineageServiceImpl;
    @Autowired
    private IndicatorLineageServiceImpl indicatorLineageServiceImpl;
    @Autowired
    private TableLineageServiceImpl tableLineageServiceImpl;
    @Autowired
    private DolphinLineageServiceImpl dolphinLineageServiceImpl;
    @Autowired
    private LineageOpLogService lineageOpLogService;
    private static final HashMap<String, IAnyDataBaseLineageService> SERVICE_MAP = new HashMap<>(3);
    public static final HashMap<String, ILineageService> LINEAGE_SERVICE_MAP = new HashMap<>(3);
    @PostConstruct
    public void init() {
        SERVICE_MAP.put(Constant.COLUMN, anyDataColumnLineageServiceImpl);
        SERVICE_MAP.put(Constant.TABLE, anyDataTableLineageServiceImpl);
        SERVICE_MAP.put(Constant.INDICATOR, anyDataIndicatorLineageServiceImpl);

        LINEAGE_SERVICE_MAP.put(Constant.TABLE, tableLineageServiceImpl);
        LINEAGE_SERVICE_MAP.put(Constant.COLUMN, columnLineageServiceImpl);
        LINEAGE_SERVICE_MAP.put(Constant.INDICATOR, indicatorLineageServiceImpl);
        LINEAGE_SERVICE_MAP.put(Constant.DOLPHIN, dolphinLineageServiceImpl);
        // 添加三方的table处理器
        LINEAGE_SERVICE_MAP.put(Constant.EXTERNAL_TABLE, tableLineageServiceImpl);
        // 添加三方的column处理器
        LINEAGE_SERVICE_MAP.put(Constant.EXTERNAL_COLUMN, columnLineageServiceImpl);
        LINEAGE_SERVICE_MAP.put(Constant.EXTERNAL_RELATION_COLUMN, customerEtlColumnLineageServiceImpl);
    }
    public String dispatcher(String type, String actionType, ArrayList<BaseLineageEntity> lineageEntityList) throws Exception {
        IAnyDataBaseLineageService iAnyDataBaseLineageService = SERVICE_MAP.get(type);
        String response = "";
        switch (actionType) {
            case Constant.DELETE:
                response = iAnyDataBaseLineageService.sendDeleteInstructionToADLineage(lineageEntityList);
                break;
            case Constant.INSERT:
                response = iAnyDataBaseLineageService.sendUpsertInstructionToADLineage(lineageEntityList);
                break;
            case Constant.UPDATE:
                response = iAnyDataBaseLineageService.sendUpdateInstructionToADLineage(lineageEntityList);
                break;
        }
        dispatcherDataBase(type, actionType, lineageEntityList);
        return response;
    }
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void dispatcherDataBase(String type, String actionType, List<BaseLineageEntity> lineageEntityList) throws Exception {
        ILineageService lineageService = LINEAGE_SERVICE_MAP.get(type);
        switch (actionType) {
            case Constant.DELETE:
                lineageService.deleteBatchEntityAndRelation(lineageEntityList);
                break;
            case Constant.INSERT:
                lineageService.insertBatchEntityAndRelation(lineageEntityList);
                break;
            case Constant.UPDATE:
                lineageService.updateBatchEntityAndRelation(lineageEntityList);
                break;
        }
        lineageOpLogService.saveLineageDataToLog(type, actionType, lineageEntityList);
    }
}
