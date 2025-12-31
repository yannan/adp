package com.eisoo.metadatamanage.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eisoo.entity.GraphInfoEntity;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/13 13:45
 * @Version:1.0
 */
public interface IGraphInfoService extends IService<GraphInfoEntity> {
    void insertOrUpdate(GraphInfoEntity graphInfoEntity);

}
