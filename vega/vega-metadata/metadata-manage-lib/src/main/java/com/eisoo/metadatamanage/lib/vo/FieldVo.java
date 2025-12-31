package com.eisoo.metadatamanage.lib.vo;


import cn.afterturn.easypoi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.vo
 * @Date: 2023/4/7 10:03
 */
@Data
public class FieldVo  implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 唯一id，雪花算法
     */
//    private Long id;

    /**
     * 字段名称
     */
    @Excel(name = "*字段名称", orderNum = "1", width = 30)
    @ApiModelProperty(value = "字段名称", example = "field1", dataType = "java.lang.String")
    @Length(max = 128, message = Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "字段名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private String fieldName;

    /**
     * 字段类型，关联字典表f_dict_type为2|3|4...时的f_dict_key
     */
    @Excel(name = "*字段类型", orderNum = "2", width = 20 , dict = "fieldType", addressList = true)
    @ApiModelProperty(value = "字段类型", example = "1", dataType = "java.lang.Integer")
    @NotNull(message = "字段类型" + Messages.MESSAGE_INPUT_NOT_EMPTY)
    private Integer fieldType;

    /**
     * 字段长度
     */
    @Excel(name = "字段长度", orderNum = "3")
    @ApiModelProperty(value = "字段长度", example = "10", dataType = "java.lang.Integer")
    @Range(min = 1, message = "字段长度值只能为空或大于等于1")
    private Integer fieldLength;

    /**
     * 字段精度
     */
    @Excel(name = "字段精度", orderNum = "4")
    @ApiModelProperty(value = "字段精度", example = "0", dataType = "java.lang.Integer")
    @Range(min = 0, message = "字段精度值只能为空或大于等于0")
    private Integer fieldPrecision;

    /**
     * 字段注释，默认为空字符串
     */
    @Excel(name = "*字段注释", orderNum = "5")
    @ApiModelProperty(value = "字段注释", example = "", dataType = "java.lang.String")
    @Length(max = 1024, message = "字段注释长度不能超过1024")
    private String fieldComment;

    /**
     * Table唯一标识
     */
    @JsonIgnore
    private Long tableId;

    /**
     * Table版本号
     */
    @JsonIgnore
    private Integer version;

    /**
     * 高级参数，默认为"[]"，格式为"[{key:key1, value:value1}]"
     */
    private String advancedParams;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String fieldTypeName;

    /**
     * 更新标识
     */
    private Boolean updateFlag;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateAt;

    /**
     * 删除标识
     */
    private Boolean deleteFlag;

}
