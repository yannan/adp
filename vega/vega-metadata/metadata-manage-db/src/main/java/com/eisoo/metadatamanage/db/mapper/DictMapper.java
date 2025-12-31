package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.DictEntity;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface DictMapper extends MPJBaseMapper<DictEntity> {
    @Select("SELECT f_dict_key FROM t_dict WHERE  f_dict_type=1 and LOWER(f_dict_value) = #{dictValue}")
    Integer getDictKey(String dictValue);
}
