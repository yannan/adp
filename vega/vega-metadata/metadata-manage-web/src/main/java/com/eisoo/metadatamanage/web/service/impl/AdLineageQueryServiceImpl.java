package com.eisoo.metadatamanage.web.service.impl;

import com.eisoo.dto.AdLineageQueryDto;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.service.IAdLineageQueryService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:22
 * @Version:1.0
 */
@Service
public class AdLineageQueryServiceImpl implements IAdLineageQueryService {
    @Autowired
    AnyDataGraphConfig anyDataGraphConfig;
    @Override
    public String getAdLineage(AdLineageQueryDto adLineageQueryDto) {
        // match p=(v:lineage_${type}{${id_type}:"${uuid}"})${direction}[*1..${step}]-(v2) return p
        String response = "";
        try {
            response =HttpRequestUtils.sendHttpsPosToAdLineageJson(anyDataGraphConfig.getAnyDataLineageQueryURL(),
                                                                   anyDataGraphConfig.getHeadMap(),
                                                                    JsonUtils.toJsonString(adLineageQueryDto));
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.UnKnowException,"AD查询出错：error=" + e.getMessage());
        }
        return response;
    }
}
