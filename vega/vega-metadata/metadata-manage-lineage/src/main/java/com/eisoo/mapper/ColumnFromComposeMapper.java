package com.eisoo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:57
 * @Version:1.0
 */
@Mapper
public interface ColumnFromComposeMapper<D> extends BaseMapper {
    void insertBatchSomeColumn();
}
