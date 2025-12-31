package com.eisoo.metadatamanage.web.controller;

import java.util.List;

import javax.validation.constraints.NotBlank;

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
import org.springframework.web.bind.annotation.RestController;

import com.eisoo.metadatamanage.lib.dto.SchemaAlterDTO;
import com.eisoo.metadatamanage.lib.dto.SchemaCreateDTO;
import com.eisoo.metadatamanage.lib.vo.SchemaItemVo;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.metadatamanage.web.service.ISchemaService;
import com.eisoo.standardization.common.api.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "schema元数据管理")
@RestController
@RequestMapping("/v1/datasource/{dsId}/schema")
@Slf4j
@Validated
public class SchemaController {
    @Autowired
    ISchemaService schemaService;

    @ApiOperation(value = "01.schema-新增", notes = "新增schema表记录")
    @PostMapping
    public Result<?> create(@PathVariable("dsId") Long dsId, @Validated @RequestBody SchemaCreateDTO req) {
        schemaService.create(dsId, req.getName());
        return Result.success();
    }

    @ApiOperation(value = "02.schema-修改", notes = "修改schema表记录")
    @PutMapping(value = "/{id}")
    public Result<?> update(@PathVariable("dsId") Long dsId, @PathVariable("id") Long id, @Validated @RequestBody SchemaAlterDTO req) {
        schemaService.update(dsId, id, req.getDataSourceId(), req.getName());
        return Result.success();
    }

    @ApiOperation(value = "03.schema-删除", notes = "删除schema表记录")
    @DeleteMapping(value = "/{id}")
    public Result<?> delete(@PathVariable("dsId") Long dsId, @PathVariable("id") Long id) {
        schemaService.delete(dsId, id);
        return Result.success();
    }

    @ApiOperation(value = "04.schema-列表查询", notes = "查询指定数据源下的所有schema列表")
    @GetMapping
    public Result<List<SchemaItemVo>> getList(@PathVariable("dsId") Long dsId) {
        return schemaService.getList(dsId);
    }

    @ApiOperation(value = "04.schema-名称冲突检查", notes = "检查schema名称是否冲突")
    @GetMapping(value = "/nameConflictCheck")
    public Result<?> checkNameConflict(
        @PathVariable("dsId") Long dsId, 
        @RequestParam(value = "name", required = true) 
        @Length(max = 128, message = "name" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
        @NotBlank(message = "name" + Messages.MESSAGE_INPUT_NOT_EMPTY)
        String schemaName
    ) {
        return schemaService.checkNameConflict(dsId, schemaName);
    }
}
