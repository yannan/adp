package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.entity.LineageOpLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/26 14:29
 * @Version:1.0
 */
@Mapper
public interface LineageOpLogMapper extends BaseMapper<LineageOpLogEntity> {
    void truncateTable();
    void insertBatchSomeColumn(@Param("list") List<LineageOpLogEntity> entityList);

}
