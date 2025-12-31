package com.eisoo.metadatamanage.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eisoo.metadatamanage.db.entity.TaskScanEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author Tian.lan
 */
public interface TaskScanMapper extends BaseMapper<TaskScanEntity> {
    @Select("SELECT count(*) FROM t_task_scan WHERE ds_id = #{dsId} and scan_status=1 and type=0")
    int getRunningDs(@Param("dsId") String dsId);

    @Update("UPDATE  t_task_scan SET scan_status=#{status} WHERE id = #{id}")
    int updateScanStatusById(@Param("id") String id, @Param("status") int status);
}
