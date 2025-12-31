package com.eisoo.metadatamanage.web.service;

import java.util.List;

import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.metadatamanage.lib.vo.SchemaCatagoryItemVo;
import com.eisoo.metadatamanage.lib.vo.SchemaItemVo;
import com.eisoo.standardization.common.api.Result;
import com.github.yulichang.base.MPJBaseService;

public interface ISchemaService extends MPJBaseService<SchemaEntity> {
    List<SchemaCatagoryItemVo> getListForCatagory();
    Result<List<SchemaItemVo>> getList(Long dsId);
    void delete(Long dsId, Long schemaId);
    void update(Long dsId, Long schemaId, Long targetDSId, String schemaName);
    void create(Long dsId, String schemaName);
    List<Long> getUsedDataSource(Long... dsIds);
    Result<?> checkNameConflict(Long dsId, String schemaName);
}
