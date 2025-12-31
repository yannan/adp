package com.eisoo.metadatamanage.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.eisoo.metadatamanage.lib.dto.TableFieldSearchDto;
import com.eisoo.metadatamanage.lib.vo.TableWithColumnVo;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eisoo.metadatamanage.lib.dto.TableAlterDTO;
import com.eisoo.metadatamanage.lib.dto.TableCreateDTO;
import com.eisoo.metadatamanage.lib.vo.TableItemVo;
import com.eisoo.metadatamanage.lib.vo.TableVo;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.metadatamanage.web.service.ITableService;
import com.eisoo.metadatamanage.web.util.CheckErrorUtil;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;
import com.google.common.base.Strings;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "table元数据管理")
@RestController
@RequestMapping("/v1")
@Slf4j
@Validated
public class TableController {
    @Autowired
    ITableService tableService;

    @ApiOperation(value = "01.table元数据-新建", notes = "新建table元数据")
    @PostMapping(value = "/datasource/{dsId}/schema/{schemaId}/table")
    public Result<?> add(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @Validated @RequestBody TableCreateDTO req
    ) {
        tableService.add(dsId, schemaId, req);
        return Result.success();
    }

    @ApiOperation(value = "02.table元数据-修改", notes = "修改table元数据")
    @PutMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/{tableId}")
    public Result<?> update(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @PathVariable("tableId") Long tableId,
            @Validated @RequestBody TableAlterDTO req
    ) {
        tableService.update(dsId, schemaId, tableId, req);
        return Result.success();
    }

    @ApiOperation(value = "03.table元数据-删除", notes = "删除table元数据")
    @DeleteMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/{tableId}")
    public Result<?> delete(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @PathVariable("tableId") Long tableId
    ) {
        tableService.delete(dsId, schemaId, tableId);
        return Result.success();
    }
    @DeleteMapping(value = "/datasource/table/{tableId}")
    public Result<?> delete(@PathVariable("tableId") Long tableId) {
        tableService.delete(tableId);
        return Result.success();
    }
    @ApiOperation(value = "04.table元数据-导入", notes = "批量导入table元数据")
    @PostMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/import")
    public void batchImport(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @RequestPart MultipartFile file,
            HttpServletResponse response
    ) throws IOException {
        tableService.importExcel(dsId, schemaId, file, response);
    }

    @ApiOperation(value = "05.table元数据-导出", notes = "导出指定table元数据")
    @GetMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/{tableId}/export")
    public void export(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @PathVariable("tableId") Long tableId,
            HttpServletResponse response
    ) {
        tableService.exportExcel(dsId, schemaId, tableId, response);
    }

    @ApiOperation(value = "06.table元数据-列表查询", notes = "查询table元数据列表")
    @GetMapping(value = "/table")
    public Result<List<TableItemVo>> getList(
            @RequestParam(value = "data_source_type", required = false)
//        @Range(min = 1, max = 3, message = "data_source_type取值范围需满足[1,3]")
            Integer dataSourceType,
            @RequestParam(value = "data_source_id", required = false)
//            @Range(min = 1, message = "data_source_id需要大于等于1")
            String dsId,
            @RequestParam(value = "schema_id", required = false)
//            @Range(min = 1, message = "schema_id需要大于等于1")
            Long schemaId,
            @RequestParam(value = "ids", required = false) String ids,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "sort", required = false, defaultValue = "create_time") String sort,
            @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
            @RequestParam(value = "checkField", required = false, defaultValue = "false") Boolean checkField
    ) {
        if (AiShuUtil.isNotEmpty(dsId)) {
            CheckErrorUtil.checkPositiveLong(dsId, "dsId");
        }
        if (AiShuUtil.isNotEmpty(schemaId)) {
            CheckErrorUtil.checkPositiveLong(schemaId, "schemaId");
        }
        List<String> sortList = new ArrayList<>();
        sortList.add(Constants.PARAMETER_SORT_UPDATE_TIME);
        sortList.add(Constants.PARAMETER_SORT_CREATE_TIME);
        List<String> directionList = new ArrayList<>();
        directionList.add(Constants.PARAMETER_DIRECTION_ASC);
        directionList.add(Constants.PARAMETER_DIRECTION_DESC);
        CheckErrorUtil.checkSelectListParameter(offset, limit, sort, sortList, direction, directionList);

        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = keyword.replaceAll("_", "\\\\_");
            keyword = keyword.replaceAll("%", "\\\\%");
        }

        List<Long> idList = null;
        if (!Strings.isNullOrEmpty(ids)) {
            try {
                idList = Arrays.stream(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
            } catch (NumberFormatException e) {
                throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("ids", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
            }
        }

        return tableService.getList(dataSourceType, dsId, schemaId, idList, keyword, offset, limit, sort, direction, checkField);
    }

    @ApiOperation(value = "07.table元数据-详情", notes = "查询指定table元数据详情")
    @GetMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/{tableId}")
    public Result<TableVo> getDetail(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @PathVariable("tableId") Long tableId
    ) {
        return tableService.getDetail(dsId, schemaId, tableId);
    }

    @ApiOperation(value = "08.table元数据-名称冲突检查", notes = "检查table名称是否冲突")
    @GetMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/nameConflictCheck")
    public Result<?> checkNameConflict(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            @RequestParam(value = "name", required = true)
            @Length(max = 128, message = "name" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
            @NotBlank(message = "name" + Messages.MESSAGE_INPUT_NOT_EMPTY)
            String tableName
    ) {
        return tableService.checkNameConflict(dsId, schemaId, tableName);
    }

    @ApiOperation(value = "09.导入模板获取", notes = "导出与指定数据源类型匹配的导入模板")
    @GetMapping(value = "/datasource/{dsId}/schema/{schemaId}/table/template")
    public void exportTemplate(
            @PathVariable("dsId") String dsId,
            @PathVariable("schemaId") Long schemaId,
            HttpServletResponse response
    ) {
        tableService.exportExcelTemplate(dsId, schemaId, response);
    }


    @ApiOperation(value = "10.表字段列表查询", notes = "根据数据源ID，schema，table查询字段列表信息")
    @PostMapping(value = "/datasource/table/list")
    public Result<?> queryTableFields(@Valid @RequestBody List<TableFieldSearchDto> queryList) {
        return tableService.queryTableFields(queryList);
    }

    @ApiOperation(value = "11.table及column元数据-列表查询", notes = "查询table元数据列表与column")
    @GetMapping(value = "/table_and_column")
    public Result<List<TableWithColumnVo>> getListWithColumn(
            @RequestParam(value = "data_source_type", required = false)
//        @Range(min = 1, max = 3, message = "data_source_type取值范围需满足[1,3]")
                    Integer dataSourceType,
            @RequestParam(value = "data_source_id", required = false)
//            @Range(min = 1, message = "data_source_id需要大于等于1")
                    String dsId,
            @RequestParam(value = "schema_id", required = false)
//            @Range(min = 1, message = "schema_id需要大于等于1")
                    Long schemaId,
            @RequestParam(value = "ids", required = false) String ids,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "sort", required = false, defaultValue = "create_time") String sort,
            @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction,
            @RequestParam(value = "checkField", required = false, defaultValue = "false") Boolean checkField
    ) {
//        if (AiShuUtil.isNotEmpty(dsId)) {
//            CheckErrorUtil.checkPositiveLong(dsId, "dsId");
//        }
        if (AiShuUtil.isNotEmpty(schemaId)) {
            CheckErrorUtil.checkPositiveLong(schemaId, "schemaId");
        }
        List<String> sortList = new ArrayList<>();
        sortList.add(Constants.PARAMETER_SORT_UPDATE_TIME);
        sortList.add(Constants.PARAMETER_SORT_CREATE_TIME);
        List<String> directionList = new ArrayList<>();
        directionList.add(Constants.PARAMETER_DIRECTION_ASC);
        directionList.add(Constants.PARAMETER_DIRECTION_DESC);
        CheckErrorUtil.checkSelectListParameter(offset, limit, sort, sortList, direction, directionList);

        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = keyword.replaceAll("_", "\\\\_");
            keyword = keyword.replaceAll("%", "\\\\%");
        }

        List<Long> idList = null;
        if (!Strings.isNullOrEmpty(ids)) {
            try {
                idList = Arrays.stream(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
            } catch (NumberFormatException e) {
                throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("ids", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
            }
        }

        if (AiShuUtil.isNotEmpty(idList)) {
            for (Long id : idList) {
                CheckErrorUtil.checkPositiveLong(id, "ids");
            }
        }

        return tableService.getListWithColumn(dataSourceType, dsId, schemaId, idList, keyword, offset, limit, sort, direction, checkField);
    }

//    @ApiOperation(value = "10.表数据量更新", notes = "计算count(*),更新单表数据量")
//    @GetMapping(value = "/updateRowNum/{tableId}")
//    public Result<?>  updateRowNum( @PathVariable("tableId") Long tableId){
//        return tableService.updateRowNum(tableId);
//    }
}
