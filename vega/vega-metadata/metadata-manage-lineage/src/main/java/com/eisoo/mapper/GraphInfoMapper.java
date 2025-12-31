package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.GraphInfoEntity;
import com.eisoo.entity.LineageOpLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/13 13:23
 * @Version:1.0
 */
@Mapper
public interface  GraphInfoMapper extends BaseMapper<GraphInfoEntity> {
    void insertOrUpdate(GraphInfoEntity graphInfoEntity);
}
