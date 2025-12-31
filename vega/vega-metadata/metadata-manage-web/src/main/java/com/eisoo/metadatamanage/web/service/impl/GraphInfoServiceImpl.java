package com.eisoo.metadatamanage.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.entity.GraphInfoEntity;
import com.eisoo.mapper.GraphInfoMapper;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/13 13:46
 * @Version:1.0
 */
import com.eisoo.metadatamanage.web.service.IGraphInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphInfoServiceImpl extends ServiceImpl<GraphInfoMapper, GraphInfoEntity> implements IGraphInfoService {
    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Override
    public void insertOrUpdate(GraphInfoEntity graphInfoEntity){
        graphInfoMapper.insertOrUpdate(graphInfoEntity);
    }
}
