package com.eisoo.metadatamanage.lib.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelCollection;
import cn.afterturn.easypoi.handler.inter.IExcelModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class TableCreateDTO implements IExcelModel{
    @Excel(name = "表名称", orderNum = "1", needMerge = true, width = 25)
    @ApiModelProperty(value = "表名称", example = "t_test", dataType = "java.lang.String")
    @Length(max = 128, message = "表名称" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "表名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String name;

    @ApiModelProperty(value = "高级参数")
    @Valid
    private List<AdvancedDTO> advancedParams;

    @Excel(name = "表描述", orderNum = "2", needMerge = true, width = 40)
    @ApiModelProperty(value = "表描述", example = "", dataType = "java.lang.String")
    @Length(max = 2048, message = "表描述长度不能超过2048")
    private String description;

    @ExcelCollection(name = "表字段", orderNum = "3")
    @ApiModelProperty(value = "表字段")
    // @Valid
    private List<TableFieldDTO> fields;

    @JsonIgnore
    @Excel(name = "表错误描述", orderNum = "4", needMerge = true, width = 40)
	private String errorMsg;
}
