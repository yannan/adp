package com.eisoo.metadatamanage.web.service;

import com.eisoo.dto.AdLineageQueryDto;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:20
 * @Version:1.0
 */
public interface IAdLineageQueryService {
    String getAdLineage(AdLineageQueryDto adLineageQueryDto) throws Exception;

}
