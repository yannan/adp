package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.IndicatorEntity;
import com.eisoo.metadatamanage.lib.dto.PartitionDTO;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.db.mapper
 * @Date: 2023/5/10 16:00
 */
public interface IndicatorMapper extends MPJBaseMapper<IndicatorEntity> {
    @Select("SELECT partition_name as partitionName,partition_description as partitionDescription FROM information_schema.partitions WHERE TABLE_NAME = #{table_name}")
    List<PartitionDTO> getPartitions(@Param("table_name")String tableName);

    @Update("ALTER TABLE t_indicator DROP PARTITION ${table_name}")
    void dropPartition(@Param("table_name")String tableName);

    @Update("ALTER TABLE t_indicator ADD PARTITION (PARTITION ${partition_name} VALUES LESS THAN (${partitionDate}))")
    void addCurrentPartition(@Param("partition_name")String partitionName, @Param("partitionDate")String partitionDate);

    @Update("ALTER TABLE t_indicator ADD PARTITION (PARTITION p_max VALUES LESS THAN (MAXVALUE))")
    void addMaxPartition();
}
