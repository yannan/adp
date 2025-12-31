package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.dto.IndicatorLineageDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.IndicatorLineageEntity;
import com.eisoo.metadatamanage.web.commons.LineageIndicatorUtil;
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
public class AnyDataIndicatorLineageServiceImpl implements IAnyDataBaseLineageService {
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Override
    public String sendUpsertInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        String response = "";
        log.info("准备upsert如下的indicator:\n{}", lineageDomainList);
        try {
            response = sendDeleteInstructionToADLineage(lineageDomainList);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第一步：删除indicator成功！开始第二步：插入indicator");
            } else {
                log.error("第一步：删除indicator失败！失败原因如下：{}", response);
                throw new Exception();
            }
            response = LineageIndicatorUtil.sendUpsertIndicatorToAD(lineageDomainList, anyDataGraphConfig);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第二步：插入indicator成功,开始第三步：构建 indicator->字段 & indicator->indicator 的edge");
            } else {
                log.error("第二步：插入indicator失败！失败原因如下：{}", response);
                throw new Exception();
            }
            response = LineageIndicatorUtil.buildIndicator2ColumnAndSelfEdge(lineageDomainList, anyDataGraphConfig);

            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第三步：构建 indicator->字段 & indicator->indicator 的edge成功！结束退出");
            } else {
                log.error("第三步：构建  indicator->字段 & indicator->indicator 的edge失败！失败原因如下：{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("upsert indicator失败！response:{}", response);
            e.printStackTrace();
            throw new Exception(e);
        }
        return response;
    }

    @Override
    public String sendDeleteInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(), Constant.LINEAGE_INDICATOR, Constant.DELETE);
        ArrayList<IndicatorLineageDto> listDelUid = new ArrayList<>(lineageDomainList.size());
        for (BaseLineageEntity domain : lineageDomainList) {
            IndicatorLineageEntity t = (IndicatorLineageEntity)domain;
            listDelUid.add(new IndicatorLineageDto(t.getUuid()));
        }
        anyDataBuilderParaDto.setGraphData(listDelUid);
        String jsonStringDel = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.info("准备delete如下的indicator：{}", jsonStringDel);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(), anyDataGraphConfig.getHeadMap(), jsonStringDel);
    }
    @Override
    public String sendUpdateInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        log.info("准备update indicator,直接调用upsert方法");
        return sendUpsertInstructionToADLineage(lineageDomainList);
    }
}
