package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.dto.TableLineageDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.TableLineageEntity;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.service.IAnyDataBaseLineageService;
import com.eisoo.util.Constant;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/7 17:30
 * @Version:1.0
 */
@Slf4j
@Service
public class AnyDataTableLineageServiceImpl implements IAnyDataBaseLineageService {
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;

    @Override
    public String sendUpsertInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(),
                                                                                Constant.LINEAGE_TABLE,
                                                                                Constant.UPSERT,
                                                                                lineageDomainList);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.info("准备upsert如下的表：{}", jsonString);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
    }
    @Override
    public String sendDeleteInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(),
                                                                                Constant.LINEAGE_TABLE,
                                                                                Constant.DELETE
        );
        ArrayList<TableLineageDto> listDelUid = new ArrayList<>(lineageDomainList.size());
        for (BaseLineageEntity domain : lineageDomainList) {
            TableLineageEntity t = (TableLineageEntity)domain;
            listDelUid.add(new TableLineageDto(t.getUuid()));
        }
        anyDataBuilderParaDto.setGraphData(listDelUid);
        String jsonString = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.warn("准备删除如下的table:{}", jsonString);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonString);
    }

    @Override
    public String sendUpdateInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        log.info("准备update表,直接调用upsert方法");
        return sendUpsertInstructionToADLineage(lineageDomainList);
    }
}
