package com.eisoo.metadatamanage.db.mapper;

import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.github.yulichang.base.MPJBaseMapper;

public interface SchemaMapper extends MPJBaseMapper<SchemaEntity> {
    int deleteById(Long dsId, Long schemaId);
}
