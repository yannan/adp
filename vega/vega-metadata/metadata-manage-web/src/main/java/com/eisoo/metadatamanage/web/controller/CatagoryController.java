package com.eisoo.metadatamanage.web.controller;

import java.util.List;

import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eisoo.metadatamanage.lib.vo.CatagoryItemVo;
import com.eisoo.metadatamanage.web.service.ICatagoryService;
import com.eisoo.standardization.common.api.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "类目管理")
@RestController
@RequestMapping("/v1/catagory")
@Slf4j
public class CatagoryController {
    @Autowired
    ICatagoryService catagoryService;

    @ApiOperation(value = "01.类目-查询", notes = "查询完整类目")
    @GetMapping
    public Result<List<CatagoryItemVo>> get(@RequestParam(value = "include_deleted", required = false, defaultValue = "0")
                                                @Range(min = 0, max = 1, message = "isDeleted取值范围需满足[0,1]")
                                                        Integer includeDeleted) {
        return catagoryService.getList(includeDeleted);
    }
}
