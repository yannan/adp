package com.eisoo.metadatamanage.web.service;

import com.eisoo.dto.build.NetWorkBuildDto;

import java.net.URISyntaxException;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/22 10:11
 * @Version:1.0
 */
public interface IAdGraphBuildService {
    void start(NetWorkBuildDto netWorkBuildDto) throws Exception;
}
