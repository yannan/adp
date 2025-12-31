package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.dto.AnyDataBuilderParaDto;
import com.eisoo.dto.ColumnLineageDto;
import com.eisoo.entity.BaseLineageEntity;
import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.metadatamanage.web.commons.LineageColumnUtil;
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
public class AnyDataColumnLineageServiceImpl implements IAnyDataBaseLineageService {
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Override
    public String sendUpsertInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        log.info("准备upsert如下的columns:\n{}", lineageDomainList);
        String response = "";
        try {
            response = sendDeleteInstructionToADLineage(lineageDomainList);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第一步：删除column成功！开始第二步：插入column");
            } else {
                log.error("第一步：删除column失败！response:{}", response);
                throw new Exception();
            }
            response = LineageColumnUtil.sendUpsertColumnListToAd(lineageDomainList, anyDataGraphConfig);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第二步：插入column成功！开始第三步：column->table 的edge");
            } else {
                log.error("第二步：插入column失败！response:{}", response);
                throw new Exception();
            }
            response = LineageColumnUtil.buildTable2ColumnEdge(lineageDomainList, anyDataGraphConfig);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第三步：构建 column->table 的edge成功！开始构建 column->column 的edge");
            } else {
                log.error("第三步：构建 column->table 的edge失败！response:{}", response);
                throw new Exception();
            }
            response = LineageColumnUtil.buildColumn2ColumnEdge(lineageDomainList, anyDataGraphConfig);
            if (Constant.AD_RESPONSE_SUCCESS.equals(response)) {
                log.info("第三步：构建 column->column 的edge成功！完成了upsert column，结束退出");
            } else {
                log.error("第三步：构建 column->column 的edge失败！response:{}", response);
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("upsert column失败！response:{}", response);
            e.printStackTrace();
            throw new Exception(e);
        }
        return response;
    }
    @Override
    public String sendDeleteInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        AnyDataBuilderParaDto anyDataBuilderParaDto = new AnyDataBuilderParaDto(anyDataGraphConfig.getGraphId(),
                                                                                Constant.LINEAGE_COLUMN,
                                                                                Constant.DELETE);
        ArrayList<ColumnLineageDto> listDelUid = new ArrayList<>(lineageDomainList.size());
        for (BaseLineageEntity domain : lineageDomainList) {
            listDelUid.add(new ColumnLineageDto(((ColumnLineageEntity) domain).getUniqueId()));
        }
        anyDataBuilderParaDto.setGraphData(listDelUid);
        String jsonStringDel = JsonUtils.toJsonString(anyDataBuilderParaDto);
        log.info("准备delete如下的column实体:{}", jsonStringDel);
        return HttpRequestUtils.sendHttpsPostJson(anyDataGraphConfig.getUrl(),
                                                  anyDataGraphConfig.getHeadMap(),
                                                  jsonStringDel);
    }
    @Override
    public String sendUpdateInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception {
        log.info("准备update columns,直接调用upsert方法......");
        return sendUpsertInstructionToADLineage(lineageDomainList);
    }
}
