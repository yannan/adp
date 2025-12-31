package com.eisoo.metadatamanage.web.service;

import com.eisoo.entity.BaseLineageEntity;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/7 13:32
 * @Version:1.0
 */
public interface IAnyDataBaseLineageService {
    String sendUpsertInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception;

    String sendDeleteInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception;
    String sendUpdateInstructionToADLineage(List<BaseLineageEntity> lineageDomainList) throws Exception;

}
