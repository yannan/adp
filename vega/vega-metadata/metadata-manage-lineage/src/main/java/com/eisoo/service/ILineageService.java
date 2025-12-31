package com.eisoo.service;

import com.eisoo.entity.BaseLineageEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/13 13:08
 * @Version:1.0
 */
public interface ILineageService  {
    Integer insertBatchEntityOrUpdate(@Param("list") List<? extends BaseLineageEntity> entityList);
    Integer deleteBatchEntity(@Param("list") List<? extends BaseLineageEntity> entityList);
    BaseLineageEntity selectById(String id);
    List<? extends BaseLineageEntity> selectBatchIds( List<String> ids);
    void deleteBatchEntityAndRelation(@Param("list") List<? extends BaseLineageEntity> entityList);
    void insertBatchEntityAndRelation(@Param("list") List<? extends BaseLineageEntity> entityList) throws Exception;
    void updateBatchEntityAndRelation(@Param("list") List<? extends BaseLineageEntity> entityList) throws Exception;
}
