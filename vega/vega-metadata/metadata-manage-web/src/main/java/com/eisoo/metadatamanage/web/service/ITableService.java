package com.eisoo.metadatamanage.web.service;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eisoo.metadatamanage.lib.dto.AdvancedDTO;
import com.eisoo.metadatamanage.lib.dto.TableFieldSearchDto;
import com.eisoo.metadatamanage.lib.vo.TableWithColumnVo;
import org.springframework.web.multipart.MultipartFile;

import com.eisoo.metadatamanage.db.entity.TableEntity;
import com.eisoo.metadatamanage.lib.dto.TableAlterDTO;
import com.eisoo.metadatamanage.lib.dto.TableCreateDTO;
import com.eisoo.metadatamanage.lib.vo.TableVo;
import com.eisoo.metadatamanage.lib.vo.TableItemVo;
import com.eisoo.standardization.common.api.Result;

public interface ITableService extends IService<TableEntity> {
    boolean isSchemaUsed(Long schemaId);
    Result<List<TableItemVo>> getList(Integer dataSourceType, String dsId, Long schemaId, List<Long> idList, String keyword, Integer offset, Integer limit, String sort, String direction, Boolean checkField);
    Result<List<TableWithColumnVo>> getListWithColumn(Integer dataSourceType, String dsId, Long schemaId, List<Long> idList, String keyword, Integer offset, Integer limit, String sort, String direction, Boolean checkField);
    void delete(String dsId, Long schemaId, Long tableId);
    TableEntity getOne(String dsId, Long schemaId, String tableName);
    Result<TableVo> getDetail(String dsId, Long schemaId, Long tableId);
    void add(String dsId, Long schemaId, TableCreateDTO params);
    void update(String dsId, Long schemaId, Long tableId, TableAlterDTO params);
    void importExcel(String dsId, Long schemaId, MultipartFile file, HttpServletResponse response);
    void exportExcel(String dsId, Long schemaId, Long tableId, HttpServletResponse response);
    void exportExcelTemplate(String dsId, Long schemaId, HttpServletResponse response);
    Result<?> checkNameConflict(String dsId, Long schemaId, String tableName);
    AdvancedDTO getAdvancedParamByKey(TableEntity table, String key);
    String isPrimaryKey(String fieldName, TableEntity table);
    Result<?> queryTableFields(List<TableFieldSearchDto> queryList);

    void delete(Long tableId);

//    Result<?> updateRowNum(Long tableId);
}
