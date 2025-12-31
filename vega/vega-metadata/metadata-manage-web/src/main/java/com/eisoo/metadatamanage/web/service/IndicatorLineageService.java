package com.eisoo.metadatamanage.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eisoo.entity.IndicatorLineageEntity;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/20 14:21
 * @Version:1.0
 */
public interface IndicatorLineageService extends IService<IndicatorLineageEntity> {
    void selectIndicatorLineageEntity();
}
