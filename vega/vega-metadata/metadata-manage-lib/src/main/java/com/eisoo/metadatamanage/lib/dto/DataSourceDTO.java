package com.eisoo.metadatamanage.lib.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.Length;

import com.eisoo.metadatamanage.util.constant.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class DataSourceDTO {
    @ApiModelProperty(value = "名称", example = "Oracle@1ocalhost", dataType = "java.lang.String")
    @Pattern(regexp = Constants.REGEX_ENGLISH_CHINESE_UNDERLINE_BAR_128, message = "名称" + Messages.MESSAGE_CHINESE_NUMBER_UNDERLINE_BAR_128)
    @NotBlank(message = "名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String name;

    @ApiModelProperty(value = "类型", example = "3306", dataType = "java.lang.Integer")
//    @Range(min = 1, max = 7, message = "类型取值范围需满足[1,7]")
    @NotNull(message = "类型" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private int dataSourceType;

    @ApiModelProperty(value = "Host", example = "localhost", dataType = "java.lang.String")
    @Length(max = 128, message = "Host" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "Host" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String host;

    @ApiModelProperty(value = "端口", example = "3306", dataType = "java.lang.Integer")
    @Range(min = 1, max = 65535, message = "端口取值范围需满足[1,65535]")
    @NotNull(message = "端口" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private Integer port;

    @ApiModelProperty(value = "用户名", example = "root", dataType = "java.lang.String")
    @Length(max = 128, message = "用户名" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "用户名" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String userName;

    @ApiModelProperty(value = "密码", example = "YmFzZTY057yW56CB", dataType = "java.lang.String")
    @Pattern(regexp = Constants.REGEX_BASE64, message = "密码" + Messages.MESSAGE_NOT_BASE64_ENCODE)
    @NotBlank(message = "密码" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String password;

    @ApiModelProperty(value = "描述", example = "", dataType = "java.lang.String")
    @Length(max = 200, message = "描述" + Messages.MESSAGE_LENGTH_MAX_CAHR_200)
    private String description;

    @ApiModelProperty(value = "扩展属性", example = "", dataType = "java.lang.String")
    @Length(max = 255, message = "扩展属性" + Messages.MESSAGE_LENGTH_MAX_CHAR_255)
    private String extendProperty;

    @ApiModelProperty(value = "数据库名称", example = "", dataType = "java.lang.String")
    @Length(max = 100, message = "数据库名称" + Messages.MESSAGE_LENGTH_MAX_CHAR_100)
    @NotBlank(message = "数据库名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String databaseName;

    @ApiModelProperty(value = "信息系统id", example = "", dataType = "java.lang.String")
    @Length(max = 128, message = "信息系统id" + Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    private String infoSystemId;
}
