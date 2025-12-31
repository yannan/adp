package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.util.constant.Messages;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/5/12 9:17
 */
@Data
@ApiModel
public class FillMetaDataDTO {
    @ApiModelProperty(value = "数据源名称", example = "dsName", dataType = "java.lang.String")
    @Length(max = 128, message = "数据源名称" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "数据源名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String name;

    @ApiModelProperty(value = "创建用户", example = "createUser", dataType = "java.lang.String")
    @Length(max = 100, message = "创建用户" + Messages.MESSAGE_LENGTH_MAX_CHAR_100)
    private String createUser;

    @ApiModelProperty(value = "信息系統ID", example = "createUser", dataType = "java.lang.String")
    @Length(max = 128, message = "信息系統ID" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    private String infoSystemId;

    public String getCreateUser(){
        return StringUtils.isEmpty(this.createUser)?"":this.createUser;
    }
}
