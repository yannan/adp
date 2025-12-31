package com.eisoo.metadatamanage.web.controller;

import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.eisoo.metadatamanage.lib.dto.DataSourceLiveUpdateStatusDto;
import com.eisoo.metadatamanage.lib.dto.FillMetaDataDTO;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.IDipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.dipDataSourceService.impl.DipDataSourceService;
import com.eisoo.metadatamanage.web.extra.service.virtualService.VirtualService;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.google.common.base.Strings;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.eisoo.metadatamanage.lib.dto.DataSourceDTO;
import com.eisoo.metadatamanage.lib.dto.DataSourceStatusDTO;
import com.eisoo.metadatamanage.lib.vo.DataSourceItemVo;
import com.eisoo.metadatamanage.lib.vo.DataSourceVo;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.metadatamanage.web.service.IDataSourceService;
import com.eisoo.metadatamanage.web.util.CheckErrorUtil;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.exception.AiShuException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "数据源元数据管理")
@RestController
@RequestMapping("/v1/datasource")
@Slf4j
@Validated
public class DataSourceController {
    @Autowired
    private IDataSourceService dataSourceService;
    @Autowired
    private IDipDataSourceService dipDataSourceService;

    @GetMapping(value = "/getConnectors")
    public ResponseEntity<?> connectorList(@RequestHeader(value = "Authorization", required = true) String token) {
        CheckErrorUtil.checkUnauthorized(token);
        VirtualService.TOKEN = token;
        return ResponseEntity.ok(dipDataSourceService.getConnectors());
    }
    @ApiOperation(value = "01.数据源-新增", notes = "新增数据源表记录")
    @PostMapping
    public Result<?> create(@Validated @RequestBody DataSourceDTO req) {
        int pwdLen = 0;
        try {
            pwdLen = new String(Base64.getDecoder().decode(req.getPassword()), "utf-8").length(); 
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("password", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
        }
        if (pwdLen > 128) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("password", Messages.MESSAGE_LENGTH_MAX_CHAR_128));
        }
        
        dataSourceService.create(req);
        return Result.success();
    }

    @ApiOperation(value = "02.数据源-修改", notes = "修改数据源表记录")
    @PutMapping(value = "/{id}")
    public Result<?> update(@PathVariable("id") Long id, @Validated @RequestBody DataSourceDTO req) {
        int pwdLen = 0;
        try {
            pwdLen = new String(Base64.getDecoder().decode(req.getPassword()), "utf-8").length(); 
        } catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("password", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
        }
        if (pwdLen > 128) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("password", Messages.MESSAGE_LENGTH_MAX_CHAR_128));
        }

        dataSourceService.update(id, req);
        return Result.success();
    }

    @ApiOperation(value = "03.数据源-批量启/停用", notes = "批量修改数据源表记录的启/停用状态")
    @PatchMapping(value = "/{ids}")
    public Result<?> batchAlterEnableStatus(@PathVariable("ids") String ids, @Validated @RequestBody DataSourceStatusDTO req) {
        List<Long> idList = null;
        try {
            idList = Arrays.stream(ids.split(",")).map(s->Long.parseLong(s.trim())).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("ids", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
        }
        dataSourceService.updateEnableStatus(req, idList);
        return Result.success();
    }

    @ApiOperation(value = "04.数据源-删除", notes = "批量删除数据源表记录")
    @DeleteMapping(value = "/{ids}")
    public Result<?> batchDelete(@PathVariable("ids") String ids) {
        List<Long> idList = null;
        try {
            idList = Arrays.stream(ids.split(",")).map(s->Long.parseLong(s.trim())).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, CheckErrorUtil.createError("ids", ErrorCodeEnum.InvalidParameter.getErrorMsg()));
        }
        return dataSourceService.delete(idList);
    }

    @ApiOperation(value = "05.数据源-详情", notes = "查询数据源详情")
    @GetMapping(value = "/{id}")
    public Result<DataSourceVo> getDetail(@PathVariable("id") Long id) {
        return dataSourceService.getDetail(id);
    }

    @ApiOperation(value = "06.数据源-列表查询", notes = "查询数据源列表")
    @GetMapping
    public Result<List<DataSourceItemVo>> getList(
        @RequestParam(value = "enable_status", required = false) 
        @Range(min = 1, max = 2, message = "enable_status取值范围需满足[1,2]") 
        Integer enableStatus,
        @RequestParam(value = "connect_status", required = false) 
        @Range(min = 1, max = 2, message = "connect_status取值范围需满足[1,2]") 
        Integer connectStatus,
        @RequestParam(value = "include_deleted", required = false, defaultValue = "0")
        @Range(min = 0, max = 1, message = "isDeleted取值范围需满足[0,1]")
                Integer includeDeleted,
        @RequestParam(value = "data_source_type", required = false) 
        @Range(min = 1, max = 3, message = "data_source_type取值范围需满足[1,3]") 
        Integer dataSourceType,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "offset", required = false, defaultValue = "1") Integer offset,
        @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
        @RequestParam(value = "sort", required = false, defaultValue = "create_time") String sort,
        @RequestParam(value = "direction", required = false, defaultValue = "desc") String direction
    ) {
        if (offset == null || offset < 1) {
            offset = 1;
        }

        if (limit == null || limit < 20) {
            limit = 20;
        } else if (limit > 1000) {
            limit = 1000;
        }

        if (!Strings.isNullOrEmpty(keyword)) {
            keyword = keyword.replaceAll("_", "\\\\_");
            keyword = keyword.replaceAll("%", "\\\\%");
        }
        
        return dataSourceService.getList(enableStatus, connectStatus, includeDeleted, dataSourceType, keyword, offset, limit, sort, direction);
    }

    @ApiOperation(value = "07.数据源-名称冲突检查", notes = "检查数据源名称是否冲突")
    @GetMapping(value = "/nameConflictCheck")
    public Result<?> checkNameConflict(
        @RequestParam(value = "data_source_type", required = true)
        @Range(min = 1, max = 3, message = "data_source_type取值范围需满足[1,3]") 
        @NotNull(message = "data_source_type" + Messages.MESSAGE_INPUT_NOT_EMPTY)
        Integer dataSourceType,
        @RequestParam(value = "name", required = true)
        @Pattern(regexp = Constants.REGEX_ENGLISH_CHINESE_UNDERLINE_BAR_128, message = "名称" + Messages.MESSAGE_CHINESE_NUMBER_UNDERLINE_BAR_128) 
        @NotBlank(message = "名称" + Messages.MESSAGE_INPUT_NOT_EMPTY) 
        String dataSourceName 
    ) {
        return dataSourceService.checkNameConflict(dataSourceType, dataSourceName);
    }

    @ApiOperation(value  = "08.数据源-通过数据源ID采集元数据（已废弃，请使用task模块同名接口）")
    @PostMapping(value = "/fillMetaData/{dsid}")
    public Result<?> fillMetaData( @PathVariable("dsid") Long dsid){
        return dataSourceService.fillMetaData(dsid);
    }

    @ApiOperation(value  = "09.数据源-通过数据源名称采集元数据（已废弃，请使用task模块同名接口）")
    @PostMapping(value = "/fillMetaData")
    public Result<?> fillMetaData(FillMetaDataDTO fillMetaDataDTO){
        return dataSourceService.fillMetaData(fillMetaDataDTO);
    }

    @ApiOperation(value  = "10.数据源-实时采集状态切换接口")
    @PostMapping(value = "/liveUpdateStatus")
    public Result<?> fillMetaData(@RequestBody DataSourceLiveUpdateStatusDto liveUpdateStatusDto){
        return Result.success(dataSourceService.setliveUpdateStatus(liveUpdateStatusDto));
    }

}
