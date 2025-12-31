package com.eisoo.metadatamanage.sdk;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.eisoo.metadatamanage.lib.vo.TableVo;
import com.eisoo.metadatamanage.lib.vo.TableItemVo;
import com.eisoo.standardization.common.api.Result;

@FeignClient(name="tableService",url = "${feign.metadataservice}")
public interface ITableService {
    @GetMapping(value = "/api/metadata-manage/v1/table")
    Result<List<TableItemVo>> getList(
        @RequestParam(value = "data_source_type", required = false) Integer dataSourceType,
        @RequestParam(value = "data_source_id", required = false) Long dsId,
        @RequestParam(value = "schema_id", required = false) Long schemaId,
        @RequestParam(value = "ids", required = false) String ids,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
        @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
        @RequestParam(value = "sort", required = false) String sort,
        @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction
    );

    @GetMapping(value = "/api/metadata-manage/v1/datasource/{dsId}/schema/{schemaId}/table/{tableId}")
    Result<TableVo> getDetail(
        @PathVariable("dsId") Long dsId, 
        @PathVariable("schemaId") Long schemaId, 
        @PathVariable("tableId") Long tableId
    );
}
