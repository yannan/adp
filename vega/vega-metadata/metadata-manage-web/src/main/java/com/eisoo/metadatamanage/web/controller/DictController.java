package com.eisoo.metadatamanage.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eisoo.metadatamanage.lib.vo.DictItemVo;
import com.eisoo.metadatamanage.web.service.IDictService;
import com.eisoo.standardization.common.api.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "字典管理")
@RestController
@RequestMapping("/v1/dict")
@Slf4j
public class DictController {
    @Autowired
    private IDictService dictService;

    @ApiOperation(value = "01.字典-查询", notes = "根据字典类型查询")
    @GetMapping(value = "/{dictType}")
    public Result<List<DictItemVo>> getList(@PathVariable("dictType") Integer dictType) {
        return dictService.getListByDictType(dictType);
    }
}
