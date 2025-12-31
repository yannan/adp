package com.eisoo.metadatamanage.lib.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel
public class TableWithColumnVo extends TableItemVo implements Serializable  {
    private List<FieldVo> fields;
}
